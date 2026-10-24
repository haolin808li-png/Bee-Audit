package nz.beeaudit.app

import android.content.Context

class AuditPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("bee_audit_state", Context.MODE_PRIVATE)

    var pin: String
        get() = prefs.getString(KEY_PIN, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var group: String
        get() = prefs.getString(KEY_GROUP, "Hive").orEmpty().ifBlank { "Hive" }
        set(value) = prefs.edit().putString(KEY_GROUP, value).apply()

    var visitDate: String
        get() = prefs.getString(KEY_VISIT_DATE, AuditStorage.todayDate()).orEmpty().ifBlank {
            AuditStorage.todayDate()
        }
        set(value) = prefs.edit().putString(KEY_VISIT_DATE, value).apply()

    var sequenceNumber: Int
        get() = prefs.getInt(KEY_SEQUENCE, 1).coerceAtLeast(1)
        set(value) = prefs.edit().putInt(KEY_SEQUENCE, value.coerceAtLeast(1)).apply()

    var forcedFlash: Boolean
        get() = prefs.getBoolean(KEY_FORCED_FLASH, true)
        set(value) = prefs.edit().putBoolean(KEY_FORCED_FLASH, value).apply()

    var saveFolderUri: String
        get() = prefs.getString(KEY_SAVE_FOLDER_URI, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_SAVE_FOLDER_URI, value).apply()

    fun saveSession(
        pin: String,
        group: String,
        visitDate: String,
        sequenceNumber: Int,
        forcedFlash: Boolean,
    ) {
        prefs.edit()
            .putString(KEY_PIN, pin)
            .putString(KEY_GROUP, group)
            .putString(KEY_VISIT_DATE, visitDate)
            .putInt(KEY_SEQUENCE, sequenceNumber.coerceAtLeast(1))
            .putBoolean(KEY_FORCED_FLASH, forcedFlash)
            .apply()
    }

    companion object {
        private const val KEY_PIN = "pin"
        private const val KEY_GROUP = "group"
        private const val KEY_VISIT_DATE = "visit_date"
        private const val KEY_SEQUENCE = "sequence"
        private const val KEY_FORCED_FLASH = "forced_flash"
        private const val KEY_SAVE_FOLDER_URI = "save_folder_uri"
    }
}
