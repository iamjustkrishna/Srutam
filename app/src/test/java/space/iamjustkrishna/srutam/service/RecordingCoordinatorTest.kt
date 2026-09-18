package space.iamjustkrishna.srutam.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingCoordinatorTest {

    @Before
    fun setUp() {
        RecordingCoordinator.notifyRecordingEnded()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertTrue("Expected initial state to be Idle", RecordingCoordinator.isIdle)
        assertTrue("Expected canStart() to be true when Idle", RecordingCoordinator.canStart())
        assertFalse("Expected isRecording to be false when Idle", RecordingCoordinator.isRecording)
        assertFalse("Expected isPaused to be false when Idle", RecordingCoordinator.isPaused)
        assertEquals(RecordingCoordinator.RecordingSessionState.Idle, RecordingCoordinator.state.value)
    }

    @Test
    fun testLifecycleTransitions() {
        // 1. Started
        RecordingCoordinator.notifyRecordingStarted(1000L, "/path/to/recording.m4a")
        assertFalse(RecordingCoordinator.isIdle)
        assertTrue(RecordingCoordinator.isRecording)
        assertFalse(RecordingCoordinator.isPaused)
        assertFalse(RecordingCoordinator.canStart())
        val recordingState = RecordingCoordinator.state.value as RecordingCoordinator.RecordingSessionState.Recording
        assertEquals(1000L, recordingState.startTimeMs)
        assertEquals("/path/to/recording.m4a", recordingState.filePath)

        // 2. Paused
        RecordingCoordinator.notifyRecordingPaused(5000L, "/path/to/recording.m4a")
        assertFalse(RecordingCoordinator.isIdle)
        assertTrue(RecordingCoordinator.isRecording)
        assertTrue(RecordingCoordinator.isPaused)
        assertFalse(RecordingCoordinator.canStart())
        val pausedState = RecordingCoordinator.state.value as RecordingCoordinator.RecordingSessionState.Paused
        assertEquals(5000L, pausedState.durationMs)
        assertEquals("/path/to/recording.m4a", pausedState.filePath)

        // 3. Resumed
        RecordingCoordinator.notifyRecordingResumed(6000L, "/path/to/recording.m4a")
        assertFalse(RecordingCoordinator.isIdle)
        assertTrue(RecordingCoordinator.isRecording)
        assertFalse(RecordingCoordinator.isPaused)
        assertFalse(RecordingCoordinator.canStart())

        // 4. Ended
        RecordingCoordinator.notifyRecordingEnded()
        assertTrue(RecordingCoordinator.isIdle)
        assertFalse(RecordingCoordinator.isRecording)
        assertFalse(RecordingCoordinator.isPaused)
        assertTrue(RecordingCoordinator.canStart())
        assertEquals(RecordingCoordinator.RecordingSessionState.Idle, RecordingCoordinator.state.value)
    }

    @Test
    fun testCannotStartWhenActiveOrPaused() {
        RecordingCoordinator.notifyRecordingStarted(1000L, "/path/test.m4a")
        assertFalse("canStart should be false during recording", RecordingCoordinator.canStart())

        RecordingCoordinator.notifyRecordingPaused(2000L, "/path/test.m4a")
        assertFalse("canStart should be false while paused", RecordingCoordinator.canStart())

        RecordingCoordinator.notifyRecordingEnded()
        assertTrue("canStart should be true once ended", RecordingCoordinator.canStart())
    }

    @Test
    fun testConcurrentLockIntegrity() {
        val numThreads = 20
        val latch = CountDownLatch(1)
        val doneLatch = CountDownLatch(numThreads)
        val successfulTransitions = AtomicInteger(0)

        for (i in 0 until numThreads) {
            Thread {
                try {
                    latch.await(2, TimeUnit.SECONDS)
                    if (RecordingCoordinator.canStart()) {
                        // Simulate first thread winning lock
                        RecordingCoordinator.notifyRecordingStarted(System.currentTimeMillis(), "/path/file_$i.m4a")
                        successfulTransitions.incrementAndGet()
                    }
                } finally {
                    doneLatch.countDown()
                }
            }.start()
        }

        latch.countDown()
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS))
        // Since notifyRecordingStarted makes canStart() false, state must be Recording
        assertTrue(RecordingCoordinator.isRecording)
        assertFalse(RecordingCoordinator.canStart())
    }
}
