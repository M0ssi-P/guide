package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val streamCache = mutableMapOf<Color, ImageVector>()

fun streamIcon(strokeColor: Color): ImageVector {
    return streamCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "stream",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // outer and inner signal arcs
                moveTo(5f, 12f)
                arcTo(7f, 7f, 0f, false, true, 19f, 12f)
                moveTo(8.5f, 12f)
                arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 12f)
                // mast
                moveTo(12f, 13f)
                verticalLineTo(20f)
                // dot, r = 1 at (12, 12)
                moveTo(13f, 12f)
                arcTo(1f, 1f, 0f, true, true, 11f, 12f)
                arcTo(1f, 1f, 0f, true, true, 13f, 12f)
                close()
            }
        }.build()
    }
}
