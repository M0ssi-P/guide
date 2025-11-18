package parsers.vgr.models

import kotlinx.serialization.Serializable

@Serializable
data class Segment(
    val style: List<Style>,
    val content: String
)