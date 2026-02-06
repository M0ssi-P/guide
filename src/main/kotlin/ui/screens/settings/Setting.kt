package ui.screens.settings

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun Setting() {
    val theme = LocalTheme.current

    Text("Setting Page", style = theme.typography.h1)
}