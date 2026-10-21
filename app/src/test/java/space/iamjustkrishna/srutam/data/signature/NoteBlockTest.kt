package space.iamjustkrishna.srutam.data.signature

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class NoteBlockTest {

    private lateinit var file: File
    private lateinit var audio: ByteArray

    private fun box(type: String, payload: ByteArray): ByteArray {
        val size = 8 + payload.size
        return byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()) +
            type.toByteArray(Charsets.US_ASCII) + payload
    }

    @Before fun setUp() {
        // A minimal MP4 shape: ftyp, mdat, moov.
        audio = box("ftyp", "M4A isom".toByteArray()) +
            box("mdat", ByteArray(5000) { (it * 7).toByte() }) +
            box("moov", ByteArray(300) { 3 })
        file = File.createTempFile("note-block", ".m4a").apply { writeBytes(audio) }
    }

    @After fun tearDown() {
        file.delete()
    }

    @Test fun aFileWithoutABlockReadsAsNull() {
        assertNull(NoteBlock.read(file))
    }

    @Test fun writeAppendsWithoutTouchingTheAudio() {
        assertTrue(NoteBlock.write(file, """{"transcript":"hello"}"""))

        assertArrayEquals(audio, file.readBytes().copyOfRange(0, audio.size))
        assertEquals("""{"transcript":"hello"}""", NoteBlock.read(file))
    }

    @Test fun audioHashIgnoresTheBlockAndSurvivesRewrites() {
        val before = NoteBlock.audioHash(file)

        NoteBlock.write(file, """{"v":1}""")
        assertEquals(before, NoteBlock.audioHash(file))

        NoteBlock.write(file, """{"v":2,"long":"${"x".repeat(10_000)}"}""")
        assertEquals(before, NoteBlock.audioHash(file))
    }

    @Test fun rewritingReplacesTheBlockInsteadOfStacking() {
        NoteBlock.write(file, """{"v":"${"y".repeat(2_000)}"}""")
        NoteBlock.write(file, """{"v":2}""")

        assertEquals("""{"v":2}""", NoteBlock.read(file))
        assertTrue(file.length() < audio.size + 200)
    }

    @Test fun aBoxAppendedAfterOursIsKeptAndOursIsStillFound() {
        NoteBlock.write(file, """{"v":1}""")
        file.appendBytes(box("free", ByteArray(10)))

        NoteBlock.write(file, """{"v":3}""")

        assertEquals("""{"v":3}""", NoteBlock.read(file))
    }

    @Test fun aCorruptedBlockIsRejected() {
        NoteBlock.write(file, """{"v":1}""")
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes.last() + 1).toByte()
        file.writeBytes(bytes)

        assertNull(NoteBlock.read(file))
    }

    @Test fun stripRestoresTheOriginalFile() {
        NoteBlock.write(file, "{}")

        assertTrue(NoteBlock.strip(file))
        assertArrayEquals(audio, file.readBytes())
    }
}
