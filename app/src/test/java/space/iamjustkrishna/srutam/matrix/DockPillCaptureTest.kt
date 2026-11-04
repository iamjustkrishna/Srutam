package space.iamjustkrishna.srutam.matrix

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import space.iamjustkrishna.srutam.R
import java.io.File

/**
 * The dock pill from its real layout, in its real states: tucked away or out, idle or recording, on the
 * left or the right. Mirrors the geometry the service uses (PEEK 50 dp, gap 6 dp).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "xxhdpi", application = Application::class)
class DockPillCaptureTest {

    private val density = 3f
    private fun dp(v: Float) = (v * density).toInt()

    private fun render(name: String, recording: Boolean, out: Boolean, left: Boolean, time: String? = null): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val root = LayoutInflater.from(context).inflate(R.layout.floating_record_button, null) as FrameLayout
        val pill = root.findViewById<View>(R.id.dock_pill)
        val idle = root.findViewById<View>(R.id.pill_idle)
        val rec = root.findViewById<View>(R.id.pill_rec)
        val logo = root.findViewById<View>(R.id.floating_record_button)
        val arc = root.findViewById<space.iamjustkrishna.srutam.service.DockTimeArcView>(R.id.dock_time_arc)
        assertNotNull(pill); assertNotNull(logo); assertNotNull(arc)

        idle.visibility = if (recording) View.GONE else View.VISIBLE
        rec.visibility = if (recording) View.VISIBLE else View.GONE
        pill.layoutDirection = if (left) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL
        (pill.background.mutate() as GradientDrawable).setColor(Color.parseColor(if (recording) "#DC2626" else "#0F172A"))
        (logo.background.mutate() as GradientDrawable).setColor(Color.parseColor(if (recording) "#7F1D1D" else "#334155"))

        // The pill's own width, as the service measures it.
        pill.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.makeMeasureSpec(dp(52f), View.MeasureSpec.EXACTLY))
        val pillWidth = pill.measuredWidth
        pill.layoutParams.width = pillWidth

        val peek = dp(50f); val gap = dp(6f); val arcExtra = dp(20f)
        val showArc = recording && !out && time != null
        val windowWidth = if (out) pillWidth + gap else peek + if (showArc) arcExtra else 0
        (pill.layoutParams as FrameLayout.LayoutParams).gravity = if (left) Gravity.START else Gravity.END
        (arc.layoutParams as FrameLayout.LayoutParams).gravity = if (left) Gravity.START else Gravity.END
        arc.leftDocked = left
        if (showArc) { arc.visibility = View.VISIBLE; arc.text = time!! }
        pill.translationX = if (out) (if (left) gap.toFloat() else -gap.toFloat())
        else (if (left) (peek - pillWidth).toFloat() else (pillWidth - peek).toFloat())

        root.measure(View.MeasureSpec.makeMeasureSpec(windowWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(52f), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // A phone screen strip: the dock's window sits at the edge; the off-screen side is hatched.
        val screenW = dp(330f); val offW = dp(26f); val padY = dp(14f)
        val bmp = Bitmap.createBitmap(screenW + offW * 2, dp(52f) + padY * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#EEF1F6"))
        val line = Paint().apply { color = Color.parseColor("#D3DAE6") }
        for (i in 0 until 4) c.drawRoundRect(dp(130f).toFloat() + offW, padY + dp(6f + i * 12f).toFloat(), offW + dp(130f + 150f - i * 24).toFloat(), padY + dp(10f + i * 12f).toFloat(), 8f, 8f, line)
        val screenLeft = offW.toFloat()
        val windowLeft = if (left) screenLeft else screenLeft + screenW - windowWidth
        c.save()
        c.clipRect(screenLeft, 0f, screenLeft + screenW, bmp.height.toFloat())
        c.translate(windowLeft, padY.toFloat())
        c.clipRect(0, 0, windowWidth, dp(52f))
        root.draw(c)
        c.restore()
        val hatch = Paint().apply { color = Color.parseColor("#20283A") }
        c.drawRect(0f, 0f, screenLeft, bmp.height.toFloat(), hatch)
        c.drawRect(screenLeft + screenW, 0f, bmp.width.toFloat(), bmp.height.toFloat(), hatch)

        val file = File("../screenshots/screen-matrix/dock-pill/$name.png").canonicalFile
        file.parentFile?.mkdirs()
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bmp
    }

    @Test fun layoutInflatesAndTheRecordingPillIsWider() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val root = LayoutInflater.from(context).inflate(R.layout.floating_record_button, null) as FrameLayout
        val pill = root.findViewById<View>(R.id.dock_pill)
        fun width(recording: Boolean): Int {
            root.findViewById<View>(R.id.pill_idle).visibility = if (recording) View.GONE else View.VISIBLE
            root.findViewById<View>(R.id.pill_rec).visibility = if (recording) View.VISIBLE else View.GONE
            pill.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.makeMeasureSpec(dp(52f), View.MeasureSpec.EXACTLY))
            return pill.measuredWidth
        }
        assertTrue(width(recording = true) > width(recording = false))
    }

    @Test fun leftDock() {
        render("left-idle-tucked", recording = false, out = false, left = true)
        render("left-idle-out", recording = false, out = true, left = true)
        render("left-rec-out", recording = true, out = true, left = true)
        render("left-rec-tucked", recording = true, out = false, left = true, time = "1:23")
    }

    @Test fun rightDock() {
        render("right-idle-tucked", recording = false, out = false, left = false)
        render("right-idle-out", recording = false, out = true, left = false)
        render("right-rec-out", recording = true, out = true, left = false)
        render("right-rec-tucked", recording = true, out = false, left = false, time = "1:23")
    }
}
