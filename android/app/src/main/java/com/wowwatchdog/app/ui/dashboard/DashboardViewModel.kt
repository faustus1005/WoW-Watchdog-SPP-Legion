package com.wowwatchdog.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wowwatchdog.app.data.local.SettingsDataStore
import com.wowwatchdog.app.data.model.ServerStatus
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val status: ServerStatus = ServerStatus(),
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0L
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: WatchdogRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            settingsDataStore.connectionConfig.collectLatest { config ->
                if (config.host.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "Server not configured. Go to Settings.") }
                    return@collectLatest
                }
                while (isActive) {
                    fetchStatus()
                    delay(config.pollingIntervalSeconds * 1000L)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { fetchStatus() }
    }

    private suspend fun fetchStatus() {
        when (val result = repository.getStatus()) {
            is ApiResult.Success -> {
                _uiState.update {
                    it.copy(
                        status = result.data,
                        isLoading = false,
                        isConnected = true,
                        error = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
            is ApiResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isConnected = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun startAll() {
        viewModelScope.launch {
            repository.startAll()
            delay(1000)
            fetchStatus()
        }
    }

    fun stopAll() {
        viewModelScope.launch {
            repository.stopAll()
            delay(1000)
            fetchStatus()
        }
    }

    fun startService(role: String) {
        viewModelScope.launch {
            repository.startService(role)
            delay(1000)
            fetchStatus()
        }
    }

    fun stopService(role: String) {
        viewModelScope.launch {
            repository.stopService(role)
            delay(1000)
            fetchStatus()
        }
    }

    fun restartService(role: String) {
        viewModelScope.launch {
            repository.restartService(role)
            delay(2000)
            fetchStatus()
        }
    }

    fun toggleHold(role: String) {
        viewModelScope.launch {
            repository.toggleHold(role)
            delay(500)
            fetchStatus()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
