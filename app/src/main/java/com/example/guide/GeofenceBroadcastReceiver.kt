package com.example.guide

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "Geofence_Channel"
    }

    private var notificationIdCounter = 1
    private val notificationMap = mutableMapOf<String, Int>()

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            return
        }

        val geofences = geofencingEvent?.triggeringGeofences

        if (geofences.isNullOrEmpty()) {
            return
        }

        for (geofence in geofences) {
            val geofenceData = getGeofenceDataFromId(context, geofence.requestId)

            val notificationId = notificationMap[geofence.requestId]
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                if (notificationId == null) {
                    val title = "You are close to ${geofenceData?.name}"
                    val text = "Tap to learn more"
                    val newNotificationId = notificationIdCounter++
                    showNotification(context, title, text, newNotificationId, geofenceData)
                    notificationMap[geofence.requestId] = newNotificationId
                }
            } else if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                val existingNotificationId = notificationMap[geofence.requestId]
                if (existingNotificationId != null) {
                    cancelNotification(context, existingNotificationId)
                    notificationMap.remove(geofence.requestId)
                }
            }
        }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    private fun getGeofenceDataFromId(context: Context, geofenceId: String): MainActivity.GeofenceData? {
        // Read data from the spreadsheet (latitude, longitude, and radius values)
        val spreadsheetData = MainActivity().readFromSpreadsheet(context)

        // Find the GeofenceData object with a matching geofenceId
        for (geofenceData in spreadsheetData) {
            if (MainActivity.GEOFENCE_ID + geofenceData.name == geofenceId) {
                return geofenceData
            }
        }

        return null
    }

    private fun shouldSendNotification(context: Context, geofenceId: String): Boolean {
        val preferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        return !preferences.getBoolean(geofenceId, false)
    }

    private fun markNotificationSent(context: Context, geofenceId: String) {
        val preferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean(geofenceId, true).apply()
    }


    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        title: String,
        text: String,
        notificationId: Int,
        geofenceData: MainActivity.GeofenceData?
    ) {        // Create a notification channel (for Android 8.0 and above)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Geofence Channel",
            NotificationManager.IMPORTANCE_HIGH // Set the importance level to HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Create an intent that will always start a new instance of DescriptionActivity
        val intent = Intent(context, DescriptionActivity::class.java)
        intent.action = "ACTION_SHOW_DESCRIPTION" // Use a custom action to distinguish intents
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(DescriptionActivity.EXTRA_DESCRIPTION, geofenceData?.description)
        intent.putExtra(DescriptionActivity.EXTRA_LOCATION_NAME, geofenceData?.name)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true) // Make the notification persistent

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }


}