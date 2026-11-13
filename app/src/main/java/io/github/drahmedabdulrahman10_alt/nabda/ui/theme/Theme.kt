package io.github.drahmedabdulrahman10_alt.nabda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val NabdaDark = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = OnEmeraldPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = OnGoldSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = OnGoldContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = OnTealTertiaryDark,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = OnTealContainerDark,
    background = BackgroundNightDark,
    onBackground = OnSurfaceLightText,
    surface = SurfaceNightDark,
    onSurface = OnSurfaceLightText,
    surfaceVariant = SurfaceVariantNightDark,
    onSurfaceVariant = OnSurfaceMutedDark
)

private val NabdaLight = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = OnEmeraldPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = OnGoldSecondaryLight,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = OnGoldContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = OnTealTertiaryLight,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = OnTealContainerLight,
    background = BackgroundParchmentLight,
    onBackground = OnSurfaceDarkText,
    surface = SurfaceIvoryLight,
    onSurface = OnSurfaceDarkText,
    surfaceVariant = SurfaceVariantWarmLight,
    onSurfaceVariant = OnSurfaceMutedText
)

/**
 * Builds a full colour scheme from the handful of colours a theme is designed with:
 * page [background], card [surface], [text], [muted] text, the [primary] accent and the text on it,
 * the [highlight] used for selected chips and the navigation pill (with [onHighlight]), and [line]s.
 */
private fun scheme(
    dark: Boolean,
    background: Long,
    surface: Long,
    text: Long,
    muted: Long,
    primary: Long,
    onPrimary: Long,
    highlight: Long,
    onHighlight: Long,
    line: Long
): ColorScheme {
    val bg = Color(background)
    val sf = Color(surface)
    val tx = Color(text)
    val pr = Color(primary)
    val primaryContainer = lerp(sf, pr, if (dark) 0.35f else 0.18f)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = pr,
        onPrimary = Color(onPrimary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = tx,
        inversePrimary = pr,
        secondary = Color(onHighlight),
        onSecondary = Color(highlight),
        secondaryContainer = Color(highlight),
        onSecondaryContainer = Color(onHighlight),
        tertiary = pr,
        onTertiary = Color(onPrimary),
        tertiaryContainer = primaryContainer,
        onTertiaryContainer = tx,
        background = bg,
        onBackground = tx,
        surface = sf,
        onSurface = tx,
        surfaceVariant = lerp(sf, Color(line), 0.6f),
        onSurfaceVariant = Color(muted),
        surfaceTint = pr,
        outline = Color(muted),
        outlineVariant = Color(line),
        surfaceContainerLowest = sf,
        surfaceContainerLow = sf,
        surfaceContainer = lerp(sf, bg, 0.5f),
        surfaceContainerHigh = lerp(sf, Color(line), 0.35f),
        surfaceContainerHighest = lerp(sf, Color(line), 0.6f)
    )
}

/** The colour themes the user can pick in Settings. Each one has a day and a night version. */
enum class AppTheme(val titleAr: String, val light: ColorScheme, val dark: ColorScheme) {
    NABDA("نَبضة", NabdaLight, NabdaDark),
    LEMONADE(
        "Lemonade",
        scheme(false, 0xFFF8FDEF, 0xFFFFFFFF, 0xFF1E2A10, 0xFF5E6B4F, 0xFF519903, 0xFFFFFFFF, 0xFFF4EFA0, 0xFF4A4A00, 0xFFE1E8CF),
        scheme(true, 0xFF141A0E, 0xFF1D2515, 0xFFEEF3E3, 0xFFA9B39A, 0xFF9BD34A, 0xFF1E2A00, 0xFF4A4A12, 0xFFF4EFA0, 0xFF2C3622)
    ),
    NORD(
        "Nord",
        scheme(false, 0xFFECEFF4, 0xFFFFFFFF, 0xFF2E3440, 0xFF4C566A, 0xFF5E81AC, 0xFFFFFFFF, 0xFFD8E3F0, 0xFF2E3440, 0xFFD8DEE9),
        scheme(true, 0xFF2E3440, 0xFF3B4252, 0xFFECEFF4, 0xFFA3ABBA, 0xFF88C0D0, 0xFF2E3440, 0xFF434C5E, 0xFFECEFF4, 0xFF4C566A)
    ),
    LOFI(
        "Lofi",
        scheme(false, 0xFFFFFFFF, 0xFFFAFAFA, 0xFF0D0D0D, 0xFF6B6B6B, 0xFF0D0D0D, 0xFFFFFFFF, 0xFFEBEBEB, 0xFF0D0D0D, 0xFFE3E3E3),
        scheme(true, 0xFF0A0A0A, 0xFF171717, 0xFFF2F2F2, 0xFF9A9A9A, 0xFFF2F2F2, 0xFF0A0A0A, 0xFF2A2A2A, 0xFFF2F2F2, 0xFF2A2A2A)
    ),
    PASTEL(
        "Pastel",
        scheme(false, 0xFFFFF8FB, 0xFFFFFFFF, 0xFF3A3346, 0xFF7A7287, 0xFFA98BC6, 0xFFFFFFFF, 0xFFF6CBD1, 0xFF5A2D3A, 0xFFEFE3EC),
        scheme(true, 0xFF1F1A26, 0xFF2A2433, 0xFFF3EDF7, 0xFFB6ADC2, 0xFFD1B8EA, 0xFF2E2140, 0xFF4A3346, 0xFFF6CBD1, 0xFF3A3346)
    ),
    GARDEN(
        "Garden",
        scheme(false, 0xFFF2F0EC, 0xFFFFFFFF, 0xFF1F2A22, 0xFF5E6A61, 0xFF5C7F67, 0xFFFFFFFF, 0xFFF6DDDD, 0xFF6B3A3A, 0xFFE0DDD6),
        scheme(true, 0xFF161B17, 0xFF1F2620, 0xFFE9EEE8, 0xFFA4AFA6, 0xFF9CC3A6, 0xFF17301E, 0xFF3E2F30, 0xFFF6DDDD, 0xFF2F3B31)
    );

    companion object {
        fun fromId(id: String?): AppTheme = entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: NABDA
    }
}

@Composable
fun NabdaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.NABDA,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) appTheme.dark else appTheme.light

    // Force Right-to-Left (RTL) layout direction for complete Arabic localization
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
