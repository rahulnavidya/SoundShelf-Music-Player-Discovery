package com.example.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class SoundShelfThemeId(
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean
) {
    MIDNIGHT_STUDIO("Midnight Studio", "Obsidian, warm gold & analog precision", true),
    PURE_AMOLED("Pure AMOLED", "True #000000 black with crisp cyan", true),
    LIQUID_GLASS("Liquid Glass", "Translucent midnight indigo & ice highlights", true),
    GLASSMORPHISM("Glassmorphism", "Frosted deep teal layers & emerald glow", true),
    EDITORIAL("Editorial", "Warm espresso ink, ivory & classic serif", true),
    RETRO_VINYL("Retro Vinyl", "Walnut wood, aged cream & brass grooves", true),
    CYBERPUNK("Cyberpunk", "Night void, acid yellow & neon magenta", true),
    SYNTHWAVE("Synthwave", "Deep violet horizon, sunset coral & laser cyan", true),
    MINIMAL_WHITE("Minimal White", "Gallery paper light theme & cobalt ink", false),
    WARM_ANALOG("Warm Analog", "Vacuum tube amber & brushed copper", true),
    OCEAN("Ocean", "Abyssal navy & bioluminescent seafoam", true),
    NEUMORPHISM("Neumorphism", "Soft tactile slate & dual-tone depth", true),
    MODERN_BROADCAST("Modern Broadcast", "Studio graphite & crisp on-air crimson", true),
    PASTEL_SOUND("Pastel Sound", "Soft lavender dusk & gentle rose", true),
    MONOCHROME("Monochrome", "Stark silver & pure architectural contrast", true)
}

@Serializable
enum class ThemeFontStyle(val label: String) {
    MODERN_SANS("Plus Jakarta Sans"),
    EDITORIAL_SERIF("Playfair Display"),
    TECH_GROTESK("Space Grotesk"),
    STUDIO_MONO("JetBrains Mono")
}

@Serializable
enum class ButtonGeometry(val label: String, val cornerDp: Int) {
    PILL("Pill", 50),
    ROUNDED("Rounded", 14),
    SHARP("Architectural", 4)
}

@Serializable
enum class CardStyle(val label: String) {
    ELEVATED("Elevated"),
    GLASS("Frosted Glass"),
    OUTLINED("Crisp Outlined"),
    FLAT("Minimal Flat")
}

@Serializable
enum class ArtworkPresentation(val label: String) {
    ROUNDED("Soft Rounded"),
    VINYL_DISC("Vinyl Disc"),
    SHARP_FRAME("Sharp Sleeve")
}

@Serializable
enum class NavigationStyle(val label: String) {
    STANDARD_BAR("Bottom Bar"),
    FLOATING_PILL("Floating Pill")
}

@Serializable
enum class AnimationIntensity(val label: String) {
    FULL("Expressive"),
    SUBTLE("Subtle"),
    MINIMAL("Reduced Motion")
}

@Serializable
enum class InterfaceDensity(val label: String) {
    COMFORTABLE("Comfortable"),
    COMPACT("Compact")
}

@Serializable
enum class NowPlayingLayoutMode(val label: String, val description: String) {
    CLASSIC("Classic Player", "Balanced artwork, metadata & precision controls"),
    MINIMAL("Minimal Player", "Oversized artwork with restrained typography"),
    ARTWORK("Artwork Player", "Full-bleed immersive cover with extracted glow"),
    LYRICS("Lyrics Player", "Synchronized lyric stream takes centre stage"),
    AMOLED("AMOLED Player", "Pure pitch-black canvas with high-contrast controls")
}

@Serializable
data class ThemeCustomizations(
    val customAccentHex: Long? = null,
    val customBackgroundHex: Long? = null,
    val fontStyle: ThemeFontStyle? = null,
    val buttonGeometry: ButtonGeometry? = null,
    val cardStyle: CardStyle? = null,
    val artworkPresentation: ArtworkPresentation? = null,
    val navigationStyle: NavigationStyle = NavigationStyle.STANDARD_BAR,
    val animationIntensity: AnimationIntensity = AnimationIntensity.FULL,
    val density: InterfaceDensity = InterfaceDensity.COMFORTABLE
)

