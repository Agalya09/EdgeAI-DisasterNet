package com.example.disasternet.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.location.LocationHelper
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.Role
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.InfoBlueLight
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SuccessGreen
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import com.example.disasternet.ui.theme.WarningAmber
import com.example.disasternet.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapScreen(
    appSession: AppSession,
    targetEmergency: EmergencyMessage? = null
) {
    val context = LocalContext.current
    val role by appSession.role.collectAsState()
    val myLocation by appSession.locationHelper.currentLocation.collectAsState()
    val activeTeamLoc by appSession.repository.activeTeamLocation.collectAsState()
    val latestUserReq by appSession.repository.getLatestUserRequestFlow().collectAsState(initial = null)

    // Determine target coordinates and origin
    val isUser = role == Role.USER
    val targetLat: Double
    val targetLon: Double
    val targetLabel: String
    val currentStatus: String

    if (isUser) {
        // User views rescue team relative to themselves
        targetLat = activeTeamLoc?.latitude ?: 0.0
        targetLon = activeTeamLoc?.longitude ?: 0.0
        targetLabel = activeTeamLoc?.rescuerName ?: "Rescue Team"
        currentStatus = latestUserReq?.status ?: "Standby"
    } else {
        // Rescuer views targeted emergency location
        val activeTarget = targetEmergency ?: latestUserReq
        targetLat = activeTarget?.latitude ?: 0.0
        targetLon = activeTarget?.longitude ?: 0.0
        targetLabel = activeTarget?.locationLabel ?: "Emergency Incident"
        currentStatus = activeTarget?.status ?: "Pending"
    }

    val hasCoordinates = myLocation != null && targetLat != 0.0 && targetLon != 0.0
    val distanceMeters = if (hasCoordinates) {
        LocationHelper.calculateDistance(myLocation!!.latitude, myLocation!!.longitude, targetLat, targetLon)
    } else null

    val bearing = if (hasCoordinates) {
        LocationHelper.calculateBearing(myLocation!!.latitude, myLocation!!.longitude, targetLat, targetLon)
    } else 0f

    val compassHeading = LocationHelper.bearingToCompass(bearing)

    val isStale = isUser && activeTeamLoc != null && LocationHelper.isLocationStale(activeTeamLoc!!.timestamp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rescue Status Radar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Schematic proximity and relative bearing visualization",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyLight,
                    fontSize = 12.sp
                )
            }
            StatusBadge(status = currentStatus)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stale-location warning for victim
        if (isStale) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarningAmberLight)
                    .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rescue team signal is older than 2 minutes. Waiting for refreshed Nearby transmission.",
                        fontSize = 12.sp,
                        color = NavyDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Radar Canvas Display
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(2.dp, Color(0xFF334155), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2f - 20f

                // Concentric range rings
                val rings = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                rings.forEach { ratio ->
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = maxRadius * ratio,
                        center = centerOffset,
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }

                // Crosshairs
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(centerOffset.x, 20f),
                    end = Offset(centerOffset.x, size.height - 20f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(20f, centerOffset.y),
                    end = Offset(size.width - 20f, centerOffset.y),
                    strokeWidth = 1f
                )

                // Center Node (Me)
                drawCircle(
                    color = InfoBlue,
                    radius = 8f,
                    center = centerOffset
                )

                // Target Node (Schematic relative position)
                if (hasCoordinates) {
                    val angleRad = Math.toRadians(bearing.toDouble() - 90.0) // 0 deg is North (upwards)
                    // Clamp visual distance into radar scale
                    val visualDistanceFraction = ((distanceMeters ?: 500.0) / 1200.0).coerceIn(0.2, 0.95)
                    val targetX = centerOffset.x + (maxRadius * visualDistanceFraction * cos(angleRad)).toFloat()
                    val targetY = centerOffset.y + (maxRadius * visualDistanceFraction * sin(angleRad)).toFloat()

                    // Vector line from origin to target
                    drawLine(
                        color = if (isUser) SuccessGreen else EmergencyRed,
                        start = centerOffset,
                        end = Offset(targetX, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Target Blip
                    drawCircle(
                        color = if (isUser) SuccessGreen else EmergencyRed,
                        radius = 11f,
                        center = Offset(targetX, targetY)
                    )
                }
            }

            // Cardinal direction labels
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "N", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "W", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "E", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "S", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Distance & Bearing Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "APPROX DISTANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (distanceMeters != null) {
                            if (distanceMeters >= 1000) String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
                            else "${distanceMeters.toInt()} m"
                        } else "Acquiring...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (distanceMeters != null && distanceMeters <= 120) SuccessGreen else NavyDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "COMPASS BEARING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$compassHeading (${bearing.toInt()}°)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InfoBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target Metadata Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) "Assigned Responder" else "Incident Location",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyLight
                )
                Text(
                    text = targetLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyDark
                )
                if (isUser && activeTeamLoc != null) {
                    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(activeTeamLoc!!.timestamp))
                    Text(
                        text = "Last beacon received: $timeFmt",
                        fontSize = 11.sp,
                        color = NavyLight,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // External Navigation Fallback (Google Maps intent if user chooses)
        Button(
            onClick = {
                if (targetLat != 0.0 && targetLon != 0.0) {
                    val uri = Uri.parse("geo:$targetLat,$targetLon?q=$targetLat,$targetLon($targetLabel)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {}
                }
            },
            enabled = targetLat != 0.0 && targetLon != 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
        ) {
            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Open in External Maps App", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Notice: Radar displays relative offline vector bearing. Turn-by-turn road navigation is not simulated offline.",
            fontSize = 11.sp,
            color = NavyLight,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
