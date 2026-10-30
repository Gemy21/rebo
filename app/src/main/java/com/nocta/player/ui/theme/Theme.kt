package com.nocta.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.player.R
import com.nocta.player.data.DefaultAccent
import com.nocta.player.data.DefaultAccentSoft

val NoctaBlack = Color(0xFF000000)
val NoctaInk = Color(0xFF07070A)
val NoctaText = Color(0xFFF7F6FA)
val NoctaMuted = Color(0xFF9E9BAA)
val NoctaFaint = Color(0xFF6B6876)

val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold)
)

data class AccentPalette(
    val primary: Color,
    val soft: Color
)

val LocalAccent = staticCompositionLocalOf {
    AccentPalette(DefaultAccent, DefaultAccentSoft)
}

private val NoctaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.4.sp
    )
)

private val NoctaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun NoctaTheme(
    accent: Color = DefaultAccent,
    accentSoft: Color = DefaultAccentSoft,
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.22f),
        onPrimaryContainer = Color.White,
        secondary = accentSoft,
        onSecondary = Color.Black,
        background = NoctaBlack,
        onBackground = NoctaText,
        surface = NoctaBlack,
        onSurface = NoctaText,
        surfaceVariant = Color(0xFF141317),
        onSurfaceVariant = NoctaMuted,
        outline = Color(0xFF3A3844),
        outlineVariant = Color(0xFF1E1D23),
        error = Color(0xFFFF6B81),
        onError = Color.Black
    )

    CompositionLocalProvider(LocalAccent provides AccentPalette(accent, accentSoft)) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NoctaTypography,
            shapes = NoctaShapes,
            content = content
        )
    }
}
