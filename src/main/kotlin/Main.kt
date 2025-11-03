import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import components.titleBarView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import navigation.LocalNavigator
import navigation.NavSystem
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarStyle
import parsers.bible.Bible
import parsers.vgr.Table
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.screens.NavHost
import ui.theme.LocalTheme
import ui.theme.ThemeUI

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp)){
        Button(onClick = {
            text = "Hello, Desktop!"
            CoroutineScope(Dispatchers.Swing).launch {
                withContext(Dispatchers.IO) {
                    val t = Table()
//                    val l = t.getAllLanguages()
//                    val one = l.find { it.iso63901 == "en"}
//                    val res = t.getAllSermons(one!!)
//                    val sermon = res[0]
//                    val req = t.getSermon(sermon)
                    val cal = t.getCalendar().activeDays.find { it.day == 11 }
                    println(t.qotd(cal!!))
//                    val b = Bible()
//                    val versions = b.getVersionsFromLanguage("eng", "all")
//                    val one = versions.find { it.localAbbreviation == "NKJV" }
//                    val fillVersion = b.getFullVersion(one!!)
//                    val getChapter = b.getChapterDoc(fillVersion, book = "PSA", 23)
//                    println(getChapter)
                }
            }
        }) {
            Text(text)
        }
    }
}

fun main() {
    initialiseNetwork()

     application {
         val ui by rememberUpdatedState(IntUiThemes.Dark)
         val textStyle = JewelTheme.createDefaultTextStyle()
         val windowState = rememberWindowState(size = DpSize(1390.dp, 865.dp))

         val themeDefinition =
             if (ui.isDark()) {
                 JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
             } else {
                 JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
             }

         ThemeUI(ui.isDark()) {
             val theme = LocalTheme.current

             IntUiTheme(
                 theme = themeDefinition,
                 styling = ComponentStyling.default().decoratedWindow(
                     titleBarStyle =
                         when(ui) {
                             IntUiThemes.Light -> TitleBarStyle.light()
                             IntUiThemes.Dark -> {
                                 TitleBarStyle.dark(
                                     colors = TitleBarColors(
                                         background = theme.colors.menu,
                                         inactiveBackground = theme.colors.menu,
                                         content = theme.colors.text, // This is what sets icon + title color
                                         border = Color.Unspecified,

                                         fullscreenControlButtonsBackground = theme.colors.text,
                                         titlePaneButtonHoveredBackground = theme.colors.text,
                                         titlePaneButtonPressedBackground = theme.colors.text,
                                         titlePaneCloseButtonHoveredBackground = theme.colors.text,
                                         titlePaneCloseButtonPressedBackground = theme.colors.text,

                                         iconButtonHoveredBackground = theme.colors.text,
                                         iconButtonPressedBackground = theme.colors.text,

                                         dropdownHoveredBackground = theme.colors.text,
                                         dropdownPressedBackground = theme.colors.text
                                     )
                                 )
                             }
                             IntUiThemes.System ->
                                 if (ui.isDark()) {
                                     TitleBarStyle.dark()
                                 } else {
                                     TitleBarStyle.light()
                                 }
                         }
                 ),
                 swingCompatMode = false
             ) {
                 DecoratedWindow(
                     onCloseRequest = { exitApplication() },
                     state = windowState,
                     title = "Jewel standalone sample",
                 ) {
                     NavSystem {
                         val navigator = LocalNavigator.current

                         Row(modifier = Modifier.fillMaxSize().background(theme.colors.background)) {
                             Box(modifier = Modifier
                                 .width(241.dp)
                                 .fillMaxHeight()
                                 .background(theme.colors.menu)
                                 .newBorder(theme.colors.border, 1.dp, setOf(BorderSide.Right))
                             ) {
                             }
                             Column(modifier = Modifier.fillMaxSize()){
                                 titleBarView(ui)
                                 Box(
                                     modifier = Modifier.fillMaxSize(),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     NavHost(navigator)
                                 }
                             }
                         }
                     }
                 }
             }
         }
     }
}
