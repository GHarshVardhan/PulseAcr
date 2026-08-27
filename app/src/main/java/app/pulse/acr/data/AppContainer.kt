package app.pulse.acr.data

import android.content.Context
import app.pulse.acr.data.db.PulseDatabase
import app.pulse.acr.data.db.RecordingDao
import app.pulse.acr.data.settings.SettingsRepository

interface AppContainer {
    val recordingDao: RecordingDao
    val settingsRepository: SettingsRepository
    val recordingRepository: RecordingRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val db = PulseDatabase.get(context)

    override val recordingDao: RecordingDao = db.recordingDao()
    override val settingsRepository: SettingsRepository = SettingsRepository(context)
    override val recordingRepository: RecordingRepository =
        RecordingRepository(context, recordingDao)
}
