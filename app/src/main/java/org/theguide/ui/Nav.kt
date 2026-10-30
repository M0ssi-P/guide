package org.theguide.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Where the app is: the tab, and the screens opened over it. Shared by the phone and TV shells. */
object Nav {
    enum class Tab { Home, Table, Bible, Live, Songs, Library }
    var tab by mutableStateOf(Tab.Home)
    var player by mutableStateOf(false)
    /** Library's first tab when opened from elsewhere ("All letters" → letters). */
    var libraryTab by mutableStateOf("week")
    var flip by mutableStateOf(0)
    /** Open sheets; the nav bar steps aside while one covers the bottom of the screen. */
    var sheets by mutableStateOf(0)

    fun go(t: Tab) { if (tab != t) { tab = t; flip++ } }
}
