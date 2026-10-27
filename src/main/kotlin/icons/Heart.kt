package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val heartCache = mutableMapOf<Pair<Color, Boolean>, ImageVector>()

/** Favourite heart; [filled] for a song that is a favourite. */
fun heartIcon(color: Color, filled: Boolean = true): ImageVector = heartCache.getOrPut(color to filled) {
    strokePaths("heart", color, "M12 21s-7.5-4.6-9.5-9.1C1 8.4 3.2 5 6.6 5c2 0 3.4 1.1 4.4 2.4C12 6.1 13.4 5 15.4 5 18.8 5 21 8.4 19.5 11.9 17.5 16.4 12 21 12 21z", fill = filled)
}
