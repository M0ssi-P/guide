package components.global

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.YearMonth

@Composable
@Preview
fun Calendar() {
    val theme = LocalTheme.current
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Day headers ---
        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (day in dayNames) {
                Box(modifier = Modifier.size(width = 53.dp, height = 32.dp).background(
                    color = Color(0x20767680),
                    shape = RoundedCornerShape(16.dp)
                ), contentAlignment = Alignment.Center) {
                    Text(
                        text = day, color = theme.colors.text, style = theme.typography.subs
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        CalendarGrid(currentMonth, today)
    }
}