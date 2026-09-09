package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val volumeMutedCache = mutableMapOf<Color, ImageVector>()

fun VolumeMuted(strokeColor: Color): ImageVector {
    return volumeMutedCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "VolumeMuted",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(10.3398f, 7.70216f)
                curveTo(9.96236f, 7.98628f, 9.51024f, 8.31011f, 9.0494f, 8.46161f)
                lineTo(9.04796f, 8.46209f)
                curveTo(8.47367f, 8.64961f, 7.94783f, 8.71269f, 7.55101f, 8.76029f)
                curveTo(7.43855f, 8.77379f, 7.33645f, 8.78603f, 7.24655f, 8.79952f)
                curveTo(6.83928f, 8.86061f, 6.53346f, 8.9507f, 6.16917f, 9.26019f)
                curveTo(5.83236f, 9.54632f, 5.63674f, 9.94599f, 5.53022f, 10.4646f)
                curveTo(5.42097f, 10.9964f, 5.41797f, 11.5862f, 5.42251f, 12.2084f)
                lineTo(5.42259f, 12.2193f)
                lineTo(5.42251f, 12.2193f)
                curveTo(5.41797f, 12.8415f, 5.42097f, 13.4313f, 5.53022f, 13.9632f)
                curveTo(5.6366f, 14.4811f, 5.83186f, 14.8804f, 6.16787f, 15.1664f)
                curveTo(6.49718f, 15.4432f, 6.77596f, 15.5398f, 7.10845f, 15.5992f)
                curveTo(7.25514f, 15.6254f, 7.40094f, 15.6426f, 7.5784f, 15.6636f)
                curveTo(7.6293f, 15.6696f, 7.68279f, 15.676f, 7.73966f, 15.6829f)
                curveTo(7.98335f, 15.7126f, 8.26316f, 15.751f, 8.57267f, 15.8227f)
                curveTo(8.97622f, 15.916f, 9.22765f, 16.3189f, 9.13427f, 16.7224f)
                curveTo(9.04089f, 17.126f, 8.63804f, 17.3774f, 8.23449f, 17.284f)
                curveTo(8.00268f, 17.2304f, 7.7848f, 17.1995f, 7.55819f, 17.1719f)
                curveTo(7.51572f, 17.1667f, 7.47168f, 17.1615f, 7.42647f, 17.1562f)
                curveTo(7.24264f, 17.1346f, 7.03949f, 17.1106f, 6.8445f, 17.0758f)
                curveTo(6.31402f, 16.981f, 5.77443f, 16.7958f, 5.20065f, 16.313f)
                lineTo(5.19799f, 16.3107f)
                curveTo(4.52374f, 15.7379f, 4.20937f, 14.9878f, 4.06089f, 14.265f)
                curveTo(3.91558f, 13.5575f, 3.91812f, 12.8165f, 3.92251f, 12.2139f)
                curveTo(3.91812f, 11.6113f, 3.91558f, 10.8702f, 4.06089f, 10.1628f)
                curveTo(4.20937f, 9.43991f, 4.52374f, 8.68984f, 5.19799f, 8.11703f)
                curveTo(5.84475f, 7.56757f, 6.44709f, 7.40266f, 7.02403f, 7.31611f)
                curveTo(7.18097f, 7.29257f, 7.32504f, 7.2751f, 7.46326f, 7.25833f)
                curveTo(7.83305f, 7.21347f, 8.16097f, 7.17369f, 8.58154f, 7.03644f)
                curveTo(8.77382f, 6.97305f, 9.04069f, 6.80259f, 9.43763f, 6.50377f)
                curveTo(9.52397f, 6.43878f, 9.61602f, 6.36808f, 9.71253f, 6.29395f)
                curveTo(10.0169f, 6.06015f, 10.3658f, 5.79223f, 10.7198f, 5.56161f)
                curveTo(11.6567f, 4.95137f, 12.9663f, 4.37899f, 14.368f, 5.20643f)
                curveTo(14.3856f, 5.21683f, 14.4028f, 5.22794f, 14.4195f, 5.23975f)
                curveTo(15.5112f, 6.01102f, 16.0324f, 7.23458f, 16.2366f, 9.39653f)
                curveTo(16.2755f, 9.8089f, 15.9728f, 10.1748f, 15.5604f, 10.2137f)
                curveTo(15.148f, 10.2526f, 14.7822f, 9.94991f, 14.7432f, 9.53753f)
                curveTo(14.5566f, 7.56081f, 14.1222f, 6.8777f, 13.5794f, 6.48308f)
                curveTo(12.9589f, 6.13116f, 12.3418f, 6.29527f, 11.5385f, 6.8185f)
                curveTo(11.2378f, 7.01438f, 10.9566f, 7.23027f, 10.6635f, 7.45526f)
                curveTo(10.5576f, 7.53654f, 10.4502f, 7.61901f, 10.3398f, 7.70216f)
                close()
            }
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(15.6085f, 12.9227f)
                curveTo(16.0224f, 12.9387f, 16.3449f, 13.2872f, 16.3289f, 13.7011f)
                curveTo(16.2118f, 16.7295f, 15.6911f, 18.2758f, 14.4212f, 19.1778f)
                curveTo(14.4047f, 19.1895f, 14.3877f, 19.2005f, 14.3703f, 19.2109f)
                curveTo(12.9338f, 20.0654f, 11.5885f, 19.4381f, 10.6497f, 18.8027f)
                curveTo(10.3066f, 18.5705f, 10.2167f, 18.1043f, 10.4489f, 17.7612f)
                curveTo(10.6811f, 17.4182f, 11.1473f, 17.3283f, 11.4904f, 17.5604f)
                curveTo(12.3119f, 18.1164f, 12.9469f, 18.2971f, 13.5777f, 17.9367f)
                curveTo(14.2108f, 17.4738f, 14.7161f, 16.5884f, 14.8301f, 13.6431f)
                curveTo(14.8461f, 13.2292f, 15.1946f, 12.9066f, 15.6085f, 12.9227f)
                close()
            }
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(20.8159f, 4.14063f)
                curveTo(21.1088f, 4.43352f, 21.1088f, 4.9084f, 20.8159f, 5.20129f)
                lineTo(5.47113f, 20.546f)
                curveTo(5.17823f, 20.8389f, 4.70336f, 20.8389f, 4.41047f, 20.546f)
                curveTo(4.11757f, 20.2531f, 4.11757f, 19.7783f, 4.41047f, 19.4854f)
                lineTo(19.7552f, 4.14063f)
                curveTo(20.0481f, 3.84774f, 20.523f, 3.84774f, 20.8159f, 4.14063f)
                close()
            }
        }.build()
    }
}
