package io.github.dimitrysaf.asko.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import io.github.dimitrysaf.asko.data.local.dao.ExerciseMediaDao
import io.github.dimitrysaf.asko.data.local.entity.ExerciseMediaEntity
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Your photos, GIFs and videos on exercises: copied into the app's files when added (so they
 * stay if the original is deleted), a video with its first frame saved for lists.
 */
class MediaRepository(private val context: Context, private val dao: ExerciseMediaDao, scope: CoroutineScope) {
    private val folder = File(context.filesDir, FOLDER)

    /** Each exercise's media, oldest first, by exercise id. */
    val media: StateFlow<Map<String, List<ExerciseMedia>>> =
        dao.observeAll().map { list -> list.map { it.toModel() }.groupBy { it.exerciseId } }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Copies [uri] (from the photo picker) onto [exerciseId]. Throws [IOException] when it can't be read. */
    suspend fun add(exerciseId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val video = mime.startsWith("video/")
        val extension = mime.substringAfter('/', "").substringBefore(';').ifEmpty { if (video) "mp4" else "jpg" }
        folder.mkdirs()
        val name = UUID.randomUUID().toString()
        val file = File(folder, "$name.$extension")
        (resolver.openInputStream(uri) ?: throw IOException("Can't read $uri")).use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val poster = if (video) savePoster(file, File(folder, "$name-poster.jpg")) else null
        dao.insert(
            ExerciseMediaEntity(
                exerciseId = exerciseId,
                file = "$FOLDER/${file.name}",
                type = (if (video) MediaType.VIDEO else MediaType.IMAGE).name,
                poster = poster?.let { "$FOLDER/${it.name}" },
                addedAtMs = System.currentTimeMillis(),
            ),
        )
    }

    /** Takes it off its exercise and deletes the copy. */
    suspend fun remove(media: ExerciseMedia) = withContext(Dispatchers.IO) {
        dao.delete(media.id)
        File(media.path).delete()
        media.posterPath?.let { File(it).delete() }
    }

    /** Takes every photo and video off [exerciseId] (an exercise of yours discarded unsaved). */
    suspend fun removeAll(exerciseId: String) {
        media.value[exerciseId].orEmpty().forEach { remove(it) }
    }

    /** A video's first frame as a JPEG; null when it can't be read. */
    private fun savePoster(video: File, to: File): File? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.path)
            val frame = retriever.getFrameAtTime(0) ?: return null
            to.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            to
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun ExerciseMediaEntity.toModel() = ExerciseMedia(
        id = id,
        exerciseId = exerciseId,
        path = File(context.filesDir, file).path,
        type = MediaType.entries.firstOrNull { it.name == type } ?: MediaType.IMAGE,
        posterPath = poster?.let { File(context.filesDir, it).path },
    )

    private companion object {
        const val FOLDER = "media"
    }
}
