package com.wowwatchdog.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wowwatchdog.app.data.repository.ApiResult
import com.wowwatchdog.app.data.repository.WatchdogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class LogUiState(
    val lines: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val autoRefresh: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: WatchdogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getLogs(200)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(lines = result.data.lines, isLoading = false, error = null) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun toggleAutoRefresh() {
        val newState = !_uiState.value.autoRefresh
        _uiState.update { it.copy(autoRefresh = newState) }
        if (newState) {
            autoRefreshJob = viewModelScope.launch {
                while (isActive) {
                    delay(5000)
                    refresh()
                }
            }
        } else {
            autoRefreshJob?.cancel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
