package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val statsCache = mutableMapOf<Color, ImageVector>()

fun statsIcon(strokeColor: Color) : ImageVector {
    return statsCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "stats",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14.5f, 10f)
                lineTo(13.4764f, 11.7059f)
                curveTo(13.2076f, 12.154f, 12.6391f, 12.3196f, 12.1717f, 12.0859f)
                lineTo(11.8282f, 11.9141f)
                curveTo(11.3608f, 11.6804f, 10.7923f, 11.846f, 10.5235f, 12.294f)
                lineTo(9.4999f, 14f)
                moveTo(21f, 12f)
                curveTo(21f, 15.326f, 20.4042f, 17.5797f, 18.9736f, 19f)
                curveTo(17.5518f, 20.4115f, 15.3055f, 21f, 12f, 21f)
                curveTo(5.36842f, 21f, 3f, 18.6316f, 3f, 12f)
                curveTo(3f, 5.36843f, 5.36839f, 3f, 12f, 3f)
                curveTo(18.6315f, 3f, 21f, 5.36842f, 21f, 12f)
                close()
            }
        }.build()
    }
}

