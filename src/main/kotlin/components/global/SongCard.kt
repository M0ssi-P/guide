package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import models.ISongDetails
import models.LyricType
import mvvm.SongBookViewModal
import org.jetbrains.skiko.Cursor
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun SongCard(song: ISongDetails, index: Int, model: SongBookViewModal) {
    val theme = LocalTheme.current
    val currentSong = model.currentSong.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (index % 2 == 1) {
        if(currentSong.value!!.id == song.id) {
            theme.colors.periwinkle
        } else theme.colors.gray2nd
    } else {
        if(currentSong.value!!.id == song.id) {
            theme.colors.periwinkle
        } else theme.colors.periwinkle.copy(alpha = 0.25f)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .pointerHoverIcon(
                PointerIcon(
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                )
            )
            .background(background)
            .clickable(
                interactionSource = interactionSource
            ) {
                model.setCurrentSong(song)
            }
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row () {
            Icon(
                painter = painterResource("src/jesus_cloud.png"),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    "${song.songNumber}. ${song.titles.first().title}",
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = theme.colors.primaryText
                )
                Text(
                    "Only Believe SongBook",
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = theme.colors.text
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (song.lyrics.isNotEmpty()) {
            if(song.lyrics.any { it.type == LyricType.VERSE }) {
                Text(
                    song.lyrics.first { it.type == LyricType.VERSE }.lines.joinToString(" "),
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W100
                    ),
                    color = theme.colors.primaryText
                )
            } else {
                Text(
                    song.lyrics.first().lines.joinToString(" "),
                    style = TextStyle(
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W100
                    ),
                    color = theme.colors.primaryText
                )
            }
        }
    }
}