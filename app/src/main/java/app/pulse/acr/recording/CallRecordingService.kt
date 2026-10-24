package app.pulse.acr.recording

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.pulse.acr.PulseApp
import app.pulse.acr.R
import app.pulse.acr.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L
    private var phoneNumber: String? = null
    private var direction: CallDirection = CallDirection.INCOMING
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (isRecording) return START_STICKY
                phoneNumber = intent.getStringExtra(EXTRA_NUMBER)
                direction = CallDirection.fromWire(intent.getStringExtra(EXTRA_DIRECTION))
                startForegroundRecording()
            }

            ACTION_STOP -> stopAndPersist()
        }
        return START_STICKY
    }

    private fun startForegroundRecording() {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
        startRecorder()
    }

    private fun startRecorder() {
        val app = application as PulseApp
        val dir = app.container.recordingRepository.recordingsDir()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "call_${direction.wire}_$stamp.m4a")
        outputFile = file
        startedAt = System.currentTimeMillis()

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mediaRecorder.apply {
                // Voice communication is the best-effort source for call audio on modern Android.
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            isRecording = true
        } catch (t: Throwable) {
            runCatching { mediaRecorder.release() }
            // Fallback to mic if voice-communication source is blocked.
            tryFallbackMic(file)
        }
    }

    private fun tryFallbackMic(file: File) {
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            isRecording = true
        } catch (_: Throwable) {
            runCatching { mediaRecorder.release() }
            stopSelf()
        }
    }

    private fun stopAndPersist() {
        if (!isRecording) {
            stopSelf()
            return
        }
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        val file = outputFile
        runCatching {
            recorder?.stop()
        }
        runCatching { recorder?.release() }
        recorder = null
        isRecording = false

        if (file != null && file.exists() && file.length() > 0L) {
            val app = application as PulseApp
            val number = phoneNumber
            scope.launch {
                val contact = number?.let { lookupContactName(it) }
                app.container.recordingRepository.saveFinishedRecording(
                    file = file,
                    phoneNumber = number,
                    contactName = contact,
                    direction = direction.wire,
                    startedAt = startedAt,
                    durationMs = duration
                )
                stopSelf()
            }
        } else {
            file?.delete()
            stopSelf()
        }
    }

    private fun lookupContactName(number: String): String? {
        return runCatching {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(number)
                .build()
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun buildNotification(): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PulseApp.RECORDING_CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(phoneNumber ?: direction.wire)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(launch)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        if (isRecording) {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        private const val ACTION_START = "app.pulse.acr.action.START_RECORDING"
        private const val ACTION_STOP = "app.pulse.acr.action.STOP_RECORDING"
        private const val EXTRA_NUMBER = "extra_number"
        private const val EXTRA_DIRECTION = "extra_direction"

        fun start(context: Context, number: String?, direction: CallDirection) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NUMBER, number)
                putExtra(EXTRA_DIRECTION, direction.wire)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
