package com.wowwatchdog.app.data.model

import com.google.gson.annotations.SerializedName

data class ServerStatus(
    val timestamp: String = "",
    val watchdog: WatchdogInfo = WatchdogInfo(),
    val services: Services = Services(),
    val worldRestartCount: Int = 0,
    val serverName: String = "",
    val expansion: String = "Unknown"
)

data class WatchdogInfo(
    val state: String = "Unknown",
    val pid: Int = 0
)

data class Services(
    val mysql: ServiceInfo = ServiceInfo(),
    val authserver: ServiceInfo = ServiceInfo(),
    val worldserver: ServiceInfo = ServiceInfo()
)

data class ServiceInfo(
    val running: Boolean = false,
    val healthy: Boolean = false,
    val held: Boolean = false
)

data class ServerConfig(
    val serverName: String = "",
    val expansion: String = "Unknown",
    val mysqlPort: Int = 3310,
    val authserverPort: Int = 1119,
    val worldserverPort: Int = 8086,
    val ntfy: NtfyConfig = NtfyConfig()
)

data class NtfyConfig(
    val server: String = "",
    val topic: String = ""
)

data class HealthResponse(
    val status: String = "",
    val timestamp: String = ""
)

data class CommandResponse(
    val ok: Boolean = false,
    val action: String = "",
    val role: String = "",
    val error: String? = null
)

data class HoldResponse(
    val ok: Boolean = false,
    val role: String = "",
    val held: Boolean = false
)

data class LogsResponse(
    val lines: List<String> = emptyList(),
    val error: String? = null
)

data class ConsoleConnectRequest(
    val host: String = "127.0.0.1",
    val port: Int = 3443,
    val username: String = "",
    val password: String = ""
)

data class ConsoleCommandRequest(
    val command: String = ""
)

data class ConsoleOutputResponse(
    val lines: List<String> = emptyList(),
    val total: Int = 0,
    val connected: Boolean = false
)

data class ConsoleSendResponse(
    val ok: Boolean = false,
    val output: List<String> = emptyList()
)

data class ConsoleConnectResponse(
    val ok: Boolean = false,
    val connected: Boolean = false,
    val error: String? = null
)

data class ErrorResponse(
    val error: String = ""
)
