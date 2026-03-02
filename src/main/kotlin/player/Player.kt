package player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrow2left
import com.composables.arrow2right
import com.composables.closeIcon
import com.composables.maximize
import com.composables.nextIcon
import com.composables.play2Icon
import com.composables.prevIcon
import com.composables.soundIcon
import components.global.AsyncImageFromFile
import components.global.TooltipIconButton
import org.jetbrains.jewel.ui.component.Slider
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme
import java.io.File

@Composable
fun PlayerUI() {
    val theme = LocalTheme.current

    BoxWithConstraints(
        modifier = Modifier
            .dropShadow(
                shape = RoundedCornerShape(0.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .background(theme.colors.surface)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 1920.dp)
                .fillMaxWidth()
                .padding(horizontal = 30.dp, vertical = 10.dp),

            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(60.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.colors.popup)
                ) {
                    AsyncImageFromFile("src/bible.jpg")
                }
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                ) {
                    Text(text = "Untitled", style = theme.typography.h4, fontSize = 18.sp)
                    Text(
                        text = "Unknown",
                        style = theme.typography.h4,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(4f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TooltipIconButton(
                        icon = ::prevIcon,
                        tooltip = "Previous chapter",
                    )
                    TooltipIconButton(
                        icon = ::arrow2left,
                        tooltip = "Previous verse",
                    )
                    TooltipIconButton(
                        icon = ::play2Icon,
                        tooltip = "Play",
                    )
                    TooltipIconButton(
                        icon = ::arrow2right,
                        tooltip = "Next verse",
                    )
                    TooltipIconButton(
                        icon = ::nextIcon,
                        tooltip = "Next chapter",
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {

                    Text("1:3", color = Color.Gray, fontSize = 12.sp)

                    Spacer(Modifier.width(8.dp))

                    Slider(
                        value = 0.5f,
                        onValueChange = { },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("1:2", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.spacedBy(30.dp, alignment = Alignment.End),
            ) {
                TooltipIconButton(
                    icon = ::soundIcon,
                    tooltip = "Volume",
                )
                TooltipIconButton(
                    icon = ::maximize,
                    tooltip = "Fullscreen",
                )
                TooltipIconButton(
                    icon = ::closeIcon,
                    tooltip = "Close",
                )
            }
        }
    }
}