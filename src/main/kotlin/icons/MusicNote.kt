package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val musicNoteCache = mutableMapOf<Color, ImageVector>()

fun musicNoteIcon(strokeColor: Color): ImageVector {
    return musicNoteCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "musicNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                verticalLineTo(5f)
                lineTo(21f, 3f)
                verticalLineTo(16f)
                // left note head, r = 3 at (6, 18)
                moveTo(9f, 18f)
                arcTo(3f, 3f, 0f, true, true, 3f, 18f)
                arcTo(3f, 3f, 0f, true, true, 9f, 18f)
                close()
                // right note head, r = 3 at (18, 16)
                moveTo(21f, 16f)
                arcTo(3f, 3f, 0f, true, true, 15f, 16f)
                arcTo(3f, 3f, 0f, true, true, 21f, 16f)
                close()
            }
        }.build()
    }
}
