package components.global

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TooltipIconButton(
    icon: (e: Color) -> ImageVector,
    tooltip: String,
    onClick: () -> Unit = {}
) {
    val theme = LocalTheme.current
    val col = remember { mutableStateOf<Color>(Color.Transparent) }

    TooltipArea(
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.ComponentRect(
            anchor = Alignment.TopCenter,
            alignment = Alignment.TopCenter,
            offset = DpOffset(0.dp, (-8).dp)
        ),
        tooltip = {
            Box(
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(4.dp),
                        block = {
                            color = theme.colors.border
                            spread = 1f
                            offset = Offset(0f, 0f)
                        }
                    )
                    .dropShadow(
                        shape = RoundedCornerShape(4.dp),
                        block = {
                            color = Color.Black.copy(alpha = 0.12f)
                            alpha = 1f
                            spread = -6f
                            radius = 28f
                            offset = Offset(0f, 14f)
                        }
                    )
                    .background(
                        theme.colors.popup,
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Text(
                    tooltip,
                    modifier = Modifier.padding(8.dp),
                    style = theme.typography.h4,
                    fontSize = 12.sp,
                    color = theme.colors.text
                )
            }
        }
    ) {
        Button(contentColor = theme.colors.text,
            modifier = Modifier.size(25.dp).clip(RoundedCornerShape(4.dp)).background(col.value)
                .onHover {
                    if(it) {
                        col.value = theme.colors.menuHoverColor
                    } else {
                        col.value = Color.Transparent
                    }
                }
                .pointerHoverIcon(
                    PointerIcon(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    )
                ).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onClick()
                    },
                ))
            {
            Icon(
                imageVector = icon(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}