package io.github.dimitrysaf.asko.lib.health

import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.data.repository.BodyWeightRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.reflect.KClass

/**
 * Keeps Health Connect in step with Asko, while Settings has it on and its permissions are given:
 * each finished workout as an exercise session (with its distance, when it covered some), and each
 * body weight logged. Records carry Asko's own ids, so a changed workout is updated there and a
 * deleted one removed; what was sent is remembered, so nothing is sent twice.
 */
class HealthConnectSync(
    private val context: Context,
    private val settings: SettingsRepository,
    private val workouts: WorkoutRepository,
    private val bodyWeights: BodyWeightRepository,
    scope: CoroutineScope,
) {
    /** Remembers what was sent: each record's Asko id and a fingerprint of what it said. */
    private val sent = context.getSharedPreferences("health_connect_sent", Context.MODE_PRIVATE)

    /** Health Connect is on this phone. */
    val available: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    init {
        @OptIn(FlowPreview::class)
        scope.launch {
            combine(settings.healthConnect, workouts.historyState.filterNotNull(), bodyWeights.weights.filterNotNull()) { on, history, weights ->
                Triple(on, history, weights)
            }
                .debounce(SETTLE_MS)
                .collect { (on, history, weights) ->
                    if (on && available) runCatching { sync(history, weights) }
                }
        }
    }

    /** Whether every permission Asko asks for is given. */
    suspend fun hasPermissions(): Boolean =
        available && HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    private suspend fun sync(history: List<Workout>, weights: List<Pair<LocalDate, Double>>) {
        if (!hasPermissions()) return
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()

        // What each record should say now, by its Asko id.
        val wanted = LinkedHashMap<String, Record>()
        history.filter { it.endedAt != null && it.exercises.isNotEmpty() }.forEach { workout ->
            wanted["$WORKOUT${workout.id}"] = session(workout, zone, "$WORKOUT${workout.id}")
            val meters = workout.exercises.filter { it.distance }.sumOf { e -> e.sets.sumOf { it.distanceMeters ?: 0.0 } }
            if (meters > 0) wanted["$DISTANCE${workout.id}"] = distance(workout, meters, zone, "$DISTANCE${workout.id}")
        }
        weights.forEach { (day, kg) ->
            val at = day.atTime(LocalTime.NOON).atZone(zone)
            wanted["$WEIGHT${day.toEpochDay()}"] = WeightRecord(
                time = at.toInstant(),
                zoneOffset = at.offset,
                weight = Mass.kilograms(kg),
                metadata = Metadata.manualEntry(clientRecordId = "$WEIGHT${day.toEpochDay()}", clientRecordVersion = version()),
            )
        }

        // Send what's new or changed; a newer version replaces what Health Connect has.
        val changed = wanted.filter { (id, record) -> sent.getString(id, null) != fingerprint(record) }
        if (changed.isNotEmpty()) {
            client.insertRecords(changed.values.toList())
            sent.edit { changed.forEach { (id, record) -> putString(id, fingerprint(record)) } }
        }

        // Take out what Asko no longer has (a deleted workout).
        val gone = sent.all.keys.filterNot { it in wanted }
        if (gone.isNotEmpty()) {
            gone.groupBy(::typeOf).forEach { (type, ids) -> type?.let { client.deleteRecords(it, emptyList(), ids) } }
            sent.edit { gone.forEach { remove(it) } }
        }
    }

    private fun session(workout: Workout, zone: ZoneId, id: String): ExerciseSessionRecord {
        val start = workout.startedAt
        // Logged afterwards without a time: Health Connect needs some length, so a minute.
        val end = workout.endedAt?.takeIf { it.isAfter(start) } ?: start.plus(Duration.ofMinutes(1))
        val allDistance = workout.exercises.all { it.distance }
        return ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = zone.rules.getOffset(start),
            endTime = end,
            endZoneOffset = zone.rules.getOffset(end),
            metadata = Metadata.manualEntry(clientRecordId = id, clientRecordVersion = version()),
            exerciseType = if (allDistance) ExerciseSessionRecord.EXERCISE_TYPE_RUNNING else ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = workout.routineName,
        )
    }

    private fun distance(workout: Workout, meters: Double, zone: ZoneId, id: String): DistanceRecord {
        val start = workout.startedAt
        val end = workout.endedAt?.takeIf { it.isAfter(start) } ?: start.plus(Duration.ofMinutes(1))
        return DistanceRecord(
            startTime = start,
            startZoneOffset = zone.rules.getOffset(start),
            endTime = end,
            endZoneOffset = zone.rules.getOffset(end),
            distance = Length.meters(meters),
            metadata = Metadata.manualEntry(clientRecordId = id, clientRecordVersion = version()),
        )
    }

    /** What a record says, without its metadata (whose version changes every time). */
    private fun fingerprint(record: Record): String = when (record) {
        is ExerciseSessionRecord -> "${record.startTime}|${record.endTime}|${record.exerciseType}|${record.title}"
        is DistanceRecord -> "${record.startTime}|${record.endTime}|${record.distance.inMeters}"
        is WeightRecord -> "${record.time}|${record.weight.inKilograms}"
        else -> record.toString()
    }

    /** Later sends win over earlier ones in Health Connect. */
    private fun version(): Long = System.currentTimeMillis()

    private fun typeOf(id: String): KClass<out Record>? = when {
        id.startsWith(WORKOUT) -> ExerciseSessionRecord::class
        id.startsWith(DISTANCE) -> DistanceRecord::class
        id.startsWith(WEIGHT) -> WeightRecord::class
        else -> null
    }

    companion object {
        private const val WORKOUT = "asko-workout-"
        private const val DISTANCE = "asko-distance-"
        private const val WEIGHT = "asko-weight-"

        /** Changes settle (a workout saved, then its notes) before anything is sent. */
        private const val SETTLE_MS = 2_000L

        /** What Asko writes: workouts, the distance covered in them, and body weight. Nothing is read. */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
        )
    }
}
