package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val calendarCache = mutableMapOf<Color, ImageVector>()

fun calendarIcon(strokeColor: Color): ImageVector {
    return calendarCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "calendar",
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
                moveTo(6.66667f, 1.66663f)
                verticalLineTo(4.16663f)
                moveTo(13.3333f, 1.66663f)
                verticalLineTo(4.16663f)
                moveTo(2.91667f, 7.57496f)
                horizontalLineTo(17.0833f)
                moveTo(13.0789f, 11.4166f)
                horizontalLineTo(13.0864f)
                moveTo(13.0789f, 13.9166f)
                horizontalLineTo(13.0864f)
                moveTo(9.99624f, 11.4166f)
                horizontalLineTo(10.0037f)
                moveTo(9.99624f, 13.9166f)
                horizontalLineTo(10.0037f)
                moveTo(6.91193f, 11.4166f)
                horizontalLineTo(6.91941f)
                moveTo(6.91193f, 13.9166f)
                horizontalLineTo(6.91941f)
                moveTo(17.5f, 7.08329f)
                verticalLineTo(14.1666f)
                curveTo(17.5f, 16.6666f, 16.25f, 18.3333f, 13.3333f, 18.3333f)
                horizontalLineTo(6.66667f)
                curveTo(3.75f, 18.3333f, 2.5f, 16.6666f, 2.5f, 14.1666f)
                verticalLineTo(7.08329f)
                curveTo(2.5f, 4.58329f, 3.75f, 2.91663f, 6.66667f, 2.91663f)
                horizontalLineTo(13.3333f)
                curveTo(16.25f, 2.91663f, 17.5f, 4.58329f, 17.5f, 7.08329f)
                close()
            }
        }.build()
    }
}

