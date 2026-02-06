package db

import db.controller.table.Languages.getAllLanguages
import db.controller.table.Languages.getLanguageDbID
import db.controller.table.Sermons.RefillSermon
import db.controller.table.importLanguageSuspand
import db.controller.table.importLineSuspend
import db.controller.table.importParagraphSuspend
import db.controller.table.importSectionSuspend
import db.controller.table.importSermonSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import loadData
import mvvm.ViewModel
import parsers.bible.models.ILanguage
import parsers.vgr.Table
import parsers.vgr.models.Sermon
import saveData
import tryWithSuspend
import ui.UiState
import ui.config.DB
import ui.config.createUpdatedAtTrigger
import ui.config.startTableStatement
import ui.settings.UserInterfaceSettings
import java.util.concurrent.atomic.AtomicInteger

class ConfigViewModel: ViewModel() {
    private val _uiSettings = MutableStateFlow(
        loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings().apply {
            saveData("ui_settings", data = this)
        }
    )
    private val _shouldHide = MutableStateFlow(
        loadData<Boolean>("should_hide") ?: true
    )
    private val _installationStatus = MutableStateFlow<String>("Initializing...")
    private val _installationStatusHeader = MutableStateFlow<String>("Initializing...")
    private val _stage = MutableStateFlow(0)
    val stage = _stage.asStateFlow()
    val installationStatus = _installationStatus.asStateFlow()
    val installationStatusHeader = _installationStatusHeader.asStateFlow()
    val shouldHide = _shouldHide.asStateFlow()
    val uiSettings = _uiSettings.asStateFlow()
    private val _db = MutableStateFlow(
        DB.connection("table_${uiSettings.value.contentLanguage}.db")
    )
    private val _languageData = MutableStateFlow<UiState<List<ILanguage>>>(UiState.Loading)
    val languageData: StateFlow<UiState<List<ILanguage>>> = _languageData.asStateFlow()
    val db get() = _db.value
    val table = Table();
    val semLimit = Semaphore(15)

    val completed = AtomicInteger(0)

    init {
        viewModelScope.launch {
            _uiSettings.collect { settings ->
                _db.value = DB.connection("table_${settings.contentLanguage}.db")
                startTableStatement(db)
                createUpdatedAtTrigger(db)
                loadLanguagesAndSave()
            }
        }
    }

    fun loadLanguagesAndSave() {
        viewModelScope.launch(Dispatchers.IO) {
            _languageData.value = UiState.Loading
            tryWithSuspend {
                if(db.checkIfEmpty("languages")) {
                    val languages = table.getAllLanguages()

                    coroutineScope {
                        languages.mapIndexed { index, it ->
                            async(Dispatchers.IO) {
                                semLimit.withPermit {
                                    db.importLanguageSuspand(it)
                                }
                            }
                        }.awaitAll()
                    }
                }

                val getLanguageData = db.getAllLanguages()
                _languageData.value = UiState.Success(getLanguageData)
            }
        }
    }

    fun loadTableData() {
        viewModelScope.launch(Dispatchers.IO) {
            val isFound = db.checkIfOneExist(
                """
                    SELECT * FROM sermons WHERE date_code = ? LIMIT 1
                """.trimIndent(),
                "62-0909E"
            )

            if (isFound) {
                setShouldHide(bool = false)
                return@launch
            }

            tryWithSuspend {
                _installationStatusHeader.value = "Installing The Table."
                val language = table.getAllLanguages().find { it.iso63901 == uiSettings.value.contentLanguage } ?: return@tryWithSuspend

                val getLanguageDbId = db.getLanguageDbID(language.tag) ?: return@tryWithSuspend
                val sermons = table.getAllSermons(language, langCode = language.tag).filter { sermon -> !sermon.isCab }

                coroutineScope {
                    sermons.mapIndexed { index, sermon ->
                        async(Dispatchers.IO) {
                            semLimit.withPermit {
                                saveSermonDeep(
                                    sermon = sermon,
                                    langDbId = getLanguageDbId,
                                    language.tag
                                )

                                val done = completed.incrementAndGet()

                                _installationStatus.value =
                                    "Installed $done / ${sermons.size} sermons"
                            }
                        }
                    }.awaitAll()
                    _shouldHide.value = false
                    saveData("should_hide", false)
                }
            }
        }
    }

    fun setLanguage(langFrom: String = "Table", lang: ILanguage) {
        val current = _uiSettings.value;

        val updated = current.copy(contentLanguage = lang.tag, downloadedLanguages = current.downloadedLanguages.filterNot {
            it.second == lang.tag
        }.plus(Triple(langFrom, lang.tag, lang)))

        _uiSettings.value = updated
        saveData("ui_settings", updated)

        _stage.value = 1

        viewModelScope.launch {
            loadTableData()
        }
    }

    fun setShouldHide(bool: Boolean) {
        _shouldHide.value = bool
        saveData("should_hide", bool)
    }

    private suspend fun saveSermonDeep(
        sermon: Sermon,
        langDbId: String,
        language: String? = null
    ) {
        val full = table.getSermon(sermon, language)

        val sermonId = db.importSermonSuspend(full, langDbId)
        db.RefillSermon(full, sermonId)
        full.sections.orEmpty().mapIndexed { index, section ->
            val secId = db.importSectionSuspend(section, sermonId)

            section.content.mapIndexed { i, paragraph ->
                val paraId = db.importParagraphSuspend(paragraph, secId)

                for (line in paragraph.content) {
                    db.importLineSuspend(line, paraId)
                }
            }
        }
    }

    fun onDispose() {
        viewModelScope.cancel()
    }
}