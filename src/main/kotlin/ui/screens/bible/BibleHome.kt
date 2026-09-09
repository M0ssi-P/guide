package ui.screens.bible

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mossip.auraplayer.engine.PlayerState
import components.global.RailView
import components.global.ReaderTab
import components.global.layeredRadialBackground
import getCurrentVerseSafe
import mvvm.BibleViewModel
import navigation.LocalTabs
import perVerseMode
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import player.LocalPlayerState
import player.PlayerBar
import player.PlayerUI
import player.loadChapters
import ui.theme.LocalTheme

@Composable
fun BibleHome() {
    val theme = LocalTheme.current
    val currentTab = LocalTabs.current.current
    val player = LocalPlayer.current
    val engine = LocalPlayerState.current
    val duration by engine.duration.collectAsState()
    val currentTime by engine.currentTime.collectAsState()
    val playerState by engine.playerState.collectAsState()
    val speed by engine.speed.collectAsState()
    val model = currentTab.viewModelStore.getOrCreate("BibleVM-${currentTab.id}", {
        BibleViewModel().apply {
            this.initilize()
        }
    })
    val verseCount = model.chapterContent?.chapter?.lastOrNull {
        !it.content.isNullOrEmpty()
    }?.content?.lastOrNull()?.verses?.lastOrNull()?.number ?: 1
    var projection by remember { mutableStateOf(false) }
    var showChrome by remember { mutableStateOf(true) }
    val selectedVerse = remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    val chapterName = model.currentChapter?.usfm?.replace(".", "-")
    val fontStep = remember { mutableStateOf(1) }
    val sub = loadChapters(chapterName)

    LaunchedEffect(model.chapterContent) {
        model.chapterContent?.let {
            it.audio?.let { audio ->
                audio.find { i -> i.default }.let { default ->
                    player.loadNowPlaying(GlobalPlayerViewModel.NowPlaying(
                        id = default?.id?.toString() ?: "",
                        title = if(model.currentChapter != null && model.currentChapter?.human != null) {
                            "${model.currentBook?.human} Chapter ${model.currentChapter?.human}"
                        } else "Untitled",
                        subtitle = "The Listener's Bible:",
                        type = GlobalPlayerType.BIBLE,
                        isPlaying = false,
                        currentPosition = 0L,
                        url = "https:${default?.playOrDwnLoad?.formattedMp3?.split("?")?.first()}",
                    ))
                }
            }
        }
    }

    val chromeVisible = !projection || showChrome

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                val lyrics = sub?.find { it.default }?.lyrics
                val lyric = getCurrentVerseSafe(currentTime, lyrics)

                val flatVerses = remember(model.chapterContent) {
                    model.chapterContent?.chapter?.let { perVerseMode(it) } ?: emptyList()
                }
                val highlightedVerseNumber = lyric?.verse?.toIntOrNull()
                val activeVerse = remember(flatVerses, highlightedVerseNumber) {
                    highlightedVerseNumber?.let { num -> flatVerses.find { it.number == num } }
                }

                RailView(
                    verseCount = verseCount,
                    activeVerseIndex = if(activeVerse != null) activeVerse.number - 1 else -1,
                    onSeekVerse = { },
                    visible = chromeVisible,
                )
                BibleContents(
                    model, Modifier.weight(1f).fillMaxHeight(),
                    fontStep,
                    sub,
                    selectedVerse,
                    verseCount
                )
//                Box(Modifier.align(Alignment.BottomCenter)) {
//                    PlayerUI(engine)
//                }
            }
        }
        PlayerBar(
            playing = PlayerState.PLAYING == playerState,
            onPlayPause = {
                val isPlaying = playerState == PlayerState.PLAYING
                engine.setPause(isPlaying)
            },
            onRewind10 = {
//                elapsed = (elapsed - 10f).coerceAtLeast(0f)
            },
            onForward10 = {
//                elapsed = (elapsed + 10f).coerceAtMost(totalDuration)
            },
            onPrevChapter = {
//                if (currentChapter > 1) selectBookChapter(currentBook, currentChapter - 1)
            },
            onNextChapter = {
//                if (currentChapter < bookInfo.chapters) selectBookChapter(currentBook, currentChapter + 1)
            },
            elapsedSeconds = currentTime.toFloat(),
            totalDurationSeconds = duration.toFloat(),
            onSeekFraction = {
//                seekToFraction(it)
            },
            speed = speed.toFloat(),
            onCycleSpeed = {
//                val speeds = 1
//                speed = speeds[(speeds.indexOf(speed) + 1) % speeds.size]
            },
            muted = false,
            onToggleMuted = {
//                muted = !muted
            },
            autoAdvance = true,
            onAutoAdvanceChange = {
//                autoAdvance = it
            },
            voice = "voice",
            onVoiceChange = {
//                voice = it
            },
            visible = chromeVisible,
            translation = "NKJV",
            onTranslationChange = { },
        )
    }
}