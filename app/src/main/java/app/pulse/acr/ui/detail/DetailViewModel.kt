package app.pulse.acr.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.pulse.acr.PulseApp
import app.pulse.acr.data.RecordingRepository
import app.pulse.acr.data.db.RecordingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val recordingId: Long,
    private val recordingRepository: RecordingRepository
) : ViewModel() {
    private val _recording = MutableStateFlow<RecordingEntity?>(null)
    val recording: StateFlow<RecordingEntity?> = _recording.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _recording.value = recordingRepository.get(recordingId)
        }
    }

    fun toggleStar() {
        viewModelScope.launch {
            recordingRepository.toggleStar(recordingId)
            refresh()
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            recordingRepository.updateNotes(recordingId, notes)
            refresh()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            recordingRepository.delete(recordingId)
            onDone()
        }
    }

    companion object {
        fun factory(recordingId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PulseApp
                return DetailViewModel(recordingId, app.container.recordingRepository) as T
            }
        }
    }
}
