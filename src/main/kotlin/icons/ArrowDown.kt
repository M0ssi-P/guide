package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val arrowDownCache = mutableMapOf<Color, ImageVector>()

fun arrowDown(strokeColor: Color): ImageVector {
    return arrowDownCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "arrowdown",
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
                moveTo(2.5f, 7f)
                curveTo(4f, 10f, 7f, 16.5f, 12f, 16.5f)
                curveTo(17f, 16.5f, 20f, 10f, 21.5f, 7f)
            }
        }.build()
    }
}

