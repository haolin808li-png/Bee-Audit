package nz.beeaudit.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AuditFileNamesTest {
    @Test
    fun photoNameUsesSortableSequenceAndPhotoNumbers() {
        assertEquals(
            "KPIN1234_Hive_0007_03.jpg",
            AuditFileNames.photoName(
                pin = "KPIN1234",
                group = "Hive",
                sequenceNumber = 7,
                photoNumber = 3,
            ),
        )
    }

    @Test
    fun unsafeCharactersAreCleanedForStorage() {
        assertEquals(
            "KPIN_12_Block_A_0001_01.m4a",
            AuditFileNames.voiceName(
                pin = "KPIN 12",
                group = "Block/A",
                sequenceNumber = 1,
                noteNumber = 1,
            ),
        )
    }
}
