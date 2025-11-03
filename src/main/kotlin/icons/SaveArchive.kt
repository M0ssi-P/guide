package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val saveArchiveCache = mutableMapOf<Color, ImageVector>()

fun saveArchive(strokeColor: Color): ImageVector {
    return saveArchiveCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "saveArchive",
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
                moveTo(10.7415f, 4.89996f)
                horizontalLineTo(4.25816f)
                curveTo(2.83316f, 4.89996f, 1.6665f, 6.06662f, 1.6665f, 7.49162f)
                verticalLineTo(16.9583f)
                moveTo(10.7415f, 4.89996f)
                curveTo(12.1665f, 4.89996f, 13.3332f, 6.06662f, 13.3248f, 7.49162f)
                verticalLineTo(16.9583f)
                curveTo(13.3248f, 18.1666f, 12.4582f, 18.6833f, 11.3998f, 18.0916f)
                lineTo(8.12484f, 16.2666f)
                curveTo(7.78317f, 16.0749f, 7.2165f, 16.0749f, 6.8665f, 16.2666f)
                moveTo(10.7415f, 4.89996f)
                curveTo(12.1665f, 4.89996f, 13.3332f, 6.06662f, 13.3332f, 7.49162f)
                moveTo(10.7415f, 4.89996f)
                horizontalLineTo(6.6665f)
                verticalLineTo(4.25828f)
                curveTo(6.6665f, 2.83328f, 7.83316f, 1.66663f, 9.25816f, 1.66663f)
                horizontalLineTo(15.7415f)
                curveTo(17.1665f, 1.66663f, 18.3332f, 2.83328f, 18.3332f, 4.25828f)
                verticalLineTo(13.725f)
                curveTo(18.3332f, 14.9333f, 17.4665f, 15.4416f, 16.4082f, 14.8583f)
                lineTo(13.3332f, 13.1416f)
                verticalLineTo(7.49162f)
                moveTo(1.6665f, 16.9583f)
                curveTo(1.6665f, 18.1666f, 2.53317f, 18.6833f, 3.5915f, 18.0916f)
                moveTo(1.6665f, 16.9583f)
                curveTo(1.6665f, 18.1666f, 2.53317f, 18.675f, 3.5915f, 18.0916f)
                moveTo(3.5915f, 18.0916f)
                lineTo(6.8665f, 16.2666f)
                moveTo(6.8665f, 16.2666f)
                curveTo(7.2165f, 16.0749f, 7.78318f, 16.0749f, 8.13318f, 16.2666f)
                lineTo(11.4082f, 18.0916f)
                curveTo(12.4665f, 18.675f, 13.3332f, 18.1666f, 13.3332f, 16.9583f)
                verticalLineTo(7.49162f)
            }
        }.build()
    }
}

