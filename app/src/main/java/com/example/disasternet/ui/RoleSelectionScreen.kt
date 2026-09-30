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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.disasternet.model.Role
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.InfoBlueLight
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.NavyMuted
import com.example.disasternet.ui.theme.SurfaceBorder
import com.example.disasternet.ui.theme.SurfaceWhite

@Composable
fun RoleSelectionScreen(
    currentRole: Role,
    onRoleConfirmed: (Role) -> Unit
) {
    var selected by remember { mutableStateOf(currentRole) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(InfoBlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "DisasterNet",
                tint = InfoBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "EdgeAI DisasterNet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        Text(
            text = "Select your operational profile for offline coordination",
            style = MaterialTheme.typography.bodyMedium,
            color = NavyLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        RoleOptionCard(
            title = "Civilian / User",
            subtitle = "Request urgent emergency help, monitor response progress, and receive community safety bulletins.",
            icon = Icons.Default.Person,
            isSelected = selected == Role.USER,
            onSelect = { selected = Role.USER }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RoleOptionCard(
            title = "Rescue Responder",
            subtitle = "Professional emergency operations: triage queue, proximity-locked status actions, and coordination radar.",
            icon = Icons.Default.LocalHospital,
            isSelected = selected == Role.RESCUER,
            onSelect = { selected = Role.RESCUER }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRoleConfirmed(selected) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
        ) {
            Text(
                text = "Enter as ${if (selected == Role.USER) "Citizen / User" else "Rescue Responder"}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SurfaceWhite
            )
        }
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) InfoBlue else SurfaceBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val cardBg = if (isSelected) SurfaceWhite else SurfaceWhite

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) InfoBlueLight else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) InfoBlue else NavyMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = NavyMuted
                )
            }
        }
    }
}
