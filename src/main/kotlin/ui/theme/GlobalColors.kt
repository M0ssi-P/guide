package ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
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
    Font("fonts/Inter-Regular.ttf", FontWeight.Normal),
    Font("fonts/Inter-Bold.ttf", FontWeight.Bold),
    Font("fonts/Inter-Light.ttf", FontWeight.Light),
    Font("fonts/Inter-Medium.ttf", FontWeight.Medium),
)

val LightColors = Colors(
    primary = Color(0xFF006BFF),
    background = Color(0xFFFFFFFF),
    text = Color(0xFF8E8E93),
    primaryText = Color(0xFF071108),
    secondaryText = Color(0xFFE5A4CB),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFEEEEEC),
    menu = Color(0xFFFAFAFA),
    activeTab = Color(0xFFFFFFFF),
    primaryHighlight = Color(0xFFFFD23F),
    secondaryHighlight = Color(0xFFFAFAFA)
)

val CustomTypography = Typography(
    h1 = TextStyle(fontFamily = Arbutus, fontSize = 24.sp, fontWeight = FontWeight.Normal),
    h2 = TextStyle(fontFamily = Inter, fontSize = 20.sp, fontWeight = FontWeight.Bold),
    h3 = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    body = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    button = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Bold),
    subs = TextStyle(fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    tab = TextStyle(fontFamily = Arbutus, fontSize = 12.sp, fontWeight = FontWeight.Normal)
)

val CustomSpacing = Spacing(
    sm = 10.dp,
    md = 16.dp,
    lg = 20.dp,
    xlg = 25.dp
)