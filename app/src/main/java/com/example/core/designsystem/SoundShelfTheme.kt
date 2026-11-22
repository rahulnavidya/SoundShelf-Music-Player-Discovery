package com.example.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSansFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

@Immutable
data class SoundShelfTokens(
    val themeId: SoundShelfThemeId,
    val palette: SoundShelfPalette,
    val buttonGeometry: ButtonGeometry,
    val cardStyle: CardStyle,
    val artworkPresentation: ArtworkPresentation,
    val navigationStyle: NavigationStyle,
    val animationIntensity: AnimationIntensity,
    val density: InterfaceDensity,
    val itemSpacing: Dp,
    val cardCornerRadius: Dp,
    val artworkCornerRadius: Dp
)

val LocalSoundShelfTokens = staticCompositionLocalOf {
    val defaultPalette = SoundShelfPalettes.get(SoundShelfThemeId.MIDNIGHT_STUDIO)
    SoundShelfTokens(
        themeId = SoundShelfThemeId.MIDNIGHT_STUDIO,
        palette = defaultPalette,
        buttonGeometry = ButtonGeometry.ROUNDED,
        cardStyle = CardStyle.ELEVATED,
        artworkPresentation = ArtworkPresentation.ROUNDED,
        navigationStyle = NavigationStyle.STANDARD_BAR,
        animationIntensity = AnimationIntensity.FULL,
        density = InterfaceDensity.COMFORTABLE,
        itemSpacing = 12.dp,
        cardCornerRadius = 16.dp,
        artworkCornerRadius = 12.dp
    )
}

private fun fontFamilyFor(style: ThemeFontStyle): FontFamily = when (style) {
    ThemeFontStyle.MODERN_SANS -> PlusJakartaSansFamily
    ThemeFontStyle.EDITORIAL_SERIF -> PlayfairDisplayFamily
    ThemeFontStyle.TECH_GROTESK -> SpaceGroteskFamily
    ThemeFontStyle.STUDIO_MONO -> JetBrainsMonoFamily
}

fun buildSoundShelfTypography(displayStyle: ThemeFontStyle): Typography {
    val displayFamily = fontFamilyFor(displayStyle)
    val bodyFamily = if (displayStyle == ThemeFontStyle.STUDIO_MONO) JetBrainsMonoFamily else PlusJakartaSansFamily
    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable
fun SoundShelfTheme(
    themeId: SoundShelfThemeId = SoundShelfThemeId.MIDNIGHT_STUDIO,
    customizations: ThemeCustomizations = ThemeCustomizations(),
    content: @Composable () -> Unit
) {
    val basePalette = SoundShelfPalettes.get(themeId)
    val effectiveAccent = customizations.customAccentHex?.let { Color(it) } ?: basePalette.primary
    val effectiveBackground = customizations.customBackgroundHex?.let { Color(it) } ?: basePalette.background
    val isDark = effectiveBackground.luminance() < 0.45f

    val effectivePalette = basePalette.copy(
        isDark = isDark,
        background = effectiveBackground,
        primary = effectiveAccent,
        onPrimary = if (effectiveAccent.luminance() > 0.55f) Color(0xFF0E1015) else Color(0xFFFFFFFF),
        textPrimary = if (isDark) Color(0xFFF6F7FB) else Color(0xFF111827),
        textSecondary = if (isDark) Color(0xFF9EA8BC) else Color(0xFF4B5563),
        surface = if (customizations.customBackgroundHex != null) {
            if (isDark) effectiveBackground.copy(red = (effectiveBackground.red + 0.05f).coerceAtMost(1f), green = (effectiveBackground.green + 0.05f).coerceAtMost(1f), blue = (effectiveBackground.blue + 0.06f).coerceAtMost(1f))
            else Color.White
        } else basePalette.surface
    )

    val fontStyle = customizations.fontStyle ?: basePalette.defaultFont
    val buttonGeometry = customizations.buttonGeometry ?: basePalette.defaultButtonGeometry
    val cardStyle = customizations.cardStyle ?: basePalette.defaultCardStyle
    val artworkPresentation = customizations.artworkPresentation ?: basePalette.defaultArtworkPresentation

    val cardCorner = when (buttonGeometry) {
        ButtonGeometry.PILL -> 20.dp
        ButtonGeometry.ROUNDED -> 14.dp
        ButtonGeometry.SHARP -> 6.dp
    }
    val artworkCorner = when (artworkPresentation) {
        ArtworkPresentation.VINYL_DISC -> 100.dp
        ArtworkPresentation.ROUNDED -> 12.dp
        ArtworkPresentation.SHARP_FRAME -> 4.dp
    }
    val spacing = if (customizations.density == InterfaceDensity.COMPACT) 8.dp else 12.dp

    val colorScheme = if (effectivePalette.isDark) {
        darkColorScheme(
            primary = effectivePalette.primary,
            onPrimary = effectivePalette.onPrimary,
            primaryContainer = effectivePalette.primary.copy(alpha = 0.18f),
            onPrimaryContainer = effectivePalette.primary,
            secondary = effectivePalette.secondary,
            onSecondary = effectivePalette.onSecondary,
            secondaryContainer = effectivePalette.secondary.copy(alpha = 0.18f),
            onSecondaryContainer = effectivePalette.secondary,
            background = effectivePalette.background,
            onBackground = effectivePalette.textPrimary,
            surface = effectivePalette.surface,
            onSurface = effectivePalette.textPrimary,
            surfaceVariant = effectivePalette.surfaceElevated,
            onSurfaceVariant = effectivePalette.textSecondary,
            outline = effectivePalette.outline,
            outlineVariant = effectivePalette.outline.copy(alpha = 0.5f),
            error = Color(0xFFF87171),
            onError = Color(0xFF2D0404)
        )
    } else {
        lightColorScheme(
            primary = effectivePalette.primary,
            onPrimary = effectivePalette.onPrimary,
            primaryContainer = effectivePalette.primary.copy(alpha = 0.14f),
            onPrimaryContainer = effectivePalette.primary,
            secondary = effectivePalette.secondary,
            onSecondary = effectivePalette.onSecondary,
            secondaryContainer = effectivePalette.secondary.copy(alpha = 0.14f),
            onSecondaryContainer = effectivePalette.secondary,
            background = effectivePalette.background,
            onBackground = effectivePalette.textPrimary,
            surface = effectivePalette.surface,
            onSurface = effectivePalette.textPrimary,
            surfaceVariant = effectivePalette.surfaceElevated,
            onSurfaceVariant = effectivePalette.textSecondary,
            outline = effectivePalette.outline,
            outlineVariant = effectivePalette.outline.copy(alpha = 0.6f),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }

    val tokens = SoundShelfTokens(
        themeId = themeId,
        palette = effectivePalette,
        buttonGeometry = buttonGeometry,
        cardStyle = cardStyle,
        artworkPresentation = artworkPresentation,
        navigationStyle = customizations.navigationStyle,
        animationIntensity = customizations.animationIntensity,
        density = customizations.density,
        itemSpacing = spacing,
        cardCornerRadius = cardCorner,
        artworkCornerRadius = artworkCorner
    )

    val shapes = Shapes(
        small = RoundedCornerShape(buttonGeometry.cornerDp.coerceAtMost(12).dp),
        medium = RoundedCornerShape(cardCorner),
        large = RoundedCornerShape((cardCorner + 4.dp))
    )

    CompositionLocalProvider(LocalSoundShelfTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildSoundShelfTypography(fontStyle),
            shapes = shapes,
            content = content
        )
    }
}
