package ui.screens.hymns

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import components.global.Button
import components.global.Search
import components.global.SongBookSelector
import components.global.SongCard
import components.layouts.HymnsPresentationSidebar
import components.layouts.SongPresentationContent
import kotlinx.coroutines.launch
import mvvm.SongBookViewModal
import org.jetbrains.jewel.ui.component.Text
import searchSongs
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

@Composable
fun Hymns() {
    val theme = LocalTheme.current
    val focusManager = LocalFocusManager.current
    val model = remember { SongBookViewModal().apply {
        this.initilize()
    } }
    val isPresentationMode = model.isPresentationMode.collectAsState()
    val songList = model.songs.collectAsState()
    var query by remember { mutableStateOf("") }
    val searchBasedSongs = remember { mutableStateOf(
        songList.value
    ) }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(query, songList.value) {
        if(query.isEmpty()) {
            searchBasedSongs.value = songList.value
        } else {
            searchBasedSongs.value = searchSongs(query, songList.value)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.width(if(!isPresentationMode.value) 430.dp else 300.dp)
                    .fillMaxHeight()
                    .dropShadow(
                        shape = RoundedCornerShape(0.dp),
                        block = {
                            color = Color.Gray
                            alpha = 0.15f
                            radius = 10f
                            offset = Offset(5f, 5f)
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusManager.clearFocus()
                        }
                    }
                    .newBorder(width = 1.dp, sides = setOf(BorderSide.Right), color = theme.colors.border)
                    .background(theme.colors.surface)
                    .padding(20.dp)
            ) {
                if(isPresentationMode.value) {
                    HymnsPresentationSidebar(model = model)
                }  else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongBookSelector(model)

                        Button(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(theme.colors.periwinkle)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            fillMaxSize = false
                        ) {
                            Text("Create", style = theme.typography.button, color = theme.colors.light)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Search(filled = true, onValueChange = {
                        query = it
                    })

                    Spacer(Modifier.height(30.dp))

                    if(searchBasedSongs.value.isNotEmpty()) {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        coroutineScope.launch {
                                            scrollState.scrollBy(-delta)
                                        }
                                    },
                                ),

                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(searchBasedSongs.value) { index, song ->
                                SongCard(
                                    song,
                                    index,
                                    model
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if(isPresentationMode.value) {
                    SongPresentationContent(model)
                } else {
                    HymnsContent(model)
                }
            }
            if (isPresentationMode.value) {
                Column(
                    modifier = Modifier.width(if(!isPresentationMode.value) 430.dp else 300.dp)
                        .fillMaxHeight()
                        .dropShadow(
                            shape = RoundedCornerShape(0.dp),
                            block = {
                                color = Color.Gray
                                alpha = 0.15f
                                radius = 10f
                                offset = Offset(5f, 5f)
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures {
                                focusManager.clearFocus()
                            }
                        }
                        .newBorder(width = 1.dp, sides = setOf(BorderSide.Left), color = theme.colors.border)
                        .background(theme.colors.surface)
                        .padding(20.dp)
                ) {
                    Text("hi")
                }
            }
        }

        if(isPresentationMode.value) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(239.dp)
                    .newBorder(width = 1.dp, sides = setOf(BorderSide.Top), color = theme.colors.border)
                    .background(theme.colors.surface)
            ) {
                Text("hi")
            }
        }
    }
}