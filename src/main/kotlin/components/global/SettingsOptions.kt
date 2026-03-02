package components.global

import APP_VERSION
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.downloadIcon
import com.composables.settingsIcon
import mvvm.UpdateViewModel
import navigation.LocalModal
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@Composable
fun SettingsOptions(state: MutableState<Boolean>, viewModel: UpdateViewModel) {
    val theme = LocalTheme.current;
    val modal = LocalModal.current

    Column(
        modifier = Modifier.width(305.dp)
            .padding(10.dp)
    ) {
        viewModel.updateInfo?.let {
            Option("Download The Guide 2026.1.0.0") {
                Icon(
                    imageVector = downloadIcon(theme.colors.periwinkle),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
            Option("Download The Guide 2026.1.0.0", onClick = {
                modal.show {
                    UpdateInfo(viewModel)
                }
                state.value = false
            }) {
                Icon(
                    imageVector = downloadIcon(theme.colors.primaryHighlight),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Option("Up to date: $APP_VERSION")

        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )
        Option("Run Analytics...") {
            Icon(
                imageVector = settingsIcon(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )

        Option("Settings...") {
            Icon(
                imageVector = settingsIcon(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        Option("Plugins...")
        Option(
            "Theme...",
            onClick = {
                modal.show {
                    SwitchTheme()
                }
                state.value = false
            }
        )
        Option("View Mode...",
            onClick = {
                modal.show {
                    ViewModeModal()
                }
                state.value = false
            })
    }
}

@Composable
fun Option(text: String, onClick: () -> Unit = {}, icon: @Composable () -> Unit = {}) {
    val theme = LocalTheme.current;
    var isHovered by remember { mutableStateOf(false) }
    var wasHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered) theme.colors.menuHoverColor else theme.colors.popup)
            .onHover{ bool ->
                if (bool != wasHovered) {
                    isHovered = bool
                    wasHovered = bool
                }
            }
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                )
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clickable(
                interactionSource = interactionSource,
                onClick = {
                    onClick()
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(20.dp)
        ) {
            icon()
        }
        Text(text, style = theme.typography.semiText, color = theme.colors.text)
    }
}