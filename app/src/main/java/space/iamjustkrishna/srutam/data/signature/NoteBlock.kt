package space.iamjustkrishna.srutam.data.signature

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * A note's data stored inside its own .m4a, so a reinstall can restore the note from the audio
 * file alone: offline, and without running AI again.
 *
 * The data goes in a top-level ISO-BMFF `uuid` box appended at the end of the file. Players skip
 * box types they don't know, and appending changes no offsets in `moov`, so the file still plays.
 * A separate sidecar file wouldn't work: after a reinstall Android only lets the app read audio.
 *
 * Box layout (big-endian):
 * ```
 * size:u32 | "uuid" | USER_TYPE (16 bytes) | version:u8 | sha256(gzipped json):32 bytes | gzipped json
 * ```
 * The second hash catches a truncated or corrupted block. The audio hash
 * ([audioHash]) is taken over every byte of the file except this box, so it stays the same
 * however many times the block is rewritten, and through renames.
 */
object NoteBlock {
    private const val VERSION: Int = 1
    private const val HASH_BYTES = 32

    /** Srutam's box id, 16 fixed random bytes (b3c1e7a2-5f4d-4e8a-9c6b-0d2f7a1e9b54). */
    internal val USER_TYPE: ByteArray = byteArrayOf(
        0xb3.toByte(), 0xc1.toByte(), 0xe7.toByte(), 0xa2.toByte(),
        0x5f, 0x4d, 0x4e, 0x8a.toByte(),
        0x9c.toByte(), 0x6b, 0x0d, 0x2f,
        0x7a, 0x1e, 0x9b.toByte(), 0x54
    )

    private val UUID_TYPE = "uuid".toByteArray(Charsets.US_ASCII)
    private val FREE_TYPE = "free".toByteArray(Charsets.US_ASCII)

    /** Where Srutam's box sits in a file: byte offset and total size including its header. */
    data class Location(val offset: Long, val size: Long)

    /**
     * Random access to an audio file, either by path or through a content URI's file descriptor.
     * Files left by a previous install can only be written through their MediaStore URI, once
     * the user has allowed it.
     */
    interface Seekable {
        fun length(): Long
        fun readFully(pos: Long, dst: ByteArray)
        fun write(pos: Long, src: ByteArray)
        fun truncate(size: Long)
    }

    private class RafSeekable(private val raf: RandomAccessFile) : Seekable {
        override fun length() = raf.length()
        override fun readFully(pos: Long, dst: ByteArray) { raf.seek(pos); raf.readFully(dst) }
        override fun write(pos: Long, src: ByteArray) { raf.seek(pos); raf.write(src) }
        override fun truncate(size: Long) = raf.setLength(size)
    }

    /**
     * Channels over one read-write file descriptor, for example from
     * `ContentResolver.openFileDescriptor(uri, "rw")`: [reader] from a FileInputStream and
     * [writer] from a FileOutputStream, since each of those channels only goes one way.
     */
    class ChannelSeekable(private val reader: FileChannel, private val writer: FileChannel) : Seekable {
        override fun length() = reader.size()
        override fun readFully(pos: Long, dst: ByteArray) {
            val buffer = ByteBuffer.wrap(dst)
            var at = pos
            while (buffer.hasRemaining()) {
                val read = reader.read(buffer, at)
                if (read < 0) throw EOFException()
                at += read
            }
        }
        override fun write(pos: Long, src: ByteArray) {
            val buffer = ByteBuffer.wrap(src)
            var at = pos
            while (buffer.hasRemaining()) at += writer.write(buffer, at)
        }
        override fun truncate(size: Long) { writer.truncate(size) }
    }

    /** Finds Srutam's box among the file's top-level boxes, or null. Stops at the first malformed box. */
    fun locate(raf: RandomAccessFile): Location? = locate(RafSeekable(raf))

