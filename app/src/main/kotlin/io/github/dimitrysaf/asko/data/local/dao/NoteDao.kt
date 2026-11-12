package io.github.dimitrysaf.asko.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.dimitrysaf.asko.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    /** A subject's notes, oldest first. */
    @Query("SELECT * FROM notes WHERE subject = :subject AND subjectId = :subjectId ORDER BY createdAtMs, id")
    fun observe(subject: String, subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subject = :subject AND subjectId = :subjectId ORDER BY createdAtMs, id")
    suspend fun all(subject: String, subjectId: String): List<NoteEntity>

    /** The notes of several subjects at once (an exercise's versions in each library), oldest first. */
    @Query("SELECT * FROM notes WHERE subject = :subject AND subjectId IN (:subjectIds) ORDER BY createdAtMs, id")
    fun observeAny(subject: String, subjectIds: List<String>): Flow<List<NoteEntity>>

    /** Changes a note's content; what it belongs to stays. */
    @Query(
        "UPDATE notes SET type = :type, text = :text, editedAtMs = :editedAtMs, trainer = :trainer, gym = :gym, " +
            "bodyPart = :bodyPart, painLevel = :painLevel WHERE id = :id",
    )
    suspend fun updateContent(
        id: Long,
        type: String,
        text: String,
        editedAtMs: Long?,
        trainer: String?,
        gym: String?,
        bodyPart: String?,
        painLevel: Int?,
    )

    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY createdAtMs, id")
    suspend fun allOf(subject: String): List<NoteEntity>

    /** Every note on one kind of subject (all workouts' notes, say), oldest first. */
    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY createdAtMs, id")
    fun observeAllOf(subject: String): Flow<List<NoteEntity>>

    /** Every note of a type, on any subject, oldest first. */
    @Query("SELECT * FROM notes WHERE type = :type ORDER BY createdAtMs, id")
    fun observeOfType(type: String): Flow<List<NoteEntity>>

    /** Trainers' names written before, for suggestions. */
    @Query("SELECT DISTINCT trainer FROM notes WHERE trainer IS NOT NULL AND trainer != '' ORDER BY trainer COLLATE NOCASE")
    fun trainers(): Flow<List<String>>

    /** Gyms written before, for suggestions. */
    @Query("SELECT DISTINCT gym FROM notes WHERE gym IS NOT NULL AND gym != '' ORDER BY gym COLLATE NOCASE")
    fun gyms(): Flow<List<String>>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes WHERE subject = :subject AND subjectId = :subjectId")
    suspend fun deleteFor(subject: String, subjectId: String)

    /** Replaces a subject's notes with [notes], all or nothing. */
    @Transaction
    suspend fun replace(subject: String, subjectId: String, notes: List<NoteEntity>) {
        deleteFor(subject, subjectId)
        insertAll(notes)
    }

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
