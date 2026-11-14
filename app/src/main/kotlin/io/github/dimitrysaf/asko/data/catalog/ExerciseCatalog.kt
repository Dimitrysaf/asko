package io.github.dimitrysaf.asko.data.catalog

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import io.github.dimitrysaf.asko.data.model.DownloadProgress
import android.os.SystemClock
import io.github.dimitrysaf.asko.data.model.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * One exercise library ([fetch] downloads it), saved to [file] so it works offline at the gym.
 * Downloads on first use, and again once the saved copy is a week old. [expectedTotal]: how many
 * steps the download reports, for its progress before the first report comes in.
 */
class ExerciseCatalog(
    private val file: File,
    private val expectedTotal: Int,
    private val fetch: suspend (onProgress: (done: Int, total: Int) -> Unit) -> List<Exercise>,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: List<Exercise>? = null

    private val _progress = MutableStateFlow<DownloadProgress?>(null)

    /** How far a download is while one runs (first use, weekly refresh); null otherwise. */
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    /** Throws [IOException] when nothing is saved yet and the library can't be reached. */
    suspend fun load(): List<Exercise> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) { readSavedOrDownload() }.also { cache = it }
    }

    private suspend fun readSavedOrDownload(): List<Exercise> {
        val saved = readSaved()
        val fresh = System.currentTimeMillis() - file.lastModified() < REFRESH_AFTER_MS
        if (saved != null && fresh) return saved
        // shortcut: the weekly refresh makes that one load wait for the download; move it to a background job if it bothers you.
        val start = SystemClock.elapsedRealtime()
        // Reported from the start (nothing in yet), not only once the first page arrives.
        _progress.value = DownloadProgress(0, expectedTotal, 0)
        return try {
            fetch { done, total ->
                _progress.value = DownloadProgress(done, total, SystemClock.elapsedRealtime() - start)
            }.also {
                _progress.value = _progress.value?.let { p -> p.copy(done = p.total) } // full while it's saved
                save(it)
            }
        } catch (e: IOException) {
            saved ?: throw e // offline: last week's copy beats nothing
        } finally {
            _progress.value = null
        }
    }

    private fun readSaved(): List<Exercise>? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString<List<Exercise>>(file.readText())
        } catch (e: SerializationException) {
            null // saved by an older app version with a different shape: download again
        }
    }

    /** It has been downloaded at some point (a copy is saved on the phone). */
    val isSaved: Boolean get() = cache != null || file.exists()

    /** Forgets the library: its saved copy and what's in memory (a library you removed). */
    suspend fun delete() = mutex.withLock {
        cache = null
        withContext(Dispatchers.IO) { file.delete() }
    }

    /** Writes to a temp file first, so a crash mid-write never leaves a half-written library. */
    private fun save(exercises: List<Exercise>) {
        val temp = File(file.path + ".tmp")
        temp.writeText(json.encodeToString(exercises))
        temp.renameTo(file)
    }

    private companion object {
        val REFRESH_AFTER_MS = TimeUnit.DAYS.toMillis(7)
    }
}
