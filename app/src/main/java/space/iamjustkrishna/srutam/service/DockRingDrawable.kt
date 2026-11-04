package space.iamjustkrishna.srutam.service

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * The timer ring on the floating dock's edge tab while it records: a line that traces the tab's
 * outline once a minute, over a faint full outline. The elapsed time is written next to it by the
 * service, on no background, so the tab itself stays small.
 */
class DockRingDrawable(private val density: Float) : Drawable() {

    /** Which screen edge the tab is attached to; its rounded side is the other one. */
    var leftDocked = true
        set(value) {
            field = value
            invalidateSelf()
        }

    /** 0 to 1 around the outline. */
    var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidateSelf()
        }

    /** Paused: the ring stays but fades. */
    var dimmed = false
        set(value) {
            field = value
            invalidateSelf()
        }

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x40FFFFFF
    }
    private val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFFFFF.toInt()
    }
    private val path = Path()
    private val segment = Path()

    override fun draw(canvas: Canvas) {
        val width = 3f * density
        track.strokeWidth = width
        sweep.strokeWidth = width
        sweep.alpha = if (dimmed) 110 else 255

        val b = bounds
        val inset = width / 2f + 1f * density
        val rect = RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset)
        val r = 21f * density
        val l = rect.left
        val t = rect.top
        val rt = rect.right
        val bt = rect.bottom

        // The side against the screen edge is square (and mostly off screen), the other is round, like the
        // tab. The line starts where the round side starts, so the first seconds of a minute show at once
        // and the last stretch of it runs along the hidden edge.
        path.reset()
        if (leftDocked) {
            path.moveTo(l, t)
            path.lineTo(rt - r, t)
            path.arcTo(RectF(rt - 2 * r, t, rt, t + 2 * r), -90f, 90f)
            path.lineTo(rt, bt - r)
            path.arcTo(RectF(rt - 2 * r, bt - 2 * r, rt, bt), 0f, 90f)
            path.lineTo(l, bt)
            path.lineTo(l, t)
        } else {
            path.moveTo(rt, t)
            path.lineTo(l + r, t)
            path.arcTo(RectF(l, t, l + 2 * r, t + 2 * r), -90f, -90f)
            path.lineTo(l, bt - r)
            path.arcTo(RectF(l, bt - 2 * r, l + 2 * r, bt), 180f, -90f)
            path.lineTo(rt, bt)
            path.lineTo(rt, t)
        }
        canvas.drawPath(path, track)

        if (progress > 0f) {
            val measure = PathMeasure(path, false)
            segment.reset()
            measure.getSegment(0f, measure.length * progress, segment, true)
            canvas.drawPath(segment, sweep)
        }
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
