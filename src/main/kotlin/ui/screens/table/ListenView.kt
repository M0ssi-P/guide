package ui.screens.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.LocalTheme
import kotlin.text.uppercase

@Composable
fun ListenView(info: NowPlayingInfo, playing: Boolean, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            FlameVisual(size = 280.dp, playing = playing)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            ) {
                Text(
                    info.code.uppercase(),
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        color = theme.colors.primaryBright,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Box(Modifier.size(3.dp).clip(CircleShape).background(theme.colors.border))
                Text(
                    info.location.uppercase(),
                    style = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        letterSpacing = 1.6.sp,
                        color = theme.colors.text
                    )
                )
            }

            Text(
                info.title,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FrauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 36.sp,
                    lineHeight = 41.sp,
                    color = theme.colors.night
                ),
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}
