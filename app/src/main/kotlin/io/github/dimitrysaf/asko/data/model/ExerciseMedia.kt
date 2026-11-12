package io.github.dimitrysaf.asko.data.model

/** What a media file of yours is. A GIF is an image (it plays as one). */
enum class MediaType { IMAGE, VIDEO }

/**
 * A photo, GIF or video you added to an exercise, shown before the library's pictures.
 * [path] and [posterPath] are files on the device.
 */
data class ExerciseMedia(
    val id: Long,
    val exerciseId: String,
    val path: String,
    val type: MediaType,
    /** A video's first frame; null for images. */
    val posterPath: String?,
) {
    /** What a list shows for it: the image, or the video's first frame. */
    val stillPath: String get() = posterPath ?: path
}
