package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(currentMonth: YearMonth, today: LocalDate) {
    val theme = LocalTheme.current
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
    val totalDays = lastDayOfMonth.dayOfMonth
    val totalCells = firstDayOfWeek + totalDays
    val weeks = (totalCells / 7) + if (totalCells % 7 > 0) 1 else 0

    Column(Modifier.fillMaxWidth()) {
        for (week in 0 until weeks) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayOfWeek in 0..6) {
                    val cellIndex = week * 7 + dayOfWeek
                    val dayNumber = cellIndex - firstDayOfWeek + 1
                    if (dayNumber in 1..totalDays) {
                        val date = currentMonth.atDay(dayNumber)
                        val isToday = date == today
                        val bgColor = if (isToday) Color(0xFF109DF5) else Color(0xFFFAFAFA)
                        val textColor = if (isToday) Color.White else theme.colors.text

                        Box(
                            modifier = Modifier// Makes squares auto-fit width
                                .aspectRatio(1f)
                                .weight(1f)
                                .padding(vertical = 1.dp, horizontal = 4.dp)
                                .background(bgColor, RoundedCornerShape(14.dp))
                                .clickable { println("Clicked $date") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}