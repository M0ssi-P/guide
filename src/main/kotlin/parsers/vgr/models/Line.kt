package parsers.vgr.models

data class Line(
    val displayNumber: Boolean,
    val number: Int,
    val segments: MutableList<Segment>
)
