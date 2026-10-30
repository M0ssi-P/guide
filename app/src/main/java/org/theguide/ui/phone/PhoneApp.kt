package org.theguide.ui.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.theguide.data.Prefs

/** The phone app: first launch until it is done, then the five tabs. */
@Composable
fun PhoneApp() {
    var opened by remember { mutableStateOf(Prefs.setupDone) }
    if (!opened) PhoneLaunch(onOpen = { opened = true }) else PhoneShell()
}
