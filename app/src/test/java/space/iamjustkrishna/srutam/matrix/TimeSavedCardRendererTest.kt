package space.iamjustkrishna.srutam.matrix

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.ui.components.TimeSavedCardData
import space.iamjustkrishna.srutam.ui.components.TimeSavedCardRenderer
import space.iamjustkrishna.srutam.utils.TimePeriod
import java.io.File

/** The share card, drawn for a few totals: sizes, short and long numbers, a big count of words. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TimeSavedCardRendererTest {

    private fun draw(name: String, data: TimeSavedCardData): Bitmap {
        val bmp = TimeSavedCardRenderer.render(ApplicationProvider.getApplicationContext(), data)
        val file = File("../screenshots/screen-matrix/share-card/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    @Test fun hoursAndMinutes() {
        val bmp = draw("hours", TimeSavedCardData("October", TimePeriod(6 * 3600L + 23 * 60, 2 * 3600L + 10 * 60, 7 * 3600L + 40 * 60, 126, 18_400)))
        assertEquals(1080, bmp.width)
        assertEquals(1350, bmp.height)
    }

    @Test fun minutesAndSeconds() {
        draw("minutes", TimeSavedCardData("All time", TimePeriod(12 * 60L + 5, 4 * 60L + 25, 16 * 60L + 30, 3, 660)))
    }

    @Test fun veryLongNumberStillFits() {
        draw("long", TimeSavedCardData("All time", TimePeriod(124 * 3600L + 59 * 60, 40 * 3600L, 165 * 3600L, 1_240, 250_000)))
    }

    @Test fun firstNote() {
        draw("first-note", TimeSavedCardData("My first note", TimePeriod(41, 12, 53, 1, 35)))
    }
}
