package components.global

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.searchIcon
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun Search(filled: Boolean = false, onValueChange: (e: String) -> Unit = {}) {
    val theme = LocalTheme.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    val smoothBorder = animateColorAsState(
        targetValue = if(isFocused) theme.colors.periwinkle else theme.colors.light
    )

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier
                    .size(25.dp)
                    .border(width = 1.dp, shape = RoundedCornerShape(50.dp), color = theme.colors.border)
                    .background(
                        shape = RoundedCornerShape(50.dp),
                        color = theme.colors.surface
                    ),
            ) {
                Text("@", style = theme.typography.tab, color = theme.colors.text)
            }

            Button(
                modifier = Modifier
                    .height(25.dp)
                    .border(width = 1.dp, shape = RoundedCornerShape(50.dp), color = theme.colors.border)
                    .background(
                        shape = RoundedCornerShape(50.dp),
                        color = theme.colors.surface
                    )
                    .padding(horizontal = 12.dp),

                fillMaxSize = false,
                secondModifier = Modifier.fillMaxHeight()
            ) {
                Text("ENGLISH", style = theme.typography.tab, color = theme.colors.text)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
        ) {
            BasicTextField(
                value = text,
                singleLine = true,
                onValueChange = {
                    text = it
                    onValueChange(it)
                },
                textStyle = TextStyle(
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = theme.colors.text
                ),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.colors.gray2nd),

                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),

                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = searchIcon(theme.colors.text),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(13.dp))
                            Box() {
                                if (text.isEmpty()) {
                                    Text("Search song by title/number...", style = theme.typography.tab, color = theme.colors.text)
                                }
                                inner()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
                    .border(width = 2.dp, shape = RoundedCornerShape(4.dp), color = smoothBorder.value)
            )
        }
    }
}