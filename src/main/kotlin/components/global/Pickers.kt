package components.global

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.arrowDown
import org.jetbrains.jewel.ui.component.Icon
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun PickerPill(label: String, open: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current

    val borderColor = if (open) theme.colors.primary else theme.colors.border
    val textColor = if (open) theme.colors.primary else theme.colors.text

    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f
    )

    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(theme.colors.popup)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = textColor)
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = arrowDown(theme.colors.deeming),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp).rotate(rotation)
            )
        }
    }
}