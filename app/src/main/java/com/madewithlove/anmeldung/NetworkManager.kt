package com.madewithlove.anmeldung

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class NetworkManager(
    private val pushManager: PushManager,
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher
) {

    var onCheck: (@MainThread () -> Unit)? = null
    var lastCheckTime: Long? = null

    @Volatile
    private var areChecksRunning = false

    suspend fun startTimetableChecks() = withContext(ioDispatcher) {
        Log.i(TAG, "Starting timetable checks!")
        areChecksRunning = true
        while (areChecksRunning) {
            doCheck()
            delay(CHECKS_DELAY)
        }
    }

    fun stopTimetableChecks() {
        Log.i(TAG, "Stopping timetable checks!")
        areChecksRunning = false
    }

    private suspend fun doCheck() {
        val initialDocument = Jsoup.connect("https://service.berlin.de/dienstleistung/120686/").get()
        val timetableUrl = initialDocument.body().select(".zmstermin-multi.inner").select(".btn").attr("href")
        val timetableDocument = Jsoup.connect(timetableUrl).get()
        val occupiedSlots = timetableDocument.select("td.nichtbuchbar").size
        val freeSlots = timetableDocument.select("td.buchbar").size
        Log.i(TAG, "Occupied slots: $occupiedSlots, free slots: $freeSlots")

        withContext(mainDispatcher) {
            if (occupiedSlots == 0) {
                Log.e(TAG, "No occupied slots found. Some network error?")
                pushManager.sendFailNotification(timetableUrl)
            }

            if (freeSlots > 0) {
                Log.w(TAG, "SUCCESS!")
                pushManager.sendSuccessNotification(timetableUrl)
            }

            lastCheckTime = System.currentTimeMillis()
            onCheck?.invoke()
        }
    }

    companion object {
        private const val TAG = "AnmeldungNetworkManager"
        private const val CHECKS_DELAY = 60_000L

        @SuppressLint("StaticFieldLeak")
        lateinit var INSTANCE: NetworkManager private set

        fun createInstance(
            pushManager: PushManager,
            mainDispatcher: CoroutineDispatcher,
            ioDispatcher: CoroutineDispatcher
        ) {
            INSTANCE = NetworkManager(pushManager, mainDispatcher, ioDispatcher)
        }
    }
}
