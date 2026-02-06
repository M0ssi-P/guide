package ui.screens.saved

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun Saved() {
    val theme = LocalTheme.current

    Text("Saved Page", style = theme.typography.h1)
}