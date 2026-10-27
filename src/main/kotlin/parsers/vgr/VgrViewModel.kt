package parsers.vgr

import activity.ActivityKind
import activity.ActivityLog
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import db.controller.table.Languages.getCurrentTable
import db.controller.table.Sermons.getFullSermon
import db.controller.table.Sermons.getSermonsList
import db.controller.table.Sermons.getThisDay
import db.runAndReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import loadData
import mvvm.ShareViewModels
import mvvm.ViewModel
import parsers.vgr.models.Calendar
import parsers.vgr.models.IQotd
import parsers.vgr.models.Sermon
import saveData
import ui.UiState
import ui.config.DB
import ui.screens.table.ContentHit
import ui.settings.UserInterfaceSettings
import java.time.LocalDate
import java.time.Month
import java.util.Date
import kotlin.collections.first

private val CONTENT_SEARCH_SQL = """
            SELECT
                s.date_code AS code,
                s.title     AS title,
                p.number    AS paragraphNumber,
                snippet(paragraphs_fts, 0, '[', ']', '…', 8) AS snippet
            FROM paragraphs_fts
            JOIN paragraphs p ON p.id = paragraphs_fts.rowid
            JOIN sections sec ON sec.id = p.section_id
            JOIN sermons s    ON s.id = sec.sermon_id
            WHERE paragraphs_fts MATCH ?
            ORDER BY bm25(paragraphs_fts)
            LIMIT 50
        """.trimIndent()

class VgrViewModel: ViewModel() {
    private val _uiState = MutableStateFlow<UiState<IQotd>>(UiState.Loading)
    val uiState: StateFlow<UiState<IQotd>> = _uiState.asStateFlow()
    private val _thisDay = MutableStateFlow<UiState<List<Sermon>>>(UiState.Loading)
    val thisDay: StateFlow<UiState<List<Sermon>>> = _thisDay
    private val _activeDays = MutableStateFlow<UiState<List<Calendar.ActiveDay>>>(UiState.Loading)
    val activeDays: StateFlow<UiState<List<Calendar.ActiveDay>>> = _activeDays.asStateFlow()
    private val _uiSettings = MutableStateFlow(
        loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings().apply {
            saveData("ui_settings", data = this)
        }
    )
    val uiSettings: StateFlow<UserInterfaceSettings> get() = _uiSettings.asStateFlow()
    private val _sermons = MutableStateFlow<List<Sermon>>(emptyList())
    val sermons: StateFlow<List<Sermon>> = _sermons.asStateFlow()
    private val _currentSermon = MutableStateFlow<UiState<Sermon>>(UiState.Loading)
    val currentSermon: StateFlow<UiState<Sermon>> = _currentSermon.asStateFlow()
    val db = DB.connection("main.db");
    val scrollState = ScrollState(0);

    init {
        viewModelScope.launch {
            uiSettings.collect { settings ->
                val tbLang = withContext(Dispatchers.IO) { db.getCurrentTable(settings.currentTableLanguage) }
                    ?: error("DB id doesn't exist")

                loadCalendar()
                loadSermons(tbLang.dbId!!)
            }
        }
    }

    fun loadSermons(langID: String) {
        viewModelScope.launch {
            val sermons = withContext(Dispatchers.IO) { db.getSermonsList(langID) }
            _sermons.value = sermons

            if (_currentSermon.value == UiState.Loading && sermons.isNotEmpty()) {
                val full = withContext(Dispatchers.IO) {
                    val savedSermon = loadData<Sermon>("current_sermon")
                    val targetSermon = savedSermon ?: sermons.first()
                    db.getFullSermon(targetSermon)
                }
                _currentSermon.value = UiState.Success(full)
            }
        }
    }

