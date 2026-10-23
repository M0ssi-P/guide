package ui.screens.table


data class Highlight(
    val id: String,
    val paragraphId: String,
    val start: Int,
    val end: Int,
    val color: HighlightColor,
    val noteId: String? = null
)

/** One entry in the Notes & Highlights drawer. */
data class NoteEntry(
    val id: String,
    val color: HighlightColor,
    val snippet: String,
    val noteText: String
)

/** Which half of the Listen/Read segmented switch is showing. */
enum class TableMode { Listen, Read }

/** Which (if any) overlay is currently open above the screen. */
enum class TableOverlay { None, Browse, Search, Menu }

/** A single reading paragraph: plain text plus which verse number(s) it starts at,
 *  matching how `ChapterContent` already numbers verses inline. */
data class ReadingParagraph(
    val id: String,
    val verseNumber: Int,
    val text: String,
    val isCurrent: Boolean = false
)

/** Static sermon/track info shown in both the Listen hero and the Read header. */
data class NowPlayingInfo(
    val code: String,
    val title: String,
    val location: String
)
