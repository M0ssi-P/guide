package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val settingsCache = mutableMapOf<Color, ImageVector>()

fun settingsIcon(strokeColor: Color): ImageVector {
    return settingsCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "setting",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9.5f)
                curveTo(13.3809f, 9.5f, 14.5f, 10.6191f, 14.5f, 12f)
                curveTo(14.5f, 13.3809f, 13.3809f, 14.5f, 12f, 14.5f)
                curveTo(10.6191f, 14.5f, 9.5f, 13.3809f, 9.5f, 12f)
                curveTo(9.5f, 10.6191f, 10.6191f, 9.5f, 12f, 9.5f)
                close()
            }
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.168f, 7.25025f)
                curveTo(19.4845f, 6.05799f, 17.9712f, 5.65004f, 16.7885f, 6.33852f)
                curveTo(15.7598f, 6.93613f, 14.4741f, 6.18838f, 14.4741f, 4.99218f)
                curveTo(14.4741f, 3.61619f, 13.3659f, 2.5f, 11.9998f, 2.5f)
                curveTo(10.6337f, 2.5f, 9.52546f, 3.61619f, 9.52546f, 4.99218f)
                curveTo(9.52546f, 6.18838f, 8.23977f, 6.93613f, 7.21199f, 6.33852f)
                curveTo(6.02829f, 5.65004f, 4.51507f, 6.05799f, 3.83153f, 7.25025f)
                curveTo(3.14896f, 8.4425f, 3.55399f, 9.96665f, 4.73769f, 10.6541f)
                curveTo(5.76546f, 11.2527f, 5.76546f, 12.7473f, 4.73769f, 13.3459f)
                curveTo(3.55399f, 14.0343f, 3.14896f, 15.5585f, 3.83153f, 16.7498f)
                curveTo(4.51507f, 17.942f, 6.02829f, 18.35f, 7.21101f, 17.6625f)
                curveTo(8.23879f, 17.0639f, 9.52546f, 17.8116f, 9.52546f, 19.0078f)
                curveTo(9.52546f, 20.3838f, 10.6337f, 21.5f, 11.9998f, 21.5f)
                curveTo(13.3659f, 21.5f, 14.4741f, 20.3838f, 14.4741f, 19.0078f)
                curveTo(14.4741f, 17.8116f, 15.7598f, 17.0639f, 16.7885f, 17.6625f)
                curveTo(17.9712f, 18.35f, 19.4845f, 17.942f, 20.168f, 16.7498f)
                curveTo(20.8515f, 15.5585f, 20.4455f, 14.0343f, 19.2628f, 13.3459f)
                curveTo(18.2351f, 12.7473f, 18.2341f, 11.2527f, 19.2628f, 10.6541f)
                curveTo(20.4455f, 9.96665f, 20.8515f, 8.4425f, 20.168f, 7.25025f)
                close()
            }
        }.build()
    }
}

