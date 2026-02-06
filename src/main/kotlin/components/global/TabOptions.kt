package components.global

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.composables.shiftIcon
import navigation.LibraryScreen
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

data class OptionTab(
    val image: String,
    val title: String,
    val description: String
)

val options = mutableListOf<OptionTab>(
    OptionTab(
        image = "src/the_table.png",
        title = "The Table",
        description = "Get access to brother Branham’s sermons."
    ),
    OptionTab(
        image = "src/the_bible.png",
        title = "Bible",
        description = "Supports multi-lingo and versions."
    ),
    OptionTab(
        image = "src/jesus_cloud.png",
        title = "Only Believe songBook",
        description = "Only believe song book."
    )
)

@Composable
fun TabOptions() {
    val theme = LocalTheme.current;
    val main = LocalTabs.current

    Column(
        modifier = Modifier.width(486.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            options.mapIndexed { key, item ->
                var isHovered by remember(key) { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }

                val animatedBgColor by animateColorAsState(
                    targetValue = if (isHovered) theme.colors.menuHoverColor else theme.colors.background,
                    animationSpec = tween(
                        durationMillis = 50,
                        easing = EaseIn
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(animatedBgColor)
                        .onHover{ bool ->
                            isHovered = bool
                        }
                        .clickable(
                            interactionSource = interactionSource
                        ) {
                            main.newTab(Screen.Library(LibraryScreen.Hymns()))
                            main.current.setMetadata(
                                title = "New Tab"
                            )
                        }
                        .pointerHoverIcon(
                            PointerIcon(
                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(item.image),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column() {
                            Text(item.title, style = theme.typography.semiText, color = theme.colors.primaryText)
                            Spacer(Modifier.height(6.dp))
                            Text(item.description, style = theme.typography.tab, color = theme.colors.text)
                        }
                    }

                    //Keybinds display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, alignment = Alignment.CenterHorizontally)
                    ) {
                        KeybindButton(
                            contentColor = theme.colors.text
                        ) {
                            Icon(
                                imageVector = shiftIcon(theme.colors.text),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        KeybindButton(
                            contentColor = theme.colors.text
                        ) {

                        }
                        KeybindButton(
                            contentColor = theme.colors.text
                        ) {
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .newBorder(width = 1.dp, sides = setOf(BorderSide.Top), color = Color(0xFFEFEFEF))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("If you can’t find the song, try adding it from book store.", style = theme.typography.tab, color = theme.colors.text)
        }
    }
}