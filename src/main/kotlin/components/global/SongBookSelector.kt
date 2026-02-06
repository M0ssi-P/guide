package components.global

import Abbreviate
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrowDown
import mvvm.SongBookViewModal
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import ui.theme.Arbutus
import ui.theme.LocalTheme

@Composable
fun SongBookSelector(model: SongBookViewModal) {
    val theme = LocalTheme.current
    val books = model.songBooks.collectAsState()
    val selectedBook = remember { mutableStateOf(books.value.first()) }
    val interactionSource = remember { MutableInteractionSource() }

    AnchoredDropdown(
        manifest = {
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(50.dp))
                    .background(theme.colors.gray2nd)
                    .padding(horizontal = 16.dp, vertical = 10.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedBook.value.name.Abbreviate(),
                    style = TextStyle(fontFamily = Arbutus, fontSize = 20.sp, fontWeight = FontWeight.Normal),
                    color = theme.colors.primaryText
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = arrowDown(theme.colors.primaryText),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .dropShadow(
                    shape = RoundedCornerShape(6.dp),
                    block = {
                        color = Color.Black
                        alpha = 0.10f
                        radius = 10f
                        offset = Offset(0f, 0f)
                    }
                )
                .clip(RoundedCornerShape(6.dp))
                .background(theme.colors.surface)
                .padding(10.dp)
        ) {
            books.value.mapIndexed { index, stats ->
                val hovered = remember(index) { mutableStateOf(false) }

                val bgOnHover = animateColorAsState(
                    targetValue = if(hovered.value) theme.colors.gray2nd else if (stats.name == selectedBook.value.name) {
                        theme.colors.gray2nd
                    } else theme.colors.surface
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            bgOnHover.value
                        )
                        .pointerHoverIcon(
                            PointerIcon(
                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource
                        ) {
                            selectedBook.value = stats
                            model.selectBook(stats)
                        }
                        .onHover { hover -> hovered.value = hover}
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(stats.name,style = theme.typography.subs, color = theme.colors.text)
                }
            }
        }
    }
}