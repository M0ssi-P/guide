package presentation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import parsers.vgr.models.Sermon

enum class DeckSource(val label: String) { TABLE("Table"), BIBLE("Bible"), SONGS("Songs"), IMAGES("Pictures") }
enum class DeckStyle(val label: String, val desc: String) {
    FOCUS("Focus scroll", "Current line centered and large, the rest fade above and below."),
    PARAGRAPH("Paragraph", "One whole paragraph or verse per screen."),
}

/**
 * One slide line. [number] is the paragraph or verse it belongs to; [rich] keeps the source text styles.
 * Table sentences carry their segment [key] ([section, block, sentence]) and, when the data has it, [start]/[end] in seconds.
 */
data class DeckItem(
    val number: Int, val label: String, val text: String, val rich: AnnotatedString? = null,
    val key: List<Int>? = null, val start: Double? = null, val end: Double? = null,
    /** Pictures: the image file shown. */
    val image: String? = null,
)

/**
 * What the projector shows: a list of paragraphs, verses or song verses and which one is live.
 * The Projection screen drives it; every projector window renders it (see WindowPresentationPreview).
 */
object ProjectionDeck : ProjectionViewModel {
    var source by mutableStateOf(DeckSource.TABLE)
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var items by mutableStateOf<List<DeckItem>>(emptyList())
    var index by mutableStateOf(0)
    /** How much of the live sentence has been spoken, 0..1 by characters (1 when moving by hand). */
    var progress by mutableStateOf(1f)
    var style by mutableStateOf(DeckStyle.FOCUS)
    /** Screens go black without losing the place (Songs' Blank / the B key). */
    var blank by mutableStateOf(false)
    /** Pictures: fill the screen (crop) or show the whole picture. */
    var fill by mutableStateOf(true)
    /** Table only: the sermon recording moves the slides. */
    var followAudio by mutableStateOf(true)
    /** Set for Table decks so the audio and live sharing know which sermon this is. */
    var sermon by mutableStateOf<Sermon?>(null)

    val footer get() = if (source == DeckSource.TABLE && sermon != null) "${title.uppercase()} · ${sermon!!.dateCode}" else title.uppercase()
    val current get() = items.getOrNull(index)
    val focusAllowed get() = source == DeckSource.TABLE || source == DeckSource.BIBLE
    val effectiveStyle get() = if (focusAllowed) style else DeckStyle.PARAGRAPH

    fun load(source: DeckSource, title: String, subtitle: String, items: List<DeckItem>, index: Int = 0, sermon: Sermon? = null) {
        this.source = source
        this.title = title
        this.subtitle = subtitle
        this.items = items
        this.sermon = sermon
        this.index = index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        progress = 1f
    }

    fun go(i: Int) { index = i.coerceIn(0, (items.size - 1).coerceAtLeast(0)); progress = 1f }
}
