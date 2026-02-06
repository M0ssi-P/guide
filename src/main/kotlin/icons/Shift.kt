package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val shiftCache = mutableMapOf<Color, ImageVector>()

fun shiftIcon(strokeColor: Color): ImageVector {
    return shiftCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "shift",
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
                moveTo(2.5f, 12f)
                lineTo(11.2929f, 3.20711f)
                curveTo(11.6834f, 2.81658f, 12.3166f, 2.81658f, 12.7071f, 3.20711f)
                lineTo(21.5f, 12f)
                moveTo(10f, 12f)
                curveTo(10f, 19.9997f, 10f, 20f, 12f, 20f)
                curveTo(14f, 20f, 14f, 19.9997f, 14f, 12f)
            }
        }.build()
    }
}