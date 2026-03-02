package components.global

import IntUiThemes
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import loadData
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import saveData
import ui.settings.UserInterfaceSettings
import ui.theme.LocalTheme
import ui.theme.LocalUi
import kotlin.collections.plus

@Composable
fun SwitchTheme() {
    val theme = LocalTheme.current

    Column(
        modifier = Modifier
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = Color.Black.copy(alpha = 0.12f)
                    alpha = 1f
                    spread = -6f
                    radius = 28f
                    offset = Offset(0f, 14f)
                }
            )
            .clip(RoundedCornerShape(6.dp))
            .background(theme.colors.popup)
            .width(205.dp)
            .padding(10.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Theme...",
                color = theme.colors.primaryText
            )
        }
        SwitchThemeOption("Light", number = 1)
        SwitchThemeOption("Dark", number = 2)
        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )
        SwitchThemeOption("High Contrast", number = 3)
        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )
        SwitchThemeOption("System", number = 4)
    }
}

@Composable
fun SwitchThemeOption(text: String, number: Int, isThemeSwitch: Boolean = true, onClick: () -> Unit = {}) {
    val theme = LocalTheme.current;
    val ui = LocalUi.current
    var isHovered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isHovered) theme.colors.menuHoverColor else theme.colors.popup)
            .onHover{ bool ->
                isHovered = bool
            }
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                )
            )
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(
                interactionSource = interactionSource,
                onClick = {
                    if(!isThemeSwitch) {
                        onClick()
                        return@clickable
                    }

                    val themeValue = when(number) {
                        1 -> {
                            IntUiThemes.Light
                        }
                        2 -> {
                            IntUiThemes.Dark
                        }
                        3 -> {
                            IntUiThemes.Light
                        }
                        4 -> {
                            IntUiThemes.System
                        }
                        else -> {
                            IntUiThemes.Light
                        }
                    }

                    ui.value = themeValue

                    val current = loadData<UserInterfaceSettings>("ui_settings");

                    val updated = current!!.copy(darkMode = themeValue)
                    saveData("ui_settings", updated)
                    onClick()
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(10.dp)) {
            Text(number.toString(), style = theme.typography.tab, color = theme.colors.text)
        }
        Text(text, style = theme.typography.semiText, color = theme.colors.night)
    }
}