package rs

data class BackblazeErrorResponse (
    val status: Int,
    val code: String,
    val message: String,
)