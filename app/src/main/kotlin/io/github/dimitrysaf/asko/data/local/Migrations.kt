package io.github.dimitrysaf.asko.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 2 → 3: routines plan individual sets instead of "sets × reps". Each exercise's
 * "3 × 10" becomes three NORMAL sets of 10 reps, and the sets/reps columns go away.
 *
 * Order matters: routine_exercises is rebuilt before anything references it (dropping a table
 * that others point at would cascade-delete their rows), so the expanded sets wait in a staging
 * table meanwhile.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Expand sets × reps into one row per set (sets are at most 20 in v2's editor; 100 covers it).
        db.execSQL("CREATE TABLE `routine_sets_staging` (`routineExerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `reps` INTEGER NOT NULL)")
        db.execSQL(
            """
            WITH RECURSIVE n(i) AS (SELECT 0 UNION ALL SELECT i + 1 FROM n WHERE i < 99)
            INSERT INTO `routine_sets_staging` (`routineExerciseId`, `position`, `reps`)
            SELECT re.`id`, n.i, re.`reps` FROM `routine_exercises` re JOIN n ON n.i < re.`sets`
            """.trimIndent(),
        )

        // 2. Rebuild routine_exercises without sets/reps and with `timed` (SQLite here can't drop columns).
        db.execSQL(
            "CREATE TABLE `routine_exercises_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routineId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "`restSeconds` INTEGER NOT NULL, `timed` INTEGER NOT NULL, " +
                "FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO `routine_exercises_new` (`id`, `routineId`, `exerciseId`, `position`, `restSeconds`, `timed`) " +
                "SELECT `id`, `routineId`, `exerciseId`, `position`, `restSeconds`, 0 FROM `routine_exercises`",
        )
        db.execSQL("DROP TABLE `routine_exercises`")
        db.execSQL("ALTER TABLE `routine_exercises_new` RENAME TO `routine_exercises`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` ON `routine_exercises` (`routineId`)")

        // 3. The new per-set table, filled from staging.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine_sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routineExerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`reps` INTEGER, `weightKg` REAL, `durationSeconds` INTEGER, " +
                "FOREIGN KEY(`routineExerciseId`) REFERENCES `routine_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_sets_routineExerciseId` ON `routine_sets` (`routineExerciseId`)")
        db.execSQL(
            "INSERT INTO `routine_sets` (`routineExerciseId`, `position`, `type`, `reps`) " +
                "SELECT `routineExerciseId`, `position`, 'NORMAL', `reps` FROM `routine_sets_staging`",
        )
        db.execSQL("DROP TABLE `routine_sets_staging`")
    }
}

/**
 * Version 4 → 5: workouts are shaped like routines. Sets were rows of the workout naming their
 * exercise; now each exercise is a workout_exercises row (rest, timed, rep range) and its sets
 * (with a type and a range top) hang off it. Existing sets are kept: grouped by exercise, in their
 * order, as normal sets.
 *
 * Nothing points at the old workout_sets, so it can simply be rebuilt after the exercises exist.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. One exercise row per exercise a workout's sets name, ordered by its first set.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                "`restSeconds` INTEGER NOT NULL, `timed` INTEGER NOT NULL, `repRange` INTEGER NOT NULL, " +
                "FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_workoutId` ON `workout_exercises` (`workoutId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_exercises_exerciseId` ON `workout_exercises` (`exerciseId`)")
        db.execSQL(
            "INSERT INTO `workout_exercises` (`workoutId`, `exerciseId`, `position`, `restSeconds`, `timed`, `repRange`) " +
                "SELECT `workoutId`, `exerciseId`, MIN(`position`), 0, 0, 0 FROM `workout_sets` GROUP BY `workoutId`, `exerciseId`",
        )

        // 2. Rebuild workout_sets under those exercises (SQLite here can't drop columns).
        db.execSQL(
            "CREATE TABLE `workout_sets_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`workoutExerciseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                "`reps` INTEGER, `repsMax` INTEGER, `weightKg` REAL, `durationSeconds` INTEGER, " +
                "FOREIGN KEY(`workoutExerciseId`) REFERENCES `workout_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO `workout_sets_new` (`id`, `workoutExerciseId`, `position`, `type`, `reps`, `repsMax`, `weightKg`, `durationSeconds`) " +
                "SELECT s.`id`, e.`id`, s.`position`, 'NORMAL', s.`reps`, NULL, s.`weightKg`, s.`durationSeconds` " +
                "FROM `workout_sets` s JOIN `workout_exercises` e ON e.`workoutId` = s.`workoutId` AND e.`exerciseId` = s.`exerciseId`",
        )
        db.execSQL("DROP TABLE `workout_sets`")
        db.execSQL("ALTER TABLE `workout_sets_new` RENAME TO `workout_sets`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_workoutExerciseId` ON `workout_sets` (`workoutExerciseId`)")
    }
}
