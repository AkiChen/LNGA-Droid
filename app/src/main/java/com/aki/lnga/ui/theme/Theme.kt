package com.aki.lnga.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import java.util.Calendar

internal enum class ThemeColorOption(
    val id: String,
    val displayName: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightContainer: Color,
    val darkContainer: Color,
) {
    Purple("purple", "紫色", Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF), Color(0xFF4F378B)),
    Blue("blue", "蓝色", Color(0xFF1565C0), Color(0xFFA8C7FA), Color(0xFFD7E3FF), Color(0xFF004A77)),
    Cyan("cyan", "青色", Color(0xFF006A6A), Color(0xFF4FDADA), Color(0xFF9CF1F0), Color(0xFF004F50)),
    Green("green", "绿色", Color(0xFF2E7D32), Color(0xFFA5D6A7), Color(0xFFC8E6C9), Color(0xFF1B5E20)),
    Orange("orange", "橙色", Color(0xFFA54B00), Color(0xFFFFB77D), Color(0xFFFFDBC2), Color(0xFF7D2D00)),
    Red("red", "红色", Color(0xFFB3261E), Color(0xFFFFB4AB), Color(0xFFFFDAD6), Color(0xFF8C1D18));

    companion object {
        fun fromId(id: String?): ThemeColorOption = entries.firstOrNull { it.id == id } ?: Purple
    }
}

internal enum class ThemeAppearanceOption(val id: String, val displayName: String) {
    System("system", "跟随系统"),
    Light("light", "浅色"),
    Dark("dark", "深色"),
    UltraDark("ultra_dark", "极致黑夜");

    companion object {
        fun fromId(id: String?): ThemeAppearanceOption = entries.firstOrNull { it.id == id } ?: System
    }
}

internal fun isWithinAutomaticNightWindow(hourOfDay: Int): Boolean = hourOfDay >= 23 || hourOfDay < 7

internal class ThemePreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences("lnga_appearance", Context.MODE_PRIVATE)

    fun load(): ThemeColorOption = ThemeColorOption.fromId(preferences.getString("theme_color", null))

    fun save(option: ThemeColorOption) {
        preferences.edit().putString("theme_color", option.id).apply()
    }

    fun loadAppearance(): ThemeAppearanceOption =
        ThemeAppearanceOption.fromId(preferences.getString("theme_appearance", null))

    fun saveAppearance(option: ThemeAppearanceOption) {
        preferences.edit().putString("theme_appearance", option.id).apply()
    }

    fun loadAutomaticNight(): Boolean = preferences.getBoolean("automatic_ultra_night", true)

    fun saveAutomaticNight(enabled: Boolean) {
        preferences.edit().putBoolean("automatic_ultra_night", enabled).apply()
    }
}

@Composable
internal fun LngaTheme(
    themeColor: ThemeColorOption,
    appearance: ThemeAppearanceOption,
    automaticNight: Boolean,
    content: @Composable () -> Unit,
) {
    var hourOfDay by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(automaticNight) {
        while (automaticNight) {
            hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(30_000)
        }
    }
    val ultraDark = appearance == ThemeAppearanceOption.UltraDark ||
        automaticNight && isWithinAutomaticNightWindow(hourOfDay)
    val dark = ultraDark || when (appearance) {
        ThemeAppearanceOption.System -> isSystemInDarkTheme()
        ThemeAppearanceOption.Light -> false
        ThemeAppearanceOption.Dark -> true
        ThemeAppearanceOption.UltraDark -> true
    }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    val colors = if (ultraDark) {
        darkColorScheme(
            primary = Color(0xFFD6B66B),
            onPrimary = Color(0xFF241A00),
            primaryContainer = Color(0xFF483B20),
            onPrimaryContainer = Color(0xFFF3E2B4),
            secondary = Color(0xFFC8B77D),
            secondaryContainer = Color(0xFF35301F),
            background = Color(0xFF000000),
            onBackground = Color(0xFFE2E2E6),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFE2E2E6),
            surfaceDim = Color(0xFF000000),
            surfaceBright = Color(0xFF1D1D21),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF0B0B0D),
            surfaceContainer = Color(0xFF0B0B0D),
            surfaceContainerHigh = Color(0xFF121216),
            surfaceContainerHighest = Color(0xFF18181D),
            surfaceVariant = Color(0xFF18181D),
            onSurfaceVariant = Color(0xFFA5A5AE),
            outline = Color(0xFF43434B),
            outlineVariant = Color(0xFF292930),
            inverseSurface = Color(0xFFE2E2E6),
            inverseOnSurface = Color(0xFF1B1B1F),
            inversePrimary = Color(0xFF66551F),
        )
    } else if (dark) {
        darkColorScheme(
            primary = themeColor.darkPrimary,
            primaryContainer = themeColor.darkContainer,
            secondary = themeColor.darkPrimary,
            secondaryContainer = themeColor.darkContainer,
        )
    } else {
        lightColorScheme(
            primary = themeColor.lightPrimary,
            primaryContainer = themeColor.lightContainer,
            secondary = themeColor.lightPrimary,
            secondaryContainer = themeColor.lightContainer,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
