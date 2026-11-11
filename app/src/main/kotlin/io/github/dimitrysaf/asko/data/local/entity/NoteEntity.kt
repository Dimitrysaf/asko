package io.github.dimitrysaf.asko.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A note on a routine, an exercise or a workout ([subject] and [subjectId]: see
 * [io.github.dimitrysaf.asko.data.model.NoteSubject]). Added in DB version 8.
 */
@Entity(tableName = "notes", indices = [Index("subject", "subjectId")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [io.github.dimitrysaf.asko.data.model.NoteSubject] name. */
    val subject: String,
    /** The routine's or workout's id, or the exercise's ExerciseDB id. */
    val subjectId: String,
    /** [io.github.dimitrysaf.asko.data.model.NoteType] name. */
    val type: String,
    val text: String,
    val createdAtMs: Long,
    /** When it was last changed; null if never. */
    val editedAtMs: Long? = null,
    /** Trainer notes: who said it. */
    val trainer: String? = null,
    /** Setup notes: which gym the settings are for. */
    val gym: String? = null,
    /** Pain notes: where it hurt. */
    val bodyPart: String? = null,
    /** Pain notes: how bad, 1–10. */
    val painLevel: Int? = null,
)
