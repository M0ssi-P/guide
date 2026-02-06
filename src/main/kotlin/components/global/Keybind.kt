package components.global

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.theme.LocalTheme

@Composable
fun KeybindButton(
    modifier: Modifier = Modifier,
    contentColor: Color,
    content: @Composable () -> Unit
){
    val theme = LocalTheme.current

    Button(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
        .border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(4.dp))
        .then(modifier), contentColor = contentColor) {
        content()
    }
};