package parsers.bible.models

import java.io.Serializable

data class IChapterContent(
    val version: IBibleVersion?,
    val audio: List<ChapterAud>? = null,
    val chapter: MutableList<ChapterSection>? = mutableListOf()
): Serializable

data class ChapterSection(
    val hasHeading: Boolean = false,
    val hasSubheading: Boolean = false,
    val heading: HeadingObject?,
    val content: MutableList<IParagraph>? = mutableListOf(),
): Serializable {
    data class HeadingObject(
        val heading: String?,
        val subHeading: String?,
        val type: String?
    ): Serializable

    data class IParagraph(
        val type: Paragraph = Paragraph.Normal,
        val verses: MutableList<IBibleVerse> = mutableListOf()
    ): Serializable

    data class IBibleVerse(
        val number: Int,
        val displayNumber: Boolean = false,
        val content: MutableList<IBibleVerseLine> = mutableListOf(),
    ): Serializable

    data class IBibleVerseLine(
        val type: VerseLineEnum,
        val isJesus: Boolean,
        val display: VerseLineDisplay,
        val content: String? = null,
        val reference: String? = null,
    ): Serializable
}

enum class Paragraph {
    Normal,
    PC,
    LI,
    Q1,
    Q2,
    M,
}

enum class VerseLineEnum {
    Reference,
    Word,
}

enum class VerseLineDisplay {
    Normal,
    Italic,
    SmallCaps,
    BdSmallCaps
}

data class ChapterAud(
    val id: Int,
    val versionId: Int,
    val title: String,
    val hasTiming: Boolean,
    val dramatized: Boolean,
    val default: Boolean,
    val playOrDwnLoad: DownloadUrls,
): Serializable {
    data class DownloadUrls(
        val formattedMp3: String,
        val formattedHls: String,
    ): Serializable
}