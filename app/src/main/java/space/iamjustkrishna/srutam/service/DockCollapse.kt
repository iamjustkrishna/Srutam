package space.iamjustkrishna.srutam.service

/**
 * When the floating dock's open controls tuck back into the small edge tab by themselves, so the
 * dock never stays a wide bar over another app. The menu you open to start a recording waits a bit
 * longer than the controls shown while recording: choosing takes longer than pausing.
 */
object DockCollapse {
    /** How long the recording controls stay open without a touch. */
    const val RECORDING_IDLE_MS = 6_000L

    /** How long the start menu stays open without a touch. */
    const val MENU_IDLE_MS = 10_000L

    fun idleLimit(recording: Boolean): Long = if (recording) RECORDING_IDLE_MS else MENU_IDLE_MS

    fun shouldCollapse(expanded: Boolean, recording: Boolean, msSinceTouch: Long): Boolean =
        expanded && msSinceTouch >= idleLimit(recording)
}
