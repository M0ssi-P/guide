package db

import backblazeb2.BackBlazeB2
import backblazeb2.File
import backblazeb2.actions.B2Credentials
import db.controller.songbooks.Books.importBook
import db.controller.songbooks.Books.importLyrics
import db.controller.songbooks.Books.importSong
import db.controller.table.Languages.getAllLanguages
import db.controller.table.Languages.getLanguageDbID
import db.controller.table.Languages.importTables
import db.controller.table.Sermons.RefillSermon
import db.controller.table.importLanguageSuspand
import db.controller.table.importLineSuspend
import db.controller.table.importParagraphSuspend
import db.controller.table.importSectionSuspend
import db.controller.table.importSermonSuspend
import extractZipToMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import loadData
import models.IProviderStats
import models.ISongDetails
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
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.math.roundToInt

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
        DB.connection("main.db")
    )
    private val _languageData = MutableStateFlow<UiState<List<ILanguage>>>(UiState.Loading)
    val languageData: StateFlow<UiState<List<ILanguage>>> = _languageData.asStateFlow()
    val db get() = _db.value
    val table = Table();
    lateinit var b2: BackBlazeB2;

    init {
        viewModelScope.launch {
            b2 = BackBlazeB2(
                B2Credentials(
                    applicationKeyId = "00522d1ad344c240000000002",
                    applicationKey = "K005792uG9GXuDBM25nRpYYXQb6/Qw8"
                )
            )

            b2.authorize()
            startTableStatement(db)
            createUpdatedAtTrigger(db)
            loadLanguagesAndSave()
        }
    }

    fun loadLanguagesAndSave() {
        viewModelScope.launch {
            _languageData.value = UiState.Loading
            if(db.checkIfEmpty("languages")) {
                val languages = table.getAllLanguages()

                languages.mapIndexed { index, it ->
                    db.importLanguageSuspand(it)
                }
            }

            val getLanguageData = db.getAllLanguages()
            _languageData.value = UiState.Success(getLanguageData.filter { it.tag == "en" || it.tag == "sw" })
        }
    }

    @OptIn(ExperimentalPathApi::class)
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

                val tempDir = Files.createTempDirectory("tables-")
                val zipPath = tempDir.resolve("${uiSettings.value.contentLanguage}-table.zip")

                val bucket = b2.bucket("the-guide")
                val f = bucket.file("tables/${uiSettings.value.contentLanguage}-table.zip")

                downloadZipToTemp(f, zipPath)

                val extracted = extractDbAndJson(zipPath, tempDir)

                val id = db.importTables(language)

                delay(100)

                mergeDb(extracted.dbPath, id)

                tempDir.deleteRecursively()

                listOf<String>("only-believe.zip", "collection-de-cantiques.zip", "nyimbo-za-wokovu.zip", "nyimbo-za-mungu.zip").forEach { saveName ->
                    val f = bucket.file("songbooks/$saveName")
                    val stream = f.createReadStream(
                        onProgress = { read, total ->
                            val percent = ((read.toDouble() / total.toDouble()) * 100).roundToInt()
                            _installationStatus.value = "Installation progress: downloading songs Infobase $percent% / 100%"
                        }
                    )

                    val zipBytes = stream.readBytes()

                    val extractedJsonFiles = extractZipToMemory(zipBytes)

                    var id: String? = null;

                    for ((name, content) in extractedJsonFiles) {
                        if (name.startsWith("stat")) {
                            val data = Json.decodeFromString<IProviderStats>(content)

                            val isFound = db.checkIfOneExist(
                                """
                                 SELECT 1 FROM books WHERE save_name = ? LIMIT 1
                                 """.trimIndent(),
                                saveName.replace(".zip", "")
                            )

                            if(!isFound) {
                                db.importBook(data) {
                                    id = it
                                }
                            }
                        } else {
                            if(!id.isNullOrEmpty()) {
                                val data = Json.decodeFromString<List<ISongDetails>>(content)

                                data.forEach { song ->
                                    db.importSong(song, id!!) { songId, lyrics ->
                                        db.importLyrics(lyrics, songId)
                                    }
                                }
                            }
                        }
                    }
                }

                setShouldHide(false)
            }
        }
    }

    fun mergeDb(incomingDb: Path, languageId: String) {
        _db.value.autoCommit = false
        try {
            _db.value.createStatement(
            ).use { stmt ->
                stmt.execute("ATTACH DATABASE '${incomingDb.toAbsolutePath()}' AS incoming")

                stmt.execute("""
                INSERT OR IGNORE INTO sermons (
                id,
                language_id,
                date,
                date_code,
                language,
                location,
                next_sermon_date,
                prev_sermon_date,
                title,
                sort_date,
                minutes,
                is_cab,
                total_sections,
                c,
                i,
                ct,
                created_at,
                updated_at
                )
                SELECT
                id,
                '$languageId',
                date,
                date_code,
                language,
                location,
                next_sermon_date,
                prev_sermon_date,
                title,
                sort_date,
                minutes,
                is_cab,
                total_sections,
                c,
                i,
                ct,
                created_at,
                updated_at
                FROM incoming.sermons
            """.trimIndent())

                stmt.execute("""
                INSERT OR IGNORE INTO sections
                SELECT * FROM incoming.sections
            """.trimIndent())

                stmt.execute("""
                INSERT OR IGNORE INTO paragraphs
                SELECT * FROM incoming.paragraphs
            """.trimIndent())

                stmt.execute("""
                INSERT OR IGNORE INTO lines
                SELECT * FROM incoming.lines
            """.trimIndent())

                _db.value.commit()

                stmt.execute("DETACH DATABASE incoming")
            }
        } catch (e: Exception) {
            _db.value.rollback()
            throw e
        } finally {
            _db.value.autoCommit = true
        }
    }

    data class ExtractedData(
        val dbPath: Path,
        val jsonContents: Map<String, String>
    )

    suspend fun downloadZipToTemp(
        file: File,
        zipPath: Path
    ) {
        file.createReadStream(
            onProgress = { read, total ->
                val percent = ((read.toDouble() / total.toDouble()) * 100).roundToInt()
                _installationStatus.value = "Installation progress: downloading Infobase - $percent% / 100%"
            }
        ).use { input ->
            Files.newOutputStream(zipPath).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun extractDbAndJson(zipPath: Path, tempDir: Path): ExtractedData {
        val jsonContents = mutableMapOf<String, String>()
        var dbPath: Path? = null

        ZipInputStream(Files.newInputStream(zipPath)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outPath = tempDir.resolve(entry.name)

                if (!entry.isDirectory) {
                    if (entry.name.endsWith(".db")) {
                        // Save db to temp
                        Files.newOutputStream(outPath).use {
                            zis.copyTo(it)
                        }
                        dbPath = outPath
                    } else if (entry.name.endsWith(".json")) {
                        // Read JSON directly into memory
                        val content = zis.readBytes().decodeToString()
                        jsonContents[entry.name] = content
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        return ExtractedData(
            dbPath = dbPath ?: error("No .db found in zip"),
            jsonContents = jsonContents
        )
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

    fun onDispose() {
        viewModelScope.cancel()
    }
}