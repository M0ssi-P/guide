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
import androidx.compose.foundation.layout.padding
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
import parsers.vgr.VgrViewModel
import parsers.vgr.models.Calendar
import ui.UiState
import java.time.LocalDate
import java.time.YearMonth

@Composable
@Preview
fun Calendar(model: VgrViewModel) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (day in dayNames) {
                val isToday = today.dayOfWeek.name.substring(0, 3).equals(day, ignoreCase = true);
                val color = if (isToday) theme.colors.primaryText else theme.colors.text

                Box(modifier = Modifier.weight(1f).background(
                    color = Color(0x20767680),
                    shape = RoundedCornerShape(16.dp)
                ), contentAlignment = Alignment.Center) {
                    Text(
                        text = day, color = color, style = theme.typography.subs,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        CalendarGrid(currentMonth, today, model)
    }
}