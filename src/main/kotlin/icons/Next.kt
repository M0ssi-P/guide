package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val nextCache = mutableMapOf<Color, ImageVector>()

fun nextIcon(strokeColor: Color): ImageVector {
    return nextCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "next",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(16f, 6.75f)
                curveTo(16f, 6.33579f, 16.3358f, 6f, 16.75f, 6f)
                curveTo(17.1642f, 6f, 17.5f, 6.33579f, 17.5f, 6.75f)
                lineTo(17.5f, 17.25f)
                curveTo(17.5f, 17.6642f, 17.1642f, 18f, 16.75f, 18f)
                curveTo(16.3358f, 18f, 16f, 17.6642f, 16f, 17.25f)
                verticalLineTo(13.5152f)
                lineTo(8.75f, 17.7009f)
                curveTo(7.75f, 18.2783f, 6.5f, 17.5566f, 6.5f, 16.4019f)
                verticalLineTo(7.59806f)
                curveTo(6.5f, 6.44336f, 7.75f, 5.72167f, 8.75f, 6.29903f)
                lineTo(16f, 10.4848f)
                verticalLineTo(6.75f)
                close()
            }
        }.build()
    }
}