package rs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Kinds(val value: String) {
    @SerialName("table") TABLE("table"),
    @SerialName("hymns") HYMNS("hymns"),
}

@Serializable
data class RSResponse(
    val kinds: Kinds?,
    val ids: List<String>?,

) {

}