package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val sizeCache = mutableMapOf<Color, ImageVector>()

fun sizeIcon(strokeColor: Color): ImageVector {
    return sizeCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "size",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path {
            }
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(12.41f, 19.107f)
                curveToRelative(0.643f, 0f, 1f, -0.33f, 1.25f, -1.17f)
                lineToRelative(0.679f, -2.07f)
                horizontalLineToRelative(4.275f)
                lineToRelative(0.688f, 2.07f)
                curveToRelative(0.24f, 0.83f, 0.616f, 1.17f, 1.267f, 1.17f)
                curveToRelative(0.688f, 0f, 1.143f, -0.41f, 1.143f, -1.053f)
                curveToRelative(0f, -0.25f, -0.063f, -0.545f, -0.188f, -0.928f)
                lineToRelative(-3.07f, -8.802f)
                curveToRelative(-0.384f, -1.143f, -0.92f, -1.607f, -1.928f, -1.607f)
                curveToRelative(-1.018f, 0f, -1.563f, 0.473f, -1.946f, 1.607f)
                lineToRelative(-3.071f, 8.801f)
                curveToRelative(-0.152f, 0.456f, -0.214f, 0.732f, -0.214f, 0.973f)
                curveToRelative(0f, 0.607f, 0.455f, 1.01f, 1.116f, 1.01f)
                close()
                moveToRelative(2.464f, -5.034f)
                lineToRelative(1.553f, -4.928f)
                horizontalLineToRelative(0.116f)
                lineToRelative(1.545f, 4.928f)
                horizontalLineToRelative(-3.214f)
                close()
                moveTo(3.564f, 18.076f)
                curveToRelative(0.611f, 0f, 0.937f, -0.312f, 1.167f, -1.117f)
                lineToRelative(0.367f, -1.132f)
                horizontalLineToRelative(3.187f)
                lineToRelative(0.361f, 1.132f)
                curveToRelative(0.23f, 0.784f, 0.583f, 1.117f, 1.194f, 1.117f)
                curveToRelative(0.66f, 0f, 1.09f, -0.388f, 1.09f, -0.992f)
                curveToRelative(0f, -0.243f, -0.062f, -0.521f, -0.187f, -0.91f)
                lineToRelative(-2.09f, -6.144f)
                curveToRelative(-0.382f, -1.132f, -0.91f, -1.576f, -1.91f, -1.576f)
                curveToRelative(-0.999f, 0f, -1.54f, 0.451f, -1.915f, 1.576f)
                lineTo(2.73f, 16.174f)
                curveToRelative(-0.16f, 0.465f, -0.222f, 0.729f, -0.222f, 0.958f)
                curveToRelative(0f, 0.556f, 0.43f, 0.944f, 1.055f, 0.944f)
                close()
                moveToRelative(2.007f, -3.908f)
                lineToRelative(1.076f, -3.59f)
                horizontalLineToRelative(0.11f)
                lineToRelative(1.063f, 3.59f)
                horizontalLineTo(5.57f)
                close()
            }
        }.build()
    }
}

