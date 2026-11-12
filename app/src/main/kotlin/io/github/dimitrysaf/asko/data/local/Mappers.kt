package io.github.dimitrysaf.asko.data.local

import io.github.dimitrysaf.asko.data.local.entity.RoutineEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineSetEntity
import io.github.dimitrysaf.asko.data.local.entity.RoutineWithExercises
import io.github.dimitrysaf.asko.data.local.entity.WorkoutExerciseEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutSetEntity
import io.github.dimitrysaf.asko.data.local.entity.WorkoutWithExercises
import io.github.dimitrysaf.asko.data.model.PlannedSet
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.model.RoutineExercise
import io.github.dimitrysaf.asko.data.model.SetType
import io.github.dimitrysaf.asko.data.model.Workout
import java.time.Instant

fun WorkoutWithExercises.toModel() = Workout(
    id = workout.id,
    startedAt = Instant.ofEpochMilli(workout.startedAt),
    endedAt = workout.endedAt?.let(Instant::ofEpochMilli),
    note = workout.note,
    routineName = workout.routineName,
    exercises = exercises.sortedBy { it.exercise.position }.map { (exercise, sets) ->
        RoutineExercise(
            exerciseId = exercise.exerciseId,
            restSeconds = exercise.restSeconds,
            timed = exercise.timed,
            repRange = exercise.repRange,
            supersetWithNext = exercise.supersetWithNext,
            distance = exercise.distance,
            sets = sets.sortedBy { it.position }.map {
                PlannedSet(
                    type = setType(it.type),
                    reps = it.reps,
                    repsMax = it.repsMax,
                    weightKg = it.weightKg,
                    durationSeconds = it.durationSeconds,
                    distanceMeters = it.distanceMeters,
                )
            },
        )
    },
)

/** A logged workout's rows; ids and positions are filled in by [io.github.dimitrysaf.asko.data.local.dao.WorkoutDao.insertLogged]. */
fun List<RoutineExercise>.toWorkoutEntities(): List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>> = map { exercise ->
    WorkoutExerciseEntity(
        workoutId = 0,
        exerciseId = exercise.exerciseId,
        position = 0,
        restSeconds = exercise.restSeconds,
        timed = exercise.timed,
        repRange = exercise.repRange,
        supersetWithNext = exercise.supersetWithNext,
        distance = exercise.distance,
    ) to exercise.sets.map {
        WorkoutSetEntity(
            workoutExerciseId = 0,
            position = 0,
            type = it.type.name,
            reps = it.reps,
            repsMax = it.repsMax,
            weightKg = it.weightKg,
            durationSeconds = it.durationSeconds,
            distanceMeters = it.distanceMeters,
        )
    }
}

/** An unknown name (written by a newer app version) reads as a normal set rather than crashing. */
private fun setType(name: String) = SetType.entries.firstOrNull { it.name == name } ?: SetType.NORMAL

fun RoutineWithExercises.toModel() = Routine(
    id = routine.id,
    name = routine.name,
    folder = routine.folder,
    exercises = exercises.sortedBy { it.exercise.position }.map { (exercise, sets) ->
        RoutineExercise(
            exerciseId = exercise.exerciseId,
            restSeconds = exercise.restSeconds,
            timed = exercise.timed,
            repRange = exercise.repRange,
            supersetWithNext = exercise.supersetWithNext,
            distance = exercise.distance,
            sets = sets.sortedBy { it.position }.map {
                PlannedSet(
                    type = setType(it.type),
                    reps = it.reps,
                    repsMax = it.repsMax,
                    weightKg = it.weightKg,
                    durationSeconds = it.durationSeconds,
                    distanceMeters = it.distanceMeters,
                )
            },
        )
    },
)

/** Entities for saving; ids and positions are filled in by [io.github.dimitrysaf.asko.data.local.dao.RoutineDao.save]. */
fun Routine.toEntities(): Pair<RoutineEntity, List<Pair<RoutineExerciseEntity, List<RoutineSetEntity>>>> =
    RoutineEntity(id = id, name = name.trim(), folder = folder?.trim()?.ifEmpty { null }) to exercises.map { exercise ->
        RoutineExerciseEntity(
            routineId = id,
            exerciseId = exercise.exerciseId,
            position = 0,
            restSeconds = exercise.restSeconds,
            timed = exercise.timed,
            repRange = exercise.repRange,
            supersetWithNext = exercise.supersetWithNext,
            distance = exercise.distance,
        ) to exercise.sets.map {
            RoutineSetEntity(
                routineExerciseId = 0,
                position = 0,
                type = it.type.name,
                reps = it.reps,
                repsMax = it.repsMax,
                weightKg = it.weightKg,
                durationSeconds = it.durationSeconds,
                distanceMeters = it.distanceMeters,
            )
        }
    }
