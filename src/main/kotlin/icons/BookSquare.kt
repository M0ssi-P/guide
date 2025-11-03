package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val bookSquareCache = mutableMapOf<Color, ImageVector>()

fun bookSquare(strokeColor: Color): ImageVector {
    return bookSquareCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "bookSquare",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.0001f, 6.75003f)
                verticalLineTo(14.7167f)
                moveTo(7.50008f, 18.3334f)
                horizontalLineTo(12.5001f)
                curveTo(16.6667f, 18.3334f, 18.3334f, 16.6667f, 18.3334f, 12.5f)
                verticalLineTo(7.50002f)
                curveTo(18.3334f, 3.33335f, 16.6667f, 1.66669f, 12.5001f, 1.66669f)
                horizontalLineTo(7.50008f)
                curveTo(3.33341f, 1.66669f, 1.66675f, 3.33335f, 1.66675f, 7.50002f)
                verticalLineTo(12.5f)
                curveTo(1.66675f, 16.6667f, 3.33341f, 18.3334f, 7.50008f, 18.3334f)
                close()
                moveTo(15.3168f, 12.725f)
                verticalLineTo(6.31668f)
                curveTo(15.3168f, 5.67501f, 14.8001f, 5.20835f, 14.1667f, 5.25835f)
                horizontalLineTo(14.1334f)
                curveTo(13.0168f, 5.35002f, 11.3251f, 5.92503f, 10.3751f, 6.51669f)
                lineTo(10.2834f, 6.57503f)
                curveTo(10.1334f, 6.6667f, 9.87506f, 6.6667f, 9.71673f, 6.57503f)
                lineTo(9.58342f, 6.4917f)
                curveTo(8.64175f, 5.90003f, 6.95008f, 5.34168f, 5.83341f, 5.25001f)
                curveTo(5.20008f, 5.20001f, 4.68341f, 5.67503f, 4.68341f, 6.30836f)
                verticalLineTo(12.725f)
                curveTo(4.68341f, 13.2333f, 5.10007f, 13.7167f, 5.6084f, 13.775f)
                lineTo(5.75839f, 13.8f)
                curveTo(6.90839f, 13.95f, 8.69177f, 14.5417f, 9.70844f, 15.1f)
                lineTo(9.73341f, 15.1084f)
                curveTo(9.87508f, 15.1917f, 10.1084f, 15.1917f, 10.2417f, 15.1084f)
                curveTo(11.2584f, 14.5417f, 13.0501f, 13.9584f, 14.2084f, 13.8f)
                lineTo(14.3834f, 13.775f)
                curveTo(14.9001f, 13.7167f, 15.3168f, 13.2417f, 15.3168f, 12.725f)
                close()
            }
        }.build()
    }
}

