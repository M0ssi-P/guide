package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val bookmarkCache = mutableMapOf<Color, ImageVector>()

fun bookmarkIcon(strokeColor: Color): ImageVector {
    return bookmarkCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "bookmark",
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
                moveTo(5.68341f, 4.15835f)
                curveTo(2.84175f, 4.63335f, 1.66675f, 6.38335f, 1.66675f, 9.91669f)
                verticalLineTo(12.4417f)
                curveTo(1.66675f, 16.65f, 3.33341f, 18.3334f, 7.50008f, 18.3334f)
                horizontalLineTo(12.5001f)
                curveTo(16.6667f, 18.3334f, 18.3334f, 16.65f, 18.3334f, 12.4417f)
                verticalLineTo(9.91669f)
                curveTo(18.3334f, 6.32502f, 17.1167f, 4.56669f, 14.1667f, 4.13335f)
                moveTo(11.6667f, 1.66669f)
                curveTo(13.3334f, 1.66669f, 14.1667f, 2.50835f, 14.1667f, 4.19169f)
                verticalLineTo(10.0667f)
                curveTo(14.1667f, 11.725f, 12.9917f, 12.3667f, 11.5501f, 11.5f)
                lineTo(10.4501f, 10.8334f)
                curveTo(10.2001f, 10.6834f, 9.80008f, 10.6834f, 9.55008f, 10.8334f)
                lineTo(8.45008f, 11.5f)
                curveTo(7.00841f, 12.3667f, 5.83341f, 11.725f, 5.83341f, 10.0667f)
                verticalLineTo(4.19169f)
                curveTo(5.83341f, 2.50835f, 6.66675f, 1.66669f, 8.33341f, 1.66669f)
                horizontalLineTo(11.6667f)
                close()
            }
        }.build()
    }
}

