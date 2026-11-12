package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import io.github.dimitrysaf.asko.constants.Dimens
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R

/** Every page's frame: a large collapsing expressive top bar, optional back button, [actions] and FAB. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = subtitle?.let { { Text(it) } },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        content = content,
    )
}

/**
 * A page list's content padding: the scaffold's [padding] (top bar, insets) plus the page gutter.
 * Given to the list as content padding instead of padding the list, it draws edge to edge and its
 * items scroll right up to the bars, rather than being cut off short of them.
 */
fun pageContentPadding(padding: PaddingValues, top: Dp = Dimens.PageGutter): PaddingValues = PaddingValues(
    start = Dimens.PageGutter,
    end = Dimens.PageGutter,
    top = padding.calculateTopPadding() + top,
    bottom = padding.calculateBottomPadding() + Dimens.PageGutter,
)

/**
 * For a page's content that doesn't scroll, like a loading skeleton: the scaffold's top padding
 * and the gutter, and nothing at the bottom, so it runs down to the bars like the lists do.
 */
fun Modifier.pageSkeletonPadding(padding: PaddingValues): Modifier = consumeWindowInsets(padding).padding(
    start = Dimens.PageGutter,
    end = Dimens.PageGutter,
    top = padding.calculateTopPadding() + Dimens.PageGutter,
)

