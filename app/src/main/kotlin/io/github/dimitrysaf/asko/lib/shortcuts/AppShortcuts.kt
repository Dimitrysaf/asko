package io.github.dimitrysaf.asko.lib.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.dimitrysaf.asko.MainActivity
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.data.model.Routine
import io.github.dimitrysaf.asko.data.repository.RoutineRepository
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutStore
import io.github.dimitrysaf.asko.data.session.ActiveWorkoutSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The launcher's shortcuts (long-press Asko's icon): Resume while a workout is in progress, your
 * first routines in their list order (each opens the routine's page, where Start is), and Log
 * workout. Kept in step with the routines and the workout in progress while the app runs.
 */
class AppShortcuts(
    private val context: Context,
    routines: RoutineRepository,
    activeWorkouts: ActiveWorkoutStore,
    scope: CoroutineScope,
) {
    init {
        scope.launch {
            combine(routines.routines, activeWorkouts.current) { list, active -> list to active }
                .distinctUntilChanged()
                .collect { (list, active) -> runCatching { publish(list, active) } }
        }
    }

    private fun publish(routines: List<Routine>, active: ActiveWorkoutSummary?) {
        // Launchers show about four; Resume takes a routine's place.
        val routineCount = if (active != null) SHOWN - 2 else SHOWN - 1
        val shortcuts = buildList {
            if (active != null) {
                add(
                    shortcut(RESUME_ID, Target.Resume, R.drawable.ic_shortcut_resume)
                        .setShortLabel(context.getString(R.string.shortcut_resume))
                        .setLongLabel(context.getString(R.string.shortcut_resume_long, active.routineName))
                        .build(),
                )
            }
            routines.take(routineCount).forEach { add(routineShortcut(it)) }
            add(
                shortcut(LOG_ID, Target.Log, R.drawable.ic_shortcut_log)
                    .setShortLabel(context.getString(R.string.shortcut_log))
                    .build(),
            )
        }.mapIndexed { rank, info -> ShortcutInfoCompat.Builder(info).setRank(rank).build() }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)

        // Routines pinned to the home screen: renamed ones follow, deleted ones are greyed out.
        val pinned = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .filter { it.id.startsWith(ROUTINE_PREFIX) }
        val byId = routines.associateBy { ROUTINE_PREFIX + it.id }
        val (kept, gone) = pinned.partition { it.id in byId }
        if (kept.isNotEmpty()) ShortcutManagerCompat.updateShortcuts(context, kept.map { routineShortcut(byId.getValue(it.id)) })
        if (gone.isNotEmpty()) {
            ShortcutManagerCompat.disableShortcuts(context, gone.map { it.id }, context.getString(R.string.shortcut_routine_gone))
        }
    }

    private fun routineShortcut(routine: Routine): ShortcutInfoCompat =
        shortcut(ROUTINE_PREFIX + routine.id, Target.Routine(routine.id), R.drawable.ic_shortcut_routine)
            .setShortLabel(routine.name)
            .build()

    private fun shortcut(id: String, target: Target, icon: Int) = ShortcutInfoCompat.Builder(context, id)
        .setIcon(IconCompat.createWithResource(context, icon))
        .setIntent(
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(EXTRA_TARGET, target.encoded),
        )

    /** Where a shortcut goes. */
    sealed interface Target {
        val encoded: String

        data object Resume : Target {
            override val encoded = "resume"
        }

        data object Log : Target {
            override val encoded = "log"
        }

        data class Routine(val id: Long) : Target {
            override val encoded = "routine:$id"
        }
    }

    companion object {
        private const val EXTRA_TARGET = "io.github.dimitrysaf.asko.SHORTCUT"
        private const val RESUME_ID = "resume"
        private const val LOG_ID = "log"
        private const val ROUTINE_PREFIX = "routine_"
        private const val SHOWN = 4

        /** The shortcut [intent] was opened from, if any. */
        fun targetOf(intent: Intent?): Target? = when (val value = intent?.getStringExtra(EXTRA_TARGET)) {
            null -> null
            Target.Resume.encoded -> Target.Resume
            Target.Log.encoded -> Target.Log
            else -> value.removePrefix("routine:").toLongOrNull()?.let(Target::Routine)
        }
    }
}
