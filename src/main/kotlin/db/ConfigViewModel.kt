package db

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import loadData
import mvvm.ViewModel
import parsers.vgr.Table
import parsers.vgr.models.Sermon
import saveData
import tryWithSuspend
import ui.config.DB
import ui.config.createUpdatedAtTrigger
import ui.config.startTableStatement
import ui.settings.UserInterfaceSettings

class ConfigViewModel: ViewModel() {
    val uiSettings = loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings().apply {
        saveData("ui_settings", data = this)
    }
    val table = Table();
    val db = DB.connection("table_${uiSettings.contentLanguage}.db");

    init {
        startTableStatement(db)
        createUpdatedAtTrigger(db)
        initializeAndConfigureDB()
    }

    fun initializeAndConfigureDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val semLimit = Semaphore(15)

            val isFound = db.checkIfOneExist(
                """
                    SELECT * FROM sermons WHERE date_code = ? LIMIT 1
                """.trimIndent(),
                "62-0909E"
            )

            if(isFound) {
                return@launch
            }

            if(db.checkIfEmpty("languages")) {
                val languages = table.getAllLanguages()

                coroutineScope {
                    languages.mapIndexed { index, it ->
                        async(Dispatchers.IO) {
                            semLimit.withPermit {
                                println("downloading language... (${index + 1}/${languages.size})")
                                db.importLanguageSuspand(it)
                            }
                        }
                    }.awaitAll()
                }
            }

            tryWithSuspend {
                val lang = table.getAllLanguages().find { it.iso63901 == uiSettings.contentLanguage }
                val sermons = table.getAllSermons(lang!!)
                val getLanguageDbId = db.getLanguageDbID(uiSettings.contentLanguage) ?: return@tryWithSuspend

                coroutineScope {
                    sermons.mapIndexed { index, sermon ->
                        async(Dispatchers.IO) {
                            semLimit.withPermit {
                                println("downloading sermon ${index + 1}/${sermons.size}: ${sermon.title}")

                                saveSermonDeep(
                                    sermon = sermon,
                                    langDbId = getLanguageDbId
                                )
                            }
                        }
                    }.awaitAll()
                }
            }


        }
    }

    private suspend fun saveSermonDeep(
        sermon: Sermon,
        langDbId: String
    ) {
        // Save sermon
        val sermonId = db.importSermonSuspend(sermon, langDbId)

        // Load full sermon content
        val full = table.getSermon(sermon)

        // Update missing fields
        db.RefillSermon(full, sermonId)

        // Save sections, paragraphs, lines SEQUENTIALLY
        for (section in full.sections.orEmpty()) {
            val secId = db.importSectionSuspend(section, sermonId)

            for (paragraph in section.content) {
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