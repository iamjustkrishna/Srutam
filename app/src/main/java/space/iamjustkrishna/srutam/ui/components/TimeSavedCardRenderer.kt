package space.iamjustkrishna.srutam.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.iamjustkrishna.srutam.R
import space.iamjustkrishna.srutam.utils.ShareLink
import space.iamjustkrishna.srutam.utils.TimePeriod
import space.iamjustkrishna.srutam.utils.TimeSaved
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** What the share card says. Only totals: never a note's content. */
data class TimeSavedCardData(
    /** The small label top right: a month name, "All time" or "My first note". */
    val label: String,
    val period: TimePeriod
) {
    companion object {
        /** The month's card, or all time early in a month (the same rule as the header line). */
        fun forTotals(totals: TimeSaved.Totals, month: TimePeriod, allTime: TimePeriod, today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): TimeSavedCardData =
            if (totals.headerIsThisMonth) {
                TimeSavedCardData(today.month.getDisplayName(TextStyle.FULL, locale), month)
            } else {
                TimeSavedCardData("All time", allTime)
            }
    }
}

/**
 * The picture people post: a big number, one plain line, how it was counted (typing against
 * saying), and the app's name. Drawn straight onto a 1080 x 1350 canvas so it looks the same on
 * every phone and doesn't depend on the screen, the theme or the font size.
 */
object TimeSavedCardRenderer {
    const val WIDTH = 1080
    const val HEIGHT = 1350

    private const val VOID = 0xFF050714.toInt()
    private const val COBALT = 0xFF2563EB.toInt()
    private const val GLOW = 0xFF5B93FF.toInt()
    private const val MIST = 0xFF93A3C7.toInt()
    private const val SLATE = 0xFF46568A.toInt()
    private const val PAD = 96f

    private fun serif(context: Context): Typeface {
        val base = ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.SERIF
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Typeface.create(base, 800, false) else Typeface.create(base, Typeface.BOLD)
    }

    private fun paint(color: Int, size: Float, face: Typeface, spacing: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = face
        letterSpacing = spacing
        // Lining figures read as numbers; the font's default old-style ones drop below the line.
        fontFeatureSettings = "lnum"
    }

    fun render(context: Context, data: TimeSavedCardData): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val serif = serif(context)
        val sans = Typeface.create("sans-serif", Typeface.NORMAL)
        val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val sansBold = Typeface.create("sans-serif", Typeface.BOLD)
        val sansLight = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val p = data.period

        // Background: deep navy with a cobalt glow behind the number and a faint ring.
        c.drawColor(VOID)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(540f, 470f, 760f, intArrayOf(0x8C2563EB.toInt(), 0x292563EB, 0x00050714), floatArrayOf(0f, 0.38f, 0.66f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glow)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
        listOf(380f to 0x295B93FF, 470f to 0x0A5B93FF, 560f to 0x085B93FF).forEach { (r, col) ->
            ring.color = col
            c.drawCircle(1000f, 560f, r, ring)
        }

        // Brand row.
        c.drawText("Srutam", PAD, 140f, paint(Color.WHITE, 56f, serif, -0.01f))
        val when_ = paint(MIST, 32f, sansMedium, 0.16f)
        val label = data.label.uppercase(Locale.getDefault())
        c.drawText(label, WIDTH - PAD - when_.measureText(label), 138f, when_)

        // "I saved", then the number: digits big, units small and blue.
        c.drawText("I SAVED", PAD, 262f, paint(GLOW, 40f, sansBold, 0.2f))
        drawHero(c, TimeSaved.hero(p.savedSeconds), serif, startX = PAD, baseline = 548f, maxWidth = WIDTH - 2 * PAD)
        c.drawText("by talking instead of typing.", PAD, 640f, paint(Color.WHITE, 52f, sansLight))

        // Typing against saying.
        val say = if (p.typingSeconds > 0) (p.spokenSeconds.toFloat() / p.typingSeconds).coerceIn(0.04f, 1f) else 1f
        compareRow(c, "Typing it", "~" + TimeSaved.hero(p.typingSeconds), 770f, 1f, sans, sansBold, solid = SLATE)
        compareRow(c, "Saying it", TimeSaved.hero(p.spokenSeconds), 900f, say, sans, sansBold, solid = null)

        // Two quiet facts.
        val n = paint(Color.WHITE, 84f, serif)
        val l = paint(MIST, 30f, sans)
        c.drawText(p.notes.toString(), PAD, 1086f, n)
        c.drawText("notes captured", PAD, 1130f, l)
        val wordsX = PAD + 330f
        c.drawText(compact(p.words), wordsX, 1086f, n)
        c.drawText("words spoken", wordsX, 1130f, l)

        // Footer.
        val line = Paint().apply { color = 0x1FFFFFFF; strokeWidth = 2f }
        c.drawLine(PAD, 1190f, WIDTH - PAD, 1190f, line)
        c.drawText("Made with Srutam", PAD, 1252f, paint(Color.WHITE, 38f, sansBold))
        c.drawText("Voice notes that become tasks", PAD, 1298f, paint(MIST, 32f, sans))
        val play = paint(GLOW, 32f, sansMedium)
        val free = "Free on Google Play"
        c.drawText(free, WIDTH - PAD - play.measureText(free), 1298f, play)
        return bmp
    }

