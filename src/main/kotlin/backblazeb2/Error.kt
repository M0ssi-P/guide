package backblazeb2

data class BackblazeErrorResponse (
    val status: Int,
    val code: String,
    val message: String,
)