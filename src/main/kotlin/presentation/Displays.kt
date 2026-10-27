package presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.io.File

/** What one screen shows. */
sealed class Shows(val tag: String) {
    /** Whatever the projection screen is controlling. */
    object Follow : Shows("FOLLOWING")
    /** The slide after the live one, for a stage monitor. */
    object Next : Shows("NEXT UP")
    object Blank : Shows("BLANK")
    data class Picture(val path: String) : Shows("PICTURE")
    /** A slide held in place while the others move on. */
    data class Pinned(val item: DeckItem, val footer: String) : Shows("PINNED")
}

/** One projector window's content; the window renders it through [DisplayView]. */
class DisplayFeed : ProjectionViewModel {
    var shows by mutableStateOf<Shows>(Shows.Follow)
}

/** Feeds by display index, created the first time a display is used and kept for the session. */
object Displays {
    val feeds = mutableStateMapOf<Int, DisplayFeed>()
    fun feed(i: Int) = feeds.getOrPut(i) { DisplayFeed() }
}

@Composable
fun DisplayView(feed: DisplayFeed, accent: Color, lit: Color, modifier: Modifier = Modifier) {
    when (val s = feed.shows) {
        Shows.Follow -> DeckSlide(accent, lit, modifier)
        Shows.Next -> DeckSlide(accent, lit, modifier, offset = 1)
        Shows.Blank -> Box(modifier.background(Color.Black))
        is Shows.Picture -> Box(modifier) { PictureSlide(s.path, ProjectionDeck.fill) }
        is Shows.Pinned -> PinnedSlide(s.item, s.footer, accent, modifier)
    }
}

/** Pictures for projection, copied into the app's own folder so they survive the originals moving. */
object PictureLibrary {
    private val dir = File(System.getProperty("user.home"), "TheGuide/pictures").apply { mkdirs() }
    private val exts = setOf("png", "jpg", "jpeg", "gif", "bmp", "webp")
    val pictures = mutableStateOf(scan())

    private fun scan() = (dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in exts } ?: emptyList()).sortedBy { it.lastModified() }

    fun add(files: List<File>) {
        files.filter { it.isFile && it.extension.lowercase() in exts }.forEach { f ->
            var target = File(dir, f.name)
            var n = 1
            while (target.exists()) target = File(dir, "${f.nameWithoutExtension} (${n++}).${f.extension}")
            runCatching { f.copyTo(target) }
        }
        pictures.value = scan()
    }

    /** Deletes our copy and stops any screen that was holding it. */
    fun remove(f: File) {
        f.delete()
        pictures.value = scan()
        Displays.feeds.values.forEach { feed -> if ((feed.shows as? Shows.Picture)?.path == f.absolutePath) feed.shows = Shows.Blank }
    }

    fun items() = pictures.value.mapIndexed { i, f -> DeckItem(i + 1, f.nameWithoutExtension, f.nameWithoutExtension, image = f.absolutePath) }

    /** The system file chooser, several pictures at once. */
    fun choose(): List<File> {
        val d = java.awt.FileDialog(null as java.awt.Frame?, "Add pictures", java.awt.FileDialog.LOAD)
        d.isMultipleMode = true
        d.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in exts }
        d.isVisible = true
        return d.files?.toList() ?: emptyList()
    }
}
