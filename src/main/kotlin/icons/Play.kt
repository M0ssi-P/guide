package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val playCache = mutableMapOf<Color, ImageVector>()

fun play(strokeColor: Color): ImageVector {
    return playCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "play",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 10f
            ) {
                moveTo(3.3335f, 7.03329f)
                curveTo(3.3335f, 3.34995f, 5.94183f, 1.84162f, 9.1335f, 3.68329f)
                lineTo(14.2835f, 6.64995f)
                curveTo(17.4752f, 8.49162f, 17.4752f, 11.5083f, 14.2835f, 13.35f)
                lineTo(9.1335f, 16.3166f)
                curveTo(5.94183f, 18.1583f, 3.3335f, 16.65f, 3.3335f, 12.9666f)
                verticalLineTo(7.03329f)
                close()
            }
        }.build()
    }
}