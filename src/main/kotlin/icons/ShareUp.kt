package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val shareUpIconCache = mutableMapOf<Color, ImageVector>()

fun shareUpIcon(strokeColor: Color): ImageVector = shareUpIconCache.getOrPut(strokeColor) {
    strokePaths("shareUpIcon", strokeColor, "M12 3v12M7 8l5-5 5 5M5 14v6h14v-6")
}
