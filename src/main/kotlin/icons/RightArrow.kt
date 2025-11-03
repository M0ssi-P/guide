package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val arrowRightCache = mutableMapOf<Color, ImageVector>()

fun arrowRight(strokeColor: Color): ImageVector {
    return arrowRightCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "arrowRight",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 10f
            ) {
                moveTo(12.0249f, 15.0583f)
                lineTo(17.0833f, 10f)
                lineTo(12.0249f, 4.94168f)
                moveTo(2.91658f, 10f)
                lineTo(16.9416f, 10f)
            }
        }.build()
    }
}
