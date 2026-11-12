package io.github.dimitrysaf.asko.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Exercises starred as favourites (by id), kept in shared preferences. */
class FavouritesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("favourites", Context.MODE_PRIVATE)

    private val _ids = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())

    /** The favourite exercises' ids. */
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun setFavourite(exerciseId: String, favourite: Boolean) =
        replaceAll(if (favourite) _ids.value + exerciseId else _ids.value - exerciseId)

    /** Puts [ids] in place of all favourites (restoring a backup). */
    fun replaceAll(ids: Set<String>) {
        prefs.edit { putStringSet(KEY, ids) }
        _ids.value = ids
    }

    private companion object {
        const val KEY = "exercise_ids"
    }
}
