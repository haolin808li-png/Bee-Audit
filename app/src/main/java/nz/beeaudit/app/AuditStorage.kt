package nz.beeaudit.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtMillis: Long,
) {
    fun isFresh(nowMillis: Long): Boolean = nowMillis - capturedAtMillis <= FRESH_FIX_MILLIS

    companion object {
        private const val FRESH_FIX_MILLIS = 2 * 60 * 1000L
    }
}

data class HiveFormData(
    val hivePresent: Boolean,
    val condition: HiveCondition,
    val activity: BeeActivity,
    val damageVisible: Boolean,
    val notes: String,
)

data class HiveRecordData(
    val pin: String,
    val group: String,
    val visitDate: String,
    val sequenceNumber: Int,
    val createdAt: String,
    val gpsFix: GpsFix?,
    val photoCount: Int,
    val voiceNoteCount: Int,
    val form: HiveFormData,
)

data class AuditDirectories(
    val root: File,
    val pinDir: File,
    val visitDir: File,
    val indexFile: File,
    val sessionFile: File,
)

data class NumberedFile(
    val file: File,
    val number: Int,
)

data class AuditFileSnapshot(
    val recordDir: File,
    val photos: List<File>,
    val voiceNotes: List<File>,
)

class AuditStorage(private val context: Context) {
    fun directories(pin: String, visitDate: String = todayDate()): AuditDirectories {
        val base = File(context.getExternalFilesDir(null) ?: context.filesDir, "audit")
        val cleanPin = AuditFileNames.clean(pin)
        val cleanVisitDate = AuditFileNames.clean(visitDate)
        val pinDir = File(base, cleanPin)
        val visitDir = File(pinDir, cleanVisitDate)
        return AuditDirectories(
            root = base,
            pinDir = pinDir,
            visitDir = visitDir,
            indexFile = File(visitDir, "index.csv"),
            sessionFile = File(visitDir, "session.json"),
        )
    }

    fun ensureSession(pin: String, group: String, visitDate: String = todayDate()): AuditDirectories {
        val dirs = directories(pin, visitDate)
        listOf(dirs.root, dirs.pinDir, dirs.visitDir).forEach { it.mkdirs() }
        if (!dirs.indexFile.exists()) {
            dirs.indexFile.writeText(AuditCsv.header + "\n")
        }
        if (!dirs.sessionFile.exists()) {
            dirs.sessionFile.writeText(
                """
                {
                  "pin": "${pin.jsonEscaped()}",
                  "group": "${group.jsonEscaped()}",
                  "visit_date": "${visitDate.jsonEscaped()}",
                  "started_at": "${nowIso()}",
                  "storage_root": "${dirs.visitDir.absolutePath.jsonEscaped()}"
                }
                """.trimIndent(),
            )
        }
        return dirs
    }

    fun recordDir(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        create: Boolean = true,
    ): File {
        val dirs = if (create) ensureSession(pin, group, visitDate) else directories(pin, visitDate)
        return File(dirs.visitDir, AuditFileNames.recordPrefix(pin, group, sequenceNumber)).also {
            if (create) it.mkdirs()
        }
    }

    fun nextPhotoFile(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        nextPhotoNumber: Int,
    ): File {
        return File(
            recordDir(pin, group, visitDate, sequenceNumber),
            AuditFileNames.photoName(pin, group, sequenceNumber, nextPhotoNumber),
        )
    }

    fun nextAvailablePhotoFile(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        startAt: Int,
    ): NumberedFile {
        var number = startAt.coerceAtLeast(1)
        while (true) {
            val file = nextPhotoFile(pin, group, visitDate, sequenceNumber, number)
            if (!file.exists()) return NumberedFile(file, number)
            number += 1
        }
    }

    fun nextVoiceFile(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        nextVoiceNumber: Int,
    ): File {
        return File(
            recordDir(pin, group, visitDate, sequenceNumber),
            AuditFileNames.voiceName(pin, group, sequenceNumber, nextVoiceNumber),
        )
    }