data class SoundShelfPalette(
    val id: SoundShelfThemeId,
    val isDark: Boolean,
    val background: Color,
    val backgroundGradientEnd: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceGlass: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val defaultFont: ThemeFontStyle,
    val defaultButtonGeometry: ButtonGeometry,
    val defaultCardStyle: CardStyle,
    val defaultArtworkPresentation: ArtworkPresentation
)

object SoundShelfPalettes {
    val all: Map<SoundShelfThemeId, SoundShelfPalette> = mapOf(
        SoundShelfThemeId.MIDNIGHT_STUDIO to SoundShelfPalette(
            id = SoundShelfThemeId.MIDNIGHT_STUDIO,
            isDark = true,
            background = Color(0xFF0B0D13),
            backgroundGradientEnd = Color(0xFF161A26),
            surface = Color(0xFF141822),
            surfaceElevated = Color(0xFF1D2332),
            surfaceGlass = Color(0xCC1A202E),
            primary = Color(0xFFF2B84B),
            onPrimary = Color(0xFF181205),
            secondary = Color(0xFFFF6B4A),
            onSecondary = Color(0xFF1D0703),
            textPrimary = Color(0xFFF4F5F7),
            textSecondary = Color(0xFF9BA3B5),
            outline = Color(0xFF2B3245),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.ELEVATED,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.PURE_AMOLED to SoundShelfPalette(
            id = SoundShelfThemeId.PURE_AMOLED,
            isDark = true,
            background = Color(0xFF000000),
            backgroundGradientEnd = Color(0xFF05070A),
            surface = Color(0xFF080A0F),
            surfaceElevated = Color(0xFF10141D),
            surfaceGlass = Color(0xCC0A0D12),
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF001824),
            secondary = Color(0xFF34D399),
            onSecondary = Color(0xFF002014),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF8E9AAF),
            outline = Color(0xFF1E2636),
            defaultFont = ThemeFontStyle.TECH_GROTESK,
            defaultButtonGeometry = ButtonGeometry.PILL,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.LIQUID_GLASS to SoundShelfPalette(
            id = SoundShelfThemeId.LIQUID_GLASS,
            isDark = true,
            background = Color(0xFF090E1C),
            backgroundGradientEnd = Color(0xFF151E3B),
            surface = Color(0xFF121A31),
            surfaceElevated = Color(0xFF1A2544),
            surfaceGlass = Color(0x9923325B),
            primary = Color(0xFF60A5FA),
            onPrimary = Color(0xFF07162E),
            secondary = Color(0xFFA78BFA),
            onSecondary = Color(0xFF170B33),
            textPrimary = Color(0xFFF0F6FF),
            textSecondary = Color(0xFFA5B4D4),
            outline = Color(0x4D93C5FD),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.PILL,
            defaultCardStyle = CardStyle.GLASS,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.GLASSMORPHISM to SoundShelfPalette(
            id = SoundShelfThemeId.GLASSMORPHISM,
            isDark = true,
            background = Color(0xFF0A161B),
            backgroundGradientEnd = Color(0xFF112932),
            surface = Color(0xFF12232B),
            surfaceElevated = Color(0xFF19303A),
            surfaceGlass = Color(0x991E3A46),
            primary = Color(0xFF2DD4BF),
            onPrimary = Color(0xFF03201C),
            secondary = Color(0xFFFB923C),
            onSecondary = Color(0xFF291103),
            textPrimary = Color(0xFFECFDFB),
            textSecondary = Color(0xFF9AC2C9),
            outline = Color(0x4D5EEAD4),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.GLASS,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.EDITORIAL to SoundShelfPalette(
            id = SoundShelfThemeId.EDITORIAL,
            isDark = true,
            background = Color(0xFF151210),
            backgroundGradientEnd = Color(0xFF221C18),
            surface = Color(0xFF1E1916),
            surfaceElevated = Color(0xFF28211C),
            surfaceGlass = Color(0xCC28211C),
            primary = Color(0xFFE07A5F),
            onPrimary = Color(0xFF230B05),
            secondary = Color(0xFFF2CC8F),
            onSecondary = Color(0xFF241A08),
            textPrimary = Color(0xFFF7F1E8),
            textSecondary = Color(0xFFB5A99B),
            outline = Color(0xFF3C322B),
            defaultFont = ThemeFontStyle.EDITORIAL_SERIF,
            defaultButtonGeometry = ButtonGeometry.SHARP,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.SHARP_FRAME
        ),
        SoundShelfThemeId.RETRO_VINYL to SoundShelfPalette(
            id = SoundShelfThemeId.RETRO_VINYL,
            isDark = true,
            background = Color(0xFF19130E),
            backgroundGradientEnd = Color(0xFF2A1F16),
            surface = Color(0xFF241B14),
            surfaceElevated = Color(0xFF31241B),
            surfaceGlass = Color(0xCC31241B),
            primary = Color(0xFFD99B26),
            onPrimary = Color(0xFF241702),
            secondary = Color(0xFFE6CCB2),
            onSecondary = Color(0xFF291D12),
            textPrimary = Color(0xFFF5ECE0),
            textSecondary = Color(0xFFC2B29F),
            outline = Color(0xFF473729),
            defaultFont = ThemeFontStyle.EDITORIAL_SERIF,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.ELEVATED,
            defaultArtworkPresentation = ArtworkPresentation.VINYL_DISC
        ),
        SoundShelfThemeId.CYBERPUNK to SoundShelfPalette(
            id = SoundShelfThemeId.CYBERPUNK,
            isDark = true,
            background = Color(0xFF090912),
            backgroundGradientEnd = Color(0xFF1B0C26),
            surface = Color(0xFF121222),
            surfaceElevated = Color(0xFF1B1B32),
            surfaceGlass = Color(0xCC1E1433),
            primary = Color(0xFFFACC15),
            onPrimary = Color(0xFF1C1600),
            secondary = Color(0xFFEC4899),
            onSecondary = Color(0xFF2B0417),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFA79FC4),
            outline = Color(0xFF4B2163),
            defaultFont = ThemeFontStyle.TECH_GROTESK,
            defaultButtonGeometry = ButtonGeometry.SHARP,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.SHARP_FRAME
        ),
        SoundShelfThemeId.SYNTHWAVE to SoundShelfPalette(
            id = SoundShelfThemeId.SYNTHWAVE,
            isDark = true,
            background = Color(0xFF120821),
            backgroundGradientEnd = Color(0xFF291042),
            surface = Color(0xFF1C0E32),
            surfaceElevated = Color(0xFF281547),
            surfaceGlass = Color(0xB32B154D),
            primary = Color(0xFFFF5E7E),
            onPrimary = Color(0xFF2B030C),
            secondary = Color(0xFF22D3EE),
            onSecondary = Color(0xFF032127),
            textPrimary = Color(0xFFFAF5FF),
            textSecondary = Color(0xFFC7B2DF),
            outline = Color(0xFF4D2B78),
            defaultFont = ThemeFontStyle.TECH_GROTESK,
            defaultButtonGeometry = ButtonGeometry.PILL,
            defaultCardStyle = CardStyle.GLASS,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.MINIMAL_WHITE to SoundShelfPalette(
            id = SoundShelfThemeId.MINIMAL_WHITE,
            isDark = false,
            background = Color(0xFFF8F9FA),
            backgroundGradientEnd = Color(0xFFECEFF4),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF1F3F6),
            surfaceGlass = Color(0xD9FFFFFF),
            primary = Color(0xFF1D4ED8),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF0F766E),
            onSecondary = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF525D70),
            outline = Color(0xFFD6DCE5),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.SHARP_FRAME
        ),
        SoundShelfThemeId.WARM_ANALOG to SoundShelfPalette(
            id = SoundShelfThemeId.WARM_ANALOG,
            isDark = true,
            background = Color(0xFF171311),
            backgroundGradientEnd = Color(0xFF251B14),
            surface = Color(0xFF211A16),
            surfaceElevated = Color(0xFF2E241E),
            surfaceGlass = Color(0xCC2E241E),
            primary = Color(0xFFF59E0B),
            onPrimary = Color(0xFF261600),
            secondary = Color(0xFFFB7185),
            onSecondary = Color(0xFF29060C),
            textPrimary = Color(0xFFFAF3EB),
            textSecondary = Color(0xFFC2B2A3),
            outline = Color(0xFF42342A),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.ELEVATED,
            defaultArtworkPresentation = ArtworkPresentation.VINYL_DISC
        ),
        SoundShelfThemeId.OCEAN to SoundShelfPalette(
            id = SoundShelfThemeId.OCEAN,
            isDark = true,
            background = Color(0xFF061520),
            backgroundGradientEnd = Color(0xFF0B273A),
            surface = Color(0xFF0C202E),
            surfaceElevated = Color(0xFF122C3F),
            surfaceGlass = Color(0xB3123146),
            primary = Color(0xFF14B8A6),
            onPrimary = Color(0xFF02201C),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF031C29),
            textPrimary = Color(0xFFEBF9FC),
            textSecondary = Color(0xFF8DB4C7),
            outline = Color(0xFF1D4259),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.PILL,
            defaultCardStyle = CardStyle.GLASS,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.NEUMORPHISM to SoundShelfPalette(
            id = SoundShelfThemeId.NEUMORPHISM,
            isDark = true,
            background = Color(0xFF1B1F28),
            backgroundGradientEnd = Color(0xFF222733),
            surface = Color(0xFF232834),
            surfaceElevated = Color(0xFF2B3140),
            surfaceGlass = Color(0xCC2B3140),
            primary = Color(0xFF818CF8),
            onPrimary = Color(0xFF0C0F2E),
            secondary = Color(0xFF6EE7B7),
            onSecondary = Color(0xFF032419),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            outline = Color(0xFF363D4F),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.ROUNDED,
            defaultCardStyle = CardStyle.ELEVATED,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.MODERN_BROADCAST to SoundShelfPalette(
            id = SoundShelfThemeId.MODERN_BROADCAST,
            isDark = true,
            background = Color(0xFF121417),
            backgroundGradientEnd = Color(0xFF1B1E23),
            surface = Color(0xFF191C21),
            surfaceElevated = Color(0xFF23272F),
            surfaceGlass = Color(0xCC23272F),
            primary = Color(0xFFEF4444),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFFE5E7EB),
            onSecondary = Color(0xFF111827),
            textPrimary = Color(0xFFF9FAFB),
            textSecondary = Color(0xFF9CA3AF),
            outline = Color(0xFF323843),
            defaultFont = ThemeFontStyle.TECH_GROTESK,
            defaultButtonGeometry = ButtonGeometry.SHARP,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.SHARP_FRAME
        ),
        SoundShelfThemeId.PASTEL_SOUND to SoundShelfPalette(
            id = SoundShelfThemeId.PASTEL_SOUND,
            isDark = true,
            background = Color(0xFF1D1A2B),
            backgroundGradientEnd = Color(0xFF2A243D),
            surface = Color(0xFF252136),
            surfaceElevated = Color(0xFF302B47),
            surfaceGlass = Color(0xB335304F),
            primary = Color(0xFFC4B5FD),
            onPrimary = Color(0xFF1B1238),
            secondary = Color(0xFFFDA4AF),
            onSecondary = Color(0xFF380912),
            textPrimary = Color(0xFFFAF5FF),
            textSecondary = Color(0xFFBDB3D6),
            outline = Color(0xFF423B61),
            defaultFont = ThemeFontStyle.MODERN_SANS,
            defaultButtonGeometry = ButtonGeometry.PILL,
            defaultCardStyle = CardStyle.ELEVATED,
            defaultArtworkPresentation = ArtworkPresentation.ROUNDED
        ),
        SoundShelfThemeId.MONOCHROME to SoundShelfPalette(
            id = SoundShelfThemeId.MONOCHROME,
            isDark = true,
            background = Color(0xFF101010),
            backgroundGradientEnd = Color(0xFF1A1A1A),
            surface = Color(0xFF171717),
            surfaceElevated = Color(0xFF222222),
            surfaceGlass = Color(0xCC222222),
            primary = Color(0xFFF5F5F5),
            onPrimary = Color(0xFF0A0A0A),
            secondary = Color(0xFFA3A3A3),
            onSecondary = Color(0xFF0A0A0A),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA3A3A3),
            outline = Color(0xFF333333),
            defaultFont = ThemeFontStyle.STUDIO_MONO,
            defaultButtonGeometry = ButtonGeometry.SHARP,
            defaultCardStyle = CardStyle.OUTLINED,
            defaultArtworkPresentation = ArtworkPresentation.SHARP_FRAME
        )
    )

    fun get(id: SoundShelfThemeId): SoundShelfPalette =
        all[id] ?: all.getValue(SoundShelfThemeId.MIDNIGHT_STUDIO)
}
