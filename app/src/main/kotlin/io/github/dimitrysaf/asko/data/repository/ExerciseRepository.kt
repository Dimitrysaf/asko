package io.github.dimitrysaf.asko.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import io.github.dimitrysaf.asko.data.catalog.ExerciseCatalog
import io.github.dimitrysaf.asko.data.model.DownloadProgress
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Workout
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.data.settings.UserLibrary
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import java.io.IOException

/**
 * The exercise libraries together. An exercise in more than one library (the same name) is the
 * same exercise: it shows as the first library's version (Settings sets the order), wherever it
 * is, so routines and workouts take that version's pictures and info. [lookup] gives that for
 * every id; [browsable] is what Discovery and the exercise picker show: hidden libraries left out,
 * each exercise once.
 */
class ExerciseRepository(
    /** The libraries Asko comes with, each saved on the device. */
    private val catalogs: Map<ExerciseSource, ExerciseCatalog>,
    /** A library you added: where it's saved and how it's read (made once per library). */
    private val linkCatalog: (UserLibrary) -> ExerciseCatalog,
    private val settings: SettingsRepository,
    /** Your exercises (the CUSTOM library); null until read. */
    private val custom: StateFlow<List<Exercise>?>,
) {
    /** How far ExerciseDB's first download (the long one) is while it runs; null otherwise. */
    val downloadProgress: StateFlow<DownloadProgress?> = catalogs.getValue(ExerciseSource.EXERCISEDB).progress

    /**
     * Every exercise from every library, by name. A library that can't be read is left out;
     * throws [IOException] only when none can.
     */
    suspend fun all(): List<Exercise> = library().all

    /** What's browsed and searched, following the order and hiding set in Settings. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val browsable: Flow<List<Exercise>> =
        combine(settings.sourceOrder, settings.hiddenSources, settings.hideDuplicates, custom) { order, hidden, once, _ ->
            order.filterNot { it in hidden } to once
        }.mapLatest { (shown, once) -> library().let { withContext(Dispatchers.Default) { it.merged(shown, once) } } }

    /**
     * Every exercise's id (from any library) → the exercise to show for it: the same exercise
     * from the first shown library that has it (or the first of all, when none shown has it).
     */
    suspend fun lookup(): Map<String, Exercise> {
        val library = library()
        return withContext(Dispatchers.Default) { library.resolved(settings.sourceOrder.value, settings.hiddenSources.value) }
    }

    /** [lookup], following changes to the order and hiding in Settings. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val lookups: Flow<Map<String, Exercise>> =
        combine(settings.sourceOrder, settings.hiddenSources, custom) { order, hidden, _ -> order to hidden }
            .mapLatest { (order, hidden) -> library().let { withContext(Dispatchers.Default) { it.resolved(order, hidden) } } }

    /** [lookup] as already worked out (instantly, for a page's first frame); null when it isn't yet. */
    fun lookupNow(): Map<String, Exercise>? = cached?.resolvedIfReady(settings.sourceOrder.value, settings.hiddenSources.value)

    /** [browsable] as already worked out (instantly, for a page's first frame); null when it isn't yet. */
    fun browsableNow(): List<Exercise>? =
        cached?.mergedIfReady(settings.sourceOrder.value.filterNot { it in settings.hiddenSources.value }, settings.hideDuplicates.value)

    /** The exercise to show for [id] (see [lookup]). */
    suspend fun byId(id: String): Exercise? = lookup()[id]

    /** The ids of [id]'s exercise in every library (itself included), e.g. to share its notes. */
    suspend fun sameExerciseIds(id: String): List<String> {
        val library = try {
            library()
        } catch (e: IOException) {
            return listOf(id)
        }
        return library.sameIds(id)
    }

    /** Similar exercises from what's browsable (the exercise itself may come from a hidden library). */
    suspend fun similarTo(exercise: Exercise, limit: Int = SIMILAR_LIMIT): List<Exercise> {
        val shown = settings.sourceOrder.value.filterNot { it in settings.hiddenSources.value }
        val library = library()
        return withContext(Dispatchers.Default) { library.merged(shown, settings.hideDuplicates.value).similarTo(exercise, limit) }
    }

    private val mutex = Mutex()

    @Volatile
    private var cached: Library? = null

    /**
     * The libraries, merged and indexed once (off the main thread) and kept until a library is
     * downloaded again: many screens ask at once, and each would otherwise sort and group them all.
     */
    private suspend fun library(): Library {
        val each = loadEach()
        return mutex.withLock {
            cached?.takeIf { it.isFrom(each) } ?: withContext(Dispatchers.Default) { Library(each) }.also { cached = it }
        }
    }

    private val linkCatalogs = ConcurrentHashMap<String, ExerciseCatalog>()

    private fun catalogOf(library: UserLibrary): ExerciseCatalog = linkCatalogs.getOrPut(library.id) { linkCatalog(library) }

    /**
     * Adds a library from a link to its JSON or CSV file (see LinkedLibraryApi), called [name]:
     * downloads it first, so a link that doesn't work isn't added. Throws [IOException] then.
     */
    suspend fun addLibrary(name: String, url: String) {
        val library = UserLibrary(UUID.randomUUID().toString().take(8), name.trim(), url.trim())
        val catalog = catalogOf(library)
        try {
            catalog.load()
        } catch (e: IOException) {
            linkCatalogs.remove(library.id)
            catalog.delete()
            throw e
        }
        settings.addUserLibrary(library)
    }

    /** Removes a library you added, and its downloaded copy. */
    suspend fun removeLibrary(library: UserLibrary) {
        settings.removeUserLibrary(library)
        linkCatalogs.remove(library.id)?.delete() ?: linkCatalog(library).delete()
    }

    /** Each library that could be read; throws the first failure when none could. */
    private suspend fun loadEach(): Map<ExerciseSource, List<Exercise>> = coroutineScope {
        val hidden = settings.hiddenSources.value
        // A hidden library never downloaded (wger, off until you switch it on) isn't fetched
        // just to be hidden: nothing of it can be in your routines yet.
        val every = (catalogs + settings.userLibraries.value.associate { it.source to catalogOf(it) })
            .filter { (source, catalog) -> source !in hidden || catalog.isSaved }
        val results = every.mapValues { (_, catalog) -> async { runCatching { catalog.load() } } }
            .mapValues { it.value.await() }
        // Your exercises are always there (once read); they don't make a library-less phone work.
        val loaded = results.filterValues { it.isSuccess }.mapValues { it.value.getOrThrow() } +
            (ExerciseSource.CUSTOM to custom.filterNotNull().first())
        if (loaded.keys == setOf(ExerciseSource.CUSTOM)) {
            throw results.values.firstNotNullOfOrNull { it.exceptionOrNull() as? IOException } ?: IOException("No exercise library")
        }
        loaded
    }

    private companion object {
        const val SIMILAR_LIMIT = 6
    }
}

