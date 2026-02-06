package ui.screens.hymns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrowDown
import com.composables.broadcastIcon
import components.global.NavigationButton
import kotlinx.coroutines.launch
import models.LyricType
import mvvm.SongBookViewModal
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import presentation.LocalWindowController
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun HymnsContent(model: SongBookViewModal) {
    val theme = LocalTheme.current
    val windowController = LocalWindowController.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var trackHeightPx by remember { mutableIntStateOf(0) }
    val currentSong = model.currentSong.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.colors.gray2nd),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 1200.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 30.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            currentSong.value?.titles?.first()?.title ?: "Unknown",
                            style = theme.typography.hOne,
                            color = theme.colors.primaryText
                        )
                        Text("Number: ${currentSong.value!!.songNumber}", style = theme.typography.body, color = theme.colors.text)
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row {
                            NavigationButton(
                                disabled = false,
                                contentColor = theme.colors.text, modifier = Modifier.size(25.dp),
                                onClick = {
                                    windowController.openPresentation()
                                    model.setPresentationMode(true)
                                }
                            ) {
                                Icon(
                                    imageVector = broadcastIcon(theme.colors.text),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            Row {
                                Text("KEY", style = theme.typography.h4, color = theme.colors.primaryText)
                                Spacer(Modifier.width(5.dp))
                                Text("C", style = theme.typography.h4BOLD, color = theme.colors.primaryText)
                            }
                            Row {
                                Text("BPM", style = theme.typography.h4, color = theme.colors.primaryText)
                                Spacer(Modifier.width(5.dp))
                                Text("31", style = theme.typography.h4BOLD, color = theme.colors.primaryText)
                            }
                            Row {
                                Text("TIME SIG", style = theme.typography.h4, color = theme.colors.primaryText)
                                Spacer(Modifier.width(5.dp))
                                Text("4/4", style = theme.typography.h4BOLD, color = theme.colors.primaryText)
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(min = 200.dp, max = 1200.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 30.dp, vertical = 30.dp)
                    .onSizeChanged { trackHeightPx = it.height }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (trackHeightPx == 0) return@rememberDraggableState
                            val scrollDelta =
                                delta / trackHeightPx * scrollState.maxValue
                            scope.launch {
                                scrollState.scrollBy(-scrollDelta)
                            }
                        },
                    )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "KEY OF:",
                        style = theme.typography.h2,
                        color = theme.colors.primaryText
                    )
                    Box(
                        modifier = Modifier.border(width = 2.dp, color = theme.colors.border, shape = RoundedCornerShape(50.dp))
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "C",
                            style = theme.typography.h2,
                            color = theme.colors.text
                        )
                    }
                    Text(
                        "SONG STRUCTURE",
                        style = theme.typography.h2,
                        color = theme.colors.primaryText
                    )
                }
                Spacer(Modifier.height(40.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp, alignment = Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier.size(57.dp, 30.dp)
                            .border(width = 2.dp, color = theme.colors.primaryHighlight, shape = RoundedCornerShape(50.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "I",
                            style = theme.typography.h3,
                            color = theme.colors.primaryHighlight
                        )
                    }
                    currentSong.value?.lyrics?.mapIndexed { index, line ->
                        val borderColor = when(line.type) {
                            LyricType.CHORUS -> {
                                theme.colors.crimson
                            }
                            LyricType.VERSE -> {
                                if(index % 2 == 1) {
                                    theme.colors.aquamarine
                                } else {
                                    theme.colors.periwinkle
                                }
                            }
                            else -> theme.colors.periwinkle
                        }
                        val textC = when(line.type) {
                            LyricType.VERSE -> "V" + line.verseNumber.toString()
                            LyricType.CHORUS -> "C"
                            else -> ""
                        }
                        Box(
                            modifier = Modifier.size(57.dp, 30.dp)
                                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                textC,
                                style = theme.typography.h3,
                                color = borderColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(75.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if(currentSong.value != null && currentSong.value?.lyrics?.isEmpty() == false) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .newBorder(width = 6.dp, sides = setOf(BorderSide.Left), color = theme.colors.primaryHighlight)
                                    .padding(horizontal = 20.dp, vertical = 5.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        "INTRO",
                                        style = TextStyle(
                                            fontFamily = Inter,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = theme.colors.primaryText
                                    )
                                    Text(
                                        "C",
                                        style = theme.typography.h3,
                                        color = theme.colors.primaryText
                                    )
                                }
                            }

                            currentSong.value!!.lyrics.mapIndexed { index, line ->
                                if(line.type == LyricType.VERSE) {
                                    Box(
                                        modifier = Modifier
                                            .newBorder(width = 6.dp, sides = setOf(BorderSide.Left), color = if(index % 2 == 1) {
                                                theme.colors.aquamarine
                                            } else {
                                                theme.colors.periwinkle
                                            })
                                            .padding(horizontal = 20.dp, vertical = 5.dp)
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                "Verse ${line.verseNumber}",
                                                style = TextStyle(
                                                    fontFamily = Inter,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = theme.colors.primaryText
                                            )
                                            Text(
                                                "C",
                                                style = theme.typography.h3,
                                                color = theme.colors.primaryText
                                            )
                                            Text(
                                                line.lines.joinToString("\n"),
                                                style = theme.typography.h4,
                                                color = theme.colors.text,
                                                lineHeight = 28.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(30.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            currentSong.value!!.lyrics.filter { it.type == LyricType.CHORUS }.mapIndexed { index, line ->
                                Box(
                                    modifier = Modifier
                                        .newBorder(width = 6.dp, sides = setOf(BorderSide.Left), color = theme.colors.crimson)
                                        .padding(horizontal = 20.dp, vertical = 5.dp),
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "Chorus",
                                            style = TextStyle(
                                                fontFamily = Inter,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = theme.colors.primaryText
                                        )
                                        Text(
                                            "C",
                                            style = theme.typography.h3,
                                            color = theme.colors.primaryText
                                        )
                                        Text(
                                            line.lines.joinToString("\n"),
                                            style = theme.typography.h4,
                                            color = theme.colors.text,
                                            lineHeight = 28.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}