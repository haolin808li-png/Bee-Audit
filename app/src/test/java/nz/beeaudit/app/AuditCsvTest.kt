package nz.beeaudit.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AuditCsvTest {
    @Test
    fun headerMatchesAuditSpec() {
        assertEquals(
            "pin,group,visit_date,seq_no,created_at,lat,lon,gps_accuracy_m,photo_count,voice_count,condition,activity,damage,notes",
            AuditCsv.header,
        )
    }

    @Test
    fun rowEscapesNotesAndPadsSequenceNumber() {
        val row = AuditCsv.row(
            AuditCsvRecord(
                pin = "KPIN1234",
                group = "Hive",
                visitDate = "2026-06-08",
                sequenceNumber = 7,
                createdAt = "2026-06-08T09:42:11+12:00",
                latitude = -43.5321,
                longitude = 172.6362,
                gpsAccuracyMeters = 8.5,
                photoCount = 3,
                voiceNoteCount = 1,
                condition = HiveCondition.Good,
                activity = BeeActivity.High,
                damageVisible = false,
                notes = "Strong hive, good entrance activity",
            ),
        )

        assertEquals(
            "KPIN1234,Hive,2026-06-08,0007,2026-06-08T09:42:11+12:00,-43.5321,172.6362,8.5,3,1,Good,High,no,\"Strong hive, good entrance activity\"",
            row,
        )
    }
}
