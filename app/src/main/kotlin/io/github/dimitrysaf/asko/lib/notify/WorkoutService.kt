package io.github.dimitrysaf.asko.lib.notify

import io.github.dimitrysaf.asko.data.model.SetType

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.dimitrysaf.asko.AskoApplication
import io.github.dimitrysaf.asko.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps a workout going while the app is in the background: an ongoing notification with the
 * workout's clock, or the rest counting down with −15 s / +15 s / Skip (the system draws the
 * countdown, so it ticks without the app). When a rest runs out with the app off screen, a
 * heads-up "Rest is over" notification buzzes. Runs while a workout is open; stops with it.
 */
class WorkoutService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var alarm: Job? = null
    private val session: WorkoutSession get() = (application as AskoApplication).container.workoutSession

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val first = session.notice.value
        // Started as a foreground service, it must go to the foreground even when the workout has
        // already ended (left the screen right after opening it): Android stops the app otherwise.
        ServiceCompat.startForeground(
            this,
            ONGOING_ID,
            first?.let(::ongoing) ?: ending(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        if (first == null) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        scope.coroutineContext[Job]?.children?.forEach { it.cancel() }
        scope.launch {
            session.notice.collect { notice ->
                if (notice == null) {
                    alarm?.cancel()
                    ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    notify(ONGOING_ID, ongoing(notice))
                    scheduleRestOver(notice)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** At the end of a running rest, a buzzing "Rest is over" — unless the app is on screen (it buzzes itself). */
    private fun scheduleRestOver(notice: WorkoutNotice) {
        alarm?.cancel()
        val endsAt = notice.countdownEndsAt
        if (notice.mode != WorkoutMode.RESTING || notice.paused || endsAt == null) return
        alarm = scope.launch {
            delay((endsAt - System.currentTimeMillis()).coerceAtLeast(0))
            if (!session.appVisible) notify(REST_OVER_ID, restOver())
        }
    }

    private fun ongoing(notice: WorkoutNotice): Notification {
        val builder = NotificationCompat.Builder(this, WORKOUT_CHANNEL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title(notice))
            .setContentText(text(notice))
            .setContentIntent(openApp())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            // Android 16+: a Live Update, on the lock screen and as a chip in the status bar.
            .setRequestPromotedOngoing(true)
            // On the lock screen too: the rest counting down, where you are.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // The workout as a bar: a segment per exercise, as long as its sets, filled as sets are done.
        val total = notice.exerciseSetCounts.sum()
        if (total > 0) {
            builder.setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgressSegments(notice.exerciseSetCounts.filter { it > 0 }.map { NotificationCompat.ProgressStyle.Segment(it) })
                    .setProgress(notice.setsDone.coerceIn(0, total))
                    .setStyledByProgress(true),
            )
        }
        // The status bar chip: the countdown while resting or holding (from the chronometer below),
        // otherwise where you are: "2/4", or "Paused".
        when {
            notice.paused -> builder.setShortCriticalText(getString(R.string.workout_notification_paused_short))
            notice.mode == WorkoutMode.WORKING && notice.setNumber > 0 ->
                builder.setShortCriticalText(getString(R.string.workout_notification_set_short, notice.setNumber, notice.setCount))
            // A warm-up and the like: its badge, "W".
            notice.mode == WorkoutMode.WORKING && notice.setType != SetType.NORMAL -> builder.setShortCriticalText(getString(badge(notice.setType)))
        }
        val endsAt = notice.countdownEndsAt
        when {
            notice.paused -> builder.setShowWhen(false)
            endsAt != null -> builder.setWhen(endsAt).setUsesChronometer(true).setChronometerCountDown(true)
            else -> builder.setWhen(notice.startedAt).setUsesChronometer(true)
        }
        if (notice.mode == WorkoutMode.RESTING) {
            builder.addAction(0, getString(R.string.workout_notification_less), action(WorkoutCommand.LESS_REST))
            builder.addAction(0, getString(R.string.workout_notification_more), action(WorkoutCommand.MORE_REST))
            builder.addAction(0, getString(R.string.workout_rest_skip), action(WorkoutCommand.SKIP_REST))
        }
        return builder.build()
    }

    /** Shown for an instant when the workout ended before the service started (removed at once). */
    private fun ending(): Notification = NotificationCompat.Builder(this, WORKOUT_CHANNEL)
        .setSmallIcon(R.drawable.ic_timer)
        .setSilent(true)
        .build()

    private fun restOver(): Notification = NotificationCompat.Builder(this, REST_OVER_CHANNEL)
        .setSmallIcon(R.drawable.ic_timer)
        .setContentTitle(getString(R.string.workout_rest_over))
        .setContentText(session.notice.value?.let(::text).orEmpty())
        .setContentIntent(openApp())
        .setAutoCancel(true)
        .setTimeoutAfter(REST_OVER_SHOWN_MS)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(BUZZ)
        .build()

    /** "Push day" while working out, "Rest" or "Hold" (with "Paused" when paused). */
    private fun title(notice: WorkoutNotice): String {
        val title = when (notice.mode) {
            WorkoutMode.WORKING -> notice.routineName
            WorkoutMode.RESTING -> getString(R.string.workout_rest)
            WorkoutMode.HOLDING -> getString(R.string.workout_hold)
        }
        val shown = if (notice.paused) getString(R.string.workout_notification_paused, title) else title
        // Some phones (Samsung's) draw the clock right after the title: keep a gap before it.
        return "$shown\u2002"
    }

    /**
     * "Bench Press · set 2 of 4", or what's next while resting: "Up next: Squat · set 1". A warm-up
     * (drop set, to failure, finisher) goes by its type, as its badge does: "Squat · Warm-up".
     */
    private fun text(notice: WorkoutNotice): String {
        val name = notice.exerciseName.orEmpty()
        val numbered = notice.setType == SetType.NORMAL && notice.setNumber > 0
        val set = when {
            !numbered -> getString(typeName(notice.setType))
            notice.mode == WorkoutMode.RESTING -> getString(R.string.workout_notification_set_number, notice.setNumber)
            else -> getString(R.string.workout_notification_set_of, notice.setNumber, notice.setCount)
        }
        return getString(if (notice.mode == WorkoutMode.RESTING) R.string.workout_notification_next else R.string.workout_notification_set, name, set)
    }

    private fun typeName(type: SetType) = when (type) {
        SetType.NORMAL -> R.string.set_type_normal
        SetType.WARMUP -> R.string.set_type_warmup
        SetType.DROP -> R.string.set_type_drop
        SetType.FAILURE -> R.string.set_type_failure
        SetType.FINISHER -> R.string.set_type_finisher
    }

    private fun badge(type: SetType) = when (type) {
        SetType.WARMUP -> R.string.set_badge_warmup
        SetType.DROP -> R.string.set_badge_drop
        SetType.FAILURE -> R.string.set_badge_failure
        SetType.FINISHER -> R.string.set_badge_finisher
        SetType.NORMAL -> R.string.set_type_normal
    }

    private fun notify(id: Int, notification: Notification) {
        // Without the permission (declined, Android 13+) the workout still runs; it just isn't shown.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    /** Back to the app, where it was (the launcher intent brings the running task forward). */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        packageManager.getLaunchIntentForPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(command: WorkoutCommand): PendingIntent = PendingIntent.getBroadcast(
        this,
        command.ordinal,
        Intent(this, WorkoutActionReceiver::class.java).setAction(command.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val WORKOUT_CHANNEL = "workout"
        private const val REST_OVER_CHANNEL = "rest_over"
        private const val ONGOING_ID = 1
        private const val REST_OVER_ID = 2
        private const val REST_OVER_SHOWN_MS = 60_000L
        private val BUZZ = longArrayOf(0, 300, 150, 300)

        /** Starts (or updates) the notification for the workout currently published to the session. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        }

        private fun createChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(WORKOUT_CHANNEL, context.getString(R.string.channel_workout), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.channel_workout_description)
                    setShowBadge(false)
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(REST_OVER_CHANNEL, context.getString(R.string.channel_rest_over), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_rest_over_description)
                    enableVibration(true)
                    vibrationPattern = BUZZ
                },
            )
        }
    }
}
