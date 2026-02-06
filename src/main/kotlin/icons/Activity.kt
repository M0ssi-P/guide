package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val activityCache = mutableMapOf<Color, ImageVector>()

fun activityIcon(strokeColor: Color): ImageVector {
    return activityCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "activity",
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
                moveTo(6.10841f, 12.075f)
                lineTo(8.09175f, 9.50002f)
                curveTo(8.37508f, 9.13335f, 8.90008f, 9.06669f, 9.26675f, 9.35002f)
                lineTo(10.7917f, 10.55f)
                curveTo(11.1584f, 10.8334f, 11.6834f, 10.7667f, 11.9667f, 10.4084f)
                lineTo(13.8917f, 7.92502f)
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
            }
        }.build()
    }
}

