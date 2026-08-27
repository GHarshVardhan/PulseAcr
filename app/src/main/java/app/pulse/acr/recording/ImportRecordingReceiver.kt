package app.pulse.acr.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import app.pulse.acr.PulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives finished recordings from Pulse ACR Helper and imports them into the library.
 */
class ImportRecordingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RecordingBridge.ACTION_IMPORT_RECORDING) return

        val uri: Uri = intent.data ?: streamUri(intent) ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? PulseApp ?: return@launch
                app.container.recordingRepository.importExternalRecording(
                    uri = uri,
                    phoneNumber = intent.getStringExtra(RecordingBridge.EXTRA_PHONE),
                    contactName = intent.getStringExtra(RecordingBridge.EXTRA_CONTACT),
                    direction = intent.getStringExtra(RecordingBridge.EXTRA_DIRECTION) ?: "incoming",
                    startedAt = intent.getLongExtra(RecordingBridge.EXTRA_STARTED_AT, System.currentTimeMillis()),
                    durationMs = intent.getLongExtra(RecordingBridge.EXTRA_DURATION_MS, 0L),
                    sourceHint = intent.getStringExtra(RecordingBridge.EXTRA_SOURCE)
                )
            } finally {
                pending.finish()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun streamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
    }
}
