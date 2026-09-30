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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.model.EmergencyCategory
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.EmergencyRedDark
import com.example.disasternet.ui.theme.EmergencyRedLight
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite

@Composable
fun EmergencyMessageScreen(
    appSession: AppSession,
    onSosSent: (String) -> Unit
) {
    val userName by appSession.userName.collectAsState()
    var selectedCategory by remember { mutableStateOf(EmergencyCategory.MEDICAL) }
    var description by remember { mutableStateOf("") }
    var manualAddress by remember { mutableStateOf("") }
    var victimCount by remember { mutableStateOf(1) }
    var isSubmitting by remember { mutableStateOf(false) }

    val currentLocation by appSession.locationHelper.currentLocation.collectAsState()
    val locationLabel by appSession.locationHelper.locationLabel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // User Identity Header (Personalized & Non-Technical)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = InfoBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Requesting as: $userName",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = NavyDark
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Emergency Request",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        Text(
            text = "Choose a category and send an immediate alert to nearby rescue responders.",
            style = MaterialTheme.typography.bodyMedium,
            color = NavyLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Category selection cards (Medical, Trapped, Fire, Other)
        Text(
            text = "EMERGENCY CATEGORY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NavyMuted
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategoryOptionItem(
                label = "Medical",
                icon = Icons.Default.MedicalServices,
                selected = selectedCategory == EmergencyCategory.MEDICAL,
                modifier = Modifier.weight(1f),
                onClick = { selectedCategory = EmergencyCategory.MEDICAL }
            )
            CategoryOptionItem(
                label = "Trapped",
                icon = Icons.Default.Warning,
                selected = selectedCategory == EmergencyCategory.TRAPPED,
                modifier = Modifier.weight(1f),
                onClick = { selectedCategory = EmergencyCategory.TRAPPED }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategoryOptionItem(
                label = "Fire",
                icon = Icons.Default.Fireplace,
                selected = selectedCategory == EmergencyCategory.FIRE,
                modifier = Modifier.weight(1f),
                onClick = { selectedCategory = EmergencyCategory.FIRE }
            )
            CategoryOptionItem(
                label = "Other",
                icon = Icons.Default.HelpOutline,
                selected = selectedCategory == EmergencyCategory.OTHER,
                modifier = Modifier.weight(1f),
                onClick = { selectedCategory = EmergencyCategory.OTHER }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Description Input
        Text(
            text = "EMERGENCY DETAILS (OPTIONAL)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NavyMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Describe injuries, current danger, or what help you need...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = InfoBlue,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // OPTIONAL HOUSE / LOCATION ADDRESS (Requirement 2)
        Text(
            text = "ADDITIONAL ADDRESS (OPTIONAL)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NavyMuted
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = manualAddress,
            onValueChange = { manualAddress = it },
            placeholder = { Text("Enter your house address or nearby landmark") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 2,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = InfoBlue,
                unfocusedBorderColor = SurfaceBorder
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Persons requiring aid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Persons Needing Help", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyDark)
                Text(text = "Estimated count of victims", fontSize = 12.sp, color = NavyLight)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { if (victimCount > 1) victimCount-- },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyDark)
                }
                Text(
                    text = "$victimCount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyDark,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { if (victimCount < 50) victimCount++ },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automatic GPS location card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = InfoBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current GPS Location",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyDark
                    )
                    Text(
                        text = locationLabel,
                        fontSize = 12.sp,
                        color = NavyMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SEND SOS BUTTON
        Button(
            onClick = {
                if (isSubmitting) return@Button
                isSubmitting = true
                val lat = currentLocation?.latitude ?: 0.0
                val lon = currentLocation?.longitude ?: 0.0
                val sosId = appSession.repository.sendSos(
                    category = selectedCategory,
                    description = description,
                    latitude = lat,
                    longitude = lon,
                    locationLabel = locationLabel,
                    manualAddress = manualAddress.trim(),
                    victimCount = victimCount
                )
                isSubmitting = false
                onSosSent(sosId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = SurfaceWhite, modifier = Modifier.size(24.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SurfaceWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEND EMERGENCY SOS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurfaceWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CategoryOptionItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) EmergencyRed else SurfaceBorder
    val bg = if (selected) EmergencyRedLight else SurfaceWhite
    val contentColor = if (selected) EmergencyRedDark else NavyDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}
