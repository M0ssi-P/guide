package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val globeIconCache = mutableMapOf<Color, ImageVector>()

fun globeIcon(strokeColor: Color): ImageVector = globeIconCache.getOrPut(strokeColor) {
    strokePaths("globeIcon", strokeColor, "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M3 12h18", "M12 3c3 3.5 3 14.5 0 18", "M12 3c-3 3.5-3 14.5 0 18")
}

val inboxIconCache = mutableMapOf<Color, ImageVector>()

fun inboxIcon(strokeColor: Color): ImageVector = inboxIconCache.getOrPut(strokeColor) {
    strokePaths("inboxIcon", strokeColor, "M4 13l2.5-7h11L20 13v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1z", "M4 13h4.5l1 2h5l1-2H20", width = 1.7f)
}
