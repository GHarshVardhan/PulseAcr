package app.pulse.acr.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.pulse.acr.PulseApp
import app.pulse.acr.data.RecordingRepository
import app.pulse.acr.data.db.RecordingEntity
import app.pulse.acr.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val recordingRepository: RecordingRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val recordings: StateFlow<List<RecordingEntity>> = _query
        .flatMapLatest { recordingRepository.observeRecordings(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val autoRecordEnabled: StateFlow<Boolean> = settingsRepository.settings
        .map { it.autoRecordEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleStar(id: Long) {
        viewModelScope.launch { recordingRepository.toggleStar(id) }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PulseApp
                return LibraryViewModel(
                    app.container.recordingRepository,
                    app.container.settingsRepository
                ) as T
            }
        }
    }
}
