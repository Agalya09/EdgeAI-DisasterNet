package com.example.disasternet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.model.EmergencyCategory
import com.example.disasternet.model.EmergencyStatus
import com.example.disasternet.model.UrgencyLevel
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.EmergencyRedLight
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.InfoBlueLight
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.NeutralGray
import com.example.disasternet.ui.theme.SuccessGreen
import com.example.disasternet.ui.theme.SuccessGreenLight
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import com.example.disasternet.ui.theme.WarningAmber
import com.example.disasternet.ui.theme.WarningAmberLight

@Composable
fun StatusBadge(status: String) {
    val (bg, fg, border) = when (status) {
        EmergencyStatus.SOS_SENT -> Triple(EmergencyRedLight, EmergencyRed, EmergencyRed.copy(alpha = 0.4f))
        EmergencyStatus.REQUEST_RECEIVED -> Triple(InfoBlueLight, InfoBlue, InfoBlue.copy(alpha = 0.4f))
        EmergencyStatus.RESCUE_TEAM_ASSIGNED -> Triple(WarningAmberLight, WarningAmber, WarningAmber.copy(alpha = 0.4f))
        EmergencyStatus.HELP_ON_THE_WAY -> Triple(WarningAmberLight, WarningAmber, WarningAmber.copy(alpha = 0.4f))
        EmergencyStatus.RESCUE_TEAM_ARRIVED -> Triple(SuccessGreenLight, SuccessGreen, SuccessGreen.copy(alpha = 0.4f))
        EmergencyStatus.RESOLVED -> Triple(SuccessGreenLight, SuccessGreen, SuccessGreen.copy(alpha = 0.4f))
        else -> Triple(Color(0xFFF1F5F9), NavyMuted, NeutralGray.copy(alpha = 0.3f))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UrgencyBadge(urgency: String) {
    val (bg, fg) = when (urgency) {
        UrgencyLevel.CRITICAL -> Pair(EmergencyRed, SurfaceWhite)
        UrgencyLevel.HIGH -> Pair(WarningAmber, SurfaceWhite)
        UrgencyLevel.MEDIUM -> Pair(InfoBlue, SurfaceWhite)
        else -> Pair(NeutralGray, SurfaceWhite)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = urgency,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bg, fg) = when (category) {
        EmergencyCategory.MEDICAL -> Pair(EmergencyRedLight, EmergencyRed)
        EmergencyCategory.TRAPPED -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
        EmergencyCategory.FIRE -> Pair(EmergencyRedLight, EmergencyRed)
        EmergencyCategory.SAFETY -> Pair(WarningAmberLight, WarningAmber)
        else -> Pair(InfoBlueLight, InfoBlue)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NavyLight,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PermissionBanner(
    missingPermissions: List<String>,
    onRequest: () -> Unit
) {
    if (missingPermissions.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningAmberLight)
            .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = WarningAmber,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hardware Permissions Needed",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NavyDark
                )
                Text(
                    text = "Bluetooth and GPS are required for offline nearby relay.",
                    fontSize = 12.sp,
                    color = NavyMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "Grant", fontSize = 12.sp, color = SurfaceWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun HardwareStateBanner(
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onOpenBluetoothSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    if (isBluetoothEnabled && isLocationEnabled) return

    val message = when {
        !isBluetoothEnabled && !isLocationEnabled -> "Bluetooth and Location (GPS) are turned off."
        !isBluetoothEnabled -> "Bluetooth is turned off."
        else -> "Location (GPS) is turned off."
    }

    val actionText = when {
        !isBluetoothEnabled -> "Enable Bluetooth"
        else -> "Enable Location"
    }

    val onAction = when {
        !isBluetoothEnabled -> onOpenBluetoothSettings
        else -> onOpenLocationSettings
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.dp, EmergencyRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = EmergencyRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hardware Service Required",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NavyDark
                )
                Text(
                    text = "$message Direct offline communication requires both services active.",
                    fontSize = 12.sp,
                    color = NavyMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = actionText, fontSize = 12.sp, color = SurfaceWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}
