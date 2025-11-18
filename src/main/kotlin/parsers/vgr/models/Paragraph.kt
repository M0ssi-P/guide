package parsers.vgr.models

data class Paragraph(
    val id: Long?,
    val number: Int?,
    var text: String,
    val isFirst: Boolean,
    val isQuestion: Boolean = false,
    val content: MutableList<Line>
)