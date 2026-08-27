package app.pulse.acr.util

import android.content.Context
import android.content.pm.PackageManager
import app.pulse.acr.recording.RecordingBridge

fun isHelperInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(RecordingBridge.HELPER_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
