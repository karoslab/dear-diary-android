package com.karoslabs.deardiary.ui.theme

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ColorDrawable
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
    val hairline: Color,
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
    val tabInactive: Color,
    val tabHighlight: Color,
    val isDark: Boolean,
)

val DarkDiary = DiaryColors(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceRaised = Color(0xFF2C2C2E),
    border = Color(0x26FFFFFF),
    hairline = Color(0x1AFFFFFF),
    gold = Color(0xFFD4AF37),
    goldMuted = Color(0xFFC4A35A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFF636366),
    pill = Color(0xFF2C2C2E),
    delete = Color(0xFFE07A7A),
    heatmap = listOf(
        Color(0xFF2C2C2E),
        Color(0xFF4A3F24),
        Color(0xFF8C7038),
        Color(0xFFC4A24A),
        Color(0xFFE8C85A),
    ),
    tabBar = Color(0xCC141416),
    tabBarBorder = Color(0x33FFFFFF),
    tabInactive = Color(0xFFFFFFFF),
    tabHighlight = Color(0x403A2E14),
    isDark = true,
)

val LightDiary = DiaryColors(
    background = Color(0xFFF4EFE4),
    surface = Color(0xFFFFFBF3),
    surfaceRaised = Color(0xFFE8E0D0),
    border = Color(0x332C2416),
    hairline = Color(0x1A2C2416),
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
    tabInactive = Color(0xFF1A1610),
    tabHighlight = Color(0x33C4A85A),
    isDark = false,
)

val LocalDiaryColors = staticCompositionLocalOf { DarkDiary }
val LocalReduceMotion = staticCompositionLocalOf { false }

val DiaryColors.scheme: ColorScheme
    get() = if (isDark) {
        darkColorScheme(
            primary = gold,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = surface,
            onSurfaceVariant = textSecondary,
            surfaceTint = Color.Transparent,
            surfaceBright = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            outline = border,
            outlineVariant = hairline,
            error = delete,
        )
    } else {
        lightColorScheme(
            primary = gold,
            onPrimary = Color.White,
            background = background,
            onBackground = textPrimary,
            surface = background,
            onSurface = textPrimary,
            surfaceVariant = surface,
            onSurfaceVariant = textSecondary,
            surfaceTint = Color.Transparent,
            outline = border,
            error = delete,
        )
    }

object DiaryType {
    val brand = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
    )
    val screenTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
    )
    val entryTitle = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = (-0.15).sp,
        lineHeight = 24.sp,
    )
    val entryTitleLarge = TextStyle(
        fontFamily = Playfair,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 34.sp,
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
        letterSpacing = 1.6.sp,
    )
    val section = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
    )
    val mono = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )
    val stat = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        letterSpacing = (-1.2).sp,
    )
    val goldTime = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = (-0.2).sp,
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
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.setBackgroundDrawable(ColorDrawable(colors.background.toArgb()))
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }
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
