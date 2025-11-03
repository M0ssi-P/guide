import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

enum class IntUiThemes {
    Light,
    Dark,
    System;

    fun isDark(): Boolean = (if (this == System) fromSystemTheme(currentSystemTheme) else this) == Dark

    companion object {
        fun fromSystemTheme(systemTheme: SystemTheme): IntUiThemes {
            return if (systemTheme == SystemTheme.LIGHT) Light else Dark
        }
    }
}