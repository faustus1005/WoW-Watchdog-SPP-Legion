package com.wowwatchdog.app.data.api

import com.wowwatchdog.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface WatchdogApiService {

    @GET("/api/v1/health")
    suspend fun health(): Response<HealthResponse>

    @GET("/api/v1/status")
    suspend fun status(): Response<ServerStatus>

    @GET("/api/v1/config")
    suspend fun config(): Response<ServerConfig>

    @GET("/api/v1/logs")
    suspend fun logs(@Query("lines") lines: Int = 50): Response<LogsResponse>

    @POST("/api/v1/services/{role}/start")
    suspend fun startService(@Path("role") role: String): Response<CommandResponse>

    @POST("/api/v1/services/{role}/stop")
    suspend fun stopService(@Path("role") role: String): Response<CommandResponse>

    @POST("/api/v1/services/{role}/restart")
    suspend fun restartService(@Path("role") role: String): Response<CommandResponse>

    @POST("/api/v1/services/start-all")
    suspend fun startAll(): Response<CommandResponse>

    @POST("/api/v1/services/stop-all")
    suspend fun stopAll(): Response<CommandResponse>

    @POST("/api/v1/services/{role}/hold")
    suspend fun toggleHold(@Path("role") role: String): Response<HoldResponse>

    @POST("/api/v1/console/connect")
    suspend fun consoleConnect(@Body request: ConsoleConnectRequest): Response<ConsoleConnectResponse>

    @POST("/api/v1/console/send")
    suspend fun consoleSend(@Body request: ConsoleCommandRequest): Response<ConsoleSendResponse>

    @GET("/api/v1/console/output")
    suspend fun consoleOutput(@Query("since") since: Int = 0): Response<ConsoleOutputResponse>

    @POST("/api/v1/console/disconnect")
    suspend fun consoleDisconnect(): Response<ConsoleConnectResponse>
}
