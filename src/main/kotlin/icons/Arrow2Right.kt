package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val arrow2rightCache = mutableMapOf<Color, ImageVector>()

fun arrow2right(strokeColor: Color): ImageVector {
    return arrow2rightCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "arrow2right",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path {
            }
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(8.293f, 18.707f)
                arcToRelative(1f, 1f, 0f, false, true, 0f, -1.414f)
                lineToRelative(4.94f, -4.94f)
                arcToRelative(0.5f, 0.5f, 0f, false, false, 0f, -0.707f)
                lineToRelative(-4.94f, -4.939f)
                arcToRelative(1f, 1f, 0f, false, true, 1.414f, -1.414f)
                lineToRelative(5.647f, 5.646f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, 0f, 2.122f)
                lineToRelative(-5.647f, 5.646f)
                arcToRelative(1f, 1f, 0f, false, true, -1.414f, 0f)
                close()
            }
        }.build()
    }
}