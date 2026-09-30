package com.example.disasternet

import android.content.Context
import android.content.SharedPreferences
import com.example.disasternet.data.AppDatabase
import com.example.disasternet.data.AppRepository
import com.example.disasternet.location.LocationHelper
import com.example.disasternet.model.Role
import com.example.disasternet.network.NearbyMeshManager
import com.example.disasternet.notifications.NotificationHelper
import com.example.disasternet.sensors.ShakeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AppSession(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("disasternet_prefs", Context.MODE_PRIVATE)

    val userId: String = prefs.getString("user_id", null) ?: run {
        val newId = "usr_${UUID.randomUUID().toString().take(6)}"
        prefs.edit().putString("user_id", newId).apply()
        newId
    }

    private val defaultName = "Citizen ${userId.takeLast(4)}"
    private val _userName = MutableStateFlow(prefs.getString("user_name", defaultName) ?: defaultName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _role = MutableStateFlow(
        Role.valueOf(prefs.getString("selected_role", Role.USER.name) ?: Role.USER.name)
    )
    val role: StateFlow<Role> = _role.asStateFlow()

    private val _shakeSosEnabled = MutableStateFlow(
        prefs.getBoolean("shake_sos_enabled", true)
    )
    val shakeSosEnabled: StateFlow<Boolean> = _shakeSosEnabled.asStateFlow()

    // Singletons
    val locationHelper = LocationHelper(context)
    val notificationHelper = NotificationHelper(context)
    val meshManager = NearbyMeshManager(context, userId, userName.value)
    val database = AppDatabase.getDatabase(context)
    val repository = AppRepository(
        context = context,
        database = database,
        meshManager = meshManager,
        locationHelper = locationHelper,
        notificationHelper = notificationHelper,
        currentUserId = userId,
        currentUserName = userName.value
    )

    var shakeDetector: ShakeDetector? = null

    fun setRole(newRole: Role) {
        _role.value = newRole
        prefs.edit().putString("selected_role", newRole.name).apply()
    }

    fun setUserName(name: String) {
        val trimmed = name.trim().ifBlank { defaultName }
        _userName.value = trimmed
        meshManager.deviceDisplayName = trimmed
        prefs.edit().putString("user_name", trimmed).apply()
    }

    fun setShakeSosEnabled(enabled: Boolean) {
        _shakeSosEnabled.value = enabled
        prefs.edit().putBoolean("shake_sos_enabled", enabled).apply()
        if (enabled) {
            shakeDetector?.startListening()
        } else {
            shakeDetector?.stopListening()
        }
    }
}
