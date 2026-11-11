package io.github.dimitrysaf.asko.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.dimitrysaf.asko.data.local.dao.DayExcuseDao
import io.github.dimitrysaf.asko.data.local.dao.NoteDao
import io.github.dimitrysaf.asko.data.local.entity.DayExcuseEntity
import io.github.dimitrysaf.asko.data.local.dao.RoutineDao
import io.github.dimitrysaf.asko.data.local.entity.NoteEntity
import io.github.dimitrysaf.asko.data.local.dao.WorkoutDao
import io.github.dimitrysaf.asko.data.local.entity.RoutineEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineSetEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutSetEntity

@Database(
    entities = [
        WorkoutEntity::class, WorkoutExerciseEntity::class, WorkoutSetEntity::class,
        RoutineEntity::class, RoutineExerciseEntity::class, RoutineSetEntity::class,
        NoteEntity::class, DayExcuseEntity::class,
    ],
    version = 9,
    // Room works out each step from the exported schemas (app/schemas), keeping existing data.
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // added routines
        // 2 → 3 needs data conversion, so it is hand-written: MIGRATION_2_3
        AutoMigration(from = 3, to = 4), // rep ranges
        // 4 → 5 regroups workout sets under exercises, hand-written: MIGRATION_4_5
        AutoMigration(from = 5, to = 6), // the routine a workout was done from
        AutoMigration(from = 6, to = 7), // supersets
        AutoMigration(from = 7, to = 8), // routine order, notes
        AutoMigration(from = 8, to = 9), // sick and travel days
    ],
)
abstract class AskoDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    abstract fun routineDao(): RoutineDao

    abstract fun noteDao(): NoteDao

    abstract fun dayExcuseDao(): DayExcuseDao

    companion object {
        fun build(context: Context): AskoDatabase =
            Room.databaseBuilder(context, AskoDatabase::class.java, "asko.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_4_5)
                .build()
    }
}
