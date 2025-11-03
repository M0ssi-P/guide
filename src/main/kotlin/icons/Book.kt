package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val bookCache = mutableMapOf<Color, ImageVector>()

fun book(strokeColor: Color): ImageVector {
    return bookCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "book",
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
                moveTo(9.99984f, 4.57495f)
                verticalLineTo(17.075f)
                moveTo(6.45817f, 7.07495f)
                horizontalLineTo(4.58317f)
                moveTo(7.08317f, 9.57495f)
                horizontalLineTo(4.58317f)
                moveTo(18.3332f, 13.95f)
                verticalLineTo(3.89162f)
                curveTo(18.3332f, 2.89162f, 17.5165f, 2.14995f, 16.5248f, 2.23329f)
                horizontalLineTo(16.4748f)
                curveTo(14.7248f, 2.38329f, 12.0665f, 3.27495f, 10.5832f, 4.20829f)
                lineTo(10.4415f, 4.29995f)
                curveTo(10.1998f, 4.44995f, 9.79984f, 4.44995f, 9.55817f, 4.29995f)
                lineTo(9.34984f, 4.17495f)
                curveTo(7.8665f, 3.24995f, 5.2165f, 2.36662f, 3.4665f, 2.22495f)
                curveTo(2.47484f, 2.14162f, 1.6665f, 2.89162f, 1.6665f, 3.88329f)
                verticalLineTo(13.95f)
                curveTo(1.6665f, 14.75f, 2.3165f, 15.5f, 3.1165f, 15.6f)
                lineTo(3.35817f, 15.6333f)
                curveTo(5.1665f, 15.875f, 7.95817f, 16.7916f, 9.55817f, 17.6666f)
                lineTo(9.5915f, 17.6833f)
                curveTo(9.8165f, 17.8083f, 10.1748f, 17.8083f, 10.3915f, 17.6833f)
                curveTo(11.9915f, 16.8f, 14.7915f, 15.875f, 16.6082f, 15.6333f)
                lineTo(16.8832f, 15.6f)
                curveTo(17.6832f, 15.5f, 18.3332f, 14.75f, 18.3332f, 13.95f)
                close()
            }
        }.build()
    }
}

