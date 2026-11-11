package io.github.dimitrysaf.asko.lib.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.dimitrysaf.asko.AskoApplication

/** The workout notification's buttons (−15 s, +15 s, Skip): passed on to the workout. */
class WorkoutActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val command = WorkoutCommand.entries.firstOrNull { it.name == intent.action } ?: return
        (context.applicationContext as AskoApplication).container.workoutSession.send(command)
    }
}
