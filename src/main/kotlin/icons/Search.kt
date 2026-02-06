package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val seachCache = mutableMapOf<Color, ImageVector>()

fun searchIcon(strokeColor: Color): ImageVector {
    return seachCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "search",
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
                moveTo(17.6785f, 17.7027f)
                curveTo(18.9931f, 16.3976f, 19.5405f, 14.3266f, 19.5405f, 11.2703f)
                curveTo(19.5405f, 5.17639f, 17.3641f, 3f, 11.2702f, 3f)
                curveTo(5.17636f, 3f, 3f, 5.17639f, 3f, 11.2703f)
                curveTo(3f, 17.3642f, 5.17638f, 19.5405f, 11.2703f, 19.5405f)
                curveTo(14.3078f, 19.5405f, 16.372f, 18.9998f, 17.6785f, 17.7027f)
                close()
                moveTo(17.6785f, 17.7027f)
                lineTo(21.5f, 21.5f)
            }
        }.build()
    }
}

