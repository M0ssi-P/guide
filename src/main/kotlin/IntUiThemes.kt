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

public enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown;

    public companion object {
        public val Current: DesktopPlatform by lazy {
            val name = System.getProperty("os.name")
            when {
                name?.startsWith("Linux") == true -> Linux
                name?.startsWith("Win") == true -> Windows
                name == "Mac OS X" -> MacOS
                else -> Unknown
            }
        }
    }
}