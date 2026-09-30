package com.example.disasternet.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.data.entity.AlertEntity
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import com.example.disasternet.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(appSession: AppSession) {
    val alerts by appSession.repository.getAlertsFlow().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = InfoBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Community Safety Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
                Text(
                    text = "Anonymized alerts received from nearby responder mesh nodes (~1500m).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NavyLight,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No active nearby emergency alerts",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyMuted
                    )
                    Text(
                        text = "Community safety bulletins broadcasted within ~1500m will show here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(alerts, key = { it.id }) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: AlertEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(alert.createdAt))

    val iconColor = when (alert.severity) {
        "CRITICAL" -> EmergencyRed
        "HIGH" -> WarningAmber
        else -> InfoBlue
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NavyDark
                    )
                }
                UrgencyBadge(urgency = alert.severity)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.body,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = NavyMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = formattedTime,
                fontSize = 11.sp,
                color = NavyLight
            )
        }
    }
}
