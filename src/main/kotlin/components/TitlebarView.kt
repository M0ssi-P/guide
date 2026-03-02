package components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.settingsIcon
import components.global.Button
import components.global.NavigationButtons
import components.global.PopoverAnchored
import components.global.SettingsOptions
import mvvm.UpdateViewModel
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@Composable
fun DecoratedWindowScope.titleBarView(height: Dp, state: MutableState<Boolean>, onHovered: MutableState<Boolean>) {
    val activeBounds = remember { mutableStateOf<Rect?>(null) }
    val theme = LocalTheme.current
    val updateViewModel = remember { UpdateViewModel() }

    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates()
    }

    TitleBar(
        Modifier
            .newFullscreenControls()
            .height(height)
            .drawWithContent {
                drawContent()

                val borderColor = theme.colors.border
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2

                val activeRect = activeBounds.value

                if (activeRect != null) {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, y),
                        end = Offset(activeRect.left, y),
                        strokeWidth = strokeWidth
                    )

                    drawLine(
                        color = borderColor,
                        start = Offset(activeRect.right, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                } else {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            },
        gradientStartColor = Color.Unspecified,
    ) {
        Row(Modifier.align(Alignment.Start)) {
            Box(contentAlignment = Alignment.CenterStart) {
                NavigationButtons(activeBounds, state, sidebarToggleHovered = onHovered)
            }
        }

        Row(Modifier.align(Alignment.End).padding(end = 10.dp)) {
            PopoverAnchored(
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
                    .background(
                        theme.colors.popup,
                        shape = RoundedCornerShape(6.dp)
                    ),
                popup = { state ->
                    SettingsOptions(state, viewModel = updateViewModel)
                }
            ) { bool ->
                val color = remember { mutableStateOf<Color>(Color.Transparent) }

                LaunchedEffect(bool) {
                    if (!bool) {
                        color.value = Color.Transparent
                    }
                }

                Button(contentColor = theme.colors.text,
                    modifier = Modifier.size(25.dp).clip(RoundedCornerShape(4.dp)).background(color.value)
                        .onHover {
                            if(it) {
                                color.value = theme.colors.menuHoverColor
                            } else {
                                color.value = if (bool) theme.colors.menuHoverColor else Color.Transparent
                            }
                        }
                        .pointerHoverIcon(
                            PointerIcon(
                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            )
                        )) {
                    Icon(
                        imageVector = settingsIcon(theme.colors.text),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}