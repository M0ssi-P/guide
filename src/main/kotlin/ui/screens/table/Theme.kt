package ui.screens.table

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import ui.theme.Fraunces
import ui.theme.Inter

object TableColors {
    val Bg = Color(0xFF090B10)
    val BgElev = Color(0xFF0E1219)
    val Ink = Color(0xFFECE5D3)
    val InkDim = Color(0xFF454B58)
    val InkMid = Color(0xFF727C90)
    val Gold = Color(0xFFC9A15F)
    val GoldBright = Color(0xFFEFCB7E)
    val Ember = Color(0xFFB0623C)
    val EmberBright = Color(0xFFD98A63)
    val Divider = Color(0xFF191E27)
    val PlayInk = Color(0xFF1B1509)

    val HighlightGold = Color(0xFFC9A15F).copy(alpha = 0.32f)
    val HighlightRose = Color(0xFFD87878).copy(alpha = 0.32f)
    val HighlightTeal = Color(0xFF6EB4AA).copy(alpha = 0.32f)
    val HighlightViolet = Color(0xFFA082C8).copy(alpha = 0.32f)
}

val FrauncesFamily: FontFamily = Fraunces
val InterFamily: FontFamily = Inter

enum class HighlightColor(val swatch: Color, val tint: Color, val label: String) {
    Gold(TableColors.Gold, TableColors.HighlightGold, "Gold"),
    Rose(Color(0xFFD87878), TableColors.HighlightRose, "Rose"),
    Teal(Color(0xFF6EB4AA), TableColors.HighlightTeal, "Teal"),
    Violet(Color(0xFFA082C8), TableColors.HighlightViolet, "Violet")
}
