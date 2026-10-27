package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val openExternalIconCache = mutableMapOf<Color, ImageVector>()

fun openExternalIcon(strokeColor: Color): ImageVector = openExternalIconCache.getOrPut(strokeColor) {
    strokePaths("openExternalIcon", strokeColor, "M14 4h6v6M20 4l-9 9M19 14v6H4V5h6")
}
