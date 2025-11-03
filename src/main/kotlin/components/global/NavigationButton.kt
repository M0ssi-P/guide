package components.global

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.composables.arrowLeft
import com.composables.arrowRight
import com.composables.safeHome
import ui.modifier.stroke.InsetX
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

@Composable
fun NavigationButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    contentColor: Color,
    content: @Composable () -> Unit
){
    val theme = LocalTheme.current

    val bg = backgroundColor ?: theme.colors.menu

    Button(modifier = modifier.width(50.dp), backgroundColor = bg, contentColor = contentColor) {
        content()
    }
};

@Composable
fun NavigationButtons(state: MutableState<Rect?>){
    val theme = LocalTheme.current

    Row(modifier = Modifier.wrapContentWidth()) {
        NavigationButton(contentColor = theme.colors.text, modifier = Modifier.padding()) {
            Icon(
                imageVector = arrowLeft(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        NavigationButton(contentColor = theme.colors.text) {
            Icon(
                imageVector = arrowRight(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }

        // Fixed home button
        NavigationButton(modifier = Modifier
            .width(50.dp)
            .newBorder(theme.colors.border, 1.dp, InsetX)
            .onGloballyPositioned { layoutCoordinates ->
                val bounds = layoutCoordinates.boundsInParent()
                state.value = bounds
            }, backgroundColor = theme.colors.activeTab, contentColor = theme.colors.text) {
            Icon(
                imageVector = safeHome(theme.colors.text),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}