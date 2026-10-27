package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val quillIconCache = mutableMapOf<Color, ImageVector>()

fun quillIcon(strokeColor: Color): ImageVector = quillIconCache.getOrPut(strokeColor) {
    strokePaths("quillIcon", strokeColor, "M5 19c3-6 7-11 14-14M8 14l3 3M11 11l3 3")
}
