package app.pulse.acr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.pulse.acr.PulseApp
import app.pulse.acr.data.settings.AppSettings
import app.pulse.acr.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setAutoRecord(enabled: Boolean) = launch { settingsRepository.setAutoRecord(enabled) }
    fun setRecordIncoming(enabled: Boolean) = launch { settingsRepository.setRecordIncoming(enabled) }
    fun setRecordOutgoing(enabled: Boolean) = launch { settingsRepository.setRecordOutgoing(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PulseApp
                return SettingsViewModel(app.container.settingsRepository) as T
            }
        }
    }
}
