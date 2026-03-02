package ui.screens.bible

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import mvvm.BibleViewModel
import navigation.LocalTabs
import player.GlobalPlayerType
import player.GlobalPlayerViewModel
import player.LocalPlayer
import ui.theme.LocalTheme

@Composable
fun BibleHome() {
    val theme = LocalTheme.current
    val currentTab = LocalTabs.current.current
    val player = LocalPlayer.current
    val model = currentTab.viewModelStore.getOrCreate("BibleVM-${currentTab.id}", {
        BibleViewModel().apply {
            this.initilize()
        }
    })

    LaunchedEffect(model.chapterContent) {
        model.chapterContent?.let {
            it.audio?.let { audio ->
                audio.find { i -> i.default }.let { default ->
                    player.loadNowPlaying(GlobalPlayerViewModel.NowPlaying(
                        id = default?.id?.toString() ?: "",
                        type = GlobalPlayerType.BIBLE,
                        isPlaying = false,
                        currentPosition = 0L,
                        url = "https:${default?.playOrDwnLoad?.formattedMp3?.split("?")?.first()}",
                    ))
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BibleContents(model)
            }
        }
    }
}