package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val prevCache = mutableMapOf<Color, ImageVector>()

fun prevIcon(strokeColor: Color): ImageVector {
    return prevCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "prev",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(8f, 6.75f)
                curveTo(8f, 6.33579f, 7.66421f, 6f, 7.25f, 6f)
                curveTo(6.83579f, 6f, 6.5f, 6.33579f, 6.5f, 6.75f)
                lineTo(6.5f, 17.25f)
                curveTo(6.5f, 17.6642f, 6.83579f, 18f, 7.25f, 18f)
                curveTo(7.66421f, 18f, 8f, 17.6642f, 8f, 17.25f)
                verticalLineTo(13.5152f)
                lineTo(15.25f, 17.7009f)
                curveTo(16.25f, 18.2783f, 17.5f, 17.5566f, 17.5f, 16.4019f)
                verticalLineTo(7.59806f)
                curveTo(17.5f, 6.44336f, 16.25f, 5.72167f, 15.25f, 6.29903f)
                lineTo(8f, 10.4848f)
                verticalLineTo(6.75f)
                close()
            }
        }.build()
    }
}

