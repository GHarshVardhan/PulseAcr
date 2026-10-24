package app.pulse.acr.ui.detail

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.pulse.acr.ui.util.formatBytes
import app.pulse.acr.ui.util.formatDuration
import app.pulse.acr.ui.util.formatTimestamp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    recordingId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(factory = DetailViewModel.factory(recordingId))
) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notes by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }
    val player = remember { MediaPlayer() }

    LaunchedEffect(recording?.notes) {
        notes = recording?.notes.orEmpty()
    }

    LaunchedEffect(recording?.filePath) {
        val path = recording?.filePath ?: return@LaunchedEffect
        runCatching {
            player.reset()
            player.setDataSource(path)
            player.prepare()
            duration = player.duration.toFloat().coerceAtLeast(1f)
        }
    }

    LaunchedEffect(playing) {
        while (playing) {
            progress = player.currentPosition.toFloat()
            if (!player.isPlaying) {
                playing = false
                break
            }
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.stop() }
            player.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recording?.displayName ?: "Recording") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareRecording(context) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { viewModel.toggleStar() }) {
                        val starred = recording?.isStarred == true
                        Icon(
                            if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.delete {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val item = recording
        if (item == null) {
            Text("Recording not found", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(formatTimestamp(item.startedAt), style = MaterialTheme.typography.bodyLarge)
            Text(
                "${item.direction.replaceFirstChar { it.uppercase() }} · ${formatDuration(item.durationMs)} · ${formatBytes(item.fileSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!item.phoneNumber.isNullOrBlank()) {
                Text(item.phoneNumber, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = progress.coerceIn(0f, duration),
                onValueChange = {
                    progress = it
                    runCatching { player.seekTo(it.toInt()) }
                },
                valueRange = 0f..duration
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        player.start()
                        playing = true
                    }
                }) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Text(if (playing) " Pause" else " Play")
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 3
            )
            Button(
                onClick = { viewModel.saveNotes(notes) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save notes") }
        }
    }
}
