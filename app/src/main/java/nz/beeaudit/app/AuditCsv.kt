package nz.beeaudit.app

data class AuditCsvRecord(
    val pin: String,
    val group: String,
    val visitDate: String,
    val sequenceNumber: Int,
    val createdAt: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracyMeters: Double? = null,
    val photoCount: Int,
    val voiceNoteCount: Int,
    val condition: HiveCondition,
    val activity: BeeActivity,
    val damageVisible: Boolean,
    val notes: String,
)

object AuditCsv {
    val header = listOf(
        "pin",
        "group",
        "visit_date",
        "seq_no",
        "created_at",
        "lat",
        "lon",
        "gps_accuracy_m",
        "photo_count",
        "voice_count",
        "condition",
        "activity",
        "damage",
        "notes",
    ).joinToString(",")

    fun row(record: AuditCsvRecord): String {
        return listOf(
            record.pin,
            record.group,
            record.visitDate,
            record.sequenceNumber.toString().padStart(4, '0'),
            record.createdAt,
            record.latitude?.toString().orEmpty(),
            record.longitude?.toString().orEmpty(),
            record.gpsAccuracyMeters?.toString().orEmpty(),
            record.photoCount.toString(),
            record.voiceNoteCount.toString(),
            record.condition.label,
            record.activity.label,
            if (record.damageVisible) "yes" else "no",
            record.notes,
        ).joinToString(",") { it.csvEscaped() }
    }

    private fun String.csvEscaped(): String {
        val requiresQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!requiresQuotes) return this
        return "\"${replace("\"", "\"\"")}\""
    }
}
