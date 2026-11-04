package space.iamjustkrishna.srutam.matrix

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.service.DockTimeArcView
import java.io.File

/** The elapsed time curved around the red edge tab, for both docking sides. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi", application = Application::class)
class DockTimeArcCaptureTest {

    private val density = 3f

    private fun render(name: String, leftDocked: Boolean, text: String, dimmed: Boolean = false): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val view = DockTimeArcView(context).apply {
            this.leftDocked = leftDocked
            this.text = text
            this.dimmed = dimmed
        }
        val w = (70 * density).toInt()
        val h = (52 * density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)

        val pad = 30
        val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#9CA3AF"))
        // The red tab, 52 dp, at the screen-edge end of the frame.
        val tabSize = 52 * density
        val tabLeft = if (leftDocked) pad.toFloat() else pad + w - tabSize
        val tab = RectF(tabLeft, pad.toFloat(), tabLeft + tabSize, pad + tabSize)
        val r = 22 * density
        val path = Path().apply {
            addRoundRect(tab, if (leftDocked) floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f) else floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r), Path.Direction.CW)
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DC2626") })
        c.translate(pad.toFloat(), pad.toFloat())
        view.draw(c)

        val file = File("../screenshots/screen-matrix/dock-time-arc/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    @Test fun leftDocked() {
        val bmp = render("left", true, "1:23")
        // Some bright text pixels exist to the right of the tab.
        var white = 0
        for (x in (pad(52f))..bmp.width - 1) for (y in 0 until bmp.height) if (Color.red(bmp.getPixel(x, y)) > 240 && Color.green(bmp.getPixel(x, y)) > 240 && Color.blue(bmp.getPixel(x, y)) > 240) white++
        assertTrue(white > 20)
    }

    private fun pad(dp: Float) = (30 + dp * density).toInt()

    @Test fun rightDockedAndPaused() {
        render("right", false, "12:05")
        render("left-paused", true, "0:42", dimmed = true)
    }
}
