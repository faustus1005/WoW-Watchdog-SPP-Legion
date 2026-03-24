package com.wowwatchdog.app.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wowwatchdog.app.data.model.ConsoleConnectRequest
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ConsoleUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val outputLines: List<String> = emptyList(),
    val totalLines: Int = 0,
    val error: String? = null,
    val host: String = "127.0.0.1",
    val port: String = "3443",
    val username: String = "",
    val password: String = ""
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val repository: WatchdogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun updateHost(value: String) { _uiState.update { it.copy(host = value) } }
    fun updatePort(value: String) { _uiState.update { it.copy(port = value) } }
    fun updateUsername(value: String) { _uiState.update { it.copy(username = value) } }
    fun updatePassword(value: String) { _uiState.update { it.copy(password = value) } }

    fun connect() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isConnecting = true, error = null) }
            val request = ConsoleConnectRequest(
                host = state.host,
                port = state.port.toIntOrNull() ?: 3443,
                username = state.username,
                password = state.password
            )
            when (val result = repository.consoleConnect(request)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isConnected = true, isConnecting = false) }
                    startOutputPolling()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isConnecting = false, error = result.message) }
                }
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        viewModelScope.launch {
            repository.consoleDisconnect()
            _uiState.update { it.copy(isConnected = false, outputLines = emptyList(), totalLines = 0) }
        }
    }

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            when (val result = repository.consoleSend(command)) {
                is ApiResult.Success -> {
                    if (result.data.output.isNotEmpty()) {
                        _uiState.update {
                            it.copy(outputLines = it.outputLines + result.data.output)
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    private fun startOutputPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                when (val result = repository.consoleOutput(_uiState.value.totalLines)) {
                    is ApiResult.Success -> {
                        if (result.data.lines.isNotEmpty()) {
                            _uiState.update {
                                it.copy(
                                    outputLines = it.outputLines + result.data.lines,
                                    totalLines = result.data.total,
                                    isConnected = result.data.connected
                                )
                            }
                        }
                        if (!result.data.connected) {
                            _uiState.update { it.copy(isConnected = false) }
                            break
                        }
                    }
                    is ApiResult.Error -> break
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
