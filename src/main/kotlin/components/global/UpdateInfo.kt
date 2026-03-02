package components.global

import APP_VERSION
import DesktopPlatform
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import formatAssetSize
import markdownBodyToAnnotatedString
import mvvm.UpdateViewModel
import org.jetbrains.jewel.ui.component.Text
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.Inter
import ui.theme.LocalTheme
import java.io.File
import java.lang.management.OperatingSystemMXBean

@Composable
fun UpdateInfo(viewModel: UpdateViewModel) {
    val theme = LocalTheme.current

    Column(
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
            .clip(RoundedCornerShape(6.dp))
            .background(theme.colors.popup)
            .width(705.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(theme.colors.popupLayer)
                .newBorder(color = theme.colors.border, width = 1.dp, sides = setOf(BorderSide.Bottom))
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 15.dp),
                text = "The Guide Updates and Plugins...",
                color = theme.colors.primaryText
            )
        }
        Box(
            Modifier.fillMaxWidth()
                .heightIn(max = 305.dp)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            viewModel.updateInfo?.body?.let {
                val annotated = markdownBodyToAnnotatedString(it)
                val uriHandler = LocalUriHandler.current

                ClickableText(
                    text = annotated,
                    onClick = { offset ->
                        annotated.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { uri ->  uriHandler.openUri(uri.item) }
                    },
                    style = TextStyle(
                        fontFamily = Inter,
                        color = theme.colors.night
                    )
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(theme.colors.popupLayer)
                .newBorder(color = theme.colors.border, width = 1.dp, sides = setOf(BorderSide.Top))
        ) {
            val asset = when(DesktopPlatform.Current) {
                DesktopPlatform.Windows -> {
                    viewModel.updateInfo?.assets?.findLast { it.name.contains(".exe") }
                }
                DesktopPlatform.MacOS -> { viewModel.updateInfo?.assets?.findLast { it.name.contains(".dmg") } }
                DesktopPlatform.Linux -> {
                    viewModel.updateInfo?.assets?.findLast { it.name.contains(".deb") }
                }
                DesktopPlatform.Unknown -> {
                    null
                }
            }
            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                text = "Updating ${java.time.Year.now().value}.${APP_VERSION.removePrefix("v")} to ${viewModel.updateInfo?.tag_name}. Patch size ${formatAssetSize(asset?.size ?: 1500L)}",
                style = theme.typography.tab,
                color = theme.colors.primaryText
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.popup).border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) {}
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    fillMaxSize = false
                ) {
                    Text("Ignore This Update", style = theme.typography.tab, color = theme.colors.text)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.popup).border(width = 1.dp, color = theme.colors.border, shape = RoundedCornerShape(4.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) {}
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        fillMaxSize = false
                    ) {
                        Text("Remind Me Later", style = theme.typography.tab, color = theme.colors.text)
                    }
                    Button(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF109DF5), Color(0xFF1B55F7))
                            )
                        ).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) {
                            val name = when(DesktopPlatform.Current) {
                                DesktopPlatform.Windows -> {
                                    "updater.exe"
                                }
                                DesktopPlatform.MacOS -> { "updater" }
                                DesktopPlatform.Linux -> {
                                    "updater"
                                }
                                DesktopPlatform.Unknown -> {
                                    error("Unsupported platform")
                                }
                            }
                            val tempDir = System.getProperty("java.io.tmpdir")
                            val updaterFile = File(tempDir, name)
                        }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        fillMaxSize = false
                    ) {
                        Text("Update and Restart", style = theme.typography.tab, color = theme.colors.night)
                    }
                }
            }
        }
    }
}