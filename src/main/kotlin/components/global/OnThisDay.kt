package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.book
import com.composables.calendarIcon
import com.composables.locationIcon
import com.composables.play
import com.composables.saveArchive
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import parsers.vgr.VgrViewModel
import parsers.vgr.models.getDdMmYy
import ui.UiState
import ui.theme.LocalTheme

@Composable
fun OnThisDay(model: VgrViewModel) {
    val theme = LocalTheme.current
    val thisDay = model.thisDay.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("On this day", style = theme.typography.semiText, color = theme.colors.text)
        Spacer(Modifier.height(20.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            when(val state = thisDay.value) {
                is UiState.Success -> {
                    if(state.data.isNotEmpty()) {
                        for (sermon in state.data) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .background(theme.colors.secondaryHighlight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(sermon.title, style = theme.typography.h3, color = theme.colors.primaryText)
                                    Spacer(Modifier.height(15.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            FlexIconText(calendarIcon(theme.colors.text)) {
                                                Text(sermon.getDdMmYy(), style = theme.typography.semiText, color = theme.colors.text)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            FlexIconText(locationIcon(theme.colors.primary)) {
                                                Text(sermon.location, style = theme.typography.semiText, color = theme.colors.primary)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(25.dp)
                                                .background(theme.colors.border)
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterHorizontally)) {
                                            Button(
                                                modifier = Modifier.background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(Color(0xFF109DF5), Color(0xFF1B55F7)) // example blue gradient fill
                                                    ),
                                                    shape = RoundedCornerShape(21.dp)
                                                ).size(38.dp).pointerHoverIcon(
                                                    PointerIcon(
                                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                                    )
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = play(theme.colors.activeTab),
                                                    contentDescription = null,
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Button(
                                                modifier = Modifier.background(
                                                    color = theme.colors.blue100,
                                                    shape = RoundedCornerShape(21.dp)
                                                ).size(38.dp).pointerHoverIcon(
                                                    PointerIcon(
                                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                                    )
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = book(theme.colors.blue3),
                                                    contentDescription = null,
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Button(
                                                modifier = Modifier.background(
                                                    color = theme.colors.blue100,
                                                    shape = RoundedCornerShape(21.dp)
                                                ).size(38.dp).pointerHoverIcon(
                                                    PointerIcon(
                                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                                    )
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = saveArchive(theme.colors.blue3),
                                                    contentDescription = null,
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(16.dp).pointerHoverIcon(
                                                        PointerIcon(
                                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Loading -> {
                    Column( modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center ){
                        Text("Loading...", color = Color.Black)
                    }
                }
                is UiState.Error -> {

                }
            }
        }
    }
}