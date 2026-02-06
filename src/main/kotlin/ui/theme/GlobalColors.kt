package ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

val Arbutus = FontFamily(
    Font("fonts/Arbutus-Regular.ttf", FontWeight.Normal)
)

val Inter = FontFamily(
    Font("fonts/Inter-Light.ttf", FontWeight.Light),
    Font("fonts/Inter-Regular.ttf", FontWeight.Normal),
    Font("fonts/Inter-Bold.ttf", FontWeight.Bold),
    Font("fonts/Inter-Light.ttf", FontWeight.Light),
    Font("fonts/Inter-Medium.ttf", FontWeight.Medium),
    Font("fonts/Inter-SemiBold.ttf", FontWeight.SemiBold),
    Font("fonts/Inter-LightItalic.ttf", FontWeight.W100)
)

val LightColors = Colors(
    primary = Color(0xFF006BFF),
    background = Color(0xFFFFFFFF),
    text = Color(0xFF8E8E93),
    primaryText = Color(0xFF071108),
    secondaryText = Color(0xFFE5A4CB),
    surface = Color(0xFFFFFFFF),
    light = Color(0xFFFFFFFF),
    border = Color(0xFFEEEEEC),
    menu = Color(0xFFFAFAFA),
    crimson = Color(0xFFD72638),
    aquamarine = Color(0xFF7FFFD4),
    activeTab = Color(0xFFFFFFFF),
    primaryHighlight = Color(0xFFFFD23F),
    secondaryHighlight = Color(0xFFFAFAFA),
    blue = Color(0xFF006BFF),
    blue3 = Color(0xFF2471DB),
    blue100 = Color(0xFFE0F2FE),
    menuHoverColor = Color(0xFFEEEEEE),
    disabledOne = Color(0xFFDFE0DE),
    borderTwo = Color(0xFFEFEFEF),
    gray2nd = Color(0xFFEDF1F5),
    deeming = Color(0xFF8BA0B2),
    periwinkle = Color(0xFFD8DCFF),
    night = Color(0xFF000000)
)

val CustomTypography = Typography(
    h1 = TextStyle(fontFamily = Arbutus, fontSize = 24.sp, fontWeight = FontWeight.Normal),
    h2 = TextStyle(fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    h3 = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    h4 = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    h4BOLD = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold),
    hOne = TextStyle(fontFamily = Inter, fontSize = 32.sp, fontWeight = FontWeight.Normal),
    body = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    button = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Bold),
    subs = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    semiText = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    tab = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal)
)

val CustomSpacing = Spacing(
    sm = 10.dp,
    md = 16.dp,
    lg = 20.dp,
    xlg = 25.dp
)