package io.github.dimitrysaf.asko.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.dimitrysaf.asko.R

/**
 * Google Sans Flex, the variable font of Material 3 Expressive (SIL OFL, license in
 * assets/licenses). One file; each weight below is an instance of its weight axis.
 */
@OptIn(ExperimentalTextApi::class)
private val GoogleSansFlex = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
        Font(
            resId = R.font.google_sans_flex,
            weight = it,
            variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)),
        )
    },
)

private fun TextStyle.flex() = copy(fontFamily = GoogleSansFlex)

private val Default = Typography()

/**
 * Material's type scale (sizes, weights, spacing) in Google Sans Flex. Use the roles
 * (display, headline, title, body, label) and keep the *Emphasized styles for the one or
 * two things on a screen that should stand out, like a stat number or the main heading.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AskoTypography = Typography(
    displayLarge = Default.displayLarge.flex(),
    displayMedium = Default.displayMedium.flex(),
    displaySmall = Default.displaySmall.flex(),
    headlineLarge = Default.headlineLarge.flex(),
    headlineMedium = Default.headlineMedium.flex(),
    headlineSmall = Default.headlineSmall.flex(),
    titleLarge = Default.titleLarge.flex(),
    titleMedium = Default.titleMedium.flex(),
    titleSmall = Default.titleSmall.flex(),
    bodyLarge = Default.bodyLarge.flex(),
    bodyMedium = Default.bodyMedium.flex(),
    bodySmall = Default.bodySmall.flex(),
    labelLarge = Default.labelLarge.flex(),
    labelMedium = Default.labelMedium.flex(),
    labelSmall = Default.labelSmall.flex(),
    displayLargeEmphasized = Default.displayLargeEmphasized.flex(),
    displayMediumEmphasized = Default.displayMediumEmphasized.flex(),
    displaySmallEmphasized = Default.displaySmallEmphasized.flex(),
    headlineLargeEmphasized = Default.headlineLargeEmphasized.flex(),
    headlineMediumEmphasized = Default.headlineMediumEmphasized.flex(),
    headlineSmallEmphasized = Default.headlineSmallEmphasized.flex(),
    titleLargeEmphasized = Default.titleLargeEmphasized.flex(),
    titleMediumEmphasized = Default.titleMediumEmphasized.flex(),
    titleSmallEmphasized = Default.titleSmallEmphasized.flex(),
    bodyLargeEmphasized = Default.bodyLargeEmphasized.flex(),
    bodyMediumEmphasized = Default.bodyMediumEmphasized.flex(),
    bodySmallEmphasized = Default.bodySmallEmphasized.flex(),
    labelLargeEmphasized = Default.labelLargeEmphasized.flex(),
    labelMediumEmphasized = Default.labelMediumEmphasized.flex(),
    labelSmallEmphasized = Default.labelSmallEmphasized.flex(),
)