    private fun compact(words: Int): String = if (words >= 10_000) "${words / 1000}k" else words.toString()

    /** "6h 23m": figures at [136 / 300] of the digits' size in the accent colour, squeezed to fit [maxWidth]. */
    private fun drawHero(c: Canvas, hero: String, face: Typeface, startX: Float, baseline: Float, maxWidth: Float) {
        val tokens = Regex("\\d+|[^\\d\\s]+").findAll(hero).map { it.value }.toList()
        var digitSize = 300f
        fun widths(size: Float): Float {
            val unit = size * 136f / 300f
            val dp = paint(Color.WHITE, size, face, -0.023f)
            val up = paint(GLOW, unit, face)
            return tokens.sumOf { t -> (if (t[0].isDigit()) dp.measureText(t) else up.measureText(t) + 26f).toDouble() }.toFloat()
        }
        while (digitSize > 120f && widths(digitSize) > maxWidth) digitSize -= 6f
        val unitSize = digitSize * 136f / 300f
        val digits = paint(Color.WHITE, digitSize, face, -0.023f)
        val units = paint(GLOW, unitSize, face)
        var x = startX
        tokens.forEach { t ->
            if (t[0].isDigit()) {
                c.drawText(t, x, baseline, digits)
                x += digits.measureText(t)
            } else {
                x += 8f
                c.drawText(t, x, baseline, units)
                x += units.measureText(t) + 18f
            }
        }
    }

    private fun compareRow(c: Canvas, label: String, value: String, baseline: Float, share: Float, sans: Typeface, bold: Typeface, solid: Int?) {
        c.drawText(label, PAD, baseline, paint(MIST, 36f, sans))
        val v = paint(Color.WHITE, 44f, bold)
        c.drawText(value, WIDTH - PAD - v.measureText(value), baseline, v)
        val top = baseline + 26f
        val full = RectF(PAD, top, WIDTH - PAD, top + 34f)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF }
        c.drawRoundRect(full, 17f, 17f, track)
        val fill = RectF(PAD, top, PAD + full.width() * share, top + 34f)
        val f = Paint(Paint.ANTI_ALIAS_FLAG)
        if (solid != null) {
            f.color = solid
        } else {
            f.shader = LinearGradient(fill.left, 0f, fill.right, 0f, COBALT, GLOW, Shader.TileMode.CLAMP)
            val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x665B93FF; maskFilter = android.graphics.BlurMaskFilter(24f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
            c.drawRoundRect(fill, 17f, 17f, halo)
        }
        c.drawRoundRect(fill, 17f, 17f, f)
    }

    /**
     * Draws the card, saves it and opens the share sheet with a caption and the tagged link.
     * Call from a coroutine: the drawing and the write run off the main thread.
     */
    suspend fun share(context: Context, data: TimeSavedCardData, campaign: String = "time_saved") {
        val uri = withContext(Dispatchers.Default) {
            val bitmap = render(context, data)
            val dir = File(context.cacheDir, "shared_cards").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
            val file = File(dir, "srutam-time-saved.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, ShareLink.caption(data.period.savedSeconds, campaign))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your time saved").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
