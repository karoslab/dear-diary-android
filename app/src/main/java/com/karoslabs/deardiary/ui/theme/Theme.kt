package com.karoslabs.deardiary.ui.theme

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.karoslabs.deardiary.R
import com.karoslabs.deardiary.domain.ThemeMode

val Playfair = FontFamily(
    Font(R.font.playfair_regular, FontWeight.Normal),
    Font(R.font.playfair_bold, FontWeight.Bold),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono, FontWeight.Normal),
)

@Immutable
data class DiaryColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val gold: Color,
    val goldMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val pill: Color,
    val delete: Color,
    val heatmap: List<Color>,
    val tabBar: Color,
    val tabBarBorder: Color,
    val isDark: Boolean,
)

val DarkDiary = DiaryColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceRaised = Color(0xFF2C2C2E),
    border = Color(0x33FFFFFF),
    gold = Color(0xFFCDB56A),
    goldMuted = Color(0xFFB89A4E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366),
    pill = Color(0xFF2A2A2C),
    delete = Color(0xFFE07A6A),
    heatmap = listOf(
        Color(0xFF242426),
        Color(0xFF3C3422),
        Color(0xFF6E5C2E),
        Color(0xFFA88B3A),
        Color(0xFFCDB56A),
    ),
    tabBar = Color(0xE61C1C1E),
    tabBarBorder = Color(0x40FFFFFF),
    isDark = true,
)

val LightDiary = DiaryColors(
    background = Color(0xFFF4EFE4),
    surface = Color(0xFFFFFBF3),
    surfaceRaised = Color(0xFFE8E0D0),
    border = Color(0x332C2416),
    gold = Color(0xFF9A7B2F),
    goldMuted = Color(0xFF7A6124),
    textPrimary = Color(0xFF1A1610),
    textSecondary = Color(0xFF6B6560),
    textTertiary = Color(0xFF8A847C),
    pill = Color(0xFFE8E0D0),
    delete = Color(0xFFC24B3A),
    heatmap = listOf(
        Color(0xFFE4DCCB),
        Color(0xFFD4C49A),
        Color(0xFFC4A85A),
        Color(0xFFA88B3A),
        Color(0xFF7A6124),
    ),
    tabBar = Color(0xF2FFFBF3),
    tabBarBorder = Color(0x332C2416),
    isDark = false,
)

val LocalDiaryColors = staticCompositionLocalOf { DarkDiary }
val LocalReduceMotion = staticCompositionLocalOf { false }

val DiaryColors.scheme: ColorScheme
    get() = if (isDark) {
        darkColorScheme(
            primary = gold,
            onPrimary = Color.Black,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaised,
            onSurfaceVariant = textSecondary,
            outline = border,
            error = delete,
        )
    } else {
        lightColorScheme(
            primary = gold,
            onPrimary = Color.White,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaised,
            onSurfaceVariant = textSecondary,
            outline = border,
            error = delete,
        )
    }

object DiaryType {
    val brand = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
    )
    val screenTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    )
    val entryTitle = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.2).sp,
    )
    val entryTitleLarge = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 36.sp,
    )
    val body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val preview = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    )
    val mono = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val stat = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        letterSpacing = (-1).sp,
    )
    val goldTime = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    )
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isReduceMotionEnabled() }
}

fun Context.isReduceMotionEnabled(): Boolean {
    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val fromA11y = try {
        am?.javaClass?.methods
            ?.firstOrNull { it.name == "isReduceMotionEnabled" && it.parameterCount == 0 }
            ?.invoke(am) as? Boolean
            ?: false
    } catch (_: Exception) {
        false
    }
    val anim = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transition = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return fromA11y || anim == 0f || transition == 0f
}

@Composable
fun DearDiaryTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val colors = if (dark) DarkDiary else LightDiary
    val reduceMotion = rememberReduceMotion()
    CompositionLocalProvider(
        LocalDiaryColors provides colors,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            content = content,
        )
    }
}
