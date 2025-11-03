package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val arrowLeftCache = mutableMapOf<Color, ImageVector>()

fun arrowLeft(strokeColor: Color): ImageVector {
    return arrowLeftCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "arrowLeft",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor), // ✅ Use the passed-in color
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 10f
            ) {
                moveTo(7.97508f, 4.94165f)
                lineTo(2.91675f, 9.99998f)
                lineTo(7.97508f, 15.0583f)
                moveTo(17.0834f, 9.99998f)
                horizontalLineTo(3.05841f)
            }
        }.build()
    }
}
