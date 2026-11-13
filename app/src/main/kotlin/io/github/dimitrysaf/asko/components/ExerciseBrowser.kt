package io.github.dimitrysaf.asko.components

import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.repository.matching

/**
 * Search field, filter chips (Muscle group, Equipment) and a segmented exercise list:
 * while browsing, Favourites and Recently used on top, then A–Z sections (exercises in the top
 * sections stay in their letter's section too);
 * while searching, best match first. Used by Discovery and the routine exercise picker; [trailing]
 * adds per-row content such as a checkbox. [favourites] are exercise ids; [recent] ids are last
 * done first.
 */
@Composable
fun ExerciseBrowser(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    favourites: Set<String> = emptySet(),
    recent: List<String> = emptyList(),
    trailing: (@Composable (Exercise) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
    /** Told whether the list is at its top (for a floating button that expands only there). */
    onAtTop: (Boolean) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscleGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    val results = remember(exercises, query, muscleGroup, equipment) {
        exercises.matching(query, muscleGroup, equipment)
    }
    // The top sections, as filtered; only while browsing (a search sorts by match instead).
    val favouriteResults = remember(results, favourites, query) {
        if (query.isNotBlank()) emptyList() else results.filter { it.id in favourites }
    }
    val recentResults = remember(results, recent, query) {
        if (query.isNotBlank()) emptyList() else results.associateBy { it.id }.let { shown -> recent.mapNotNull(shown::get) }
    }
    Column(
        modifier = modifier.padding(horizontal = Dimens.PageGutter).padding(top = contentPadding.calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        SearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(top = Dimens.SpaceS))
        FilterChips(
            exercises = exercises,
            muscleGroup = muscleGroup,
            onMuscleGroupChange = { muscleGroup = it },
            equipment = equipment,
            onEquipmentChange = { equipment = it },
        )
        ExerciseList(
            results = results,
            grouped = query.isBlank(),
            // Fresh scroll position per search/filter (starts at the best match); saved per key,
            // so coming back from an exercise page restores it.
            listState = key(query, muscleGroup, equipment) { rememberLazyListState() }.also { state ->
                val atTop by remember(state) { derivedStateOf { state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0 } }
                LaunchedEffect(atTop) { onAtTop(atTop) }
            },
            favourites = favouriteResults,
            recent = recentResults,
            onExerciseClick = onExerciseClick,
            trailing = trailing,
            bottomPadding = contentPadding.calculateBottomPadding(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** The browser's layout while the library loads, so nothing jumps when it arrives. */
@Composable
fun ExerciseBrowserSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = Dimens.PageGutter),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
        SkeletonBox(Modifier.padding(top = Dimens.SpaceS).fillMaxWidth().height(56.dp), CircleShape)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            repeat(2) { SkeletonBox(Modifier.size(width = 140.dp, height = 32.dp)) }
        }
        ExerciseListSkeleton(Modifier.padding(top = Dimens.SpaceS))
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) null else {
            {
                WithTooltip(stringResource(R.string.action_clear_search)) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.action_clear_search))
                    }
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun FilterChips(
    exercises: List<Exercise>,
    muscleGroup: String?,
    onMuscleGroupChange: (String?) -> Unit,
    equipment: String?,
    onEquipmentChange: (String?) -> Unit,
) {
    val muscleGroups = remember(exercises) { exercises.map { it.category }.distinct().sorted() }
    // Only equipment used in the chosen muscle group; the selected one stays so it can be cleared.
    val equipmentOptions = remember(exercises, muscleGroup, equipment) {
        (exercises.filter { muscleGroup == null || it.category == muscleGroup }.flatMap { it.equipment } +
            listOfNotNull(equipment)).distinct().sorted()
    }
    // Scrolls sideways when the chips don't fit the width.
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        SheetFilterChip(stringResource(R.string.discovery_muscle_group), muscleGroup) { close ->
            MuscleGroupSheet(muscleGroups, muscleGroup, onMuscleGroupChange, close)
        }
        if (equipmentOptions.isNotEmpty()) {
            SheetFilterChip(stringResource(R.string.discovery_equipment), equipment) { close ->
                EquipmentSheet(equipmentOptions, exercises, equipment, onEquipmentChange, close)
            }
        }
    }
}

/** Section letter for the A–Z list: the first letter, or "#" for names starting with a digit or symbol. */
private fun String.indexLetter(): String =
    firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#"

/**
 * Segmented exercise list; [grouped] splits it into A–Z sections, each its own segmented group.
 * [favourites] and then [recent] come first, each a section of its own (their exercises also stay
 * in their letter's section).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseList(
    results: List<Exercise>,
    grouped: Boolean,
    listState: LazyListState,
    favourites: List<Exercise>,
    recent: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    trailing: (@Composable (Exercise) -> Unit)?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    if (results.isEmpty()) {
        EmptyState(rememberVectorPainter(Icons.Filled.Search), stringResource(R.string.discovery_no_results), modifier)
        return
    }
    // Favourites and Recently used arrive a moment after the list: when they do, it starts at the
    // top again (a list keeps its first item in place, which would leave them above, out of view).
    val topCount = favourites.size + recent.size
    LaunchedEffect(topCount > 0) {
        if (topCount > 0 && listState.firstVisibleItemIndex <= topCount + 2) listState.scrollToItem(0)
    }
    val groups = remember(results, grouped) {
        if (grouped) results.groupBy { it.name.indexLetter() } else mapOf("" to results)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.widthIn(max = Dimens.ListMaxWidth),
        // [bottomPadding]: the page's bottom bars, so results scroll right up to them.
        contentPadding = PaddingValues(top = Dimens.SpaceS, bottom = Dimens.SpaceS + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        // Their own keys: the same exercises are in the A–Z sections below too.
        listOf("favourite" to favourites, "recent" to recent).forEach { (section, list) ->
            if (list.isEmpty()) return@forEach
            item(key = section) {
                SectionTitle(stringResource(if (section == "favourite") R.string.discovery_favourites else R.string.discovery_recent))
            }
            itemsIndexed(list, key = { _, exercise -> "$section-${exercise.id}" }) { index, exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    index = index,
                    count = list.size,
                    onClick = { onExerciseClick(exercise) },
                    trailing = trailing?.let { { it(exercise) } },
                )
            }
        }
        groups.forEach { (letter, group) ->
            if (letter.isNotEmpty()) {
                item(key = "letter-$letter") { SectionTitle(letter) }
            }
            // Index and count within the group give each group its own rounded top and bottom.
            itemsIndexed(group, key = { _, exercise -> exercise.id }) { index, exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    index = index,
                    count = group.size,
                    onClick = { onExerciseClick(exercise) },
                    trailing = trailing?.let { { it(exercise) } },
                )
            }
        }
    }
}

/** A section's title in the list: a letter, Favourites or Recently used. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Dimens.SpaceS, top = Dimens.SpaceM, bottom = Dimens.SpaceXs),
    )
}
