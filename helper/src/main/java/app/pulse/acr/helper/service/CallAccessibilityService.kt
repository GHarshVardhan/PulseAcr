package app.pulse.acr.helper.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import app.pulse.acr.helper.recording.CallSessionTracker

class CallAccessibilityService : AccessibilityService() {
    private val tracker = CallSessionTracker()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }
        val pkg = event.packageName?.toString().orEmpty()
        val className = event.className?.toString().orEmpty()
        val text = event.text.joinToString(" ").lowercase()

        val looksLikeCallUi = CALL_PACKAGES.any { pkg.startsWith(it) } ||
            className.contains("InCall", ignoreCase = true) ||
            className.contains("CallActivity", ignoreCase = true) ||
            text.contains("incoming call") ||
            text.contains("ongoing call") ||
            text.contains("calling")

        if (looksLikeCallUi) {
            tracker.onCallUiVisible(this, pkg)
        } else if (tracker.isTrackingPackage(pkg).not() && tracker.hasActiveSession) {
            // Ignore unrelated window noise while a session is open.
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        tracker.forceStop(this)
        super.onDestroy()
    }

    companion object {
        private val CALL_PACKAGES = listOf(
            "com.android.incallui",
            "com.google.android.dialer",
            "com.samsung.android.incallui",
            "com.whatsapp",
            "org.telegram.messenger",
            "com.facebook.orca",
            "com.skype.raider",
            "com.viber.voip",
            "us.zoom.videomeetings",
            "com.google.android.apps.tachyon"
        )

        fun isEnabled(context: Context): Boolean {
            val expected = ComponentName(context, CallAccessibilityService::class.java)
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(enabled)
            while (splitter.hasNext()) {
                val component = ComponentName.unflattenFromString(splitter.next())
                if (component == expected) return true
            }
            return false
        }
    }
}
