package ui.screens.myactivity

import androidx.compose.runtime.Composable
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun MyActivity() {
    val theme = LocalTheme.current

    Text("My Activity", style = theme.typography.h1)
}