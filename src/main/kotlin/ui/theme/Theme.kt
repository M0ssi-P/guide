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
import androidx.compose.runtime.rememberUpdatedState
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

@Composable
fun ThemeUI(
    content: @Composable () -> Unit
) {
    val ui = remember { mutableStateOf(
        loadData<UserInterfaceSettings>("ui_settings")?.darkMode ?: IntUiThemes.Dark.apply {
            saveData("ui_settings", UserInterfaceSettings(darkMode = this))
        }
    ) }

    val colorPalette by derivedStateOf {
        if (ui.value.isDark()) DarkColors else LightColors
    }

    val theme = Theme(
        colors = colorPalette,
        typography = CustomTypography,
        spacing = CustomSpacing
    )

    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalUi provides ui,
        LocalTextStyle provides theme.typography.body,
        LocalContentColor provides theme.colors.text
    ) {
        content()
    }
}