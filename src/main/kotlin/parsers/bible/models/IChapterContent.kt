package parsers.bible.models

data class IChapterContent(
    val version: IBibleVersion?,
    val chapter: MutableList<ChapterSection>? = mutableListOf()
)

data class ChapterSection(
    val hasHeading: Boolean = false,
    val hasSubheading: Boolean = false,
    val heading: HeadingObject?,
    val content: MutableList<IParagraph>? = mutableListOf(),
) {
    data class HeadingObject(
        val heading: String?,
        val subHeading: String?,
        val type: String?
    )

    data class IParagraph(
        val type: Paragraph = Paragraph.Normal,
        val verses: MutableList<IBibleVerse> = mutableListOf()
    )

    data class IBibleVerse(
        val number: Int,
        val content: MutableList<IBibleVerseLine> = mutableListOf(),
    )

    data class IBibleVerseLine(
        val type: VerseLineEnum,
        val isJesus: Boolean,
        val display: VerseLineDisplay,
        val content: String? = null,
        val reference: String? = null,
    )
}

enum class Paragraph {
    Normal,
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