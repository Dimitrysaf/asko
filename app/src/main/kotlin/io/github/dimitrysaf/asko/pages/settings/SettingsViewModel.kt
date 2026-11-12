package io.github.dimitrysaf.asko.pages.settings

import io.github.dimitrysaf.asko.data.importing.ImportCounts
import io.github.dimitrysaf.asko.data.importing.WorkoutImporter

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.backup.BackupCounts
import io.github.dimitrysaf.asko.data.backup.BackupRepository
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import java.io.IOException

/** What the last backup or restore did, for the snackbar. */
sealed interface BackupResult {
    data class BackedUp(val counts: BackupCounts) : BackupResult
    data class Restored(val counts: BackupCounts) : BackupResult
    data class Imported(val counts: ImportCounts) : BackupResult
    data object Failed : BackupResult
}

class SettingsViewModel(
    val settings: SettingsRepository,
    private val backups: BackupRepository,
    private val importer: WorkoutImporter,
) : ViewModel() {
    /** A backup or restore is running (its buttons wait). */
    var busy by mutableStateOf(false)
        private set

    /** The last result, shown once; [resultShown] clears it. */
    var result by mutableStateOf<BackupResult?>(null)
        private set

    fun backUp(resolver: ContentResolver, uri: Uri) = run {
        val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("Can't write $uri")
        BackupResult.BackedUp(output.use { backups.export(it) })
    }

    fun restore(resolver: ContentResolver, uri: Uri) = run {
        val input = resolver.openInputStream(uri) ?: throw IOException("Can't read $uri")
        BackupResult.Restored(input.use { backups.restore(it) })
    }

    /** Adds the workouts in a Strong or Hevy CSV export. */
    fun importWorkouts(resolver: ContentResolver, uri: Uri) = run {
        val input = resolver.openInputStream(uri) ?: throw IOException("Can't read $uri")
        BackupResult.Imported(input.use { importer.import(it) })
    }

    fun resultShown() {
        result = null
    }

    private fun run(work: suspend () -> BackupResult) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            result = try {
                work()
            } catch (e: IOException) {
                BackupResult.Failed
            } catch (e: SecurityException) {
                BackupResult.Failed // the file's permission was lost
            }
            busy = false
        }
    }
}
