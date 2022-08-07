package com.madewithlove.anmeldung

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : Activity() {

    private val dateFormat = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())

    private val networkManager by lazy {
        NetworkManager.INSTANCE
    }

    private val markwon by lazy {
        Markwon.create(this)
    }

    private val lastCheckTextView by lazy {
        findViewById<TextView>(R.id.lastCheckTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textView)
        val button = findViewById<Button>(R.id.button)

        val markdown = """
            ### Hi!
            This app automatically checks **[Berlin Anmeldung einer Wohnung](https://service.berlin.de/dienstleistung/120686/)** for you every **one minute**.
            ### You can close the app, it will continue working in the background.
            Once a free slot is found, you'll receive a push notification.
            Click on the notification to open the webpage and then book an appointment as fast as you can!
            ### Good luck!
        """.trimIndent()
        markwon.setMarkdown(textView, markdown)

        val serviceIntent = Intent(this, TimetableCheckService::class.java)
        startService(serviceIntent)

        button.setOnClickListener {
            val isServiceStopped = stopService(Intent(this, TimetableCheckService::class.java))
            Log.i(TAG, "Foreground Service is stopped: $isServiceStopped")
            finish()
        }

        updateLastCheckText()
        networkManager.onCheck = { result ->
            if (result is Fail) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            } else {
                updateLastCheckText()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.extras?.containsKey(INTENT_KEY_CHECK_FAILED) == true) {
            val webAddress = intent.extras?.get(INTENT_KEY_WEB_ADDRESS) ?: ""
            val message = """
                There may be different reasons for the check fail. 
                You can check yourself how the timetable looks like right now: 
                [service.berlin.de](${webAddress}). If web page is not opening, the reason may be 
                either government server is down (then just wait) or 
                your IP is banned (then turn on and off airplane mode, if you're on mobile network) 
                or API has been changed (then contact developer: [@alexal1](https://t.me/alexal1/). 
                Good luck!
            """.trimIndent()
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_fail_title)
                .setMessage(markwon.toMarkdown(message))
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
                .also {
                    val messageView = it.findViewById<TextView>(android.R.id.message)
                    messageView?.movementMethod = LinkMovementMethod.getInstance()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkManager.onCheck = null
    }

    private fun updateLastCheckText() {
        val currentTime = networkManager.lastCheckTime?.let {
            dateFormat.format(networkManager.lastCheckTime)
        } ?: "–"
        lastCheckTextView.text = String.format(getString(R.string.last_check), currentTime)
    }

    companion object {
        const val INTENT_KEY_CHECK_FAILED = "check_failed"
        const val INTENT_KEY_WEB_ADDRESS = "web_address"

        private const val TAG = "AnmeldungMainActivity"
    }
}
