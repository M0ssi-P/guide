package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun SegmentedToggle() {
    val theme = LocalTheme.current
    var selected by remember { mutableStateOf("Songs") }
    val options = listOf("Songs", "Posts")

    // Container background pill
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(theme.colors.gray2nd)
            .padding(4.dp)
    ) {
        Row {
            options.forEach { option ->
                val isSelected = option == selected

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (isSelected) {
                            Modifier.dropShadow(
                                shape = RoundedCornerShape(4.dp),
                                block = {
                                    color = Color.Gray
                                    alpha = 0.15f
                                    radius = 10f
                                    offset = Offset(5f, 5f)
                                }
                            )
                        } else Modifier)
                        .background(
                            color = if (isSelected) theme.colors.light else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { selected = option }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if(isSelected) option.toUpperCase() else option,
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if(isSelected) {
                            theme.colors.primaryText
                        } else theme.colors.deeming
                    )
                }
            }
        }
    }
}