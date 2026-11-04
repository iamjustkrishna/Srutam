package space.iamjustkrishna.srutam.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * The elapsed time, written along the curved edge of the floating dock's tab while it records with its
 * controls tucked away. The letters follow the tab's rounded side, running down the screen (turned a
 * quarter, tops pointing away from the tab), on no background, so the tab itself stays small.
 *
 * The view is as tall as the pill and 20 dp wider than what shows of it, for the text.
 */
class DockTimeArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** The tab is attached to the left edge of the screen (the text goes on its right) or to the right. */
    var leftDocked = true
        set(value) {
            field = value
            invalidate()
        }

    var text = ""
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Paused: the time stays but dims. */
    var dimmed = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 12f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(4f * density, 0f, 0f, 0xCC000000.toInt())
    }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        if (text.isEmpty()) return
        val centerY = height / 2f
        // Centre of the pill's rounded end: 24 dp in from the screen-edge side of the window, which shows the
        // pill's last 50 dp. The text circles it, a few dp outside the pill's edge.
        val centerX = if (leftDocked) 24f * density else width - 24f * density
        val radius = 26f * density + 5f * density
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // Clockwise: down the right side for a left dock, up the left side for a right dock, so the letters
        // always run along the path with their tops pointing away from the tab.
        val sweep = 100f
        val start = if (leftDocked) -sweep / 2f else 180f - sweep / 2f
        path.reset()
        path.addArc(oval, start, sweep)

        val arcLength = (Math.PI * radius * sweep / 180.0).toFloat()
        val offset = ((arcLength - paint.measureText(text)) / 2f).coerceAtLeast(0f)
        paint.alpha = if (dimmed) 130 else 255
        canvas.drawTextOnPath(text, path, offset, 0f, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Tab (52 dp) plus the text's room: 5 dp gap and about 13 dp of letter height.
        setMeasuredDimension(
            resolveSize((70f * density).toInt(), widthMeasureSpec),
            resolveSize((52f * density).toInt(), heightMeasureSpec)
        )
    }
}
