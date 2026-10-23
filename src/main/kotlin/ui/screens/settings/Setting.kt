package ui.screens.settings

import androidx.compose.runtime.getValue
import ui.theme.LocalTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ThemeToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val theme = LocalTheme.current
    val trackColor = if (checked) theme.colors.primary else theme.colors.disabledOne
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = tween(160),
        label = "toggleKnob"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    indent: Boolean = false
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = if (indent) 16.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
            Text(title, style = theme.typography.h4BOLD.copy(color = theme.colors.primaryText))
            if (description != null) {
                Text(
                    description,
                    style = theme.typography.body.copy(color = theme.colors.text),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        ThemeToggleSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSectionHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    Text(
        label.uppercase(),
        style = theme.typography.tab.copy(color = theme.colors.text, letterSpacing = 1.sp),
        modifier = modifier.padding(bottom = 8.dp)
    )
}