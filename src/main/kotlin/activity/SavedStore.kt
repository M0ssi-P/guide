package activity

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import loadData
import parsers.vgr.models.Sermon
import saveData
import java.io.Serializable

enum class SavedSource(val label: String) { BIBLE("BIBLE"), TABLE("THE TABLE") }

enum class Tone(val label: String, val argb: Long) {
    GOLD("Gold", 0xFFC9A15F), ROSE("Rose", 0xFFD87878), TEAL("Teal", 0xFF6EB4AA), VIOLET("Violet", 0xFFA082C8);
    val color get() = Color(argb)
}

/**
 * Where a saved item points back to. Bible: version id, chapter usfm ("ROM.12") and verse.
 * Table: the sermon (without its text) and the paragraph number.
 */
data class SavedTarget(
    val source: SavedSource,
    val bibleVersionId: Int? = null,
    val bibleVersion: String? = null,
    val chapterUsfm: String? = null,
    val sermon: Sermon? = null,
    val number: Int? = null,
) : Serializable {
    val key get() = when (source) {
        SavedSource.BIBLE -> "bible:$bibleVersionId:$chapterUsfm:$number"
        SavedSource.TABLE -> "table:${sermon?.stringId}:$number"
    }
}

data class SavedHighlight(val id: String, val target: SavedTarget, val tone: Tone, val text: String, val ref: String, val at: Long) : Serializable
data class SavedBookmark(val id: String, val target: SavedTarget, val title: String, val meta: String, val progress: Float, val at: Long) : Serializable
data class SavedNote(val id: String, val target: SavedTarget, val title: String, val ref: String, val body: String, val at: Long) : Serializable

/** Highlights, bookmarks and notes made in the Bible and The Table, kept on this machine. */
object SavedStore {
    val highlights = mutableStateOf(loadData<ArrayList<SavedHighlight>>("saved_highlights")?.toList() ?: emptyList())
    val bookmarks = mutableStateOf(loadData<ArrayList<SavedBookmark>>("saved_bookmarks")?.toList() ?: emptyList())
    val notes = mutableStateOf(loadData<ArrayList<SavedNote>>("saved_notes")?.toList() ?: emptyList())

    private fun id() = System.nanoTime().toString(36)

    fun highlightFor(target: SavedTarget): SavedHighlight? = highlights.value.firstOrNull { it.target.key == target.key }
    fun noteFor(target: SavedTarget): SavedNote? = notes.value.firstOrNull { it.target.key == target.key }
    fun isBookmarked(target: SavedTarget) = bookmarks.value.any { it.target.key == target.key }

    /** Same tone again removes the highlight, another tone recolours it. */
    fun highlight(target: SavedTarget, tone: Tone, text: String, ref: String) {
        val existing = highlightFor(target)
        highlights.value = highlights.value.filterNot { it.target.key == target.key } +
            listOfNotNull(if (existing?.tone == tone) null else SavedHighlight(existing?.id ?: id(), target, tone, text, ref, System.currentTimeMillis()))
        saveData("saved_highlights", ArrayList(highlights.value))
        if (existing?.tone != tone) {
            val kind = if (target.source == SavedSource.BIBLE) "Highlighted $ref" else "Highlighted a paragraph in ${target.sermon?.title}"
            ActivityLog.record(ActivityKind.HIGHLIGHTS, kind, "$ref · ${tone.label}", "hl:${target.key}", "Open", quote = text.take(240))
        }
    }

    fun toggleBookmark(target: SavedTarget, title: String, meta: String, progress: Float) {
        bookmarks.value = if (isBookmarked(target)) bookmarks.value.filterNot { it.target.key == target.key }
        // one bookmark per chapter or sermon: moving it keeps the list short
        else bookmarks.value.filterNot { it.target.copy(number = null).key == target.copy(number = null).key } +
            SavedBookmark(id(), target, title, meta, progress, System.currentTimeMillis())
        saveData("saved_bookmarks", ArrayList(bookmarks.value))
    }

    fun saveNote(target: SavedTarget, title: String, ref: String, body: String) {
        notes.value = notes.value.filterNot { it.target.key == target.key } +
            listOfNotNull(body.trim().takeIf { it.isNotEmpty() }?.let { SavedNote(noteFor(target)?.id ?: id(), target, title.ifBlank { ref }, ref, it, System.currentTimeMillis()) })
        saveData("saved_notes", ArrayList(notes.value))
    }

    fun remove(item: Any) {
        when (item) {
            is SavedHighlight -> { highlights.value -= item; saveData("saved_highlights", ArrayList(highlights.value)) }
            is SavedBookmark -> { bookmarks.value -= item; saveData("saved_bookmarks", ArrayList(bookmarks.value)) }
            is SavedNote -> { notes.value -= item; saveData("saved_notes", ArrayList(notes.value)) }
        }
    }
}
