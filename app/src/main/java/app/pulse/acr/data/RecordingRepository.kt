package app.pulse.acr.data

import android.content.Context
import android.net.Uri
import app.pulse.acr.data.db.RecordingDao
import app.pulse.acr.data.db.RecordingEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(
    private val context: Context,
    private val dao: RecordingDao
) {
    fun observeRecordings(query: String): Flow<List<RecordingEntity>> {
        return if (query.isBlank()) dao.observeAll() else dao.search(query.trim())
    }

    suspend fun get(id: Long): RecordingEntity? = dao.getById(id)

    suspend fun saveFinishedRecording(
        file: File,
        phoneNumber: String?,
        contactName: String?,
        direction: String,
        startedAt: Long,
        durationMs: Long
    ): Long {
        val display = contactName?.takeIf { it.isNotBlank() }
            ?: phoneNumber?.takeIf { it.isNotBlank() }
            ?: "Unknown"
        return dao.insert(
            RecordingEntity(
                filePath = file.absolutePath,
                displayName = display,
                phoneNumber = phoneNumber,
                contactName = contactName,
                direction = direction,
                startedAt = startedAt,
                durationMs = durationMs,
                fileSizeBytes = file.length()
            )
        )
    }

    /**
     * Imports a finished recording delivered from Pulse ACR Helper as a content [uri].
     * The stream is copied into the app's private recordings directory and then persisted
     * so it shows up in the library alongside self-recorded calls.
     */
    suspend fun importExternalRecording(
        uri: Uri,
        phoneNumber: String?,
        contactName: String?,
        direction: String,
        startedAt: Long,
        durationMs: Long,
        sourceHint: String?
    ): Long {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startedAt))
        val prefix = sourceHint?.takeIf { it.isNotBlank() }?.let { "${it.trim()}_" } ?: "import_"
        val dest = File(recordingsDir(), "$prefix$stamp.m4a")

        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Unable to open recording stream for $uri")

        return saveFinishedRecording(
            file = dest,
            phoneNumber = phoneNumber,
            contactName = contactName,
            direction = direction,
            startedAt = startedAt,
            durationMs = durationMs
        )
    }

    suspend fun toggleStar(id: Long) {
        val item = dao.getById(id) ?: return
        dao.setStarred(id, !item.isStarred)
    }

    suspend fun updateNotes(id: Long, notes: String) {
        val item = dao.getById(id) ?: return
        dao.update(item.copy(notes = notes))
    }

    suspend fun delete(id: Long) {
        val item = dao.getById(id) ?: return
        runCatching { File(item.filePath).delete() }
        dao.deleteById(id)
    }

    fun recordingsDir(): File {
        val dir = File(context.getExternalFilesDir(null), "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
