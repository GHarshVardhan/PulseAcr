package app.pulse.acr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import app.pulse.acr.data.AppContainer
import app.pulse.acr.data.DefaultAppContainer

class PulseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            RECORDING_CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.recording_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val RECORDING_CHANNEL_ID = "pulse_recording"
    }
}
