package com.example.disasternet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.EmergencyStatus
import com.example.disasternet.model.Role
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.EmergencyRedLight
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.InfoBlueLight
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SuccessGreen
import com.example.disasternet.ui.theme.SuccessGreenLight
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import com.example.disasternet.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    appSession: AppSession,
    onNavigateToSos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val role by appSession.role.collectAsState()
    val userName by appSession.userName.collectAsState()
    val latestUserReq by appSession.repository.getLatestUserRequestFlow().collectAsState(initial = null)
    val alerts by appSession.repository.getAlertsFlow().collectAsState(initial = emptyList())
    val rescuerQueue by appSession.repository.getRescuerQueueFlow().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Greeting & Role Header (Personalized & Clean)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, $userName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = if (role == Role.USER) "Offline Emergency Assistance" else "Rescue Coordination Console",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyLight
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (role == Role.USER) InfoBlueLight else Color(0xFFEDE9FE))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (role == Role.USER) "CITIZEN" else "RESPONDER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (role == Role.USER) InfoBlue else Color(0xFF6D28D9)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (role == Role.USER) {
            // ==========================================
            // USER DASHBOARD
            // ==========================================

            // Large SOS Action Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EmergencyRed)
                    .clickable(onClick = onNavigateToSos)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Assistance / SOS",
                            color = SurfaceWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Send an urgent emergency request to nearby rescue responders without internet.",
                            color = SurfaceWhite.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Current Request Status Card (Privacy-protected: only current user's request)
            Text(
                text = "CURRENT RESCUE STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (latestUserReq != null && latestUserReq?.status != EmergencyStatus.RESOLVED) {
                ActiveRequestTrackerCard(
                    emergency = latestUserReq!!,
                    onOpenRadar = onNavigateToRadar
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "No Active Emergency", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyDark)
                            Text(text = "Ready on standby. Emergency communications active.", fontSize = 12.sp, color = NavyLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Navigation Shortcuts
            Text(
                text = "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardShortcutCard(
                    title = "Request History",
                    subtitle = "Review past SOS dispatches",
                    icon = Icons.Default.History,
                    iconTint = InfoBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToHistory
                )

                DashboardShortcutCard(
                    title = "Community Alerts",
                    subtitle = "${alerts.size} active notices",
                    icon = Icons.Default.Campaign,
                    iconTint = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAlerts
                )
            }

        } else {
            // ==========================================
            // RESCUER DASHBOARD
            // ==========================================

            // Incident Queue Overview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
                    .clickable(onClick = onNavigateToQueue)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Emergency Triage Queue", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${rescuerQueue.size} pending emergency incidents requiring response.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = SurfaceWhite)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rescuer Shortcuts
            Text(
                text = "OPERATIONAL CONTROLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardShortcutCard(
                    title = "Radar / Map",
                    subtitle = "Schematic bearing display",
                    icon = Icons.Default.Navigation,
                    iconTint = InfoBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToRadar
                )

                DashboardShortcutCard(
                    title = "Alerts Broadcast",
                    subtitle = "Community safety notices",
                    icon = Icons.Default.Campaign,
                    iconTint = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAlerts
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rescuer location broadcasting notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Responder GPS Broadcast Active", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyDark)
                        Text(text = "Victims will see your approximate range and compass vector when responding.", fontSize = 12.sp, color = NavyLight)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun ActiveRequestTrackerCard(
    emergency: EmergencyMessage,
    onOpenRadar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category = emergency.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    UrgencyBadge(urgency = emergency.urgency)
                }
                StatusBadge(status = emergency.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step timeline
            StatusFlowIndicator(status = emergency.status)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = emergency.message,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = NavyDark
            )

            if (emergency.manualAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = emergency.manualAddress,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenRadar,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
            ) {
                Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "View Rescue Status Radar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusFlowIndicator(status: String) {
    val steps = listOf(
        EmergencyStatus.SOS_SENT,
        EmergencyStatus.REQUEST_RECEIVED,
        EmergencyStatus.RESCUE_TEAM_ASSIGNED,
        EmergencyStatus.HELP_ON_THE_WAY,
        EmergencyStatus.RESCUE_TEAM_ARRIVED,
        EmergencyStatus.RESOLVED
    )

    val currentIdx = steps.indexOf(status).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index <= currentIdx
            val dotColor = if (isCompleted) SuccessGreen else Color(0xFFCBD5E1)

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            if (index < steps.size - 1) {
                val lineColor = if (index < currentIdx) SuccessGreen else Color(0xFFE2E8F0)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = NavyLight)
        }
    }
}
