package com.madewithlove.anmeldung

import android.app.*
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*

class TimetableCheckService : Service() {

    private val pushManager by lazy { PushManager.INSTANCE }
    private val networkManager by lazy { NetworkManager.INSTANCE }
    private val serviceScope = CoroutineScope(Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = pushManager.createForegroundServiceNotification()
        startForeground(PushManager.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch(CoroutineName("Timetable checks from TimetableCheckService")) {
            networkManager.startTimetableChecks()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        networkManager.stopTimetableChecks()
        serviceScope.cancel()
    }
}
