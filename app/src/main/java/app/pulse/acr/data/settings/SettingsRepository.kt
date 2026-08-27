package app.pulse.acr.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("pulse_settings")

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            autoRecordEnabled = prefs[Keys.AUTO_RECORD] ?: true,
            recordIncoming = prefs[Keys.RECORD_INCOMING] ?: true,
            recordOutgoing = prefs[Keys.RECORD_OUTGOING] ?: true,
            consentAccepted = prefs[Keys.CONSENT] ?: false,
            onboardingDone = prefs[Keys.ONBOARDING] ?: false
        )
    }

    suspend fun setAutoRecord(enabled: Boolean) = set(Keys.AUTO_RECORD, enabled)
    suspend fun setRecordIncoming(enabled: Boolean) = set(Keys.RECORD_INCOMING, enabled)
    suspend fun setRecordOutgoing(enabled: Boolean) = set(Keys.RECORD_OUTGOING, enabled)
    suspend fun setConsentAccepted(accepted: Boolean) = set(Keys.CONSENT, accepted)
    suspend fun setOnboardingDone(done: Boolean) = set(Keys.ONBOARDING, done)

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    private object Keys {
        val AUTO_RECORD = booleanPreferencesKey("auto_record")
        val RECORD_INCOMING = booleanPreferencesKey("record_incoming")
        val RECORD_OUTGOING = booleanPreferencesKey("record_outgoing")
        val CONSENT = booleanPreferencesKey("consent_accepted")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }
}

data class AppSettings(
    val autoRecordEnabled: Boolean = true,
    val recordIncoming: Boolean = true,
    val recordOutgoing: Boolean = true,
    val consentAccepted: Boolean = false,
    val onboardingDone: Boolean = false
)
