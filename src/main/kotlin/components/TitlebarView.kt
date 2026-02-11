package components

import IntUiThemes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.arrowLeft
import components.global.NavigationButton
import components.global.NavigationButtons
import db.ConfigViewModel
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import org.jetbrains.jewel.window.styling.LocalTitleBarStyle
import org.jetbrains.jewel.window.styling.TitleBarMetrics
import org.jetbrains.jewel.window.styling.TitleBarStyle
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

@Composable
fun DecoratedWindowScope.titleBarView(height: Dp, state: MutableState<Boolean>, onHovered: MutableState<Boolean>) {
    val activeBounds = remember { mutableStateOf<Rect?>(null) }
    val theme = LocalTheme.current
    val style = LocalTitleBarStyle.current

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

        Box(modifier = Modifier.fillMaxSize()
            .offset(x = 70.dp, y = 0.dp)
            .padding(end = style.metrics.titlePaneButtonSize.width*3), contentAlignment = Alignment.CenterStart) {
            NavigationButtons(activeBounds, state, sidebarToggleHovered = onHovered)
        }
    }
}