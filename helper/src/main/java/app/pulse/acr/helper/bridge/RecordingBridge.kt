package app.pulse.acr.helper.bridge

/**
 * Cross-app contract between Pulse ACR Helper and Pulse ACR.
 * Both modules must stay in sync on these constants.
 */
object RecordingBridge {
    const val HELPER_PACKAGE = "app.pulse.acr.helper"
    const val APP_PACKAGE = "app.pulse.acr"

    const val ACTION_IMPORT_RECORDING = "app.pulse.acr.action.IMPORT_RECORDING"
    const val PERMISSION_IMPORT = "app.pulse.acr.permission.IMPORT_RECORDING"

    const val EXTRA_PHONE = "extra_phone"
    const val EXTRA_CONTACT = "extra_contact"
    const val EXTRA_DIRECTION = "extra_direction"
    const val EXTRA_STARTED_AT = "extra_started_at"
    const val EXTRA_DURATION_MS = "extra_duration_ms"
    const val EXTRA_SOURCE = "extra_source"
}
