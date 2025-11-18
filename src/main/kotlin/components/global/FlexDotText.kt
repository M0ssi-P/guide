package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun FlexDotText(text: String, bgColor: Color) {
    val theme = LocalTheme.current

    Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(5.dp).clip(RoundedCornerShape(6.dp)).background(bgColor))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, style = theme.typography.semiText, color = theme.colors.text)
    }
}