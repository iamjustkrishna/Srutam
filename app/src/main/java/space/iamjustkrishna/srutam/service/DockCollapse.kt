package space.iamjustkrishna.srutam.service

/**
 * When the floating dock's recording controls fold back into the small edge tab by themselves.
 * Recording starts with the dock collapsed (the tab just turns red); someone who taps it to pause or
 * save gets the controls, and they tuck away again after a few quiet seconds, so the dock never
 * stays a wide bar over another app.
 */
object DockCollapse {
    /** How long the controls stay open without a touch. */
    const val IDLE_MS = 5_000L

    fun shouldCollapse(expanded: Boolean, recording: Boolean, msSinceTouch: Long): Boolean =
        expanded && recording && msSinceTouch >= IDLE_MS
}
