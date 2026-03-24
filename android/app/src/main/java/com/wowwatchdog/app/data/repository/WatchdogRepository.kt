package com.wowwatchdog.app.data.repository

import com.wowwatchdog.app.data.api.WatchdogApiService
import com.wowwatchdog.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

@Singleton
class WatchdogRepository @Inject constructor(
    private val api: WatchdogApiService
) {
    suspend fun getHealth(): ApiResult<HealthResponse> = safeCall { api.health() }

    suspend fun getStatus(): ApiResult<ServerStatus> = safeCall { api.status() }

    suspend fun getConfig(): ApiResult<ServerConfig> = safeCall { api.config() }

    suspend fun getLogs(lines: Int = 50): ApiResult<LogsResponse> = safeCall { api.logs(lines) }

    suspend fun startService(role: String): ApiResult<CommandResponse> = safeCall { api.startService(role) }

    suspend fun stopService(role: String): ApiResult<CommandResponse> = safeCall { api.stopService(role) }

    suspend fun restartService(role: String): ApiResult<CommandResponse> = safeCall { api.restartService(role) }

    suspend fun startAll(): ApiResult<CommandResponse> = safeCall { api.startAll() }

    suspend fun stopAll(): ApiResult<CommandResponse> = safeCall { api.stopAll() }

    suspend fun toggleHold(role: String): ApiResult<HoldResponse> = safeCall { api.toggleHold(role) }

    suspend fun consoleConnect(request: ConsoleConnectRequest): ApiResult<ConsoleConnectResponse> =
        safeCall { api.consoleConnect(request) }

    suspend fun consoleSend(command: String): ApiResult<ConsoleSendResponse> =
        safeCall { api.consoleSend(ConsoleCommandRequest(command)) }

    suspend fun consoleOutput(since: Int = 0): ApiResult<ConsoleOutputResponse> =
        safeCall { api.consoleOutput(since) }

    suspend fun consoleDisconnect(): ApiResult<ConsoleConnectResponse> =
        safeCall { api.consoleDisconnect() }

    private suspend fun <T> safeCall(
        call: suspend () -> retrofit2.Response<T>
    ): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(
                    message = response.errorBody()?.string() ?: "Unknown error",
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(message = e.message ?: "Connection failed")
        }
    }
}