    fun locate(io: Seekable): Location? {
        val length = io.length()
        var pos = 0L
        var found: Location? = null
        val header8 = ByteArray(8)
        val large = ByteArray(8)
        val userType = ByteArray(16)
        while (pos + 8 <= length) {
            io.readFully(pos, header8)
            var size = ByteBuffer.wrap(header8, 0, 4).int.toLong() and 0xFFFFFFFFL
            val type = header8.copyOfRange(4, 8)
            var header = 8L
            if (size == 1L) {
                if (pos + 16 > length) break
                io.readFully(pos + 8, large)
                size = ByteBuffer.wrap(large).long
                header = 16L
            } else if (size == 0L) {
                size = length - pos
            }
            if (size < header || pos + size > length) break
            if (type.contentEquals(UUID_TYPE) && size >= header + 16) {
                io.readFully(pos + header, userType)
                if (userType.contentEquals(USER_TYPE)) found = Location(pos, size)
            }
            pos += size
        }
        return found
    }

    /** SHA-256 (hex) of the file's bytes, skipping Srutam's box. */
    fun audioHash(file: File): String = RandomAccessFile(file, "r").use { raf ->
        val skip = locate(raf)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        fun hashRange(from: Long, to: Long) {
            raf.seek(from)
            var remaining = to - from
            while (remaining > 0) {
                val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }
        if (skip == null) {
            hashRange(0, raf.length())
        } else {
            hashRange(0, skip.offset)
            hashRange(skip.offset + skip.size, raf.length())
        }
        digest.digest().toHex()
    }

    /** The JSON stored in [file], or null when there is none or it fails its integrity check. */
    fun read(file: File): String? = try {
        RandomAccessFile(file, "r").use { raf ->
            val location = locate(raf) ?: return null
            val header = 8L + 16L
            val bodySize = location.size - header
            if (bodySize < 1 + HASH_BYTES || bodySize > Int.MAX_VALUE) return null
            raf.seek(location.offset + header)
            val body = ByteArray(bodySize.toInt())
            raf.readFully(body)
            decodeBody(body)
        }
    } catch (e: IOException) {
        null
    }

    /**
     * Stores [json] in [file], replacing any earlier block. Returns false when the file can't be
     * written, for example an audio file left by a previous install, which this one can only read.
     */
    fun write(file: File, json: String): Boolean = try {
        RandomAccessFile(file, "rw").use { raf -> write(RafSeekable(raf), json) }
        true
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    /** Stores [json] through [io], replacing any earlier block. Throws on I/O failure. */
    fun write(io: Seekable, json: String) {
        val existing = locate(io)
        if (existing != null) {
            if (existing.offset + existing.size == io.length()) {
                io.truncate(existing.offset)
            } else {
                // Something was appended after our box: retire it in place as a "free" box.
                io.write(existing.offset + 4, FREE_TYPE)
            }
        }
        io.write(io.length(), encodeBox(json))
    }

    /** Removes Srutam's box, for example before sharing the audio. Returns false if the file can't be written. */
    fun strip(file: File): Boolean = try {
        RandomAccessFile(file, "rw").use { raf ->
            val existing = locate(raf) ?: return true
            if (existing.offset + existing.size == raf.length()) {
                raf.setLength(existing.offset)
            } else {
                raf.seek(existing.offset + 4)
                raf.write(FREE_TYPE)
            }
        }
        true
    } catch (e: IOException) {
        false
    }

    internal fun encodeBox(json: String): ByteArray {
        val gzipped = ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        }.toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(gzipped)
        val size = 8 + 16 + 1 + HASH_BYTES + gzipped.size
        val out = ByteArrayOutputStream(size)
        out.write(byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()))
        out.write(UUID_TYPE)
        out.write(USER_TYPE)
        out.write(VERSION)
        out.write(hash)
        out.write(gzipped)
        return out.toByteArray()
    }

    internal fun decodeBody(body: ByteArray): String? {
        if (body[0].toInt() != VERSION) return null
        val stored = body.copyOfRange(1, 1 + HASH_BYTES)
        val gzipped = body.copyOfRange(1 + HASH_BYTES, body.size)
        if (!MessageDigest.getInstance("SHA-256").digest(gzipped).contentEquals(stored)) return null
        return try {
            GZIPInputStream(ByteArrayInputStream(gzipped)).use { it.readBytes() }.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            null
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
