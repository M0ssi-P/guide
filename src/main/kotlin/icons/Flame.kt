package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

val flameCache = mutableMapOf<Pair<Color, Color>, ImageVector>()

/** The two-tone streak flame: [outer] body with a lighter [inner] core. */
fun flameIcon(outer: Color, inner: Color): ImageVector = flameCache.getOrPut(outer to inner) {
    ImageVector.Builder(name = "flame", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        addPath(addPathNodes("M12 2c1 4 6 6 6 12a6 6 0 0 1-12 0c0-3 2-5 3-6 0 2 1 3 2 3 0-3-1-6 1-9z"), fill = SolidColor(outer))
        addPath(addPathNodes("M12 13c.6 1.6 2.5 2.4 2.5 4.6a2.5 2.5 0 0 1-5 0c0-1.3.9-2 1.4-2.6.1.9.5 1.3 1.1 1.3 0-1.2-.4-2.2 0-3.3z"), fill = SolidColor(inner))
    }.build()
}
