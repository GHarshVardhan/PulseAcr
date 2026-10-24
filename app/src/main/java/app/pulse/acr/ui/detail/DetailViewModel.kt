package app.pulse.acr.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
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

    fun shareRecording(context: Context) {
        val current = _recording.value ?: return
        val file = File(current.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share recording"))
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
