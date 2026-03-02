package components.global

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrowDown
import kotlinx.coroutines.launch
import mvvm.BibleViewModel
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme

@Composable
fun VersionSelector(model: BibleViewModel) {
    val theme = LocalTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    var show by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (show) 180f else 0f
    )

    PopoverAnchored(
        modifier = Modifier
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .dropShadow(
                shape = RoundedCornerShape(6.dp),
                block = {
                    color = Color.Black.copy(alpha = 0.12f)
                    alpha = 1f
                    spread = -6f
                    radius = 28f
                    offset = Offset(0f, 14f)
                }
            )
            .background(
                theme.colors.popup,
                shape = RoundedCornerShape(6.dp)
            ),
        hoverEnabled = false,
        onChange = {
            show = it.value
        },
        popup = { state ->
            LanguageVersionSelectorPopup(state, model)
        }
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 150.dp)
                .clip(shape = RoundedCornerShape(4.dp))
                .background(theme.colors.popup)
                .padding(horizontal = 11.dp, vertical = 8.dp)
                .pointerHoverIcon(
                    PointerIcon(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    )
                ),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(model.currentVersion?.abbreviation ?: "Loading...", style = theme.typography.button, color = theme.colors.deeming)
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = arrowDown(theme.colors.deeming),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp).rotate(rotation)
            )
        }
    }
}

@Composable
fun LanguageVersionSelectorPopup(state: MutableState<Boolean>, model: BibleViewModel) {
    val theme = LocalTheme.current
    val languages = model.languages
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .heightIn(max = 405.dp, min = 405.dp)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ){
        Column(Modifier.widthIn(min = 150.dp, max = 150.dp).fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Languages", style = theme.typography.body)
            }
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    state = model.languageScrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    model.bookScrollState.scrollBy(-delta)
                                }
                            },
                        ),
                ) {
                    languages.let {
                        if(languages.isNotEmpty()) {
                            itemsIndexed(languages) { index, language ->
                                SelectorOption(language.localName, isActive = model.currentLanguage?.localName == language.localName, onClick = {
                                    model.putCurrentLanguage(language)
                                })
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState = model.languageScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
        Column(Modifier.widthIn(max = 500.dp).fillMaxWidth()) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("Select version", style = theme.typography.body)
            }
            Box {
                LazyColumn(
                    state = model.versionScrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    model.versionScrollState.scrollBy(-delta)
                                }
                            },
                        ),
                ) {
                    if(model.versions.isNotEmpty()) {
                        itemsIndexed(model.versions) { index, version ->
                            val interactionSource = remember { MutableInteractionSource() }
                            var isHovered by remember { mutableStateOf(false) }
                            val isActive = model.currentVersion == version

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isHovered || isActive) theme.colors.menuHoverColor else theme.colors.popup)
                                    .onHover{ bool ->
                                        isHovered = bool
                                    }
                                    .pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        onClick = {
                                        }
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(version.title, style = theme.typography.tab, fontSize = 10.sp, color = theme.colors.text)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(version.abbreviation, style = theme.typography.tab)
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalScrollbar(
                    scrollState = model.versionScrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
            }
        }
    }
}