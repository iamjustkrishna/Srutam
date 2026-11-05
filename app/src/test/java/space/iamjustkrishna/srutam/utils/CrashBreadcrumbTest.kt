package space.iamjustkrishna.srutam.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CrashBreadcrumbTest {

    private fun frame(cls: String, method: String) = StackTraceElement(cls, method, "File.kt", 1)

    @Test fun reportsTheRootCauseAndTheFirstFrameInSrutamsOwnCode() {
        val root = IllegalStateException("secret words the person said")
        root.stackTrace = arrayOf(
            frame("android.os.Handler", "dispatchMessage"),
            frame("space.iamjustkrishna.srutam.service.AiProcessingWorker", "doWork"),
            frame("space.iamjustkrishna.srutam.MainActivity", "onCreate")
        )
        val wrapper = RuntimeException("wrapper", root)

        val (exception, at) = CrashBreadcrumb.describe(wrapper)

        assertEquals("IllegalStateException", exception)
        assertEquals("service.AiProcessingWorker.doWork:1", at)
    }

    @Test fun neverCarriesTheMessageText() {
        val error = IllegalArgumentException("my private note")
        error.stackTrace = arrayOf(frame("space.iamjustkrishna.srutam.ui.Foo", "bar"))

        val (exception, at) = CrashBreadcrumb.describe(error)

        assertFalse(exception.contains("private"))
        assertFalse(at.contains("private"))
    }

    @Test fun aRenamedReleaseFrameIsStillFound() {
        // In a release build the app's classes have short names like "f6.a".
        val error = IllegalStateException("x")
        error.stackTrace = arrayOf(
            frame("android.os.Looper", "loop"),
            frame("f6.a", "b"),
            frame("androidx.compose.runtime.Recomposer", "run")
        )
        assertEquals("IllegalStateException" to "f6.a.b:1", CrashBreadcrumb.describe(error))
    }

    @Test fun aCrashEntirelyOutsideSrutamHasNoLocation() {
        val error = OutOfMemoryError()
        error.stackTrace = arrayOf(frame("java.util.ArrayList", "grow"))
        assertEquals("OutOfMemoryError" to "", CrashBreadcrumb.describe(error))
    }
}
