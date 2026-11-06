package com.example.automatesmartbot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.model.DashboardStatusResponse

/** A badge is a *current account entitlement*, not an illustration or an upsell label. */
enum class MembershipTier { PRO, FREE, UNVERIFIED }
enum class MembershipBadgeStyle { HEADER, LIGHT, SOLID }

/** Offline, missing session, or missing membership claim must never be presented as verified FREE. */
fun membershipTier(status: DashboardStatusResponse?): MembershipTier = when {
    status?.membershipVerified != true -> MembershipTier.UNVERIFIED
    status.isPro -> MembershipTier.PRO
    else -> MembershipTier.FREE
}

val LocalMembershipTier = compositionLocalOf { MembershipTier.UNVERIFIED }

@Composable
fun MembershipBadge(
    modifier: Modifier = Modifier,
    style: MembershipBadgeStyle = MembershipBadgeStyle.HEADER
) {
    val tier = LocalMembershipTier.current
    val darkHeader = style == MembershipBadgeStyle.HEADER
    val ink = when {
        darkHeader -> Color(0xFFFFD56B)
        tier == MembershipTier.PRO -> Color(0xFF9D670A)
        tier == MembershipTier.FREE -> Color(0xFF145B97)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backdrop = when (style) {
        MembershipBadgeStyle.HEADER -> Color(0xFF075088).copy(alpha = .69f)
        MembershipBadgeStyle.LIGHT -> when (tier) {
            MembershipTier.PRO -> Color(0xFFFFF2DC)
            MembershipTier.FREE -> Color(0xFFE6F3FF)
            MembershipTier.UNVERIFIED -> MaterialTheme.colorScheme.surfaceVariant
        }
        MembershipBadgeStyle.SOLID -> when (tier) {
            MembershipTier.PRO -> Color(0xFF0B65B4)
            MembershipTier.FREE -> Color(0xFFDBEDFB)
            MembershipTier.UNVERIFIED -> MaterialTheme.colorScheme.surfaceVariant
        }
    }
    val foreground = if (style == MembershipBadgeStyle.SOLID && tier == MembershipTier.PRO) Color.White else ink
    val label = when (tier) {
        MembershipTier.PRO -> "PRO"
        MembershipTier.FREE -> "FREE"
        MembershipTier.UNVERIFIED -> "CEK"
    }
    val icon = when (tier) {
        MembershipTier.PRO -> Icons.Default.WorkspacePremium
        MembershipTier.FREE -> Icons.Default.PersonOutline
        MembershipTier.UNVERIFIED -> Icons.Default.Refresh
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backdrop,
        border = BorderStroke(1.dp, if (darkHeader) Color(0xFFFFD05E) else foreground.copy(alpha = .43f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = foreground)
            Text(
                text = label,
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1
            )
        }
    }
}
