package parsers.vgr.models

import kotlinx.serialization.Serializable

@Serializable
data class Line(
    val displayNumber: Boolean,
    val number: Int,
    val segments: MutableList<Segment>
)
