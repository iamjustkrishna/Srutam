package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFileNamesTest {
    private val existing = listOf("abc.m4a", "Standup.m4a", "recording_1.m4a")

    @Test fun anUnusedNameIsFine() {
        assertNull(RecordingFileNames.validate("Weekly sync", existing, ownFileName = "recording_1.m4a"))
    }

    @Test fun aNameAnotherNoteHasIsRefusedWithAMessage() {
        val error = RecordingFileNames.validate("abc", existing, ownFileName = "recording_1.m4a")

        assertEquals("A note named \"abc\" already exists", error)
    }

    @Test fun theClashIgnoresCase() {
        assertNotNull(RecordingFileNames.validate("ABC", existing, ownFileName = "recording_1.m4a"))
        assertNotNull(RecordingFileNames.validate("standup", existing, ownFileName = "recording_1.m4a"))
    }

    @Test fun aTypedExtensionIsNotCountedAsPartOfTheName() {
        assertNotNull(RecordingFileNames.validate("abc.m4a", existing, ownFileName = "recording_1.m4a"))
        assertEquals("abc", RecordingFileNames.titleOf("  abc.m4a "))
        assertEquals("abc.m4a", RecordingFileNames.fileNameFor("abc"))
    }

    @Test fun keepingOrReCasingYourOwnNameIsNotAClash() {
        assertNull(RecordingFileNames.validate("abc", existing, ownFileName = "abc.m4a"))
        assertNull(RecordingFileNames.validate("ABC", existing, ownFileName = "abc.m4a"))
    }

    @Test fun blankNamesAreRefused() {
        assertEquals("Enter a name", RecordingFileNames.validate("", existing, null))
        assertEquals("Enter a name", RecordingFileNames.validate("   ", existing, null))
        assertEquals("Enter a name", RecordingFileNames.validate(".m4a", existing, null))
        assertEquals("Enter a name", RecordingFileNames.validate("..", existing, null))
    }

    @Test fun charactersThatCannotBeInAFileNameAreRefused() {
        for (name in listOf("a/b", "a\\b", "a:b", "a*b", "a?b", "a\"b", "a<b", "a>b", "a|b")) {
            val error = RecordingFileNames.validate(name, existing, null)
            assertNotNull("$name should be refused", error)
            assertTrue(error!!.startsWith("These characters can't be used"))
        }
    }

    @Test fun veryLongNamesAreRefused() {
        assertNotNull(RecordingFileNames.validate("x".repeat(121), existing, null))
        assertNull(RecordingFileNames.validate("x".repeat(120), existing, null))
    }
}
