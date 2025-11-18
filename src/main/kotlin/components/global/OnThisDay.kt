package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.calendarIcon
import org.jetbrains.jewel.ui.component.Text
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
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            FlexIconText(calendarIcon(theme.colors.text)) {
                                                Text(sermon.getDdMmYy(), style = theme.typography.semiText, color = theme.colors.text)
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            FlexIconText(calendarIcon(theme.colors.primary)) {
                                                Text(sermon.location, style = theme.typography.semiText, color = theme.colors.primary)
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