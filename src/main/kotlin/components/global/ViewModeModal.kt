package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import presentation.LocalWindowController
import ui.theme.LocalTheme

@Composable
fun ViewModeModal() {
    val windowController = LocalWindowController.current
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
            .width(255.dp)
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
                text = "View Mode...",
                color = theme.colors.primaryText
            )
        }
        SwitchThemeOption("${if (windowController.presentation) "Exit" else "Enter"} Presentation Mode", number = 1, false, onClick = {
            if (windowController.presentation) {
                windowController.closePresentation()
            } else {
                windowController.openPresentation()
            }
        })
        SwitchThemeOption("Enter Distraction Free Mode", number = 2)
        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )
        SwitchThemeOption("Enter Full Screen", number = 3, false)
        Divider(
            thickness = 1.dp,
            color = theme.colors.border
        )
        SwitchThemeOption("Enter Zen Mode", number = 4, false)
        SwitchThemeOption("Enter Compact Mode", number = 6, false)
        SwitchThemeOption("Enter Presentation Assist", number = 6, false)
    }
}