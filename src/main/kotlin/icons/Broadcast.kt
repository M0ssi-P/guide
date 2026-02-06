package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val broadcastCache = mutableMapOf<Color, ImageVector>()

fun broadcastIcon(strokeColor: Color): ImageVector {
    return broadcastCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "broadcast",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 12.5f)
                curveTo(9f, 12.5f, 11f, 13.5f, 11f, 16.5f)
                moveTo(7f, 10f)
                curveTo(9f, 10f, 13.5f, 11f, 13.5f, 16.5f)
                moveTo(7f, 7.5f)
                curveTo(9f, 7.5f, 16f, 8.50001f, 16f, 16.5f)
                moveTo(2.5f, 12f)
                curveTo(2.5f, 19.5f, 2.5f, 19.5f, 12f, 19.5f)
                curveTo(21.5f, 19.5f, 21.5f, 19.5f, 21.5f, 12f)
                curveTo(21.5f, 4.5f, 21.5f, 4.5f, 12f, 4.5f)
                curveTo(2.5f, 4.5f, 2.5f, 4.5f, 2.5f, 12f)
                close()
            }
        }.build()
    }
}

