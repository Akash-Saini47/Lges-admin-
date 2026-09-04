package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.example.R
import com.example.database.Certificate

/**
 * CertificateDrawer
 * ------------------------------------------------------------------
 * Renders the LGES certificate bitmap (2400 x 1600, landscape).
 *
 * Refactored Features:
 *  1. Outer Golden Frame: Spans the entire canvas (from 40f to W-40f, 40f to H-40f),
 *     unifying both the left navy sidebar strip and right content area in a single double-border frame.
 *  2. Left Sidebar Curve: Smooth, symmetric Bezier outward sweep across the middle height.
 *  3. Asset & Vector Icons: Decodes R.drawable.img_lges_logo cleanly with a ContextCompat fallback,
 *     and draws sharp vector line-art icons for the details grid.
 */
data class CertificateData(
    val rollNo: String,
    val studentName: String,
    val guardian: String,
    val course: String,
    val session: String,
    val grade: String,
    val runBy: String,
    val duration: String,
    val dateOfIssue: String,
    val placeOfIssue: String,
    val website: String,
)

object CertificateDrawer {

    // ---------------- canvas metrics ----------------
    private const val W = 2400
    private const val H = 1600

    // ---------------- sidebar geometry ----------------
    private const val SB_TOP_X = 660f       // curve top anchor
    private const val SB_CP1_X = 820f       // top control point (outward sweep)
    private const val SB_CP1_Y = H * 0.35f  // ~560f
    private const val SB_CP2_X = 740f       // lower control point
    private const val SB_CP2_Y = H * 0.65f  // ~1040f
    private const val SB_END_X = 580f       // curve bottom anchor
    private const val SB_CENTER = 330f      // horizontal centre for sidebar content

    // ---------------- body frame ----------------
    private const val BODY_LEFT = 800f
    private const val BODY_RIGHT = 2320f
    private const val BODY_CX = (BODY_LEFT + BODY_RIGHT) / 2f

    // ---------------- vertical rhythm (single source of truth) ------------
    private const val Y_HEADER = 214f
    private const val Y_HEADER_RULE = 260f
    private const val Y_SUB_1 = 322f
    private const val Y_SUB_2 = 368f
    private const val Y_SUB_3 = 406f
    private const val Y_ROLL = 462f
    private const val Y_AWARDED = 512f
    private const val Y_NAME = 606f
    private const val Y_NAME_RULE = 648f
    private const val Y_GUARDIAN = 704f
    private const val Y_RECOGNITION = 764f
    private const val Y_COURSE = 856f
    private const val Y_INSTITUTION = 934f
    private const val Y_BADGE_TOP = 972f
    private const val BADGE_H = 92f
    private const val Y_GRID_TOP = 1120f
    private const val Y_GRID_BOTTOM = 1476f
    private const val GRID_RIGHT = 1740f
    private const val Y_SIGN_RULE = 1404f

    // ---------------- palette ----------------
    private const val NAVY = 0xFF0B1B3D.toInt()
    private const val GOLD = 0xFFD4AF37.toInt()
    private const val GOLD_SOFT = 0xFFC5A880.toInt()
    private const val CREAM = 0xFFFAF6EC.toInt()
    private const val PARCHMENT = 0xFFF3EBD8.toInt()
    private const val MAROON = 0xFF990000.toInt()
    private const val TEAL = 0xFF0F624C.toInt()
    private const val INK = 0xFF1E1E1E.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    // ---------------- typefaces ----------------
    private val SERIF = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    private val SERIF_BOLD = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val SERIF_ITALIC = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private val SANS = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    // ==================================================================
    // PUBLIC API
    // ==================================================================
    fun drawCertificate(context: Context, cert: Certificate, qrBitmap: Bitmap?): Bitmap {
        val certData = CertificateData(
            rollNo = cert.rollNo,
            studentName = cert.studentName,
            guardian = cert.fatherName,
            course = cert.courseName,
            session = cert.sessionRange,
            grade = cert.grade,
            runBy = "LAKSHMI GROUP OF EDUCATION SOCIETY",
            duration = cert.duration,
            dateOfIssue = cert.dateOfIssue,
            placeOfIssue = cert.placeOfIssue,
            website = "https://lges-computer-classes.netlify.app/"
        )
        return draw(context, certData, qrBitmap)
    }

