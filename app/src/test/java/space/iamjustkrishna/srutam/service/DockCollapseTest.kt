package space.iamjustkrishna.srutam.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockCollapseTest {

    @Test fun openRecordingControlsFoldAwayAfterFiveQuietSeconds() {
        assertFalse(DockCollapse.shouldCollapse(expanded = true, recording = true, msSinceTouch = 4_999))
        assertTrue(DockCollapse.shouldCollapse(expanded = true, recording = true, msSinceTouch = 5_000))
    }

    @Test fun aCollapsedDockHasNothingToFold() {
        assertFalse(DockCollapse.shouldCollapse(expanded = false, recording = true, msSinceTouch = 60_000))
    }

    @Test fun theIdleMenuStaysOpenUntilYouCloseIt() {
        // Not recording: someone is choosing what to do, so it doesn't close by itself.
        assertFalse(DockCollapse.shouldCollapse(expanded = true, recording = false, msSinceTouch = 60_000))
    }
}
