package space.iamjustkrishna.srutam.service

/** Plays a recorded clip as if it were the microphone. */
internal class FilePcmSource(
    private val pcm: ShortArray,
    override val sampleRate: Int,
    private val realTime: Boolean
) : PcmSource {
    @Volatile private var active = false
    @Volatile var position = 0
        private set

    val drained: Boolean get() = position >= pcm.size

    override fun start() { active = true }

    override fun stop() { active = false }

    override fun release() {}

    override fun read(buffer: ShortArray): Int {
        if (active && position < pcm.size) {
            val count = minOf(buffer.size, pcm.size - position)
            System.arraycopy(pcm, position, buffer, 0, count)
            position += count
            if (realTime) Thread.sleep(count * 1000L / sampleRate)
            return count
        }
        Thread.sleep(10) // paused, finished or stopped: nothing to deliver
        return 0
    }
}
