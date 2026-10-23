package parsers.vgr.models

import java.io.Serializable

data class Section(
    val dbId: String? = null,
    val canHit: Boolean,
    val isHeader: Boolean,
    val key: String,
    val paragraph: String,
    val content: MutableList<Paragraph>
): Serializable