package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val savedCache = mutableMapOf<Color, ImageVector>()

fun savedIcon(strokeColor: Color): ImageVector {
    return savedCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "saved",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f
            ) {
                moveTo(8.07817f, 4.69385f)
                curveTo(3.47968f, 5.27793f, 2.5f, 7.34285f, 2.5f, 13f)
                curveTo(2.5f, 20f, 4f, 21.5f, 12f, 21.5f)
                curveTo(20f, 21.5f, 21.5f, 20f, 21.5f, 13f)
                curveTo(21.5f, 7.37505f, 20.5314f, 5.30155f, 16f, 4.70397f)
                moveTo(12f, 2.5f)
                curveTo(16f, 2.5f, 16f, 2.5f, 16f, 8.5f)
                curveTo(16f, 11.6049f, 16.154f, 13.2164f, 15.2519f, 13.2164f)
                curveTo(14.6345f, 13.2164f, 13.5f, 12f, 12f, 12f)
                curveTo(10.5f, 12f, 9.32087f, 13.4469f, 8.74814f, 13.2164f)
                curveTo(8.1339f, 12.9692f, 8f, 11.6049f, 8f, 8.5f)
                curveTo(8f, 2.5f, 8f, 2.5f, 12f, 2.5f)
                close()
            }
        }.build()
    }
}

