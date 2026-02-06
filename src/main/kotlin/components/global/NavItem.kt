package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@Composable
fun NavItem(isActive: Boolean, key: Int, name: String, icon: (Color) -> ImageVector, onClick: () -> Unit) {
    val theme = LocalTheme.current

    var isHovered by remember(key) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val animatedColor by animateColorAsState(
        targetValue = if (isActive || isHovered) theme.colors.menuHoverColor else theme.colors.menu,
        animationSpec = tween(
            durationMillis = 50,
            easing = EaseIn
        )
    )

    val animatedTextColor by animateColorAsState(
        targetValue = if (isActive || isHovered) theme.colors.primaryText else theme.colors.text,
        animationSpec = tween(
            durationMillis = 50,
            easing = EaseIn
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .onHover { hovered ->
                isHovered = hovered
                false
            }
            .background(animatedColor)
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
            ) {
                onClick()
            }
            .padding(8.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon(animatedTextColor),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(name, style = theme.typography.tab, color = animatedTextColor)
    }
}