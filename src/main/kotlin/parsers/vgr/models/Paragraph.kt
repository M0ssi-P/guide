package parsers.vgr.models

import java.io.Serializable

data class Paragraph(
    val id: Long?,
    val dbId: String? = null,
    val number: Int?,
    var text: String,
    val isFirst: Boolean,
    val isQuestion: Boolean = false,
    val content: MutableList<Line>
): Serializable