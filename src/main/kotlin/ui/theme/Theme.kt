package ui.theme

import IntUiThemes
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import loadData
import saveData
import ui.settings.UserInterfaceSettings

data class Theme(
    val colors: Colors,
    val typography: Typography,
    val spacing: Spacing
)

val LocalTheme = staticCompositionLocalOf<Theme> { error("no theme provided") }
val LocalUi = staticCompositionLocalOf<MutableState<IntUiThemes>> { error("no theme provided") }

val LocalAccentTheme = staticCompositionLocalOf<MutableState<String>> { error("no theme provided") }

@Composable
fun ThemeUI(
    content: @Composable () -> Unit
) {
    val savedSettings = remember { loadData<UserInterfaceSettings>("ui_settings") }

    val ui = remember {
        mutableStateOf(
            savedSettings?.darkMode ?: IntUiThemes.Dark.apply {
                saveData("ui_settings", UserInterfaceSettings(darkMode = this))
            }
        )
    }

    val accentTheme = remember {
        mutableStateOf(savedSettings?.accentThemeId ?: DefaultAccentThemeId)
    }

    val colorPalette by derivedStateOf {
        val base = if (ui.value.isDark()) DarkColors else LightColors
        val selected = AppThemes.firstOrNull { it.id == accentTheme.value } ?: AppThemes.first()
        selected.colorsFor(base)
    }

    val theme = Theme(
        colors = colorPalette,
        typography = CustomTypography,
        spacing = CustomSpacing
    )

    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalUi provides ui,
        LocalAccentTheme provides accentTheme,
        LocalTextStyle provides theme.typography.body,
        LocalContentColor provides theme.colors.text
    ) {
        content()
    }
}


fun updateUiSettings(
    ui: MutableState<IntUiThemes>,
    accentTheme: MutableState<String>,
    darkMode: IntUiThemes = ui.value,
    accentThemeId: String = accentTheme.value
) {
    ui.value = darkMode
    accentTheme.value = accentThemeId
    saveData("ui_settings", UserInterfaceSettings(darkMode = darkMode, accentThemeId = accentThemeId))
}