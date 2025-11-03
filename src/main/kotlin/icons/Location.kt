package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val locationCache = mutableMapOf<Color, ImageVector>()

fun location(strokeColor: Color): ImageVector {
    return locationCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "location",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor)
            ) {
                moveTo(10.0001f, 11.1916f)
                curveTo(11.436f, 11.1916f, 12.6001f, 10.0276f, 12.6001f, 8.59163f)
                curveTo(12.6001f, 7.15569f, 11.436f, 5.99163f, 10.0001f, 5.99163f)
                curveTo(8.56414f, 5.99163f, 7.40008f, 7.15569f, 7.40008f, 8.59163f)
                curveTo(7.40008f, 10.0276f, 8.56414f, 11.1916f, 10.0001f, 11.1916f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF006BFF))
            ) {
                moveTo(3.01675f, 7.07496f)
                curveTo(4.65842f, -0.141705f, 15.3501f, -0.133372f, 16.9834f, 7.08329f)
                curveTo(17.9417f, 11.3166f, 15.3084f, 14.9f, 13.0001f, 17.1166f)
                curveTo(11.3251f, 18.7333f, 8.67508f, 18.7333f, 6.99175f, 17.1166f)
                curveTo(4.69175f, 14.9f, 2.05842f, 11.3083f, 3.01675f, 7.07496f)
                close()
            }
        }.build()
    }
}

