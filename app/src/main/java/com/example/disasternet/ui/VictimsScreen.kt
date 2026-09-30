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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.location.LocationHelper
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.EmergencyStatus
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.EmergencyRedDark
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.InfoBlueLight
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SuccessGreen
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import com.example.disasternet.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VictimsScreen(
    appSession: AppSession,
    onNavigateToRadar: (EmergencyMessage) -> Unit
) {
    val emergencyQueue by appSession.repository.getRescuerQueueFlow().collectAsState(initial = emptyList())
    val rescuerLocation by appSession.locationHelper.currentLocation.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Emergency Triage Queue",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Active field emergencies prioritized for rescue dispatch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyLight,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(InfoBlueLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${emergencyQueue.size} Incidents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = InfoBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (emergencyQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All Dispatches Clear",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyMuted
                    )
                    Text(
                        text = "No pending emergency requests detected on the local mesh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(emergencyQueue, key = { it.id }) { item ->
                    val distanceMeters = if (rescuerLocation != null && item.latitude != 0.0 && item.longitude != 0.0) {
                        LocationHelper.calculateDistance(
                            rescuerLocation!!.latitude,
                            rescuerLocation!!.longitude,
                            item.latitude,
                            item.longitude
                        )
                    } else null

                    RescuerEmergencyCard(
                        emergency = item,
                        distanceMeters = distanceMeters,
                        onStatusChange = { newStatus ->
                            appSession.repository.updateEmergencyStatus(item.id, newStatus, rescuerLocation)
                        },
                        onBroadcastLocation = {
                            if (rescuerLocation != null) {
                                appSession.repository.broadcastResponderLocation(
                                    rescuerLocation!!.latitude,
                                    rescuerLocation!!.longitude,
                                    appSession.userName.value
                                )
                            }
                        },
                        onOpenRadar = { onNavigateToRadar(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun RescuerEmergencyCard(
    emergency: EmergencyMessage,
    distanceMeters: Double?,
    onStatusChange: (String) -> Unit,
    onBroadcastLocation: () -> Unit,
    onOpenRadar: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = dateFormat.format(Date(emergency.createdAt))

    val isWithin120m = distanceMeters != null && distanceMeters <= 120.0
    val canMarkArrived = isWithin120m && emergency.status != EmergencyStatus.RESCUE_TEAM_ARRIVED && emergency.status != EmergencyStatus.RESOLVED
    val canMarkResolved = isWithin120m && emergency.status == EmergencyStatus.RESCUE_TEAM_ARRIVED

    val distStr = if (distanceMeters != null) {
        if (distanceMeters >= 1000) String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        else "${distanceMeters.toInt()} m"
    } else "GPS Unknown"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header Row: Category, Urgency, Priority Score, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category = emergency.category)
                    Spacer(modifier = Modifier.width(6.dp))
                    UrgencyBadge(urgency = emergency.urgency)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEDE9FE))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Score: ${emergency.priorityScore}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6D28D9)
                        )
                    }
                }
                StatusBadge(status = emergency.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Victim / Caller Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Victim: ${emergency.senderName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "($timeStr)",
                    fontSize = 12.sp,
                    color = NavyLight
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message text
            Text(
                text = emergency.message,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = NavyDark
            )

            // Optional Manual House / Location Address (Requirement 2 & 6)
            if (emergency.manualAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(text = "Specific Address / Landmark:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        Text(text = emergency.manualAddress, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavyDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Confidence & Persons Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AI Confidence: ${emergency.aiConfidence}% (DN-Softmax-1.0)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyLight
                )
                Text(
                    text = "Persons Needing Aid: ${emergency.victimCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location label & live distance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = InfoBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = emergency.locationLabel,
                        fontSize = 12.sp,
                        color = NavyDark,
                        maxLines = 1
                    )
                }
                Text(
                    text = "Distance: $distStr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWithin120m) SuccessGreen else InfoBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Responder Actions:
            // ACCEPT REQUEST -> HELP ON THE WAY -> TEAM NEARBY -> MORE SUPPORT -> UNABLE TO REACH -> MARK ARRIVED -> MARK RESOLVED
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (emergency.status == EmergencyStatus.SOS_SENT || emergency.status == EmergencyStatus.REQUEST_RECEIVED) {
                        Button(
                            onClick = { onStatusChange(EmergencyStatus.RESCUE_TEAM_ASSIGNED) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
                        ) {
                            Text(text = "ACCEPT REQUEST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (emergency.status == EmergencyStatus.RESCUE_TEAM_ASSIGNED) {
                        Button(
                            onClick = { onStatusChange(EmergencyStatus.HELP_ON_THE_WAY) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                        ) {
                            Text(text = "HELP ON THE WAY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Radar navigation button
                    OutlinedButton(
                        onClick = onOpenRadar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "RADAR / MAP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // TEAM NEARBY action (broadcasts live coordinates to victim)
                    OutlinedButton(
                        onClick = onBroadcastLocation,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "TEAM NEARBY", fontSize = 11.sp)
                    }

                    // UNABLE TO REACH
                    OutlinedButton(
                        onClick = { onStatusChange(EmergencyStatus.UNABLE_TO_REACH) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "UNABLE TO REACH", fontSize = 10.sp, color = EmergencyRed)
                    }
                }

                // Proximity-Locked Actions (120m check enforced)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // MARK ARRIVED (only when responder is approx <= 120m)
                    Button(
                        onClick = { onStatusChange(EmergencyStatus.RESCUE_TEAM_ARRIVED) },
                        enabled = canMarkArrived,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            disabledContainerColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        if (!isWithin120m) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyLight)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isWithin120m) "MARK ARRIVED" else "ARRIVED (<=120m)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canMarkArrived) SurfaceWhite else NavyLight
                        )
                    }

                    // MARK RESOLVED (only when status is RESCUE TEAM ARRIVED AND within 120m)
                    Button(
                        onClick = { onStatusChange(EmergencyStatus.RESOLVED) },
                        enabled = canMarkResolved,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            disabledContainerColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        if (!canMarkResolved) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyLight)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (canMarkResolved) "MARK RESOLVED" else "RESOLVED (Lock)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canMarkResolved) SurfaceWhite else NavyLight
                        )
                    }
                }
            }
        }
    }
}
