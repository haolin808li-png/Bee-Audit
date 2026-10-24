package nz.beeaudit.app

data class AuditSession(
    val pin: String = "",
    val group: String = "Hive",
    val visitDate: String = "",
    val sequenceNumber: Int = 1,
    val forcedFlash: Boolean = true,
    val photoCount: Int = 0,
    val voiceNoteCount: Int = 0,
    val hivePresent: Boolean = true,
    val hiveCondition: HiveCondition = HiveCondition.Good,
    val beeActivity: BeeActivity = BeeActivity.High,
    val damageVisible: Boolean = false,
    val notes: String = "",
)

enum class HiveCondition(val label: String) {
    Good("Good"),
    Fair("Fair"),
    Poor("Poor"),
    Missing("Missing"),
}

enum class BeeActivity(val label: String) {
    High("High"),
    Medium("Medium"),
    Low("Low"),
    None("None"),
}

object AuditFileNames {
    fun clean(value: String): String {
        return value
            .trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "UNKNOWN" }
    }

    fun recordPrefix(pin: String, group: String, sequenceNumber: Int): String {
        return "${clean(pin)}_${clean(group)}_${sequenceNumber.padded(4)}"
    }

    fun photoName(pin: String, group: String, sequenceNumber: Int, photoNumber: Int): String {
        return "${recordPrefix(pin, group, sequenceNumber)}_${photoNumber.padded(2)}.jpg"
    }

    fun voiceName(pin: String, group: String, sequenceNumber: Int, noteNumber: Int): String {
        return "${recordPrefix(pin, group, sequenceNumber)}_${noteNumber.padded(2)}.m4a"
    }

    fun formName(pin: String, group: String, sequenceNumber: Int): String {
        return "${recordPrefix(pin, group, sequenceNumber)}.json"
    }

    private fun Int.padded(width: Int): String = toString().padStart(width, '0')
}
