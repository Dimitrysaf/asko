package io.github.dimitrysaf.asko.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene

/*
 * Page transitions: slides only, no fading or scaling, and quick.
 *
 *   Opening a page    it comes in from the right, over the page it leaves (which drifts left a little)
 *   Going back        the page slides off to the right, uncovering the one under it
 *   Back gesture      the page follows the finger to the right; let go and it finishes sliding off
 *                     (or slides back if the gesture is cancelled)
 *   Switching tabs    the pages slide left or right, by the tabs' order
 */

/** How long a page takes to slide. */
private const val SLIDE_MS = 320

/** How far the page underneath drifts, as a share of the width (a parallax, as on Android). */
private const val UNDER_SHIFT = 4

private val slide = tween<IntOffset>(SLIDE_MS, easing = FastOutSlowInEasing)

/** A new page comes in from the right over the current one. */
fun <T : Any> slidePushTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { it },
        initialContentExit = slideOutHorizontally(slide) { -it / UNDER_SHIFT },
    )
}

/** Back (the back button or a page's own back): the page slides off to the right, on top of the one it uncovers. */
fun <T : Any> slidePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { -it / UNDER_SHIFT },
        initialContentExit = slideOutHorizontally(slide) { it },
        targetContentZIndex = -1f,
    )
}

/**
 * The back gesture. NavDisplay seeks this transition with the gesture's progress, so the page
 * moves exactly with the finger (linear: no easing between finger and page); on release it plays
 * the rest, finishing the slide off.
 */
fun <T : Any> slidePredictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
    val follow = tween<IntOffset>(SLIDE_MS, easing = LinearEasing)
    ContentTransform(
        targetContentEnter = slideInHorizontally(follow) { -it / UNDER_SHIFT },
        initialContentExit = slideOutHorizontally(follow) { it },
        targetContentZIndex = -1f,
    )
}

/**
 * Switching tabs: the new tab slides in from the side it sits on ([direction] 1: a tab to the
 * right, -1: to the left), pushing the old one out the other side.
 */
fun <T : Any> slideTabTransitionSpec(direction: Int): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { it * direction },
        initialContentExit = slideOutHorizontally(slide) { -it * direction },
    )
}
