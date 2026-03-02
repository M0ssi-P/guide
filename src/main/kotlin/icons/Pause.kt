package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val pauseCache = mutableMapOf<Color, ImageVector>()

fun pauseIcon(strokeColor: Color): ImageVector {
    return pauseCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(9f, 19f)
                verticalLineTo(5f)
                curveTo(8.99282f, 4.45072f, 8.54928f, 4.00718f, 8f, 4f)
                horizontalLineTo(6f)
                curveTo(5.45072f, 4.00718f, 5.00718f, 4.45072f, 5f, 5f)
                verticalLineTo(19f)
                curveTo(5.00718f, 19.5493f, 5.45072f, 19.9928f, 6f, 20f)
                horizontalLineTo(8f)
                curveTo(8.54928f, 19.9928f, 8.99282f, 19.5493f, 9f, 19f)
                close()
                moveTo(15.3333f, 4f)
                horizontalLineTo(17.3333f)
                curveTo(17.8826f, 4.00718f, 18.3262f, 4.45072f, 18.3333f, 5f)
                verticalLineTo(19f)
                curveTo(18.3262f, 19.5493f, 17.8826f, 19.9928f, 17.3333f, 20f)
                horizontalLineTo(15.3333f)
                curveTo(14.784f, 19.9928f, 14.3405f, 19.5493f, 14.3333f, 19f)
                verticalLineTo(5f)
                curveTo(14.3405f, 4.45072f, 14.784f, 4.00718f, 15.3333f, 4f)
                close()
            }
        }.build()
    }
}

