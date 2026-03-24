package com.wowwatchdog.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wowwatchdog.app.MainActivity
import com.wowwatchdog.app.data.local.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class NtfyListenerService : Service() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventSource: EventSource? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private const val CHANNEL_ID = "ntfy_listener"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_CHANNEL_ID = "watchdog_alerts"

        fun start(context: Context) {
            val intent = Intent(context, NtfyListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NtfyListenerService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { connectToNtfy() }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        eventSource?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun connectToNtfy() {
        val config = settingsDataStore.connectionConfig.first()
        if (config.ntfyServer.isBlank() || config.ntfyTopic.isBlank()) return

        val url = "${config.ntfyServer.trimEnd('/')}/${config.ntfyTopic}/sse"
        val request = Request.Builder().url(url).build()

        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = JSONObject(data)
                    val message = json.optString("message", "")
                    val title = json.optString("title", "WoW Watchdog")
                    val priority = json.optInt("priority", 3)
                    if (message.isNotEmpty()) {
                        showAlertNotification(title, message, priority)
                    }
                } catch (_: Exception) { }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // Reconnect after delay
                scope.launch {
                    delay(15000)
                    connectToNtfy()
                }
            }
        })
    }

    private fun showAlertNotification(title: String, message: String, priority: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notifPriority = when {
            priority >= 4 -> NotificationCompat.PRIORITY_HIGH
            priority >= 3 -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(notifPriority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("WoW Watchdog")
            .setContentText("Listening for server alerts...")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val listenerChannel = NotificationChannel(
                CHANNEL_ID, "NTFY Listener", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Background notification listener" }
            manager.createNotificationChannel(listenerChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID, "Server Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when server services go down or up" }
            manager.createNotificationChannel(alertChannel)
        }
    }
}
