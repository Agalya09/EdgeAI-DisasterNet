package com.example.disasternet

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.disasternet.model.EmergencyCategory
import com.example.disasternet.model.EmergencyMessage
import com.example.disasternet.model.Role
import com.example.disasternet.sensors.ShakeDetector
import com.example.disasternet.ui.AlertsScreen
import com.example.disasternet.ui.DashboardScreen
import com.example.disasternet.ui.EmergencyMessageScreen
import com.example.disasternet.ui.HardwareStateBanner
import com.example.disasternet.ui.MapScreen
import com.example.disasternet.ui.MessageHistoryScreen
import com.example.disasternet.ui.PermissionBanner
import com.example.disasternet.ui.RoleSelectionScreen
import com.example.disasternet.ui.SettingsScreen
import com.example.disasternet.ui.VictimsScreen
import com.example.disasternet.ui.theme.DisasterNetTheme
import com.example.disasternet.ui.theme.EmergencyRed
import com.example.disasternet.ui.theme.InfoBlue
import com.example.disasternet.ui.theme.NavyDark
import com.example.disasternet.ui.theme.NavyLight
import com.example.disasternet.ui.theme.SurfaceWhite

enum class ScreenTab {
    HOME,
    REQUESTS_OR_QUEUE,
    RADAR,
    ALERTS,
    PROFILE,
    SOS_FORM,
    ROLE_SELECT
}

class MainActivity : ComponentActivity() {

    private lateinit var appSession: AppSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSession = AppSession(applicationContext)

        setContent {
            DisasterNetTheme {
                MainApp(appSession = appSession)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appSession.locationHelper.startLocationUpdates()
        appSession.meshManager.startMesh()
    }

    override fun onStop() {
        super.onStop()
        appSession.locationHelper.stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        appSession.meshManager.stopMesh()
        appSession.shakeDetector?.stopListening()
    }
}

private fun isBluetoothEnabled(context: Context): Boolean {
    return try {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bm?.adapter?.isEnabled ?: true
    } catch (e: Exception) {
        true
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    return try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm?.isLocationEnabled ?: true
        } else {
            lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }
    } catch (e: Exception) {
        true
    }
}

