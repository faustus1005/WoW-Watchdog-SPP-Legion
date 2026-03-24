package com.wowwatchdog.app.data.model

data class ConnectionConfig(
    val host: String = "",
    val port: Int = 8099,
    val apiKey: String = "",
    val ntfyServer: String = "https://ntfy.sh",
    val ntfyTopic: String = "",
    val theme: String = "Default",
    val pollingIntervalSeconds: Int = 10
)
