package ui.theme

import androidx.compose.ui.graphics.Color

data class AppTheme(
    val id: String,
    val label: String,
    val swatch: Color,
    val swatchBright: Color = swatch
) {
    fun colorsFor(base: Colors): Colors {
        if (id == "default") return base
        return base.copy(
            primary = swatch,
            primaryBright = swatchBright,
            primaryHighlight = swatchBright,
            blue = mix(swatchBright, swatch, 0.08f),
//            activeTab = swatch,
            disabledOne = mix(base.disabledOne, swatch, 0.08f),
            background = mix(base.background, swatch, 0.05f),
            menuHoverColor = mix(base.menuHoverColor, swatch, 0.05f),
            surface = mix(base.surface, swatch, 0.06f),
            blue3 = mix(base.blue3, swatch, 0.05f),
            menu = mix(base.menu, swatch, 0.05f),
            popup = mix(base.popup, swatch, 0.06f),
            popupLayer = mix(base.popupLayer, swatch, 0.06f),
            border = mix(base.border, swatch, 0.08f),
            deeming = mix(base.deeming, swatch, 0.12f)
        )
    }
}

private fun mix(base: Color, accent: Color, amount: Float): Color = Color(
    red = base.red + (accent.red - base.red) * amount,
    green = base.green + (accent.green - base.green) * amount,
    blue = base.blue + (accent.blue - base.blue) * amount,
    alpha = base.alpha
)

private val TableGold = Color(0xFFC9A15F)
private val TableGoldBright = Color(0xFFEFCB7E)

const val DefaultAccentThemeId = "gold"

val AppThemes: List<AppTheme> = listOf(
    AppTheme(id = "default", label = "Default", swatch = DarkColors.primary, swatchBright = DarkColors.primaryBright),
    AppTheme(id = "classic", label = "Classic", swatch = Color(0xFF9B8FE0), swatchBright = Color(0xFFBDB4EE)),
    AppTheme(id = "blue", label = "Blue", swatch = Color(0xFF5B6EE8), swatchBright = Color(0xFF8A97F2)),
    AppTheme(id = "teal", label = "Teal", swatch = Color(0xFF2FBF9F), swatchBright = Color(0xFF6BDCC3)),
    AppTheme(id = "red", label = "Red", swatch = Color(0xFFE0566B), swatchBright = Color(0xFFEC8B9A)),
    AppTheme(id = "gray", label = "Gray", swatch = Color(0xFFB5BAC1), swatchBright = Color(0xFFD8DBDF)),
    AppTheme(id = "green", label = "Green", swatch = Color(0xFF3FB950), swatchBright = Color(0xFF7BD988)),
    AppTheme(id = "forest", label = "Forest", swatch = Color(0xFF2C6E49), swatchBright = Color(0xFF5B9678)),
    AppTheme(id = "autumn", label = "Autumn", swatch = Color(0xFFE0813A), swatchBright = Color(0xFFEDA76B)),
    AppTheme(id = "frost", label = "Frost", swatch = Color(0xFF6FB8E8), swatchBright = Color(0xFFA3D4F2)),
    AppTheme(id = "gold", label = "Gold", swatch = TableGold, swatchBright = TableGoldBright)
)