package components.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.global.Items
import components.global.SelectMenu
import mvvm.SongBookViewModal
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme


@Composable
fun HymnsPresentationSidebar(
    model: SongBookViewModal,
) {
    val theme = LocalTheme.current
    val books = model.songBooks.collectAsState()
    val currentBook = model.currentBook.collectAsState()
    val songList = model.songs.collectAsState()
    val currentSong = model.currentSong.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column {
            Text("Song book", style = theme.typography.semiText, color = theme.colors.text)
            Spacer(Modifier.height(6.dp))
            SelectMenu(
                data = books.value.map { Items(key = it.name, value = it) },
                default = Items( key = currentBook.value.name, value = currentBook.value ),
                onValueChange = {
                    model.selectBook(it.value)
                }
            )
        }
        Column {
            Text("Select a Song", style = theme.typography.semiText, color = theme.colors.text)
            Spacer(Modifier.height(6.dp))
            SelectMenu(
                data = songList.value.map { Items(key = it.songNumber.toString(), value = it) },
                default = Items( key = currentSong.value?.songNumber.toString(), value = currentSong.value ),
                fill = false,
                onValueChange = {
                    model.setCurrentSong(it.value!!)
                    model.setCurrentLine(it.value.lyrics.first())
                }
            )
        }
        Divider(
            color = theme.colors.border,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}