    fun nextAvailableVoiceFile(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        startAt: Int,
    ): NumberedFile {
        var number = startAt.coerceAtLeast(1)
        while (true) {
            val file = nextVoiceFile(pin, group, visitDate, sequenceNumber, number)
            if (!file.exists()) return NumberedFile(file, number)
            number += 1
        }
    }

    fun saveHiveRecord(record: HiveRecordData): File {
        val dirs = ensureSession(record.pin, record.group, record.visitDate)
        val formFile = File(
            recordDir(record.pin, record.group, record.visitDate, record.sequenceNumber),
            AuditFileNames.formName(record.pin, record.group, record.sequenceNumber),
        )
        formFile.writeText(record.toJson())
        dirs.indexFile.appendText(
            AuditCsv.row(
                AuditCsvRecord(
                    pin = record.pin,
                    group = record.group,
                    visitDate = record.visitDate,
                    sequenceNumber = record.sequenceNumber,
                    createdAt = record.createdAt,
                    latitude = record.gpsFix?.latitude,
                    longitude = record.gpsFix?.longitude,
                    gpsAccuracyMeters = record.gpsFix?.accuracyMeters?.toDouble(),
                    photoCount = record.photoCount,
                    voiceNoteCount = record.voiceNoteCount,
                    condition = record.form.condition,
                    activity = record.form.activity,
                    damageVisible = record.form.damageVisible,
                    notes = record.form.notes,
                ),
            ) + "\n",
        )
        return formFile
    }

    fun filesFor(pin: String, group: String, visitDate: String, sequenceNumber: Int): AuditFileSnapshot {
        val recordDir = recordDir(pin, group, visitDate, sequenceNumber, create = false)
        val prefix = AuditFileNames.recordPrefix(pin, group, sequenceNumber)
        return AuditFileSnapshot(
            recordDir = recordDir,
            photos = recordDir.filesWithPrefix(prefix, ".jpg"),
            voiceNotes = recordDir.filesWithPrefix(prefix, ".m4a"),
        )
    }

    fun exportVisitZip(pin: String, visitDate: String): File {
        val dirs = directories(pin, visitDate)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "${AuditFileNames.clean(pin)}_${AuditFileNames.clean(visitDate)}.zip")
        ZipOutputStream(zipFile.outputStream()).use { zip ->
            if (dirs.visitDir.exists()) {
                dirs.visitDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relativePath = file.relativeTo(dirs.visitDir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry(relativePath))
                        file.inputStream().use { input -> input.copyTo(zip) }
                        zip.closeEntry()
                    }
            }
        }
        return zipFile
    }

    fun uriFor(file: File): Uri = Uri.fromFile(file)

    companion object {
        fun nowIso(): String {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                .format(Date())
                .replace(Regex("(\\d{2})(\\d{2})$"), "$1:$2")
        }

        fun todayDate(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}

private fun File.filesWithPrefix(prefix: String, extension: String): List<File> {
    return listFiles()
        ?.filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(extension) }
        ?.sortedBy { it.name }
        .orEmpty()
}

private fun HiveRecordData.toJson(): String {
    val gpsJson = gpsFix?.let {
        """
        {
          "lat": ${it.latitude},
          "lon": ${it.longitude},
          "accuracy_m": ${it.accuracyMeters},
          "captured_at_millis": ${it.capturedAtMillis}
        }
        """.trimIndent()
    } ?: "null"

    return """
    {
      "pin": "${pin.jsonEscaped()}",
      "group": "${group.jsonEscaped()}",
      "visit_date": "${visitDate.jsonEscaped()}",
      "sequence_number": $sequenceNumber,
      "created_at": "${createdAt.jsonEscaped()}",
      "gps": $gpsJson,
      "photo_count": $photoCount,
      "voice_note_count": $voiceNoteCount,
      "form": {
        "hive_present": ${form.hivePresent},
        "condition": "${form.condition.label.jsonEscaped()}",
        "activity": "${form.activity.label.jsonEscaped()}",
        "damage_visible": ${form.damageVisible},
        "notes": "${form.notes.jsonEscaped()}"
      }
    }
    """.trimIndent()
}

private fun String.jsonEscaped(): String {
    return buildString {
        this@jsonEscaped.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