    fun draw(context: Context, cert: CertificateData, qr: Bitmap? = null): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(CREAM)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            style = Paint.Style.FILL
        }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // 1. Navy sidebar strip
        drawSidebar(c, fill, line)

        // 2. Sidebar logo & brand text
        drawSidebarBrand(c, context, fill, line)

        // 3. Outer golden frame (spans entire canvas width & height)
        drawOuterBorder(c, line)

        // 4. Content area
        drawHeader(c, fill, line)
        drawSubtitle(c, fill)
        drawAwardBlock(c, fill, line, cert)
        drawBadgeBox(c, fill, line, cert)
        drawDetailsGrid(c, fill, line, cert)
        drawQr(c, fill, line, qr)
        drawSignatureBlock(c, fill, line, cert)

        return bmp
    }

    /** Shrinks textSize until [text] fits [maxWidth]; returns the size applied. */
    private fun fitTextSize(p: Paint, text: String, maxWidth: Float, preferred: Float, min: Float = 20f): Float {
        var size = preferred
        p.textSize = size
        while (size > min && p.measureText(text) > maxWidth) {
            size -= 1f
            p.textSize = size
        }
        return size
    }

    // ==================================================================
    // OUTER BORDER (Entire Canvas Frame)
    // ==================================================================
    private fun drawOuterBorder(c: Canvas, line: Paint) {
        val l = 40f
        val t = 40f
        val r = W - 40f
        val b = H - 40f

        // Outer primary gold border
        line.color = GOLD
        line.strokeWidth = 4f
        c.drawRect(l, t, r, b, line)

        // Inner soft gold border
        line.color = GOLD_SOFT
        line.strokeWidth = 2f
        c.drawRect(l + 14f, t + 14f, r - 14f, b - 14f, line)

        // Corner accents
        fun corner(x: Float, y: Float, sx: Int, sy: Int) {
            val len = 60f
            line.color = GOLD
            line.strokeWidth = 5f
            c.drawLine(x, y + sy * len, x, y, line)
            c.drawLine(x, y, x + sx * len, y, line)

            line.color = GOLD_SOFT
            line.strokeWidth = 2f
            c.drawLine(x + sx * 14f, y + sy * 14f + sy * len * 0.6f, x + sx * 14f, y + sy * 14f, line)
            c.drawLine(x + sx * 14f, y + sy * 14f, x + sx * 14f + sx * len * 0.6f, y + sy * 14f, line)
        }
        corner(l, t, 1, 1)
        corner(r, t, -1, 1)
        corner(l, b, 1, -1)
        corner(r, b, -1, -1)
    }

    // ==================================================================
    // SIDEBAR & BEZIER CURVE
    // ==================================================================
    private fun drawSidebar(c: Canvas, fill: Paint, line: Paint) {
        // Filled navy background with smooth outward Bezier curve on the right
        val sidebarPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(SB_TOP_X, 0f)
            cubicTo(SB_CP1_X, SB_CP1_Y, SB_CP2_X, SB_CP2_Y, SB_END_X, H.toFloat())
            lineTo(0f, H.toFloat())
            close()
        }
        fill.color = NAVY
        c.drawPath(sidebarPath, fill)

        // Gold Bezier curve borders
        val goldCurvePath = Path().apply {
            moveTo(SB_TOP_X, 0f)
            cubicTo(SB_CP1_X, SB_CP1_Y, SB_CP2_X, SB_CP2_Y, SB_END_X, H.toFloat())
        }
        line.color = GOLD
        line.strokeWidth = 6f
        c.drawPath(goldCurvePath, line)

        // Inner soft gold accent curve
        val goldInnerPath = Path().apply {
            moveTo(SB_TOP_X - 16f, 0f)
            cubicTo(SB_CP1_X - 16f, SB_CP1_Y, SB_CP2_X - 16f, SB_CP2_Y, SB_END_X - 16f, H.toFloat())
        }
        line.color = GOLD_SOFT
        line.strokeWidth = 3f
        c.drawPath(goldInnerPath, line)
    }

    /** Logo card + logo drawable + wordmark + motto, all inside the navy sidebar. */
    private fun drawSidebarBrand(c: Canvas, context: Context, fill: Paint, line: Paint) {
        val cardW = 360f
        val cardH = 360f
        val card = RectF(
            SB_CENTER - cardW / 2f, 180f,
            SB_CENTER + cardW / 2f, 180f + cardH
        )

        fill.color = WHITE
        c.drawRoundRect(card, 40f, 40f, fill)
        line.color = GOLD
        line.strokeWidth = 4f
        c.drawRoundRect(card, 40f, 40f, line)

        val inset = 30f
        val drawBounds = Rect(
            (card.left + inset).toInt(),
            (card.top + inset).toInt(),
            (card.right - inset).toInt(),
            (card.bottom - inset).toInt()
        )

        val drawable = try {
            ContextCompat.getDrawable(context, R.drawable.img_lges_logo)
        } catch (t: Throwable) {
            null
        }

        if (drawable != null) {
            drawable.setBounds(drawBounds)
            drawable.draw(c)
        } else {
            drawLogoFallback(c, fill, line, card)
        }

        // wordmark
        fill.color = GOLD
        fill.typeface = SERIF_BOLD
        fill.textSize = 140f
        fill.textAlign = Align.CENTER
        c.drawText("LGES", SB_CENTER, card.bottom + 170f, fill)

        // dotted rule
        val ruleY = card.bottom + 220f
        line.color = GOLD_SOFT
        line.strokeWidth = 3f
        c.drawLine(SB_CENTER - 150f, ruleY, SB_CENTER - 20f, ruleY, line)
        c.drawLine(SB_CENTER + 20f, ruleY, SB_CENTER + 150f, ruleY, line)
        fill.color = GOLD
        c.drawCircle(SB_CENTER, ruleY, 6f, fill)

        // motto
        fill.typeface = SERIF_ITALIC
        fill.color = GOLD_SOFT
        fill.textSize = 36f
        c.drawText("Empowering Learners,", SB_CENTER, ruleY + 90f, fill)
        c.drawText("Enriching Futures.", SB_CENTER, ruleY + 140f, fill)
    }

    /** Decodes logo resource (vector or raster) safely into a Bitmap. */
    private fun loadLogo(context: Context): Bitmap? = try {
        val drawable = ContextCompat.getDrawable(context, R.drawable.img_lges_logo)
        if (drawable != null) {
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bitmap
        } else {
            null
        }
    } catch (t: Throwable) {
        null
    }

    /** Draws [logo] centred inside [target], preserving its aspect ratio. */
    private fun drawLogoWithAspectRatio(
        c: Canvas,
        logo: Bitmap,
        target: RectF,
        inset: Float,
    ) {
        if (logo.width <= 0 || logo.height <= 0) return
        val availW = target.width() - inset * 2f
        val availH = target.height() - inset * 2f
        if (availW <= 0f || availH <= 0f) return

        val scale = minOf(availW / logo.width, availH / logo.height)
        val dw = logo.width * scale
        val dh = logo.height * scale
        val dst = RectF(
            target.centerX() - dw / 2f,
            target.centerY() - dh / 2f,
            target.centerX() + dw / 2f,
            target.centerY() + dh / 2f
        )

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        c.drawBitmap(logo, null, dst, bitmapPaint)
    }

    /** Vector monogram fallback if logo resource cannot be decoded. */
    private fun drawLogoFallback(c: Canvas, fill: Paint, line: Paint, card: RectF) {
        val cx = card.centerX()
        val cy = card.centerY()
        line.color = NAVY
        line.strokeWidth = 6f
        c.drawCircle(cx, cy, card.width() * 0.33f, line)
        line.color = GOLD
        line.strokeWidth = 3f
        c.drawCircle(cx, cy, card.width() * 0.27f, line)

        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textAlign = Align.CENTER
        fill.textSize = 110f
        c.drawText("LG", cx, cy + 38f, fill)
    }

    // ==================================================================
    // HEADER
    // ==================================================================
    private fun drawHeader(c: Canvas, fill: Paint, line: Paint) {
        val big = 132f
        val tm = 36f
        fill.typeface = SERIF_BOLD
        fill.textAlign = Align.LEFT

        fill.textSize = big
        val lgesW = fill.measureText("LGES")
        val certW = fill.measureText("CERTIFICATE")
        fill.textSize = tm
        val tmW = fill.measureText("TM")

        val gap = 44f
        val startX = BODY_CX - (lgesW + tmW + gap + certW) / 2f

        fill.color = MAROON
        fill.textSize = big
        c.drawText("LGES", startX, Y_HEADER, fill)

        fill.textSize = tm
        c.drawText("TM", startX + lgesW + 8f, Y_HEADER - big * 0.62f, fill)

        fill.color = NAVY
        fill.textSize = big
        c.drawText("CERTIFICATE", startX + lgesW + tmW + gap, Y_HEADER, fill)

        drawFlourishRule(c, fill, line, BODY_CX, Y_HEADER_RULE, 250f)
    }

    private fun drawSubtitle(c: Canvas, fill: Paint) {
        fill.textAlign = Align.CENTER
        fill.color = NAVY

        fill.typeface = SERIF_ITALIC
        fill.textSize = 30f
        c.drawText("(A NATIONAL COMPUTER LITERACY PROGRAMME)", BODY_CX, Y_SUB_1, fill)

        fill.typeface = SERIF
        fill.textSize = 26f
        c.drawText("An Autonomous Institution Registered Under the Society & C.R. Act", BODY_CX, Y_SUB_2, fill)
        c.drawText("Ministry of HRD, Govt. of India, NCT, New Delhi", BODY_CX, Y_SUB_3, fill)
    }

    // ==================================================================
    // AWARD BLOCK
    // ==================================================================
    private fun drawAwardBlock(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textSize = 28f
        fill.textAlign = Align.LEFT
        c.drawText("Roll No.: ${cert.rollNo}", BODY_LEFT + 20f, Y_ROLL, fill)

        fill.textAlign = Align.CENTER
        fill.typeface = SERIF
        fill.textSize = 40f
        c.drawText("This certificate is awarded to", BODY_CX, Y_AWARDED, fill)

        fill.typeface = SERIF_ITALIC
        fitTextSize(fill, cert.studentName, 900f, 92f, 40f)
        c.drawText(cert.studentName, BODY_CX, Y_NAME, fill)

        line.color = GOLD
        line.strokeWidth = 3f
        c.drawLine(BODY_CX - 380f, Y_NAME_RULE, BODY_CX + 380f, Y_NAME_RULE, line)

        fill.typeface = SERIF_ITALIC
        fill.textSize = 32f
        c.drawText("D/O or S/O ${cert.guardian}", BODY_CX, Y_GUARDIAN, fill)

        fill.typeface = SERIF
        fill.textSize = 36f
        c.drawText("In recognition of successful completion of", BODY_CX, Y_RECOGNITION, fill)

        // course title with flourishes flanking it
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fitTextSize(fill, cert.course, 1120f, 80f, 32f)
        val courseW = fill.measureText(cert.course)
        c.drawText(cert.course, BODY_CX, Y_COURSE, fill)

        val flourishY = Y_COURSE - 22f
        drawFlourish(c, line, BODY_CX - courseW / 2f - 58f, flourishY, -1)
        drawFlourish(c, line, BODY_CX + courseW / 2f + 58f, flourishY, 1)

        // teal institution line flanked by short gold rules
        fill.color = TEAL
        fill.typeface = SERIF_BOLD
        fill.textSize = 30f
        val instTxt = "AT LAKSHMI GROUP OF EDUCATION SOCIETY"
        val instW = fill.measureText(instTxt)
        c.drawText(instTxt, BODY_CX, Y_INSTITUTION, fill)

        line.color = GOLD
        line.strokeWidth = 2f
        val ruleY = Y_INSTITUTION - 10f
        c.drawLine(BODY_CX - instW / 2f - 150f, ruleY, BODY_CX - instW / 2f - 34f, ruleY, line)
        c.drawLine(BODY_CX + instW / 2f + 34f, ruleY, BODY_CX + instW / 2f + 150f, ruleY, line)
    }

    /** Small symmetrical vector flourish (swash + leaf). dir = -1 left, +1 right. */
    private fun drawFlourish(c: Canvas, line: Paint, x: Float, y: Float, dir: Int) {
        line.color = GOLD
        line.strokeWidth = 3f
        val d = dir.toFloat()

        val swash = Path().apply {
            moveTo(x + d * 4f, y)
            cubicTo(x + d * 30f, y - 24f, x + d * 56f, y - 16f, x + d * 64f, y + 4f)
            cubicTo(x + d * 56f, y + 22f, x + d * 30f, y + 20f, x + d * 12f, y + 6f)
        }
        c.drawPath(swash, line)

        val leaf = Path().apply {
            moveTo(x + d * 6f, y + 26f)
            quadTo(x + d * 32f, y + 16f, x + d * 52f, y + 32f)
            quadTo(x + d * 30f, y + 42f, x + d * 6f, y + 26f)
            close()
        }
        c.drawPath(leaf, line)
    }

    /** Long divider: rule — ornament — rule. */
    private fun drawFlourishRule(c: Canvas, fill: Paint, line: Paint, cx: Float, y: Float, half: Float) {
        line.color = GOLD
        line.strokeWidth = 2f
        c.drawLine(cx - half, y, cx - 40f, y, line)
        c.drawLine(cx + 40f, y, cx + half, y, line)

        line.strokeWidth = 3f
        val diamond = Path().apply {
            moveTo(cx, y - 14f)
            quadTo(cx + 16f, y, cx, y + 14f)
            quadTo(cx - 16f, y, cx, y - 14f)
            close()
        }
        c.drawPath(diamond, line)

        fill.color = GOLD
        c.drawCircle(cx - 30f, y, 4f, fill)
        c.drawCircle(cx + 30f, y, 4f, fill)
    }

    // ==================================================================
    // SESSION / GRADE BADGE BOX
    // ==================================================================
    private fun drawBadgeBox(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        val boxW = 1080f
        val box = RectF(BODY_CX - boxW / 2f, Y_BADGE_TOP, BODY_CX + boxW / 2f, Y_BADGE_TOP + BADGE_H)

        fill.color = PARCHMENT
        c.drawRoundRect(box, 18f, 18f, fill)
        line.color = GOLD
        line.strokeWidth = 3f
        c.drawRoundRect(box, 18f, 18f, line)

        val midY = box.centerY()

        drawCalendarIcon(c, fill, line, box.left + 60f, midY, 24f, NAVY)
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textSize = 30f
        fill.textAlign = Align.LEFT
        c.drawText("Session: ${cert.session}", box.left + 104f, midY + 11f, fill)

        line.color = GOLD_SOFT
        line.strokeWidth = 2f
        c.drawLine(box.centerX(), box.top + 16f, box.centerX(), box.bottom - 16f, line)

        drawStarIcon(c, fill, box.centerX() + 60f, midY, 24f, GOLD)
        fill.color = NAVY
        c.drawText("Performance Grade: ${cert.grade}", box.centerX() + 104f, midY + 11f, fill)
    }

    // ==================================================================
    // DETAILS GRID (bottom-left of the body)
    // ==================================================================
    private fun drawDetailsGrid(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        val gx1 = BODY_LEFT
        val gx2 = GRID_RIGHT
        val gy1 = Y_GRID_TOP
        val gy2 = Y_GRID_BOTTOM

        line.color = TEAL
        line.strokeWidth = 3f
        c.drawRect(gx1, gy1, gx2, gy2, line)

        val rows = listOf(
            Triple(Icon.USER, "Run By", cert.runBy),
            Triple(Icon.CLOCK, "Course Duration", cert.duration),
            Triple(Icon.CALENDAR, "Date of Issue", cert.dateOfIssue),
            Triple(Icon.PIN, "Place of Issue", cert.placeOfIssue),
            Triple(Icon.GLOBE, "Website", cert.website),
        )

        val rowH = (gy2 - gy1) / rows.size
        val iconColRight = gx1 + 92f          // icon cell: gx1 .. gx1+92
        val labelColRight = gx1 + 430f        // label cell
        val iconCx = (gx1 + iconColRight) / 2f
        val iconR = minOf(22f, rowH * 0.28f)

        for ((i, row) in rows.withIndex()) {
            val top = gy1 + i * rowH
            val midY = top + rowH / 2f

            if (i > 0) {
                line.color = TEAL
                line.strokeWidth = 2f
                c.drawLine(gx1, top, gx2, top, line)
            }
            line.color = TEAL
            line.strokeWidth = 2f
            c.drawLine(iconColRight, top, iconColRight, top + rowH, line)
            c.drawLine(labelColRight, top, labelColRight, top + rowH, line)

            drawRowIcon(c, fill, line, row.first, iconCx, midY, iconR)

            fill.color = INK
            fill.typeface = SERIF_BOLD
            fill.textAlign = Align.LEFT
            fill.textSize = 30f
            c.drawText(row.second, iconColRight + 26f, midY + 11f, fill)

            fill.typeface = SERIF
            fill.textSize = 30f
            c.drawText(":", labelColRight + 18f, midY + 11f, fill)

            val valueX = labelColRight + 50f
            fitTextSize(fill, row.third, gx2 - valueX - 24f, 30f, 18f)
            c.drawText(row.third, valueX, midY + 11f, fill)
        }
    }

    // ==================================================================
    // VECTOR ICONS
    // ==================================================================
    private enum class Icon { USER, CLOCK, CALENDAR, PIN, GLOBE }

    private fun drawRowIcon(c: Canvas, fill: Paint, line: Paint, kind: Icon, cx: Float, cy: Float, r: Float) {
        when (kind) {
            Icon.USER -> drawUserIcon(c, line, cx, cy, r, TEAL)
            Icon.CLOCK -> drawClockIcon(c, line, cx, cy, r, TEAL)
            Icon.CALENDAR -> drawCalendarIcon(c, fill, line, cx, cy, r, TEAL)
            Icon.PIN -> drawPinIcon(c, line, cx, cy, r, TEAL)
            Icon.GLOBE -> drawGlobeIcon(c, line, cx, cy, r, TEAL)
        }
    }

    private fun drawUserIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 3f
        c.drawCircle(cx, cy - r * 0.35f, r * 0.35f, line)
        val shoulders = RectF(cx - r * 0.70f, cy + r * 0.05f, cx + r * 0.70f, cy + r * 1.05f)
        c.drawArc(shoulders, 180f, 180f, false, line)
    }

    private fun drawClockIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 3f
        c.drawCircle(cx, cy, r * 0.85f, line)
        c.drawLine(cx, cy, cx, cy - r * 0.45f, line)
        c.drawLine(cx, cy, cx + r * 0.40f, cy + r * 0.15f, line)
    }

    private fun drawCalendarIcon(c: Canvas, fill: Paint, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 3f
        val body = RectF(cx - r * 0.75f, cy - r * 0.55f, cx + r * 0.75f, cy + r * 0.85f)
        c.drawRoundRect(body, 4f, 4f, line)
        c.drawLine(body.left, cy - r * 0.18f, body.right, cy - r * 0.18f, line)
        c.drawLine(cx - r * 0.40f, cy - r * 0.85f, cx - r * 0.40f, cy - r * 0.40f, line)
        c.drawLine(cx + r * 0.40f, cy - r * 0.85f, cx + r * 0.40f, cy - r * 0.40f, line)
        fill.color = color
        c.drawCircle(cx - r * 0.30f, cy + r * 0.35f, 2.5f, fill)
        c.drawCircle(cx, cy + r * 0.35f, 2.5f, fill)
        c.drawCircle(cx + r * 0.30f, cy + r * 0.35f, 2.5f, fill)
    }

    private fun drawPinIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 3f
        val p = Path().apply {
            moveTo(cx, cy + r * 0.90f)
            cubicTo(cx - r * 0.85f, cy - r * 0.10f, cx - r * 0.60f, cy - r * 0.90f, cx, cy - r * 0.85f)
            cubicTo(cx + r * 0.60f, cy - r * 0.90f, cx + r * 0.85f, cy - r * 0.10f, cx, cy + r * 0.90f)
            close()
        }
        c.drawPath(p, line)
        c.drawCircle(cx, cy - r * 0.25f, r * 0.22f, line)
    }

    private fun drawGlobeIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 3f
        c.drawCircle(cx, cy, r * 0.85f, line)
        c.drawLine(cx - r * 0.85f, cy, cx + r * 0.85f, cy, line)
        c.drawOval(RectF(cx - r * 0.38f, cy - r * 0.85f, cx + r * 0.38f, cy + r * 0.85f), line)
    }

    private fun drawStarIcon(c: Canvas, fill: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        fill.color = color
        val path = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val x = cx + rad * Math.cos(a).toFloat()
            val y = cy + rad * Math.sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, fill)
    }

    // ==================================================================
    // QR + SIGNATURE
    // ==================================================================
    private fun drawQr(c: Canvas, fill: Paint, line: Paint, qr: Bitmap?) {
        val box = Rect(1830, 1120, 2020, 1310)
        fill.color = WHITE
        c.drawRect(box, fill)
        if (qr != null) {
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            c.drawBitmap(qr, null, box, bitmapPaint)
        }
        line.color = GOLD_SOFT
        line.strokeWidth = 2f
        c.drawRect(RectF(box), line)

        fill.color = NAVY
        fill.typeface = SANS
        fill.textSize = 22f
        fill.textAlign = Align.CENTER
        c.drawText("Scan to verify", box.exactCenterX(), box.bottom + 36f, fill)
    }

    /**
     * Director signature block — intentionally BLANK above the rule.
     * Only the rule, the "Director Signature" label and the roll/reg number.
     */
    private fun drawSignatureBlock(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        val cx = 2140f

        line.color = NAVY
        line.strokeWidth = 3f
        c.drawLine(cx - 150f, Y_SIGN_RULE, cx + 150f, Y_SIGN_RULE, line)

        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textSize = 30f
        fill.textAlign = Align.CENTER
        c.drawText("Director Signature", cx, Y_SIGN_RULE + 46f, fill)

        fill.typeface = SERIF
        fill.textSize = 24f
        c.drawText("ROLL NO/REG NO: ${cert.rollNo}", cx, Y_SIGN_RULE + 88f, fill)
    }
}