/**
 * The libraries, indexed: every exercise by name, each id's name key (the same exercise in
 * another library has the same key), and the results for an order and hiding, worked out once.
 */
private class Library(private val libraries: Map<ExerciseSource, List<Exercise>>) {
    val all: List<Exercise> = libraries.values.flatten().sortedBy { it.name.lowercase() }
    private val keyOf: Map<String, String> = all.associate { it.id to it.name.sameNameKey() }
    private val byKey: Map<String, List<Exercise>> = all.groupBy { keyOf.getValue(it.id) }

    /** It was built from these very lists (the catalogs keep theirs until they download again). */
    fun isFrom(each: Map<ExerciseSource, List<Exercise>>): Boolean =
        each.keys == libraries.keys && each.all { (source, list) -> libraries[source] === list }

    fun sameIds(id: String): List<String> {
        val key = keyOf[id] ?: return listOf(id)
        return (byKey[key].orEmpty().map { it.id } + id).distinct()
    }

    private val mergedCache = HashMap<Pair<List<ExerciseSource>, Boolean>, List<Exercise>>()

    @Synchronized
    fun mergedIfReady(shown: List<ExerciseSource>, hideDuplicates: Boolean): List<Exercise>? = mergedCache[shown to hideDuplicates]

    @Synchronized
    fun resolvedIfReady(order: List<ExerciseSource>, hidden: Set<ExerciseSource>): Map<String, Exercise>? = resolvedCache[order to hidden]

