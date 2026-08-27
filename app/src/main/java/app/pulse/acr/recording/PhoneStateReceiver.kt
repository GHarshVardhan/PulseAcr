package app.pulse.acr.recording

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import app.pulse.acr.PulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED &&
            intent.action != "android.intent.action.PHONE_STATE"
        ) {
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? PulseApp ?: return@launch
                val settings = app.container.settingsRepository.settings.first()
                if (!settings.autoRecordEnabled || !settings.consentAccepted) return@launch

                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return@launch
                val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        lastNumber = number
                        lastDirection = CallDirection.INCOMING
                    }

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        val direction = lastDirection
                        if (direction == CallDirection.INCOMING && !settings.recordIncoming) return@launch
                        if (direction == CallDirection.OUTGOING && !settings.recordOutgoing) return@launch
                        // If we never saw RINGING, treat as outgoing.
                        val resolved = if (lastDirection == CallDirection.INCOMING && lastNumber != null) {
                            CallDirection.INCOMING
                        } else {
                            CallDirection.OUTGOING
                        }
                        if (resolved == CallDirection.INCOMING && !settings.recordIncoming) return@launch
                        if (resolved == CallDirection.OUTGOING && !settings.recordOutgoing) return@launch
                        CallRecordingService.start(
                            context,
                            number ?: lastNumber,
                            resolved
                        )
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        CallRecordingService.stop(context)
                        lastNumber = null
                        lastDirection = CallDirection.OUTGOING
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        @Volatile
        private var lastNumber: String? = null

        @Volatile
        private var lastDirection: CallDirection = CallDirection.OUTGOING
    }
}

enum class CallDirection(val wire: String) {
    INCOMING("incoming"),
    OUTGOING("outgoing");

    companion object {
        fun fromWire(value: String?): CallDirection =
            entries.firstOrNull { it.wire == value } ?: INCOMING
    }
}
