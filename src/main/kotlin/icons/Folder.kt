package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val folderIconCache = mutableMapOf<Color, ImageVector>()

fun folderIcon(strokeColor: Color): ImageVector = folderIconCache.getOrPut(strokeColor) {
    strokePaths("folderIcon", strokeColor, "M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
}
