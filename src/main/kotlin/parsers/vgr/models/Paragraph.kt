package parsers.vgr.models

data class Paragraph(
    val id: Long?,
    val number: Int?,
    var text: String,
    val isFirst: Boolean,
    val content: MutableList<Line>
)