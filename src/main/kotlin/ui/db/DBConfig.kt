package ui.db

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import components.global.Button
import db.ConfigViewModel
import loadData
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import parsers.bible.models.ILanguage
import ui.UiState
import ui.settings.UserInterfaceSettings
import ui.theme.LocalTheme

@Composable
fun DBConfig(model: ConfigViewModel) {
    val theme = LocalTheme.current
    val languageData = model.languageData.collectAsState()
    val scrollState = rememberScrollState()
    val stage = model.stage.collectAsState()

    when {
        stage.value == 0 -> {
            Column {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(80.dp, 124.dp, 80.dp, 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome, The Guide.", style = theme.typography.h1, color = theme.colors.primaryText)
                    Text("To get started please select your language.", style = theme.typography.tab, color = theme.colors.text,)
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 40.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            when(val lngSt = languageData.value) {
                                is UiState.Loading -> {
                                }
                                is UiState.Success -> {
                                    val data = lngSt.data.sortedWith(
                                        compareBy<ILanguage> { if (it.tag == "en") 0 else 1 }
                                            .thenBy { it.name }
                                    )
                                    for (lng in data) {
                                        val isHovered = remember { mutableStateOf(false) }
                                        val isActiveColor = if(lng.tag == "en") theme.colors.blue3 else {
                                            if (isHovered.value) theme.colors.blue3 else theme.colors.text
                                        };

                                        Button(
                                            modifier = Modifier
                                                .widthIn(max = 228.dp).fillMaxWidth().background(
                                                    color = if (lng.tag == "en") theme.colors.blue100 else {
                                                        if (isHovered.value) theme.colors.blue100 else Color.Unspecified
                                                    },
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .onHover{ isHovered.value = it }
                                                .clickable{
                                                    model.setLanguage(lang = lng)
                                                }
                                                .pointerHoverIcon(
                                                    PointerIcon(
                                                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                                    )
                                                ).padding(vertical = 12.dp, horizontal = 20.dp),
                                            padding = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                                            horizontalArrangement = Arrangement.Start,
                                        ) {
                                            Text(
                                                text = lng.name, color = isActiveColor, style = theme.typography.subs
                                            )
                                        }
                                    }
                                }
                                is UiState.Error -> {}
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
        stage.value == 1 -> {
            InstallationStatus(model)
        }
    }
}