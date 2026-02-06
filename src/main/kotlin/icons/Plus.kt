package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val plusCache = mutableMapOf<Color, ImageVector>()

fun plusIcon(strokeColor: Color): ImageVector {
    return plusCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "plus",
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
                moveTo(11.9999f, 12f)
                verticalLineTo(21.5f)
                moveTo(11.9999f, 12f)
                lineTo(21.5f, 12f)
                moveTo(11.9999f, 12f)
                lineTo(2.5f, 12f)
                moveTo(11.9999f, 12f)
                lineTo(11.9999f, 2.5f)
            }
        }.build()
    }
}
