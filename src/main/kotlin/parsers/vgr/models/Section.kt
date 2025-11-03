package parsers.vgr.models

data class Section(
    val canHit: Boolean,
    val isHeader: Boolean,
    val key: String,
    val paragraph: String,
    val content: MutableList<Paragraph>
)
