package app.pulse.acr.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val displayName: String,
    val phoneNumber: String?,
    val contactName: String?,
    val direction: String,
    val startedAt: Long,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val isStarred: Boolean = false,
    val notes: String = ""
)
