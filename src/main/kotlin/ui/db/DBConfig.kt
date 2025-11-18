package ui.db

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import db.ConfigViewModel
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun DBConfig() {
    val theme = LocalTheme.current
    val configVM = remember { ConfigViewModel() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxHeight()
            .widthIn(min = 200.dp, max = 1200.dp)
            .fillMaxSize()
            .padding(80.dp, 60.dp, 80.dp, 0.dp)
    ) {
        val maxWidth = this.maxWidth
        val maxHeight = this.maxHeight

        Text("HI", color = theme.colors.primary)
    }
}