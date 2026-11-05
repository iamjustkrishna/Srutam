package space.iamjustkrishna.srutam.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockCollapseTest {

    @Test fun recordingControlsTuckAwayAfterSixQuietSeconds() {
        assertFalse(DockCollapse.shouldCollapse(expanded = true, recording = true, msSinceTouch = 5_999))
        assertTrue(DockCollapse.shouldCollapse(expanded = true, recording = true, msSinceTouch = 6_000))
    }

    @Test fun theStartMenuWaitsLongerThanTheRecordingControls() {
        assertEquals(10_000L, DockCollapse.idleLimit(recording = false))
        assertFalse(DockCollapse.shouldCollapse(expanded = true, recording = false, msSinceTouch = 9_999))
        assertTrue(DockCollapse.shouldCollapse(expanded = true, recording = false, msSinceTouch = 10_000))
        assertTrue(DockCollapse.idleLimit(recording = false) > DockCollapse.idleLimit(recording = true))
    }

    @Test fun aCollapsedDockHasNothingToTuckAway() {
        assertFalse(DockCollapse.shouldCollapse(expanded = false, recording = true, msSinceTouch = 60_000))
        assertFalse(DockCollapse.shouldCollapse(expanded = false, recording = false, msSinceTouch = 60_000))
    }
}
