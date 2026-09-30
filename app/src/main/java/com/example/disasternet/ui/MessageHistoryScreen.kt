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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageHistoryScreen(appSession: AppSession) {
    // Privacy-preserving query: Only requests initiated by current device user
    val myRequests by appSession.repository.getUserRequestsFlow().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Text(
            text = "My Emergency Requests",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        Text(
            text = "History and live dispatch status of your emergency requests.",
            style = MaterialTheme.typography.bodyMedium,
            color = NavyLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        if (myRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No emergency requests filed",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyMuted
                    )
                    Text(
                        text = "Your submitted emergency requests will appear here with live rescue status.",
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
                items(myRequests, key = { it.id }) { request ->
                    UserRequestCard(request = request)
                }
            }
        }
    }
}

@Composable
fun UserRequestCard(request: EmergencyMessage) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(request.createdAt))

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
                    CategoryBadge(category = request.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    UrgencyBadge(urgency = request.urgency)
                }
                StatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = request.message,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = NavyDark
            )

            if (request.manualAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.manualAddress,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = NavyLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = request.locationLabel,
                        fontSize = 12.sp,
                        color = NavyLight,
                        maxLines = 1
                    )
                }
                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = NavyLight
                )
            }
        }
    }
}
