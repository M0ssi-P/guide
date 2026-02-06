package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import parsers.vgr.VgrViewModel
import parsers.vgr.models.Calendar
import ui.UiState
import ui.theme.LocalTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(currentMonth: YearMonth, today: LocalDate, model: VgrViewModel) {
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val calendarData = model.activeDays.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value + 6) % 7 // Sunday = 7
    val totalDays = lastDayOfMonth.dayOfMonth
    val totalCells = firstDayOfWeek + totalDays
    val weeks = (totalCells / 7) + if (totalCells % 7 > 0) 1 else 0

    Column(Modifier.fillMaxWidth()) {
        for (week in 0 until weeks) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (dayOfWeek in 0..6) {
                    val cellIndex = week * 7 + dayOfWeek
                    val dayNumber = cellIndex - firstDayOfWeek + 1
                    if (dayNumber in 1..totalDays) {
                        val date = currentMonth.atDay(dayNumber)
                        val isToday = date == today
                        val bgColor = Color(0xFFFAFAFA)
                        val textColor = theme.colors.text
                        val recordedQotd = if(calendarData.value is UiState.Success) {
                            val cal = calendarData.value as UiState.Success<List<Calendar.ActiveDay>>
                            cal.data.find { it.day == dayNumber }
                        } else null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.9f)
                                .background(bgColor, RoundedCornerShape(14.dp))
                                .clickable(
                                    interactionSource = interactionSource
                                ) {
                                    model.loadCalendar(recordedQotd?.day ?: LocalDate.now().dayOfMonth )
                                }
                                .zIndex(1f)
                                .then( if(recordedQotd != null) {
                                    Modifier.pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    )
                                } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp, 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    if (recordedQotd != null) {
                                        Box(modifier = Modifier.width(5.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(
                                            theme.colors.primaryHighlight
                                        ))
                                    }
                                }
                                Text(
                                    text = dayNumber.toString(),
                                    style = theme.typography.subs,
                                    color = textColor,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }

                            if(isToday) {
                                val gapPx = with(density) { 5.dp.toPx() }

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .align(Alignment.Center)
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            val width = placeable.width + (gapPx * 2).toInt()
                                            val height = placeable.height + (gapPx * 2).toInt()
                                            layout(width, height) {
                                                // Center it by placing offset = half of added size
                                                placeable.place(gapPx.toInt(), gapPx.toInt())
                                            }
                                        }
                                        .border(2.dp, theme.colors.primary, RoundedCornerShape(14.dp))
                                        .zIndex(20f) // ensures it renders above all neighbors
                                        .pointerInput(Unit) {} // click-through
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            if (week < weeks - 1) {
                Spacer(modifier = Modifier.height(1.dp)) // 1px vertical gap between weeks
            }
        }
    }
}