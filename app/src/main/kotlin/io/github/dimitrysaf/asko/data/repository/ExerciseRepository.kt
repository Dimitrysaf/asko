package io.github.dimitrysaf.asko.data.repository

import io.github.dimitrysaf.asko.data.catalog.ExerciseCatalog
import io.github.dimitrysaf.asko.data.model.DownloadProgress
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.ExerciseSource
import io.github.dimitrysaf.asko.data.model.Workout
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
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
    private val catalogs: Map<ExerciseSource, ExerciseCatalog>,
    private val settings: SettingsRepository,
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
        combine(settings.sourceOrder, settings.hiddenSources) { order, hidden -> order.filterNot { it in hidden } }
            .mapLatest { shown -> library().let { withContext(Dispatchers.Default) { it.merged(shown) } } }

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
        combine(settings.sourceOrder, settings.hiddenSources) { order, hidden -> order to hidden }
            .mapLatest { (order, hidden) -> library().let { withContext(Dispatchers.Default) { it.resolved(order, hidden) } } }

    /** [lookup] as already worked out (instantly, for a page's first frame); null when it isn't yet. */
    fun lookupNow(): Map<String, Exercise>? = cached?.resolvedIfReady(settings.sourceOrder.value, settings.hiddenSources.value)

    /** [browsable] as already worked out (instantly, for a page's first frame); null when it isn't yet. */
    fun browsableNow(): List<Exercise>? =
        cached?.mergedIfReady(settings.sourceOrder.value.filterNot { it in settings.hiddenSources.value })

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
        return withContext(Dispatchers.Default) { library.merged(shown).similarTo(exercise, limit) }
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

    /** Each library that could be read; throws the first failure when none could. */
    private suspend fun loadEach(): Map<ExerciseSource, List<Exercise>> = coroutineScope {
        val results = catalogs.mapValues { (_, catalog) -> async { runCatching { catalog.load() } } }
            .mapValues { it.value.await() }
        val loaded = results.filterValues { it.isSuccess }.mapValues { it.value.getOrThrow() }
        if (loaded.isEmpty()) {
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

    private val mergedCache = HashMap<List<ExerciseSource>, List<Exercise>>()

    @Synchronized
    fun mergedIfReady(shown: List<ExerciseSource>): List<Exercise>? = mergedCache[shown]

    @Synchronized
    fun resolvedIfReady(order: List<ExerciseSource>, hidden: Set<ExerciseSource>): Map<String, Exercise>? = resolvedCache[order to hidden]

    private val resolvedCache = HashMap<Pair<List<ExerciseSource>, Set<ExerciseSource>>, Map<String, Exercise>>()

    /**
     * The [shown] libraries' exercises in one list, by name: an exercise in more than one comes
     * from the first of them.
     */
    @Synchronized
    fun merged(shown: List<ExerciseSource>): List<Exercise> = mergedCache.getOrPut(shown) {
        val rank = shown.withIndex().associate { (i, source) -> source to i }
        all.filter { it.source in rank }
            .groupBy { keyOf.getValue(it.id) }
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
