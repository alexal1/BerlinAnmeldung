package com.madewithlove.anmeldung

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PushManager private constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        // Create channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                context.getString(R.string.push_background_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).also {
                it.description = context.getString(R.string.push_background_channel_description)
            })

            notificationManager.createNotificationChannel(NotificationChannel(
                TIMETABLE_CHANNEL_ID,
                context.getString(R.string.push_timetable_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).also {
                it.description = context.getString(R.string.push_timetable_channel_description)
                it.vibrationPattern = vibrationPattern
                it.enableVibration(true)
                it.enableLights(true)
            })
        }
    }

    fun createForegroundServiceNotification(): Notification {
        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun sendSuccessNotification(webAddress: String) {
        val notification = NotificationCompat.Builder(context, TIMETABLE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.push_timetable_success_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.push_timetable_success_description)))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(createBrowserIntent(webAddress))
            .setAutoCancel(true)
            .setVibrate(vibrationPattern)
            .build()
        notificationManager.notify(TIMETABLE_NOTIFICATION_ID, notification)
    }

    fun sendFailNotification(webAddress: String) {
        val notification = NotificationCompat.Builder(context, TIMETABLE_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.push_timetable_fail_title))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.push_timetable_fail_description)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(createFailIntent(webAddress))
            .setAutoCancel(true)
            .build()
        notificationManager.notify(TIMETABLE_NOTIFICATION_ID, notification)
    }

    private fun createBrowserIntent(webAddress: String): PendingIntent {
        val intent = Intent(ACTION_VIEW)
        intent.data = Uri.parse(webAddress)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, REQUEST_CODE_SUCCESS, intent, flag)
    }

    private fun createFailIntent(webAddress: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(MainActivity.INTENT_KEY_CHECK_FAILED, true)
        intent.putExtra(MainActivity.INTENT_KEY_WEB_ADDRESS, webAddress)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, REQUEST_CODE_FAIL, intent, flag)
    }

    companion object {
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
        const val TIMETABLE_NOTIFICATION_ID = 2

        private const val FOREGROUND_SERVICE_CHANNEL_ID = "background_work_notification"
        private const val TIMETABLE_CHANNEL_ID = "timetable_notifications"
        private const val REQUEST_CODE_SUCCESS = 1
        private const val REQUEST_CODE_FAIL = 2

        private val vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)

        @SuppressLint("StaticFieldLeak")
        lateinit var INSTANCE: PushManager private set

        fun createInstance(application: Application) {
            INSTANCE = PushManager(application)
        }
    }
}