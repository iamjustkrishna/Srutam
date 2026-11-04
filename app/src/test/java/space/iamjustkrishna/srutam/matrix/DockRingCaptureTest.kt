package space.iamjustkrishna.srutam.matrix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.service.DockRingDrawable
import java.io.File

/** The timer ring over the red edge tab, at a few moments, for both docking sides. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DockRingCaptureTest {

    private val density = 3f

    private fun render(name: String, leftDocked: Boolean, progress: Float, dimmed: Boolean = false): Bitmap {
        val side = (52 * density).toInt()
        val bmp = Bitmap.createBitmap(side + 40, side + 70, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#E5E7EB"))
        val tab = RectF(20f, 20f, 20f + side, 20f + side)
        val r = 22 * density
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DC2626") }
        val path = android.graphics.Path().apply {
            addRoundRect(tab, if (leftDocked) floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f) else floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r), android.graphics.Path.Direction.CW)
        }
        c.drawPath(path, fill)
        val ring = DockRingDrawable(density).apply {
            this.leftDocked = leftDocked
            this.progress = progress
            this.dimmed = dimmed
            setBounds(tab.left.toInt(), tab.top.toInt(), tab.right.toInt(), tab.bottom.toInt())
        }
        ring.draw(c)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 11 * density; isFakeBoldText = true; textAlign = Paint.Align.CENTER; setShadowLayer(4f, 0f, 0f, 0xCC000000.toInt()) }
        c.drawText("1:23", tab.centerX(), tab.bottom + 14 * density, text)

        val file = File("../screenshots/screen-matrix/dock-ring/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    @Test fun leftDocked() {
        render("left-0", true, 0f)
        render("left-35", true, 0.35f)
        val bmp = render("left-75", true, 0.75f)
        // Somewhere along the outline there is a bright sweep pixel.
        var white = 0
        for (x in 0 until bmp.width) for (y in 0 until bmp.height) if (bmp.getPixel(x, y) == Color.WHITE) white++
        assertTrue(white > 100)
    }

    @Test fun rightDockedAndPaused() {
        render("right-50", false, 0.5f)
        render("left-paused", true, 0.5f, dimmed = true)
    }
}
