package ui.theme

import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class Theme(
    val colors: Colors,
    val typography: Typography,
    val spacing: Spacing
)

val LocalTheme = staticCompositionLocalOf<Theme> { error("no theme provided") }

@Composable
fun ThemeUI(
    isDark: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorPallete: Colors = if(isDark) LightColors else LightColors

    val theme = Theme(
        colors = colorPallete,
        typography = CustomTypography,
        spacing = CustomSpacing
    )

    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalTextStyle provides theme.typography.body,
        LocalContentColor provides theme.colors.text
    ) {
        content()
    }
}