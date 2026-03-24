package com.wowwatchdog.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wowwatchdog.app.data.local.SettingsDataStore
import com.wowwatchdog.app.data.model.ConnectionConfig
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val config: ConnectionConfig = ConnectionConfig(),
    val testResult: String? = null,
    val isTesting: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: WatchdogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.connectionConfig.collect { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
    }

    fun updateHost(value: String) {
        _uiState.update { it.copy(config = it.config.copy(host = value), isSaved = false) }
    }

    fun updatePort(value: String) {
        val port = value.toIntOrNull() ?: return
        _uiState.update { it.copy(config = it.config.copy(port = port), isSaved = false) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(config = it.config.copy(apiKey = value), isSaved = false) }
    }

    fun updateNtfyServer(value: String) {
        _uiState.update { it.copy(config = it.config.copy(ntfyServer = value), isSaved = false) }
    }

    fun updateNtfyTopic(value: String) {
        _uiState.update { it.copy(config = it.config.copy(ntfyTopic = value), isSaved = false) }
    }

    fun updateTheme(value: String) {
        _uiState.update { it.copy(config = it.config.copy(theme = value), isSaved = false) }
    }

    fun updatePollingInterval(value: Int) {
        _uiState.update { it.copy(config = it.config.copy(pollingIntervalSeconds = value), isSaved = false) }
    }

    fun save() {
        viewModelScope.launch {
            settingsDataStore.updateConfig(_uiState.value.config)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            when (val result = repository.getHealth()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isTesting = false, testResult = "Connected successfully!") }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isTesting = false, testResult = "Failed: ${result.message}") }
                }
            }
        }
    }
}
