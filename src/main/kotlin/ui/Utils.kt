package ui

import java.time.Month

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

fun Int.toOrdinal(): String {
    if (this in 11..13) return "${this}th"

    return when (this % 10) {
        1 -> "${this}st"
        2 -> "${this}nd"
        3 -> "${this}rd"
        else -> "${this}th"
    }
}

fun Month.displayName(): String {
    return this.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
}