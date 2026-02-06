package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val safeHomeCache = mutableMapOf<Color, ImageVector>()

fun safeHome(strokeColor: Color): ImageVector {
    return safeHomeCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "safeHome",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.7835f, 17.5f)
                curveTo(13.3918f, 18.275f, 14.2585f, 18.875f, 15.2418f, 19.1416f)
                curveTo(15.3752f, 19.175f, 15.5251f, 19.175f, 15.6668f, 19.1416f)
                curveTo(17.6835f, 18.5833f, 19.1668f, 16.6667f, 19.1668f, 14.6334f)
                verticalLineTo(12.8083f)
                curveTo(19.1668f, 12.4583f, 18.9085f, 12.075f, 18.5918f, 11.9416f)
                lineTo(17.1001f, 11.3333f)
                moveTo(12.7835f, 17.5f)
                curveTo(12.1251f, 16.6833f, 11.7418f, 15.675f, 11.7418f, 14.6334f)
                verticalLineTo(12.8083f)
                curveTo(11.7418f, 12.4583f, 12.0001f, 12.075f, 12.3168f, 11.9416f)
                lineTo(14.6335f, 10.9916f)
                curveTo(15.1585f, 10.7833f, 15.7418f, 10.7833f, 16.2668f, 10.9916f)
                lineTo(17.1001f, 11.3333f)
                moveTo(12.7835f, 17.5f)
                lineTo(5.15015f, 17.5f)
                curveTo(4.00848f, 17.5f, 2.93349f, 16.5918f, 2.74183f, 15.4668f)
                lineTo(1.69182f, 9.17505f)
                curveTo(1.55849f, 8.40005f, 1.95014f, 7.35837f, 2.56681f, 6.8667f)
                lineTo(8.05016f, 2.48336f)
                curveTo(8.89182f, 1.80836f, 10.2585f, 1.80837f, 11.1002f, 2.4917f)
                lineTo(16.5835f, 6.8667f)
                curveTo(17.1918f, 7.35837f, 17.5918f, 8.40005f, 17.4585f, 9.17505f)
                lineTo(17.1001f, 11.3333f)
            }
        }.build()
    }
}

