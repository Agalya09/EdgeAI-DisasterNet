package com.example.disasternet.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.disasternet.MainActivity
import com.example.disasternet.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Disaster Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for emergency requests and status shifts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS,
                "Rescue Coordination Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Routine status and location tracking updates"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(emergencyChannel)
            manager.createNotificationChannel(statusChannel)
        }
    }

    private fun canPostNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showSosSentNotification(category: String) {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SOS Sent")
            .setContentText("Emergency ($category) broadcasted to nearby rescue mesh nodes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_SOS, notification)
        } catch (_: SecurityException) {}
    }

    fun showRequestReceivedNotification() {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Request Received")
            .setContentText("A nearby responder node acknowledged your emergency request.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (_: SecurityException) {}
    }

    fun showRescueTeamAssignedNotification(teamName: String = "Rescue Unit") {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Rescue Team Assigned")
            .setContentText("$teamName has accepted your emergency request.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (_: SecurityException) {}
    }

    fun showHelpOnTheWayNotification() {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Help On The Way")
            .setContentText("Emergency responders are en route to your recorded coordinates.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (_: SecurityException) {}
    }

    fun showRescueTeamArrivedNotification() {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Rescue Team Arrived")
            .setContentText("Responders are at your location (within 120m). Stay visible.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (_: SecurityException) {}
    }

    fun showResolvedNotification() {
        if (!canPostNotification()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("Rescue Operation Resolved")
            .setContentText("Your emergency incident has been marked resolved.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
        } catch (_: SecurityException) {}
    }

    fun showEmergencyNearbyNotification(distanceMeters: Double) {
        if (!canPostNotification()) return
        val distText = if (distanceMeters >= 1000) {
            String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0)
        } else {
            "${distanceMeters.toInt()} m"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Emergency Nearby")
            .setContentText("An emergency incident was reported ~$distText from your area.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getPendingIntent())
            .setAutoCancel(true)
            .build()
        try {
            notificationManager.notify(NOTIFICATION_ID_COMMUNITY, notification)
        } catch (_: SecurityException) {}
    }

    companion object {
        const val CHANNEL_EMERGENCY = "disaster_emergency_channel"
        const val CHANNEL_STATUS = "disaster_status_channel"

        const val NOTIFICATION_ID_SOS = 1001
        const val NOTIFICATION_ID_STATUS = 1002
        const val NOTIFICATION_ID_COMMUNITY = 1003
    }
}
