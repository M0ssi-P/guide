package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val clockCache = mutableMapOf<Color, ImageVector>()

fun clock(strokeColor: Color): ImageVector {
    return clockCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "clock",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.0917f, 12.65f)
                lineTo(10.5084f, 11.1083f)
                curveTo(10.0584f, 10.8416f, 9.69175f, 10.2f, 9.69175f, 9.67496f)
                verticalLineTo(6.25829f)
                moveTo(18.3334f, 9.99996f)
                curveTo(18.3334f, 14.6f, 14.6001f, 18.3333f, 10.0001f, 18.3333f)
                curveTo(5.40008f, 18.3333f, 1.66675f, 14.6f, 1.66675f, 9.99996f)
                curveTo(1.66675f, 5.39996f, 5.40008f, 1.66663f, 10.0001f, 1.66663f)
                curveTo(14.6001f, 1.66663f, 18.3334f, 5.39996f, 18.3334f, 9.99996f)
                close()
            }
        }.build()
    }
}

