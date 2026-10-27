package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val projectorIconCache = mutableMapOf<Color, ImageVector>()

fun projectorIcon(strokeColor: Color): ImageVector = projectorIconCache.getOrPut(strokeColor) {
    strokePaths("projectorIcon", strokeColor, "M5 4h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M8 20h8M12 16v4")
}
