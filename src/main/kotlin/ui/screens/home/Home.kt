package ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle as TxTStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.bookSquare
import components.global.FlexDotText
import components.global.FlexIconText
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import ui.theme.Inter
import ui.theme.LocalTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun Home(){
    val theme = LocalTheme.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxHeight()
                .widthIn(min = 200.dp, max = 1200.dp)
                .fillMaxWidth()
                .padding(80.dp, 60.dp, 80.dp, 0.dp),
        ) {
            val maxWidth = this.maxWidth
            val maxHeight = this.maxHeight

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                FlexIconText(icon = bookSquare(theme.colors.text), iconSize = 16.dp) {
                    Text("Verse of the Day", style = theme.typography.semiText, color = theme.colors.text)
                }
                Box(modifier = Modifier.fillMaxWidth().padding(0.dp, 20.dp, 0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column() {
                            val currentDate = remember { LocalDate.now() }
                            val monthFormatted = currentDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                            Text("$monthFormatted, ${currentDate.year}", style = theme.typography.h1, color = theme.colors.primaryText)
                            Row(modifier = Modifier.wrapContentWidth().padding(0.dp, 7.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
                                FlexDotText("Activity", bgColor = theme.colors.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                FlexDotText("Event", bgColor = theme.colors.primaryHighlight)
                            }
                        }
                        Row(modifier = Modifier.wrapContentWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("This year active days: ", style = TxTStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Medium), color = theme.colors.text)
                            Text("176d", style = TxTStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Medium), color = theme.colors.primaryText)
                        }
                    }
                }

                VerseOfTheDay()
            }
        }

        VerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
    }
}