@Composable
fun MainApp(appSession: AppSession) {
    val context = LocalContext.current
    val role by appSession.role.collectAsState()
    val shakeEnabled by appSession.shakeSosEnabled.collectAsState()

    var currentTab by remember { mutableStateOf(ScreenTab.HOME) }
    var selectedTargetEmergency by remember { mutableStateOf<EmergencyMessage?>(null) }
    var showShakeConfirmDialog by remember { mutableStateOf(false) }

    // Required Permissions Check
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    var missingPermissions by remember {
        mutableStateOf(
            requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        )
    }

    var isBtOn by remember { mutableStateOf(isBluetoothEnabled(context)) }
    var isLocOn by remember { mutableStateOf(isLocationEnabled(context)) }

    // Modern Activity Result API
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        isBtOn = isBluetoothEnabled(context)
        isLocOn = isLocationEnabled(context)
        // If granted, kick off location and mesh
        appSession.locationHelper.startLocationUpdates()
        appSession.meshManager.startMesh()
    }

    // Auto-prompt runtime permissions on initial launch
    LaunchedEffect(Unit) {
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    // Re-check hardware toggles & permissions when returning to app (ON_RESUME)
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missingPermissions = requiredPermissions.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                isBtOn = isBluetoothEnabled(context)
                isLocOn = isLocationEnabled(context)
                if (missingPermissions.isEmpty()) {
                    appSession.locationHelper.startLocationUpdates()
                    appSession.meshManager.startMesh()
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
        }
    }

    // Shake SOS sensor listener setup
    DisposableEffect(shakeEnabled) {
        if (shakeEnabled) {
            val detector = ShakeDetector(context) {
                showShakeConfirmDialog = true
            }
            appSession.shakeDetector = detector
            detector.startListening()
        }
        onDispose {
            appSession.shakeDetector?.stopListening()
        }
    }

    // Shake Confirmation Dialog (NO countdown as required)
    if (showShakeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showShakeConfirmDialog = false },
            title = {
                Text(
                    text = "Shake SOS Detected",
                    fontWeight = FontWeight.Bold,
                    color = EmergencyRed,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "A sudden 3-shake motion was registered. Would you like to transmit an urgent emergency SOS to nearby rescue teams?",
                    fontSize = 14.sp,
                    color = NavyDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showShakeConfirmDialog = false
                        val loc = appSession.locationHelper.currentLocation.value
                        appSession.repository.sendSos(
                            category = EmergencyCategory.OTHER,
                            description = "EMERGENCY: Immediate assistance triggered via Shake SOS sensor.",
                            latitude = loc?.latitude ?: 0.0,
                            longitude = loc?.longitude ?: 0.0,
                            locationLabel = appSession.locationHelper.locationLabel.value,
                            manualAddress = "",
                            victimCount = 1
                        )
                        currentTab = ScreenTab.HOME
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(text = "CONFIRM SOS", fontWeight = FontWeight.Bold, color = SurfaceWhite)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showShakeConfirmDialog = false }) {
                    Text(text = "Cancel", color = NavyDark)
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (currentTab != ScreenTab.ROLE_SELECT && currentTab != ScreenTab.SOS_FORM) {
                NavigationBar(
                    containerColor = SurfaceWhite,
                    tonalElevation = 6.dp
                ) {
                    // TAB 1: Home
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.HOME,
                        onClick = { currentTab = ScreenTab.HOME },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = InfoBlue,
                            selectedTextColor = InfoBlue,
                            indicatorColor = Color(0xFFDBEAFE)
                        )
                    )

                    // TAB 2: Requests (User) or Triage Queue (Rescuer)
                    if (role == Role.USER) {
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.REQUESTS_OR_QUEUE,
                            onClick = { currentTab = ScreenTab.REQUESTS_OR_QUEUE },
                            icon = { Icon(imageVector = Icons.Default.History, contentDescription = "Requests") },
                            label = { Text("Requests", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = InfoBlue,
                                selectedTextColor = InfoBlue,
                                indicatorColor = Color(0xFFDBEAFE)
                            )
                        )
                    } else {
                        NavigationBarItem(
                            selected = currentTab == ScreenTab.REQUESTS_OR_QUEUE,
                            onClick = { currentTab = ScreenTab.REQUESTS_OR_QUEUE },
                            icon = { Icon(imageVector = Icons.Default.LocalHospital, contentDescription = "Queue") },
                            label = { Text("Queue", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = InfoBlue,
                                selectedTextColor = InfoBlue,
                                indicatorColor = Color(0xFFDBEAFE)
                            )
                        )

                        NavigationBarItem(
                            selected = currentTab == ScreenTab.RADAR,
                            onClick = { currentTab = ScreenTab.RADAR },
                            icon = { Icon(imageVector = Icons.Default.Navigation, contentDescription = "Radar") },
                            label = { Text("Radar", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = InfoBlue,
                                selectedTextColor = InfoBlue,
                                indicatorColor = Color(0xFFDBEAFE)
                            )
                        )
                    }

                    // TAB 3: Alerts
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.ALERTS,
                        onClick = { currentTab = ScreenTab.ALERTS },
                        icon = { Icon(imageVector = Icons.Default.Campaign, contentDescription = "Alerts") },
                        label = { Text("Alerts", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = InfoBlue,
                            selectedTextColor = InfoBlue,
                            indicatorColor = Color(0xFFDBEAFE)
                        )
                    )

                    // TAB 4: Profile / Settings
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.PROFILE,
                        onClick = { currentTab = ScreenTab.PROFILE },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = InfoBlue,
                            selectedTextColor = InfoBlue,
                            indicatorColor = Color(0xFFDBEAFE)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Non-intrusive permission warning banner if permissions missing
            if (missingPermissions.isNotEmpty() && currentTab != ScreenTab.ROLE_SELECT) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    PermissionBanner(
                        missingPermissions = missingPermissions,
                        onRequest = { permissionLauncher.launch(missingPermissions.toTypedArray()) }
                    )
                }
            }

            // Non-intrusive hardware status banner if Bluetooth or Location is disabled
            if ((!isBtOn || !isLocOn) && currentTab != ScreenTab.ROLE_SELECT) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    HardwareStateBanner(
                        isBluetoothEnabled = isBtOn,
                        isLocationEnabled = isLocOn,
                        onOpenBluetoothSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        onOpenLocationSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )
                }
            }

            when (currentTab) {
                ScreenTab.HOME -> {
                    DashboardScreen(
                        appSession = appSession,
                        onNavigateToSos = { currentTab = ScreenTab.SOS_FORM },
                        onNavigateToHistory = { currentTab = ScreenTab.REQUESTS_OR_QUEUE },
                        onNavigateToAlerts = { currentTab = ScreenTab.ALERTS },
                        onNavigateToQueue = { currentTab = ScreenTab.REQUESTS_OR_QUEUE },
                        onNavigateToRadar = { currentTab = ScreenTab.RADAR },
                        onNavigateToSettings = { currentTab = ScreenTab.PROFILE }
                    )
                }
                ScreenTab.REQUESTS_OR_QUEUE -> {
                    if (role == Role.USER) {
                        MessageHistoryScreen(appSession = appSession)
                    } else {
                        VictimsScreen(
                            appSession = appSession,
                            onNavigateToRadar = { emergency ->
                                selectedTargetEmergency = emergency
                                currentTab = ScreenTab.RADAR
                            }
                        )
                    }
                }
                ScreenTab.RADAR -> {
                    MapScreen(
                        appSession = appSession,
                        targetEmergency = selectedTargetEmergency
                    )
                }
                ScreenTab.ALERTS -> {
                    AlertsScreen(appSession = appSession)
                }
                ScreenTab.PROFILE -> {
                    SettingsScreen(
                        appSession = appSession,
                        onChangeRoleClicked = { currentTab = ScreenTab.ROLE_SELECT }
                    )
                }
                ScreenTab.SOS_FORM -> {
                    EmergencyMessageScreen(
                        appSession = appSession,
                        onSosSent = {
                            currentTab = ScreenTab.HOME
                        }
                    )
                }
                ScreenTab.ROLE_SELECT -> {
                    RoleSelectionScreen(
                        currentRole = role,
                        onRoleConfirmed = { newRole ->
                            appSession.setRole(newRole)
                            currentTab = ScreenTab.HOME
                        }
                    )
                }
            }
        }
    }
}
