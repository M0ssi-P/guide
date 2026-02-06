package ui.screens.home

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.Button
import components.global.Calendar
import components.global.OnThisDay
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import parsers.vgr.VgrViewModel
import ui.UiState
import ui.displayName
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.Inter
import ui.theme.LocalTheme
import ui.toOrdinal
import java.time.Month

@Composable
@Preview
fun VerseOfTheDay() {
    val vgrVM = remember { VgrViewModel() }
    val uiState = vgrVM.uiState.collectAsState()

    val theme = LocalTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 21.dp, 0.dp, 0.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
        ) {
            when(val state = uiState.value) {
                is UiState.Success -> {
                    Column {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .newBorder(
                                    theme.colors.border,
                                    1.dp,
                                    setOf(BorderSide.Top, BorderSide.Bottom, BorderSide.Left, BorderSide.Right),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        ){
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(37.dp, 30.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "${state.data.bookHuman} ${state.data.chapter}:${
                                            if(state.data.verseNumbers.size > 1) {
                                                state.data.verseNumbers.first().toString() + "-" + state.data.verseNumbers.last()
                                            } else {
                                                state.data.verseNumbers.first().toString()
                                            }
                                        }",
                                        color = theme.colors.secondaryText,
                                        style = theme.typography.h2

                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("KJV", color = theme.colors.text, style = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(0.dp, 30.dp)
                                        .newBorder(
                                            theme.colors.border,
                                            3.dp,
                                            setOf(BorderSide.Left),
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                ){
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(30.dp, 10.dp, 0.dp, 10.dp)
                                    ) {
                                        Text(
                                            state.data.content,
                                            color = theme.colors.primaryText,
                                            style = theme.typography.h3,
                                            lineHeight = 24.sp,
                                            letterSpacing = 0.5.sp,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }

                                Row {
                                    Button(
                                        modifier = Modifier.background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFF109DF5), Color(0xFF1B55F7)) // example blue gradient fill
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ).border(
                                            width = 2.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF109DF5),
                                                    Color(0xFF1B55F7),
                                                ),
                                                start = Offset(0f, 0f),
                                                end = Offset(400f, 400f)
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ).pointerHoverIcon(
                                            PointerIcon(
                                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                            )
                                        ).padding(vertical = 11.dp, horizontal = 19.dp),
                                        fillMaxSize = false,
                                        padding = PaddingValues(vertical = 0.dp, horizontal = 0.dp)
                                    ) {
                                        Text(
                                            text = "Full Chapter", color = theme.colors.activeTab, style = theme.typography.button
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(63.dp))

                        Column {
                            Text(
                                text = state.data.TheTable.title, color = theme.colors.primaryText, style = theme.typography.h2
                            )
                            Text(
                                text = state.data.TheTable.day.toOrdinal() + " " + Month.of(state.data.TheTable.month).displayName() + " " + state.data.TheTable.year,
                                color = theme.colors.text, style = theme.typography.semiText
                            )

                            Spacer(modifier = Modifier.height(17.dp))
                            Row (horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                                Button (
                                    modifier = Modifier.background(
                                        color = theme.colors.primaryText,
                                        shape = RoundedCornerShape(4.dp)
                                    ).pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    ).padding(vertical = 8.dp, horizontal = 14.dp),
                                    padding = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                                    fillMaxSize = false
                                ) {
                                    Text(
                                        text = vgrVM.uiSettings.downloadedLanguages.find {
                                            it.second == vgrVM.uiSettings.contentLanguage
                                        }?.second?.toUpperCase() ?: "ENG", color = theme.colors.activeTab, style = theme.typography.subs
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(17.dp))
                            Text(
                                text = state.data.TheTable.text, color = theme.colors.text, style = theme.typography.body
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        Row {
                            Button (
                                modifier = Modifier.background(
                                    color = theme.colors.blue100,
                                    shape = RoundedCornerShape(4.dp)
                                ).pointerHoverIcon(
                                    PointerIcon(
                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    )
                                ).padding(vertical = 12.dp, horizontal = 20.dp),
                                padding = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                                fillMaxSize = false
                            ) {
                                Text(
                                    text = "Read More", color = theme.colors.blue3, style = theme.typography.subs
                                )
                            }
                        }
                    }
                }
                is UiState.Loading -> {
                    Column( modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center ){
                        Text("Loading...", color = Color.Black)
                    }
                } is UiState.Error -> {
                Text("Error... ${state.message}", color = Color.White)
            }
            }
        }

        Spacer(modifier = Modifier.width(22.dp))

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            Column {
                Calendar(model = vgrVM)
                Spacer(modifier = Modifier.height(24.dp))
                OnThisDay(model = vgrVM)
            }
        }
    }
}