    /** Switches The Table to another installed language, staying on the same sermon when that language has it. */
    fun setTableLanguage(tag: String) {
        if (tag == _uiSettings.value.currentTableLanguage) return
        val dateCode = (_currentSermon.value as? UiState.Success)?.data?.dateCode
        viewModelScope.launch {
            val target = withContext(Dispatchers.IO) {
                val tb = db.getCurrentTable(tag)?.dbId ?: return@withContext null
                val list = db.getSermonsList(tb)
                (list.firstOrNull { it.dateCode == dateCode } ?: list.firstOrNull())?.let { it to db.getFullSermon(it) }
            }
            target?.let { (s, full) -> saveData("current_sermon", s); _currentSermon.value = UiState.Success(full) }
            _uiSettings.value = _uiSettings.value.copy(currentTableLanguage = tag).also { saveData("ui_settings", it) }
        }
    }

    fun setCurrentSermon(sermon: Sermon) {
        viewModelScope.launch {
            saveData("current_sermon", sermon)
            ActivityLog.record(ActivityKind.TABLE, "Opened ${sermon.title}", "${sermon.dateCode} · ${sermon.location}", "sermon:${sermon.stringId}", "Open")
            val fullSermon = withContext(Dispatchers.IO) { db.getFullSermon(sermon) }
            _currentSermon.value = UiState.Success(fullSermon)
        }
    }

    fun loadCalendar(day: Int = LocalDate.now().dayOfMonth) {
        val isStored = ShareViewModels.verseOfTheDay.getOrElse(day) {
            0
        }

        if(isStored != 0) {
            val cal = ShareViewModels.cal.value
            val ofDay = cal?.activeDays?.find {
                val lastDay = cal.activeDays.last().day
                it.day == day || it.day == lastDay && day == lastDay + 1
            }
            val currentMonth = LocalDate.now().monthValue
            val code = "${String.format("%02d", currentMonth)}${String.format("%02d", ofDay!!.day)}"
            val tbLang = db.getCurrentTable(uiSettings.value.currentTableLanguage)
            val dayData = db.getThisDay(code, tbLang?.dbId!!)
            _uiState.value = UiState.Success(isStored as IQotd)
            _activeDays.value = UiState.Success(cal.activeDays)
            _thisDay.value = UiState.Success(dayData)
            return
        }

        _uiState.value = UiState.Loading
        _thisDay.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val table = Table()
                val cal = table.getCalendar()
                ShareViewModels.cal.value = cal
                val ofDay = cal.activeDays.find {
                    val lastDay = cal.activeDays.last().day
                    it.day == day || it.day == lastDay && day == lastDay + 1
                }
                val result = table.qotd(ofDay!!)
                val currentMonth = LocalDate.now().monthValue
                val code = "${String.format("%02d", currentMonth)}${String.format("%02d", ofDay.day)}"
                _uiState.value = UiState.Success(result)
                _activeDays.value = UiState.Success(cal.activeDays)
                ShareViewModels.verseOfTheDay[day] = result
                val tbLang = db.getCurrentTable(uiSettings.value.currentTableLanguage)
                val dayData = db.getThisDay(code, tbLang?.dbId!!)
                _thisDay.value = UiState.Success(dayData)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data")
            }
        }
    }

    suspend fun searchContent(rawQuery: String): List<ContentHit> = withContext(Dispatchers.IO) {
        val ftsQuery = rawQuery.toFtsQuery()
        if (ftsQuery.isBlank()) return@withContext emptyList()

        db.prepareStatement(CONTENT_SEARCH_SQL).use { ps ->
            ps.setString(1, ftsQuery)
            ps.executeQuery().use { rs ->
                val results = mutableListOf<ContentHit>()
                while (rs.next()) {
                    results += ContentHit(
                        code = rs.getString("code"),
                        title = rs.getString("title"),
                        paragraphNumber = rs.getInt("paragraphNumber"),
                        snippet = rs.getString("snippet")
                    )
                }
                results
            }
        }
    }

    fun searchSermons(rawQuery: String): List<Sermon> {
        val term = rawQuery.trim()
        if (term.isBlank()) return sermons.value.ifEmpty { emptyList() }

        return sermons.value.filter {
            it.title.contains(term, ignoreCase = true) || it.dateCode.contains(term, ignoreCase = true)
        }.take(50)
    }

    fun onDispose() {
        viewModelScope.cancel()
    }
}
fun String.toFtsQuery(): String =
    this.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { "\"${it.replace("\"", "\"\"")}\"*" }