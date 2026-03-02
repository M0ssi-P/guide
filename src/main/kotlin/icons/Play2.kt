package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val play2Cache = mutableMapOf<Color, ImageVector>()

fun play2Icon(strokeColor: Color): ImageVector {
    return play2Cache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "play2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(19.4486f, 10.4948f)
                curveTo(20.6073f, 11.1638f, 20.6073f, 12.8362f, 19.4486f, 13.5052f)
                lineTo(8.60712f, 19.7645f)
                curveTo(7.4484f, 20.4335f, 6f, 19.5973f, 6f, 18.2593f)
                lineTo(6f, 5.74067f)
                curveTo(6f, 4.4027f, 7.4484f, 3.56646f, 8.60712f, 4.23545f)
                lineTo(19.4486f, 10.4948f)
                close()
            }
        }.build()
    }
}