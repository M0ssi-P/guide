package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun FullscreenTitleBarView()  {
    val theme = LocalTheme.current

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(theme.colors.surface)
            .height(70.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterStart)) {
                Text("Genesis")
            }
            Box(Modifier.size(width = 60.dp, height = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.colors.border)
                .align(Alignment.Center)
            )
            Box(Modifier.align(Alignment.CenterEnd)) {
                Text("New International Version")
            }
        }
    }
}