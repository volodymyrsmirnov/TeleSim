package com.volodymyrsmirnov.telesim.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.volodymyrsmirnov.telesim.data.AppSettings
import com.volodymyrsmirnov.telesim.data.SettingsRepository
import com.volodymyrsmirnov.telesim.data.SimCardInfo
import com.volodymyrsmirnov.telesim.sim.SimCardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    data class MainUiState(
        val simCards: Map<Int, SimCardInfo> = emptyMap(),
        val botToken: String = "",
        val simChannels: Map<Int, String> = emptyMap(),
        val isSaving: Boolean = false,
        val saveMessage: String? = null
    )

    private val settingsRepository = SettingsRepository(application)
    private val simCardManager = SimCardManager(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSimCards()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    botToken = settings.botToken,
                    simChannels = settings.simChannels
                )
            }
        }
    }

    fun loadSimCards() {
        viewModelScope.launch {
            val simCards = simCardManager.getAvailableSimCards()

            _uiState.value = _uiState.value.copy(simCards = simCards)
        }
    }

    fun updateBotToken(token: String) {
        _uiState.value = _uiState.value.copy(botToken = token)
    }

    fun updateSimChannel(slotIndex: Int, channelId: String) {
        val updatedChannels = _uiState.value.simChannels.toMutableMap()

        if (channelId.isBlank()) {
            updatedChannels.remove(slotIndex)
        } else {
            updatedChannels[slotIndex] = channelId
        }

        _uiState.value = _uiState.value.copy(simChannels = updatedChannels)
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            try {
                val settings = AppSettings(
                    botToken = _uiState.value.botToken,
                    simChannels = _uiState.value.simChannels
                )

                settingsRepository.updateSettings(settings)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveMessage = "Settings saved successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveMessage = "Error saving settings: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }
}