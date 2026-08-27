package app.pulse.acr.helper.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.pulse.acr.helper.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HelperRecordingService : Service() {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.helper_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> if (!recording) startRecording(intent.getStringExtra(EXTRA_SOURCE))
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording(source: String?) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pulse Helper recording")
            .setContentText(source ?: "Call in progress")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            77,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )

        val dir = File(getExternalFilesDir(null), "recordings").also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "helper_$stamp.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mediaRecorder.apply {
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
            recording = true
        } catch (_: Throwable) {
            runCatching { mediaRecorder.release() }
            try {
                val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                fallback.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorder = fallback
                recording = true
            } catch (_: Throwable) {
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        if (recording) {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            recorder = null
            recording = false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (recording) stopRecording()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "pulse_helper_recording"
        private const val ACTION_START = "app.pulse.acr.helper.START"
        private const val ACTION_STOP = "app.pulse.acr.helper.STOP"
        private const val EXTRA_SOURCE = "source"

        fun start(context: Context, source: String?) {
            val intent = Intent(context, HelperRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOURCE, source)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, HelperRecordingService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK ->
                HelperRecordingService.start(context, "phone")
            TelephonyManager.EXTRA_STATE_IDLE ->
                HelperRecordingService.stop(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Accessibility service is rebound by the system after boot when enabled.
    }
}
