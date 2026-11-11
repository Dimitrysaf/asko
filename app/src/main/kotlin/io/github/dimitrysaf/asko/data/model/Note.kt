package io.github.dimitrysaf.asko.data.model

import java.time.Instant

/** Who a note is from, or what it's about. */
enum class NoteType {
    /** Your own thoughts. */
    SELF,

    /** What your trainer told you (typed in by you). */
    TRAINER,

    /** Machine and equipment settings: seat height, pin, grip. */
    SETUP,

    /** Something that hurt, to keep track of. */
    PAIN,
}

/** What a note belongs to; [Note]s are stored against its id. */
enum class NoteSubject {
    /** A routine (by its id). */
    ROUTINE,

    /** An exercise (by its ExerciseDB id): the same notes in every routine and workout. */
    EXERCISE,

    /** A done workout (by its id). */
    WORKOUT,
}

/**
 * A note as written. [id] is 0 for one not saved yet. The other details depend on the type:
 * [trainer] for Trainer notes, [gym] for Setup notes, [bodyPart] and [painLevel] (1–10) for Pain
 * notes; each is optional.
 */
data class Note(
    val id: Long,
    val type: NoteType,
    val text: String,
    val createdAt: Instant,
    /** When it was last changed; null if never. */
    val editedAt: Instant? = null,
    val trainer: String? = null,
    val gym: String? = null,
    val bodyPart: String? = null,
    val painLevel: Int? = null,
) {
    /** Only the details its type has (switching types leaves no stray ones behind). */
    fun trimmedToType(): Note = copy(
        trainer = trainer?.trim()?.takeIf { type == NoteType.TRAINER && it.isNotEmpty() },
        gym = gym?.trim()?.takeIf { type == NoteType.SETUP && it.isNotEmpty() },
        bodyPart = bodyPart?.trim()?.takeIf { type == NoteType.PAIN && it.isNotEmpty() },
        painLevel = painLevel?.takeIf { type == NoteType.PAIN && it in 1..10 },
    )
}
