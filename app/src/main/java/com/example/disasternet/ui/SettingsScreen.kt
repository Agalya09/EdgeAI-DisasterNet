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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.disasternet.AppSession
import com.example.disasternet.ai.EdgeAiEngine
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
fun SettingsScreen(
    appSession: AppSession,
    onChangeRoleClicked: () -> Unit
) {
    val role by appSession.role.collectAsState()
    val shakeSosEnabled by appSession.shakeSosEnabled.collectAsState()
    val userName by appSession.userName.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(userName) }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Your Name", fontWeight = FontWeight.Bold, color = NavyDark) },
            text = {
                Column {
                    Text("Enter your name so rescue responders can address you properly:", fontSize = 13.sp, color = NavyMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        appSession.setUserName(nameInput)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
                ) {
                    Text("Save", color = SurfaceWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = NavyDark)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )
        Text(
            text = if (role == Role.USER) "Manage your profile, emergency triggers, and role settings."
            else "Operational parameters, responder configuration, and diagnostics.",
            style = MaterialTheme.typography.bodyMedium,
            color = NavyLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Profile Card
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
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(InfoBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                        IconButton(
                            onClick = {
                                nameInput = userName
                                showNameDialog = true
                            },
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name", tint = InfoBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "Profile Role: ${if (role == Role.USER) "Citizen / User" else "Rescue Responder"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InfoBlue
                    )
                }
                Button(
                    onClick = onChangeRoleClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Switch Role", color = NavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Shake SOS Setting
        Text(
            text = "EMERGENCY SENSORS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NavyMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Shake for SOS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyDark)
                        Text(
                            text = "3 strong shakes trigger an immediate SOS confirmation dialog (no countdown).",
                            fontSize = 12.sp,
                            color = NavyLight
                        )
                    }
                }
                Switch(
                    checked = shakeSosEnabled,
                    onCheckedChange = { appSession.setShakeSosEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = SurfaceWhite, checkedTrackColor = EmergencyRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (role == Role.USER) {
            // USER VIEW: About Emergency System (Clean, simple, non-technical)
            Text(
                text = "ABOUT OFFLINE EMERGENCY ASSISTANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Direct Device-to-Device Communication", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DisasterNet uses direct peer-to-peer radio (Bluetooth and Wi-Fi) to transmit your emergency requests directly to nearby responders, even when cell towers, power grids, or internet connections are completely down.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = NavyMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Keep Bluetooth and Location services turned ON.\n• Keep your phone within range of other devices or responders.\n• Stay in a safe position until responders arrive.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = NavyLight
                    )
                }
            }

        } else {
            // RESPONDER VIEW: Detailed Diagnostic & Operational Architecture
            Text(
                text = "OPERATIONAL DIAGNOSTICS (RESPONDER MODE)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Classifier: ${EdgeAiEngine.MODEL_VERSION}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "On-device multinomial softmax emergency classifier. Evaluated offline on synthetic disaster benchmark data.",
                        fontSize = 12.sp,
                        color = NavyMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${EdgeAiEngine.BENCHMARK_ACCURACY_PERCENT}%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InfoBlue)
                                Text(text = "Synthetic Accuracy", fontSize = 10.sp, color = NavyLight)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${EdgeAiEngine.BENCHMARK_F1_PERCENT}%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InfoBlue)
                                Text(text = "Weighted F1-Score", fontSize = 10.sp, color = NavyLight)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "500 / 5", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                                Text(text = "Samples / Classes", fontSize = 10.sp, color = NavyLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = EdgeAiEngine.BENCHMARK_DISCLAIMER,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFF92400E)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Node ID: ${appSession.userId}\n• Service ID: com.example.disasternet.mesh.v3\n• Strategy: P2P_CLUSTER (Max 4 hops)\n• Routing: 0.50 Urgency + 0.30 Hop Efficiency + 0.20 Freshness",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = NavyLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