    private val resolvedCache = HashMap<Pair<List<ExerciseSource>, Set<ExerciseSource>>, Map<String, Exercise>>()

    /**
     * The [shown] libraries' exercises in one list, by name. With [hideDuplicates], an exercise
     * in more than one comes once, from the first of them; without, each library's copy is there.
     */
    @Synchronized
    fun merged(shown: List<ExerciseSource>, hideDuplicates: Boolean): List<Exercise> = mergedCache.getOrPut(shown to hideDuplicates) {
        val rank = shown.withIndex().associate { (i, source) -> source to i }
        val visible = all.filter { it.source in rank && !it.retired }
        if (!hideDuplicates) return@getOrPut visible
        visible.groupBy { keyOf.getValue(it.id) }
            .values.map { same -> same.minBy { rank.getValue(it.source) } }
            .sortedBy { it.name.lowercase() }
    }

    /** Every id → the version of its exercise to show: from the first shown library, else the first of all. */
    @Synchronized
    fun resolved(order: List<ExerciseSource>, hidden: Set<ExerciseSource>): Map<String, Exercise> =
        resolvedCache.getOrPut(order to hidden) {
            val preference = order.filterNot { it in hidden } + order.filter { it in hidden }
            val rank = { e: Exercise -> preference.indexOf(e.source).let { if (it < 0) Int.MAX_VALUE else it } }
            val lookup = HashMap<String, Exercise>(all.size * 2)
            byKey.values.forEach { same ->
                val shown = same.minBy(rank)
                same.forEach { lookup[it.id] = shown }
            }
            lookup
        }
}

/**
 * Exercises that train at least one of [exercise]'s target muscles, best first: more shared
 * target muscles, then more shared secondary muscles, then different equipment (an alternative
 * when the machine is taken). Ties keep the library's alphabetical order.
 */
fun List<Exercise>.similarTo(exercise: Exercise, limit: Int): List<Exercise> {
    val targets = exercise.primaryMuscles.toSet()
    val secondary = exercise.secondaryMuscles.toSet()
    val equipment = exercise.equipment.toSet()
    return asSequence()
        .filter { it.id != exercise.id }
        .map { candidate ->
            val sharedTargets = candidate.primaryMuscles.count { it in targets }
            val score = sharedTargets * 10 +
                candidate.secondaryMuscles.count { it in secondary } +
                (if (candidate.equipment.none { it in equipment }) 1 else 0)
            candidate to (if (sharedTargets == 0) 0 else score)
        }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(limit)
        .map { it.first }
        .toList()
}

/**
 * Workouts with each exercise under the id of the version shown for it ([lookup]), so an
 * exercise's versions in each library count as one exercise in stats and records.
 */
fun List<Workout>.asShown(lookup: Map<String, Exercise>): List<Workout> = map { workout ->
    workout.copy(exercises = workout.exercises.map { e -> lookup[e.exerciseId]?.let { e.copy(exerciseId = it.id) } ?: e })
}

/** [ExerciseRepository.lookups] that never fails: before (or without) a library, ids stand as they are. */
fun ExerciseRepository.lookupsOrEmpty(): Flow<Map<String, Exercise>> =
    lookups.onStart { emit(emptyMap()) }.catch { e -> if (e is IOException) emit(emptyMap()) else throw e }

/** The same name, give or take spaces, hyphens and a plural "s": "Push-Ups" and "push up" match. */
private fun String.sameNameKey() = lowercase().replace(NON_WORD, "").removeSuffix("s")

private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
