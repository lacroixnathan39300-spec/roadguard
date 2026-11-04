package fr.roadguard.drive.ui

import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.drive.link.PhoneNotification

/** Message ou appel du téléphone, avec ses boutons (répondre, refuser…) et « Fermer ». */
@Composable
fun PhoneNotificationCard(
    notification: PhoneNotification,
    onAction: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (notification.isCall) Color(0xFF2E7D32) else Color(0xFF1565C0)
    Column(
        modifier.width(420.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xF0141B24)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            (if (notification.isCall) "📞 Appel · " else "💬 ") + notification.app,
            color = accent.copy(alpha = 1f), fontSize = 15.sp, fontWeight = FontWeight.Bold,
        )
        Text(notification.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        if (notification.text.isNotBlank()) {
            Text(notification.text, color = DriveColors.TextSecondary, fontSize = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            notification.actions.forEachIndexed { i, label ->
                if (label.isNotBlank()) {
                    val color = when {
                        Regex("(?i)refus|raccroch|decline|hang").containsMatchIn(label) -> Color(0xFFC62828)
                        Regex("(?i)r[ée]pondre|answer|accept").containsMatchIn(label) -> Color(0xFF2E7D32)
                        else -> DriveColors.Panel
                    }
                    BigButton(label, color) { onAction(i) }
                }
            }
            BigButton("Fermer", DriveColors.Track, onDismiss)
        }
    }
}
