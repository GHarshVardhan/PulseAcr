package app.pulse.acr.data

import android.content.Context
import app.pulse.acr.data.db.RecordingDao
import app.pulse.acr.data.db.RecordingEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

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
