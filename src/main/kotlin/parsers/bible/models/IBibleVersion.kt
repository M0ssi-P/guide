package parsers.bible.models

data class IBibleVersion(
    val id: Int,
    val abbreviation: String,
    val localAbbreviation: String,
    val title: String,
    val localTitle: String,
    val audio: Boolean,
    val audioCount: Int,
    val text: Boolean,
    val language: ILanguage,
    val copyrightShort: HTMLorText? = null,
    val copyrightLong: HTMLorText? = null,
    val readerFooter: HTMLorText? = null,
    val publisher: Publisher? = null,
    val books: List<Book>? = null
) {
    data class HTMLorText(
        val html: String?,
        val text: String?
    )

    data class Book(
        val hasText: Boolean,
        val hasAudio: Boolean,
        val usfm: String,
        val canon: String,
        val human: String,
        val humanLong: String,
        val abbreviation: String,
        val chapters: List<Chapter>,
    )

    data class Chapter(
        val toc: Boolean,
        val usfm: String,
        val human: String,
        val canonical: Boolean,
    )

    data class Publisher(
        val id: Int,
        val name: String,
        val localName: String?,
        val url: String,
        val description: Any?,
    )
}