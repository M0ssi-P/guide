package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.arrowLeft
import com.composables.arrowRight
import com.composables.plusIcon
import com.composables.safeHome
import com.composables.tabIcon
import navigation.LocalTabs
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.skiko.Cursor
import saveData
import ui.modifier.stroke.InsetX
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

@Composable
fun NavigationButton(
    disabled: Boolean = false,
    animateBg: Boolean = true,
    modifier: Modifier = Modifier,
    max: Boolean = true,
    backgroundColor: Color? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    contentColor: Color,
    onClick: () -> Unit = { },
    onHover: (e: Boolean) -> Unit = {},
    secondModifier: Modifier = Modifier,
    content: @Composable () -> Unit
){
    val theme = LocalTheme.current
    val color = remember { mutableStateOf<Color>(backgroundColor ?: Color.Transparent) }
    val interactionSource = remember { MutableInteractionSource() }

    Button(fillMaxSize = max, secondModifier = secondModifier, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.value).onHover { bool ->
        if (disabled) return@onHover

        if(animateBg && bool) {
            color.value = theme.colors.menuHoverColor
        } else {
            color.value = backgroundColor ?: Color.Transparent
        }

        onHover(bool)
    }.then(modifier)
        .clickable(
            interactionSource = interactionSource,
        ) {
            if (disabled) return@clickable

            onClick()
        }
        .pointerHoverIcon(
            PointerIcon(
                if (disabled) {
                    Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                } else {
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                }
            )
        ), contentColor = contentColor, padding = padding) {
        content()
    }
};

@Composable
fun NavigationButtons(state: MutableState<Rect?>, stat: MutableState<Boolean>, sidebarToggleHovered: MutableState<Boolean>){
    val theme = LocalTheme.current
    val main = LocalTabs.current
    val navigation = main.current

    val arrowLeftColor by animateColorAsState(
        targetValue = if(navigation.canGoBack) {
            theme.colors.text
        } else theme.colors.disabledOne
    )

    val arrowRightColor by animateColorAsState(
        targetValue = if(navigation.canGoNext) theme.colors.text else theme.colors.disabledOne
    )

    Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        if(stat.value) {
            Spacer(modifier = Modifier.width(10.dp))
            NavigationButton(contentColor = theme.colors.text, modifier = Modifier.size(25.dp),  padding = PaddingValues()) {
                Icon(
                    painter = painterResource("icons/logo.png"),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            NavigationButton(contentColor = theme.colors.text,
                modifier =  Modifier.size(25.dp)
                    .onHover { hovered -> sidebarToggleHovered.value = hovered},
                onClick = {
                    val newValue = !stat.value
                    stat.value = newValue
                    saveData("should_hide_sidebar", newValue)
                },
                padding = PaddingValues())
            {
                Icon(
                    imageVector = tabIcon(theme.colors.text),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        NavigationButton(
            disabled = !navigation.canGoBack,
            contentColor = theme.colors.text, modifier = Modifier.size(25.dp),
            onClick = { navigation.back() }
        ) {
            Icon(
                imageVector = arrowLeft(arrowLeftColor),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
        }
        NavigationButton(
            disabled = !navigation.canGoNext,
            contentColor = theme.colors.text, modifier = Modifier.size(25.dp),
            onClick = { navigation.next() }
        ) {
            Icon(
                imageVector = arrowRight(arrowRightColor),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        val tabBounds = remember { mutableStateMapOf<String, Rect>() }

        // Fixed home button
        main.tabs.forEachIndexed { index, navigator ->
            key(main.current) {
                NavigationButton(max = false, modifier = Modifier
                    .newBorder(theme.colors.border, 1.dp, InsetX)
                    .fillMaxHeight()
                    .widthIn(min = 180.dp)
                    .onGloballyPositioned { layoutCoordinates ->
                        if (navigator.id == main.current.id) {
                            val bounds = layoutCoordinates.boundsInParent()
                            state.value = bounds
                            tabBounds[navigator.id] = bounds
                        } else {
                            return@onGloballyPositioned
                        }
                    }
                    .padding(horizontal = 20.dp),
                    backgroundColor = if (main.current.id === navigator.id) theme.colors.activeTab else theme.colors.menu,
                    contentColor = theme.colors.text,
                    disabled = main.current.id === navigator.id,
                    secondModifier = Modifier.fillMaxHeight(),
                    onClick = {
                        main.setCurrent(navigator)
                        state.value = tabBounds.getValue(navigator.id)
                    },
                ) {
                    Icon(
                        imageVector = safeHome(theme.colors.text),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (!navigator.metadata.title.isNullOrEmpty()) {
                        Text(navigator.metadata.title!!)
                    } else {
                        Text("Home")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        //new tab button


        PopoverAnchored(
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(6.dp),
                    block = {
                        color = Color.Black
                        alpha = 0.15f
                        radius = 10f
                        offset = Offset(0f, 0f)
                    }
                )
                .background(
                    Color.White,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(1.dp, color = Color(0xFFEFEFEF), shape = RoundedCornerShape(
                    6.dp
                )),
            popup = {
                TabOptions()
            }
        ) {
            NavigationButton(contentColor = theme.colors.text, modifier = Modifier.size(25.dp)) {
                Icon(
                    imageVector = plusIcon(theme.colors.text),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}