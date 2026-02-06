package parsers.vgr

import db.controller.table.Sermons.getThisDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import loadData
import mvvm.ViewModel
import parsers.vgr.models.Calendar
import parsers.vgr.models.IQotd
import parsers.vgr.models.Sermon
import saveData
import ui.UiState
import ui.config.DB
import ui.settings.UserInterfaceSettings
import java.time.LocalDate
import java.time.Month
import java.util.Date

class VgrViewModel: ViewModel() {
    private val _uiState = MutableStateFlow<UiState<IQotd>>(UiState.Loading)
    val uiState: StateFlow<UiState<IQotd>> = _uiState.asStateFlow()
    private val _thisDay = MutableStateFlow<UiState<List<Sermon>>>(UiState.Loading)
    val thisDay: StateFlow<UiState<List<Sermon>>> = _thisDay
    private val _activeDays = MutableStateFlow<UiState<List<Calendar.ActiveDay>>>(UiState.Loading)
    val activeDays: StateFlow<UiState<List<Calendar.ActiveDay>>> = _activeDays.asStateFlow()
    val uiSettings = loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings().apply {
        saveData("ui_settings", data = this)
    }
    val db = DB.connection("table_${uiSettings.contentLanguage}.db");

    init {
        loadCalendar()
    }

    fun loadCalendar(day: Int = LocalDate.now().dayOfMonth) {
        _uiState.value = UiState.Loading
        _thisDay.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val table = Table()
                val cal = table.getCalendar()
                val ofDay = cal.activeDays.find {
                    val lastDay = cal.activeDays.last().day
                    it.day == day || it.day == lastDay && day == lastDay + 1
                }
                val result = table.qotd(ofDay!!)

                val currentMonth = LocalDate.now().monthValue
                val code = "${String.format("%02d", currentMonth)}${String.format("%02d", ofDay.day)}"
                _uiState.value = UiState.Success(result)
                _activeDays.value = UiState.Success(cal.activeDays)
                val dayData = db.getThisDay(code)
                _thisDay.value = UiState.Success(dayData)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data")
            }
        }
    }

    fun onDispose() {
        viewModelScope.cancel()
    }
}