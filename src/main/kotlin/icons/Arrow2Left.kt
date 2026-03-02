package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val arrow2leftCache = mutableMapOf<Color, ImageVector>()

fun arrow2left(strokeColor: Color): ImageVector {
    return arrow2leftCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "arrow2left",
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
                moveTo(15.707f, 5.293f)
                arcToRelative(1f, 1f, 0f, false, true, 0f, 1.414f)
                lineToRelative(-5.116f, 5.116f)
                arcToRelative(0.25f, 0.25f, 0f, false, false, 0f, 0.354f)
                lineToRelative(5.116f, 5.116f)
                arcToRelative(1f, 1f, 0f, true, true, -1.414f, 1.414f)
                lineToRelative(-5.647f, -5.646f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, 0f, -2.122f)
                lineToRelative(5.647f, -5.646f)
                arcToRelative(1f, 1f, 0f, false, true, 1.414f, 0f)
                close()
            }
        }.build()
    }
}

