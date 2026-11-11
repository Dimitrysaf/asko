package io.github.dimitrysaf.asko.components

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement
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
 * Search field, Muscle group / Equipment dropdown chips and a segmented exercise list (A–Z sections
 * while browsing, best match first while searching). Used by Discovery and the routine exercise
 * picker; [trailing] adds per-row content such as a checkbox.
 */
@Composable
fun ExerciseBrowser(
    exercises: List<Exercise>,
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable (Exercise) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscleGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    val results = remember(exercises, query, muscleGroup, equipment) {
        exercises.matching(query, muscleGroup, equipment)
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
            listState = key(query, muscleGroup, equipment) { rememberLazyListState() },
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
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_clear_search))
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
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        DropdownFilterChip(
            label = stringResource(R.string.discovery_muscle_group),
            anyLabel = stringResource(R.string.discovery_any_muscle_group),
            options = muscleGroups,
            selected = muscleGroup,
            onSelect = onMuscleGroupChange,
        )
        if (equipmentOptions.isNotEmpty()) {
            DropdownFilterChip(
                label = stringResource(R.string.discovery_equipment),
                anyLabel = stringResource(R.string.discovery_any_equipment),
                options = equipmentOptions,
                selected = equipment,
                onSelect = onEquipmentChange,
            )
        }
    }
}

/** Section letter for the A–Z list: the first letter, or "#" for names starting with a digit or symbol. */
private fun String.indexLetter(): String =
    firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#"

/** Segmented exercise list; [grouped] splits it into A–Z sections, each its own segmented group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExerciseList(
    results: List<Exercise>,
    grouped: Boolean,
    listState: LazyListState,
    onExerciseClick: (Exercise) -> Unit,
    trailing: (@Composable (Exercise) -> Unit)?,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    if (results.isEmpty()) {
        EmptyState(rememberVectorPainter(Icons.Filled.Search), stringResource(R.string.discovery_no_results), modifier)
        return
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
        groups.forEach { (letter, group) ->
            if (letter.isNotEmpty()) {
                item(key = "letter-$letter") {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = Dimens.SpaceS, top = Dimens.SpaceM, bottom = Dimens.SpaceXs),
                    )
                }
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
