package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val tab: ImageVector
    get() {
        if (_tab != null) return _tab!!
        
        _tab = ImageVector.Builder(
            name = "tab",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF8E8E93))
            ) {
                moveTo(10.0002f, 3.125f)
                curveTo(11.967f, 3.125f, 13.484f, 3.12401f, 14.6311f, 3.22461f)
                curveTo(15.7637f, 3.32396f, 16.6671f, 3.52974f, 17.3147f, 4.04102f)
                curveTo(17.9935f, 4.57711f, 18.2783f, 5.35432f, 18.4114f, 6.2998f)
                curveTo(18.5426f, 7.23252f, 18.5413f, 8.45681f, 18.5413f, 10f)
                curveTo(18.5413f, 11.5432f, 18.5426f, 12.7675f, 18.4114f, 13.7002f)
                curveTo(18.2783f, 14.6457f, 17.9935f, 15.4229f, 17.3147f, 15.959f)
                curveTo(16.6671f, 16.4703f, 15.7637f, 16.676f, 14.6311f, 16.7754f)
                curveTo(13.484f, 16.876f, 11.967f, 16.875f, 10.0002f, 16.875f)
                curveTo(8.03317f, 16.875f, 6.51556f, 16.876f, 5.36841f, 16.7754f)
                curveTo(4.23587f, 16.676f, 3.3334f, 16.4703f, 2.68579f, 15.959f)
                curveTo(2.00674f, 15.4229f, 1.72123f, 14.6459f, 1.58813f, 13.7002f)
                curveTo(1.45689f, 12.7675f, 1.45825f, 11.5432f, 1.45825f, 10f)
                curveTo(1.45825f, 8.45684f, 1.45688f, 7.23252f, 1.58813f, 6.2998f)
                curveTo(1.72123f, 5.35413f, 2.00673f, 4.57712f, 2.68579f, 4.04102f)
                curveTo(3.3334f, 3.52974f, 4.23587f, 3.32396f, 5.36841f, 3.22461f)
                curveTo(6.51556f, 3.12398f, 8.03317f, 3.125f, 10.0002f, 3.125f)
                close()
                moveTo(10.0002f, 4.375f)
                curveTo(9.31333f, 4.375f, 8.69063f, 4.37681f, 8.12524f, 4.38086f)
                verticalLineTo(15.6182f)
                curveTo(8.69063f, 15.6222f, 9.31333f, 15.625f, 10.0002f, 15.625f)
                curveTo(11.9911f, 15.625f, 13.4424f, 15.6239f, 14.5217f, 15.5293f)
                curveTo(15.6153f, 15.4334f, 16.1973f, 15.2483f, 16.5393f, 14.9785f)
                curveTo(16.8498f, 14.7334f, 17.0598f, 14.3384f, 17.1741f, 13.5264f)
                curveTo(17.2902f, 12.7013f, 17.2913f, 11.5817f, 17.2913f, 10f)
                curveTo(17.2913f, 8.41826f, 17.2902f, 7.29871f, 17.1741f, 6.47363f)
                curveTo(17.0598f, 5.66161f, 16.8498f, 5.26661f, 16.5393f, 5.02148f)
                curveTo(16.1973f, 4.75169f, 15.6153f, 4.56663f, 14.5217f, 4.4707f)
                curveTo(13.4424f, 4.37605f, 11.9911f, 4.375f, 10.0002f, 4.375f)
                close()
                moveTo(6.87524f, 4.40137f)
                curveTo(6.34985f, 4.41539f, 5.88701f, 4.43481f, 5.47778f, 4.4707f)
                curveTo(4.384f, 4.56665f, 3.8022f, 4.7516f, 3.46021f, 5.02148f)
                curveTo(3.1497f, 5.26662f, 2.93974f, 5.66159f, 2.82544f, 6.47363f)
                curveTo(2.70932f, 7.29871f, 2.70825f, 8.41827f, 2.70825f, 10f)
                curveTo(2.70825f, 11.5817f, 2.70932f, 12.7013f, 2.82544f, 13.5264f)
                curveTo(2.93974f, 14.3384f, 3.1497f, 14.7334f, 3.46021f, 14.9785f)
                curveTo(3.8022f, 15.2484f, 4.384f, 15.4334f, 5.47778f, 15.5293f)
                curveTo(5.88701f, 15.5652f, 6.34987f, 15.5836f, 6.87524f, 15.5977f)
                verticalLineTo(4.40137f)
                close()
            }
        }.build()
        
        return _tab!!
    }

private var _tab: ImageVector? = null

