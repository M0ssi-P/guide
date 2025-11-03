package parsers.vgr

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mvvm.ViewModel
import parsers.vgr.models.IQotd
import ui.UiState
import java.time.LocalDate

class VgrViewModel: ViewModel() {
    private val _uiState = MutableStateFlow<UiState<IQotd>>(UiState.Loading)
    val uiState: StateFlow<UiState<IQotd>> = _uiState.asStateFlow()

    init {
        loadCalendar()
    }

    fun loadCalendar(day: Int = LocalDate.now().dayOfMonth) {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val table = Table()
                val cal = table.getCalendar()
                val ofDay = cal.activeDays.find {
                    val lastDay = cal.activeDays.last().day
                    it.day == day || it.day == lastDay && day == lastDay + 1
                }
                val result = table.qotd(ofDay!!)
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data")
            }
        }
    }

    fun onDispose() {
        viewModelScope.cancel()
    }
}