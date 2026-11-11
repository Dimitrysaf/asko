package io.github.dimitrysaf.asko.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation3.scene.Scene

/*
 * Android's predictive back spec for full-screen in-app surfaces
 * (developer.android.com/design/ui/mobile/guides/patterns/predictive-back):
 *
 *   Exit scale   100% → 90%
 *   Enter scale  110% → 100%
 *   Exit fade    100% → 0%,  reaches 0 at 35% of the gesture
 *   Enter fade     0% → 100%, starts at 35% of the gesture
 *   Interpolator (.1, .1, 0, 1)
 *
 * NavDisplay seeks this transition with the gesture's progress, so the timeline below *is* the
 * gesture: 0 ms = gesture start, TIMELINE_MS = fully dragged. On release or cancel NavDisplay
 * plays the rest of the same timeline, so it is kept short to make that "swift".
 */
private const val TIMELINE_MS = 400
private const val FADE_THROUGH_MS = TIMELINE_MS * 35 / 100

private val SystemUiInterpolator = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

fun <T : Any> materialPredictivePopTransitionSpec(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
    ContentTransform(
        targetContentEnter =
            scaleIn(tween(TIMELINE_MS, easing = SystemUiInterpolator), initialScale = 1.1f) +
                fadeIn(tween(TIMELINE_MS - FADE_THROUGH_MS, delayMillis = FADE_THROUGH_MS, easing = LinearEasing)),
        initialContentExit =
            scaleOut(tween(TIMELINE_MS, easing = SystemUiInterpolator), targetScale = 0.9f) +
                fadeOut(tween(FADE_THROUGH_MS, easing = LinearEasing)),
    )
}
