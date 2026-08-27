package app.pulse.acr.helper.recording

import android.content.Context
import app.pulse.acr.helper.service.HelperRecordingService

/**
 * Lightweight session tracker: when call UI appears we start recording;
 * when the UI disappears for a grace period we stop.
 */
class CallSessionTracker {
    @Volatile
    var hasActiveSession: Boolean = false
        private set

    private var activePackage: String? = null
    private var lastSeenAt: Long = 0L

    fun isTrackingPackage(pkg: String): Boolean = pkg == activePackage

    fun onCallUiVisible(context: Context, pkg: String) {
        lastSeenAt = System.currentTimeMillis()
        if (!hasActiveSession) {
            hasActiveSession = true
            activePackage = pkg
            HelperRecordingService.start(context, pkg)
        } else {
            activePackage = pkg
        }
    }

    fun forceStop(context: Context) {
        if (!hasActiveSession) return
        hasActiveSession = false
        activePackage = null
        HelperRecordingService.stop(context)
    }
}
