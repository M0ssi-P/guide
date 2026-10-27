package ui.screens.table

import com.composables.projectorIcon
import presentation.DeckSource
import presentation.ProjectionDeck
import ui.screens.projection.deckItems
import navigation.Screen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.locationIcon
import com.composables.settingsIcon
import com.mossip.auraplayer.engine.PlayerState
import components.global.ReaderTab
import components.global.TabPill
import formatTime
import icons.bookmarkIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import navigation.LocalModal
import navigation.LocalTabs
import navigation.dismiss
import navigation.show
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import parsers.vgr.SermonMedia
import parsers.vgr.VgrViewModel
import parsers.vgr.models.Sermon
import ui.UiState
import components.global.crossfadeOn
import ui.theme.LocalTheme

@Composable
fun TableDesktopScreen(modifier: Modifier = Modifier) {
    val mode = remember { mutableStateOf(SearchMode.SERMONS) }
    val state = rememberSampleTableState()
    val theme = LocalTheme.current
    val modal  = LocalModal.current
    val currentTab = LocalTabs.current.current
    val vgrVM = currentTab.viewModelStore.getOrCreate("HomeVM-${currentTab.id}", {
        VgrViewModel()
    })
    val currentSermon by vgrVM.currentSermon.collectAsState()
    val tableSettings by vgrVM.uiSettings.collectAsState()
    val langCode = remember(tableSettings.currentTableLanguage) { tableLanguageCode(tableSettings.currentTableLanguage) }
    val openLanguages = { modal.show { TableLanguageModal(vgrVM) { modal.dismiss() } } }
    val globalPlayer = LocalPlayer.current
    val engine = LocalPlayerState.current
    val nowPlaying by globalPlayer.nowPlaying.collectAsState()

    val scope = rememberCoroutineScope()

    var sermonResults by remember { mutableStateOf<List<Sermon>>(emptyList()) }
    var contentResults by remember { mutableStateOf<List<ContentHit>>(emptyList()) }

    val playerState by engine.playerState.collectAsState()
    val duration by engine.duration.collectAsState()
    val currentTime by engine.currentTime.collectAsState()

    LaunchedEffect(state.searchQuery, mode.value) {
        delay(180)
        try {
            when (mode.value) {
                SearchMode.SERMONS -> sermonResults = vgrVM.searchSermons(rawQuery = state.searchQuery)
                SearchMode.CONTENT -> contentResults = vgrVM.searchContent(rawQuery = state.searchQuery)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .drawBehind {
                val goldColor = TableColors.Gold.copy(alpha = 0.08f)

                val radiusX = size.width * 0.6f

                val verticalScale = 0.66f

                val centerX = size.width * 0.5f
                val centerY = size.height * 0.08f

                withTransform({
                    scale(
                        scaleX = 1f,
                        scaleY = verticalScale,
                        pivot = Offset(centerX, 0f)
                    )
                }) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to goldColor,
                            0.6f to Color.Transparent, // transparent 60%
                            1.0f to Color.Transparent,
                            center = Offset(centerX, centerY),
                            radius = radiusX
                        )
                    )
                }
            }
    ) {

        Column(Modifier.fillMaxSize()) {
            Titlebar(
                state = state,
                // Shown directly, not via state.overlay, so the backdrop's dismiss can't leave overlay stuck on Search.
                onSearchClick = {
                    modal.show {
                        SearchModal(
                            query = state.searchQuery,
                            onQueryChange = { state.searchQuery = it },
                            onModeChange = { mode.value = it },
                            currentSermon = when(val se = currentSermon) {
                                is UiState.Success -> se.data
                                else -> null
                            },
                            sermonResults = sermonResults,
                            contentResults = contentResults,
                            onOpenSermon = {
                                vgrVM.setCurrentSermon(it)
                            },
                            onOpenContentHit = {},
                            mode = mode.value,
                            onDismiss = { modal.dismiss() }
                        )
                    }
                },
                onBrowseClick = { state.overlay = TableOverlay.Browse },
                onMenuClick = { state.overlay = TableOverlay.Menu },
                languageCode = langCode,
                onLanguageClick = openLanguages
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                Crossfade(targetState = state.mode, animationSpec = tween(400), modifier = Modifier.fillMaxSize().crossfadeOn(tableSettings.currentTableLanguage)) { mode ->
                    when (mode) {
                        TableMode.Listen -> ListenView(
                            info = state.info,
                            playing = playerState == PlayerState.PLAYING,
                            modifier = Modifier.fillMaxSize()
                        )
                        TableMode.Read -> when(val data = currentSermon) {
                            is UiState.Success -> Box(Modifier.fillMaxSize()) {
                                val sermon = data.data
                                val tablePrefs by ui.screens.reading.ReadingStores.table.prefs
                                if (tablePrefs.style == ui.screens.reading.ReadStyle.SCROLL) TableScrollView(sermon, tablePrefs)
                                else Box(Modifier.fillMaxSize()) { StudyReadView(
                                    document = StudyDocument(
                                        title = sermon.title,
                                        sessionLabel = "${sermon.dateCode} - ${sermon.location}",
                                        pullQuote = "We are so glad tonight to know that we have in our beings immortal Life.",
                                        sections = sermon.sections ?: emptyList(),
                                        outline = listOf(
                                            StudySection(
                                                label = "",
                                                state = SectionState.CURRENT
                                            )
                                        ),
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                    sermon = sermon,
                                    prefs = tablePrefs,
                                ) }
                                TableSettingsButton(Modifier.align(Alignment.TopEnd).padding(16.dp))
                            }
                            else -> StudyReadView(
                                document = StudyDocument(
                                    title = "Not Found",
                                    sessionLabel = "",
                                    pullQuote = "",
                                    sections = emptyList(),
                                    outline = listOf(
                                        StudySection(
                                            label = "",
                                            state = SectionState.CURRENT
                                        )
                                    ),
                                ),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                when(val data = currentSermon) {
                    is UiState.Success -> {
                        val sermon = data.data
                        FloatingPlayer(
                            player = engine,
                            track = PlayerTrack(
                                title = sermon.title,
                                breadcrumb = "${sermon.dateCode} - ${sermon.location}",
                                languageCode = langCode,
                                elapsedLabel = formatTime(currentTime),
                                remainingLabel = formatTime(duration),
                                progressFraction = 0.5f,
                                isPlaying = playerState == PlayerState.PLAYING,
                                readAlongOn = false
                            ),
                            onLanguageClick = openLanguages,
                            onTogglePlay = {
                                val playSermon = { url: String ->
                                    globalPlayer.loadNowPlaying(GlobalPlayerViewModel.NowPlaying(
                                        id = sermon.stringId,
                                        title = sermon.title,
                                        subtitle = "${sermon.dateCode} - ${sermon.location}",
                                        type = GlobalPlayerType.SERMON,
                                        isPlaying = playerState == PlayerState.PLAYING,
                                        currentPosition = 0L,
                                        url = url.trim(),
                                    ))
                                    engine.setPause(false)
                                    state.playing = playerState == PlayerState.PLAYING
                                }
                                when {
                                    nowPlaying?.id == sermon.stringId -> {
                                        engine.setPause(!state.playing)
                                        state.playing = playerState == PlayerState.PLAYING
                                    }
                                    else -> scope.launch {
                                        // Reuse a copy we or another app already saved before asking to download.
                                        val local = SermonMedia.findLocal(sermon)
                                        if (local != null) playSermon(local.absolutePath)
                                        else modal.show {
                                            ListenModal(
                                                sermon = sermon,
                                                onPlay = { playSermon(it); modal.dismiss() },
                                                onDismiss = { modal.dismiss() }
                                            )
                                        }
                                    }
                                }
                            },
                            onSkipBack = { state.progress = (state.progress - 10f / state.totalSeconds).coerceIn(0f, 1f) },
                            onSkipForward = { state.progress = (state.progress + 10f / state.totalSeconds).coerceIn(0f, 1f) },
                            onToggleReadAlong = { state.repeatOn = !state.repeatOn },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp)
                        )
                    }

                    else -> {}
                }
            }
        }

        when (state.overlay) {
            TableOverlay.Browse -> BrowseModal(
                tiles = state.sermonTiles,
                onClose = { state.overlay = TableOverlay.None },
                onTileClick = { state.overlay = TableOverlay.None },
                modifier = Modifier.align(Alignment.Center)
            )
            TableOverlay.Search -> Unit
            TableOverlay.Menu -> MenuDrawer(
                items = state.menuItems,
                onClose = { state.overlay = TableOverlay.None },
                onItemClick = { item ->
                    state.overlay = TableOverlay.None
                    // the multi-purpose projection opens on the sermon being read; Bible and Songs are a tab away there
                    if (item.label == "Project") {
                        (currentSermon as? UiState.Success)?.data?.let { s ->
                            ProjectionDeck.load(DeckSource.TABLE, s.title, "${s.dateCode} · ${s.location}", s.deckItems(), 0, s.copy(sections = null))
                        }
                        currentTab.navigate(Screen.Projection)
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            )
            TableOverlay.None -> Unit
        }

//        if (state.notesDrawerOpen) {
//            NotesDrawer(
//                notes = state.notes,
//                onClose = { state.notesDrawerOpen = false },
//                modifier = Modifier.align(Alignment.CenterEnd)
//            )
//        }
    }
}

@Composable
fun ModeSwitch(mode: TableMode, onModeChange: (TableMode) -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current

    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(theme.colors.popup)
            .border(1.dp, theme.colors.border, RoundedCornerShape(999.dp))
            .padding(3.dp)
    ) {
        TabPill("Read", mode == TableMode.Read) { onModeChange(TableMode.Read) }
        TabPill("Listen", mode == TableMode.Listen) { onModeChange(TableMode.Listen) }
    }
}

private fun formatSeconds(seconds: Float): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

class TableState(
    val info: NowPlayingInfo,
    initialParagraphs: List<ReadingParagraph>,
    val totalSeconds: Float
) {
    var mode by mutableStateOf(TableMode.Read)
    var playing by mutableStateOf(false)
    var progress by mutableStateOf(0.06f)
    var repeatOn by mutableStateOf(true)
    var overlay by mutableStateOf(TableOverlay.None)
    var notesDrawerOpen by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    var paragraphs by mutableStateOf(initialParagraphs)
    var highlights by mutableStateOf(listOf<Highlight>())
    var notes by mutableStateOf(listOf<NoteEntry>())

    val sermonTiles = listOf(
        SermonTile("60-1206", "The Smyrnaean Church Age"),
        SermonTile("47-0412", "Faith Is The Substance"),
        SermonTile("49-1225", "Deity Of Jesus Christ"),
        SermonTile("50-0010", "Moses"),
        SermonTile("60-1204", "The Ephesian Church Age"),
        SermonTile("63-1110m", "Souls In Prison Now")
    )

    val menuItems = listOf(
        MenuItem("Project", ::projectorIcon),
        MenuItem("Bookmarks", ::bookmarkIcon),
        MenuItem("Language", ::locationIcon),
        MenuItem("Settings", ::settingsIcon),
        MenuItem("About", ::bookmarkIcon)
    )
}

@Composable
private fun rememberSampleTableState(): TableState {
    val info = remember {
        NowPlayingInfo(code = "60-1206", title = "The Smyrnaean\nChurch Age", location = "Jeffersonville, IN")
    }
    val paragraphs = remember {
        listOf(
            ReadingParagraph(
                id = "p1",
                verseNumber = 1,
                text = "Most Glorious Father, we are so glad tonight to know that we have in our beings immortal Life. The Life of our God separated by tongues of fire and set upon each of them, and they were all filled with the Holy Ghost."
            ),
            ReadingParagraph(
                id = "p2",
                verseNumber = 2,
                text = "Lord, seeing the climax of all ages, time is fixing to run out and Eternity sets in. Father God, we are so glad to know that we have a hiding place from all the confusion that's in the world today.",
                isCurrent = true
            ),
            ReadingParagraph(
                id = "p3",
                verseNumber = 3,
                text = "We thank Thee for this privilege that we have this afternoon, Lord, of gathering together to open up Thy Word and to see the things that You have promised."
            )
        )
    }
    return remember { TableState(info = info, initialParagraphs = paragraphs, totalSeconds = 300f) }
}
