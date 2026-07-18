package com.cortex.dl.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.kyant.monet.TonalPalettes
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import io.material.hct.Hct

// ---------------------------------------------------------------------------
// LocalFixedColorRoles – a thin CompositionLocal replacing the deleted
// ui.common.LocalFixedColorRoles so that any surviving Composable that needs
// fixed (non-dynamic) color roles can still compile.
// ---------------------------------------------------------------------------

val LocalFixedColorRoles = compositionLocalOf { FixedColorRoles.unspecified }

// ---------------------------------------------------------------------------
// autoDark helper – formerly depended on LocalDarkTheme from the deleted
// ui.common package. Since the new MVP always uses a dark theme the value is
// always `true`; the full mapping is kept for completeness.
// ---------------------------------------------------------------------------

@Composable
fun Number.autoDark(isDarkTheme: Boolean = true): Double =
    if (!isDarkTheme) this.toDouble()
    else
        when (this.toDouble()) {
            6.0 -> 98.0
            10.0 -> 99.0
            20.0 -> 95.0
            25.0 -> 90.0
            30.0 -> 90.0
            40.0 -> 80.0
            50.0 -> 60.0
            60.0 -> 50.0
            70.0 -> 40.0
            80.0 -> 40.0
            90.0 -> 30.0
            95.0 -> 20.0
            98.0 -> 10.0
            99.0 -> 10.0
            100.0 -> 20.0
            else -> this.toDouble()
        }

// ---------------------------------------------------------------------------
// FixedAccentColors – delegates to LocalFixedColorRoles (now in ui.theme).
// Kept so that vector drawables like Coder.kt continue to compile without
// changes to their fill colours.
// ---------------------------------------------------------------------------
object FixedAccentColors {
    val primaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixed

    val primaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.primaryFixedDim

    val onPrimaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixed

    val onPrimaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onPrimaryFixedVariant

    val secondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixed

    val secondaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.secondaryFixedDim

    val onSecondaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixed

    val onSecondaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onSecondaryFixedVariant

    val tertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixed

    val tertiaryFixedDim: Color
        @Composable get() = LocalFixedColorRoles.current.tertiaryFixedDim

    val onTertiaryFixed: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixed

    val onTertiaryFixedVariant: Color
        @Composable get() = LocalFixedColorRoles.current.onTertiaryFixedVariant
}

@Immutable
data class FixedColorRoles(
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondaryFixed: Color,
    val secondaryFixedDim: Color,
    val onSecondaryFixed: Color,
    val onSecondaryFixedVariant: Color,
    val tertiaryFixed: Color,
    val tertiaryFixedDim: Color,
    val onTertiaryFixed: Color,
    val onTertiaryFixedVariant: Color,
) {
    companion object {
        internal val unspecified =
            FixedColorRoles(
                primaryFixed = Color.Unspecified,
                primaryFixedDim = Color.Unspecified,
                onPrimaryFixed = Color.Unspecified,
                onPrimaryFixedVariant = Color.Unspecified,
                secondaryFixed = Color.Unspecified,
                secondaryFixedDim = Color.Unspecified,
                onSecondaryFixed = Color.Unspecified,
                onSecondaryFixedVariant = Color.Unspecified,
                tertiaryFixed = Color.Unspecified,
                tertiaryFixedDim = Color.Unspecified,
                onTertiaryFixed = Color.Unspecified,
                onTertiaryFixedVariant = Color.Unspecified,
            )

        @Stable
        internal fun fromTonalPalettes(palettes: TonalPalettes): FixedColorRoles {
            return with(palettes) {
                FixedColorRoles(
                    primaryFixed = accent1(90.toDouble()),
                    primaryFixedDim = accent1(80.toDouble()),
                    onPrimaryFixed = accent1(10.toDouble()),
                    onPrimaryFixedVariant = accent1(30.toDouble()),
                    secondaryFixed = accent2(90.toDouble()),
                    secondaryFixedDim = accent2(80.toDouble()),
                    onSecondaryFixed = accent2(10.toDouble()),
                    onSecondaryFixedVariant = accent2(30.toDouble()),
                    tertiaryFixed = accent3(90.toDouble()),
                    tertiaryFixedDim = accent3(80.toDouble()),
                    onTertiaryFixed = accent3(10.toDouble()),
                    onTertiaryFixedVariant = accent3(30.toDouble()),
                )
            }
        }

        @Stable
        internal fun fromColorSchemes(
            lightColors: ColorScheme,
            darkColors: ColorScheme,
        ): FixedColorRoles {
            return FixedColorRoles(
                primaryFixed = lightColors.primaryContainer,
                onPrimaryFixed = lightColors.onPrimaryContainer,
                onPrimaryFixedVariant = darkColors.primaryContainer,
                secondaryFixed = lightColors.secondaryContainer,
                onSecondaryFixed = lightColors.onSecondaryContainer,
                onSecondaryFixedVariant = darkColors.secondaryContainer,
                tertiaryFixed = lightColors.tertiaryContainer,
                onTertiaryFixed = lightColors.onTertiaryContainer,
                onTertiaryFixedVariant = darkColors.tertiaryContainer,
                primaryFixedDim = darkColors.primary,
                secondaryFixedDim = darkColors.secondary,
                tertiaryFixedDim = darkColors.tertiary,
            )
        }
    }
}

const val DEFAULT_SEED_COLOR = 0xa3d48d

/**
 * @return a [Color] generated using [Hct] algorithm, harmonized with `primary` color
 * @receiver Seed number used for generating color
 */
@Composable
@ReadOnlyComposable
fun Int.generateLabelColor(): Color =
    Color(Hct.from(hue = (this % 360).toDouble(), chroma = 36.0, tone = 80.0).toInt())
        .harmonizeWithPrimary()

/**
 * @return a [Color] generated using [Hct] algorithm, harmonized with `primary` color
 * @receiver Seed number used for generating color
 */
@Composable
@ReadOnlyComposable
fun Int.generateOnLabelColor(): Color =
    Color(Hct.from(hue = (this % 360).toDouble(), chroma = 36.0, tone = 20.0).toInt())
        .harmonizeWithPrimary()

val ErrorTonalPalettes = Color.Red.toTonalPalettes()

// ---------------------------------------------------------------------------
// harmonizeWithPrimary – extension used by generateLabelColor /
// generateOnLabelColor. Uses MaterialColors.harmonize which correctly blends
// the color toward the primary hue.
// ---------------------------------------------------------------------------
@Composable
@ReadOnlyComposable
fun Color.harmonizeWithPrimary(): Color {
    val argb = android.graphics.Color.argb(
        (this.alpha * 255).toInt(),
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
    )
    val primaryArgb = MaterialTheme.colorScheme.primary.let { p ->
        android.graphics.Color.argb(
            (p.alpha * 255).toInt(),
            (p.red * 255).toInt(),
            (p.green * 255).toInt(),
            (p.blue * 255).toInt(),
        )
    }
    return Color(com.google.android.material.color.MaterialColors.harmonize(argb, primaryArgb))
}
