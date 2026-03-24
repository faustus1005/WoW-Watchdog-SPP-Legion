package com.wowwatchdog.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wowwatchdog.app.data.model.ServiceInfo
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDetailUiState(
    val role: String = "",
    val displayName: String = "",
    val info: ServiceInfo = ServiceInfo(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val repository: WatchdogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    fun loadService(role: String) {
        val displayName = when (role) {
            "mysql" -> "MySQL"
            "auth" -> "Authserver"
            "world" -> "Worldserver"
            else -> role
        }
        _uiState.update { it.copy(role = role, displayName = displayName) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getStatus()) {
                is ApiResult.Success -> {
                    val info = when (_uiState.value.role) {
                        "mysql" -> result.data.services.mysql
                        "auth" -> result.data.services.authserver
                        "world" -> result.data.services.worldserver
                        else -> ServiceInfo()
                    }
                    _uiState.update { it.copy(info = info, isLoading = false, error = null) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun start() {
        viewModelScope.launch {
            repository.startService(_uiState.value.role)
            delay(1500)
            refresh()
        }
    }

    fun stop() {
        viewModelScope.launch {
            repository.stopService(_uiState.value.role)
            delay(1500)
            refresh()
        }
    }

    fun restart() {
        viewModelScope.launch {
            repository.restartService(_uiState.value.role)
            delay(2500)
            refresh()
        }
    }

    fun toggleHold() {
        viewModelScope.launch {
            repository.toggleHold(_uiState.value.role)
            delay(500)
            refresh()
        }
    }
}
