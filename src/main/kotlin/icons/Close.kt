package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val closeCache = mutableMapOf<Color, ImageVector>()

fun closeIcon(strokeColor: Color): ImageVector {
    return closeCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "close",
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
                moveTo(2.5f, 21.5f)
                lineTo(21.5f, 2.5f)
                moveTo(21.5f, 21.5f)
                lineTo(2.5f, 2.5f)
            }
        }.build()
    }
}

