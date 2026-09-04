package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
 * Renders the authoritative LGES certificate bitmap (2400 x 1600, landscape 3:2).
 *
 * Architecture:
 *   Static Certificate Structure + Dynamic Certificate Data + Dynamic QR Code
 *
 * Strict Compliance:
 *   - Absolute zero signature (no image, no line, no "Director Signature", clean blank background)
 *   - Authentic circular LGES logo with ribbon placed directly on navy panel (no white box)
 *   - Golden double-border with 4 corner flourishes
 *   - Golden curved Bezier separator for left navy panel
 *   - Golden details table with vector icons
 *   - Centered session & performance grade row without parchment card
 *   - Dynamic scannable QR code without extraneous text
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

    // ---------------- canvas metrics (2400 x 1600, 3:2 landscape) ----------------
    private const val W = 2400
    private const val H = 1600

    // ---------------- sidebar geometry ----------------
    private const val SB_TOP_X = 640f       // curve top anchor
    private const val SB_CP1_X = 810f       // top control point (outward sweep)
    private const val SB_CP1_Y = 540f
    private const val SB_CP2_X = 720f       // lower control point
    private const val SB_CP2_Y = 1060f
    private const val SB_END_X = 570f       // curve bottom anchor
    private const val SB_CENTER = 310f      // horizontal centre for sidebar content

    // ---------------- body frame ----------------
    private const val BODY_LEFT = 760f
    private const val BODY_RIGHT = 2320f
    private const val BODY_CX = 1530f

    // ---------------- vertical rhythm ----------------
    private const val Y_HEADER = 210f
    private const val Y_HEADER_RULE = 258f
    private const val Y_SUB_1 = 320f
    private const val Y_SUB_2 = 366f
    private const val Y_SUB_3 = 404f
    private const val Y_ROLL = 468f
    private const val Y_AWARDED = 520f
    private const val Y_NAME = 614f
    private const val Y_NAME_RULE = 656f
    private const val Y_GUARDIAN = 712f
    private const val Y_RECOGNITION = 770f
    private const val Y_COURSE = 862f
    private const val Y_INSTITUTION = 938f
    private const val Y_SESSION_ROW = 1018f
    private const val Y_GRID_TOP = 1110f
    private const val Y_GRID_BOTTOM = 1480f
    private const val GRID_RIGHT = 1720f

    // ---------------- palette (exact match with reference) ----------------
    private const val NAVY = 0xFF0B1B3D.toInt()
    private const val GOLD = 0xFFC89D3C.toInt()
    private const val GOLD_LIGHT = 0xFFDFC07A.toInt()
    private const val GOLD_DEEP = 0xFFB3832B.toInt()
    private const val CREAM = 0xFFFAF9F5.toInt()
    private const val MAROON = 0xFF800000.toInt()
    private const val INK = 0xFF1E293B.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    // ---------------- typefaces ----------------
    private val SERIF = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    private val SERIF_BOLD = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val SERIF_ITALIC = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private val SANS = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val SANS_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

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
            runBy = "Lakshmi Group of Education Society",
            duration = cert.duration,
            dateOfIssue = cert.dateOfIssue,
            placeOfIssue = cert.placeOfIssue,
            website = "www.lges-computer-classes.netlify.app"
        )
        return draw(context, certData, qrBitmap)
    }

    fun draw(context: Context, cert: CertificateData, qr: Bitmap? = null): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // 1. Pristine warm ivory background
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

        // 2. Left navy sidebar strip with Bezier curve boundary
        drawSidebar(c, fill, line)

        // 3. Sidebar logo & brand identity (drawn directly on navy background)
        drawSidebarBrand(c, context, fill, line)

        // 4. Outer golden double frame with corner flourishes across full canvas
        drawOuterBorder(c, line, fill)

        // 5. Certificate Header (Maroon LGES, superscript TM, Navy CERTIFICATE, Subtitles)
        drawHeader(c, fill, line)
        drawSubtitle(c, fill)

        // 6. Award Information (Roll No, Awardee Name, Guardian, Course, Society)
        drawAwardBlock(c, fill, line, cert)

        // 7. Session & Performance Grade row
        drawSessionGradeRow(c, fill, line, cert)

        // 8. Certificate Information Table (Rounded golden card with vector icons)
        drawDetailsGrid(c, fill, line, cert)

        // 9. Scannable Dynamic QR Code
        drawQr(c, fill, line, qr)

        // Note: Director signature block is COMPLETELY OMITTED as per strict mandate.
        // The bottom-right area remains clean, blank certificate background.

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
    // OUTER BORDER (Double Gold Frame + 4 Corner Flourishes)
    // ==================================================================
    private fun drawOuterBorder(c: Canvas, line: Paint, fill: Paint) {
        val l = 40f
        val t = 40f
        val r = W - 40f
        val b = H - 40f

        // Outer primary gold border
        line.color = GOLD
        line.strokeWidth = 4.5f
        c.drawRect(l, t, r, b, line)

        // Inner soft gold border
        val innerOffset = 16f
        line.color = GOLD_LIGHT
        line.strokeWidth = 2f
        c.drawRect(l + innerOffset, t + innerOffset, r - innerOffset, b - innerOffset, line)

        // Classical certificate corner flourishes
        fun drawCornerFlourish(cx: Float, cy: Float, sx: Float, sy: Float) {
            line.color = GOLD
            line.strokeWidth = 3f

            val armLen = 70f
            c.drawLine(cx, cy + sy * armLen, cx, cy, line)
            c.drawLine(cx, cy, cx + sx * armLen, cy, line)

            // Ornate scroll arc
            val path = Path().apply {
                moveTo(cx + sx * 22f, cy)
                cubicTo(cx + sx * 22f, cy + sy * 22f, cx, cy + sy * 22f, cx, cy + sy * 22f)
            }
            c.drawPath(path, line)

            // Inner fleuron dot
            fill.color = GOLD_DEEP
            c.drawCircle(cx + sx * 28f, cy + sy * 28f, 5f, fill)
        }

        val il = l + innerOffset
        val it = t + innerOffset
        val ir = r - innerOffset
        val ib = b - innerOffset

        drawCornerFlourish(il, it, 1f, 1f)
        drawCornerFlourish(ir, it, -1f, 1f)
        drawCornerFlourish(il, ib, 1f, -1f)
        drawCornerFlourish(ir, ib, -1f, -1f)
    }

    // ==================================================================
    // SIDEBAR & BEZIER CURVE
    // ==================================================================
    private fun drawSidebar(c: Canvas, fill: Paint, line: Paint) {
        // Deep navy filled background with graceful Bezier curve
        val sidebarPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(SB_TOP_X, 0f)
            cubicTo(SB_CP1_X, SB_CP1_Y, SB_CP2_X, SB_CP2_Y, SB_END_X, H.toFloat())
            lineTo(0f, H.toFloat())
            close()
        }
        fill.color = NAVY
        c.drawPath(sidebarPath, fill)

        // Outer gold Bezier curve border
        val goldCurvePath = Path().apply {
            moveTo(SB_TOP_X, 0f)
            cubicTo(SB_CP1_X, SB_CP1_Y, SB_CP2_X, SB_CP2_Y, SB_END_X, H.toFloat())
        }
        line.color = GOLD
        line.strokeWidth = 5.5f
        c.drawPath(goldCurvePath, line)

        // Parallel inner soft gold curve
        val goldInnerPath = Path().apply {
            moveTo(SB_TOP_X - 14f, 0f)
            cubicTo(SB_CP1_X - 14f, SB_CP1_Y, SB_CP2_X - 14f, SB_CP2_Y, SB_END_X - 14f, H.toFloat())
        }
        line.color = GOLD_LIGHT
        line.strokeWidth = 2.5f
        c.drawPath(goldInnerPath, line)
    }

    /**
     * Sidebar Brand Identity:
     * - Circular LGES emblem logo with ribbon drawn directly on navy background (NO white card)
     * - Large gold "LGES" wordmark
     * - Gold ornamental divider line with center diamond
     * - Gold italic motto
     */
    private fun drawSidebarBrand(c: Canvas, context: Context, fill: Paint, line: Paint) {
        val logoSize = 360f
        val logoTop = 160f
        val logoRect = RectF(
            SB_CENTER - logoSize / 2f,
            logoTop,
            SB_CENTER + logoSize / 2f,
            logoTop + logoSize
        )

        // Load and draw the official circular LGES logo
        val logoBitmap = loadLogoBitmap(context)
        if (logoBitmap != null) {
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            c.drawBitmap(logoBitmap, null, logoRect, bmpPaint)
        } else {
            drawFallbackLogo(c, fill, line, logoRect)
        }

        // Wordmark: LGES in bold gold serif
        fill.color = GOLD
        fill.typeface = SERIF_BOLD
        fill.textSize = 135f
        fill.textAlign = Align.CENTER
        c.drawText("LGES", SB_CENTER, logoRect.bottom + 155f, fill)

        // Ornamental gold divider line with center diamond
        val ruleY = logoRect.bottom + 198f
        line.color = GOLD_LIGHT
        line.strokeWidth = 2.5f
        c.drawLine(SB_CENTER - 140f, ruleY, SB_CENTER - 18f, ruleY, line)
        c.drawLine(SB_CENTER + 18f, ruleY, SB_CENTER + 140f, ruleY, line)

        fill.color = GOLD
        val diamond = Path().apply {
            moveTo(SB_CENTER, ruleY - 8f)
            lineTo(SB_CENTER + 10f, ruleY)
            lineTo(SB_CENTER, ruleY + 8f)
            lineTo(SB_CENTER - 10f, ruleY)
            close()
        }
        c.drawPath(diamond, fill)

        // Motto
        fill.typeface = SERIF_ITALIC
        fill.color = GOLD_LIGHT
        fill.textSize = 34f
        c.drawText("Empowering Learners,", SB_CENTER, ruleY + 75f, fill)
        c.drawText("Enriching Futures.", SB_CENTER, ruleY + 120f, fill)
    }

    private fun loadLogoBitmap(context: Context): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_lges_logo)
                ?: run {
                    val d = ContextCompat.getDrawable(context, R.drawable.img_lges_logo) ?: return null
                    val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 512
                    val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 512
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    d.setBounds(0, 0, w, h)
                    d.draw(canvas)
                    bmp
                }
        } catch (_: Throwable) {
            null
        }
    }

    private fun drawFallbackLogo(c: Canvas, fill: Paint, line: Paint, rect: RectF) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = rect.width() / 2f
        line.color = GOLD
        line.strokeWidth = 6f
        c.drawCircle(cx, cy, r - 6f, line)
        line.color = GOLD_LIGHT
        line.strokeWidth = 3f
        c.drawCircle(cx, cy, r - 18f, line)

        fill.color = GOLD
        fill.typeface = SERIF_BOLD
        fill.textSize = 90f
        fill.textAlign = Align.CENTER
        c.drawText("LGES", cx, cy + 30f, fill)
    }

    // ==================================================================
    // HEADER
    // ==================================================================
    private fun drawHeader(c: Canvas, fill: Paint, line: Paint) {
        val titleSize = 126f
        val tmSize = 34f

        fill.typeface = SERIF_BOLD
        fill.textSize = titleSize
        val lgesW = fill.measureText("LGES")
        val certW = fill.measureText("CERTIFICATE")

        fill.textSize = tmSize
        val tmW = fill.measureText("TM")

        val gap = 42f
        val totalW = lgesW + tmW + gap + certW
        val startX = BODY_CX - totalW / 2f

        // "LGES" in Maroon
        fill.color = MAROON
        fill.textSize = titleSize
        fill.textAlign = Align.LEFT
        c.drawText("LGES", startX, Y_HEADER, fill)

        // "TM" superscript above "ES"
        fill.textSize = tmSize
        c.drawText("TM", startX + lgesW + 4f, Y_HEADER - titleSize * 0.60f, fill)

        // "CERTIFICATE" in Navy
        fill.color = NAVY
        fill.textSize = titleSize
        c.drawText("CERTIFICATE", startX + lgesW + tmW + gap, Y_HEADER, fill)

        // Symmetrical Gold Flourish Divider below Title
        drawHeaderFlourishRule(c, fill, line, BODY_CX, Y_HEADER_RULE, 280f)
    }

    private fun drawHeaderFlourishRule(c: Canvas, fill: Paint, line: Paint, cx: Float, y: Float, halfWidth: Float) {
        line.color = GOLD
        line.strokeWidth = 2.5f

        // Flanking lines
        c.drawLine(cx - halfWidth, y, cx - 44f, y, line)
        c.drawLine(cx + 44f, y, cx + halfWidth, y, line)

        // Center ornate diamond
        fill.color = GOLD
        val diamond = Path().apply {
            moveTo(cx, y - 14f)
            lineTo(cx + 18f, y)
            lineTo(cx, y + 14f)
            lineTo(cx - 18f, y)
            close()
        }
        c.drawPath(diamond, fill)

        // Decorative satellite beads
        fill.color = GOLD_LIGHT
        c.drawCircle(cx - 30f, y, 4f, fill)
        c.drawCircle(cx + 30f, y, 4f, fill)
    }

    private fun drawSubtitle(c: Canvas, fill: Paint) {
        fill.textAlign = Align.CENTER
        fill.color = NAVY

        fill.typeface = SANS_BOLD
        fill.textSize = 29f
        c.drawText("(A NATIONAL COMPUTER LITERACY PROGRAMME)", BODY_CX, Y_SUB_1, fill)

        fill.typeface = SERIF
        fill.textSize = 26f
        c.drawText("An Autonomous Institution Registered Under the Society & C.R. Act", BODY_CX, Y_SUB_2, fill)

        fill.textSize = 25f
        c.drawText("Ministry of HRD, Govt. of India, NCT, New Delhi", BODY_CX, Y_SUB_3, fill)
    }

    // ==================================================================
    // AWARD BLOCK
    // ==================================================================
    private fun drawAwardBlock(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        // Roll No. on the left side
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textSize = 28f
        fill.textAlign = Align.LEFT
        val rollLabel = "Roll No.: "
        val rollLabelW = fill.measureText(rollLabel)
        c.drawText(rollLabel, BODY_LEFT, Y_ROLL, fill)

        fill.typeface = SERIF
        c.drawText(cert.rollNo, BODY_LEFT + rollLabelW, Y_ROLL, fill)
        val rollValW = fill.measureText(cert.rollNo).coerceAtLeast(180f)
        line.color = GOLD_LIGHT
        line.strokeWidth = 2f
        c.drawLine(BODY_LEFT + rollLabelW, Y_ROLL + 6f, BODY_LEFT + rollLabelW + rollValW, Y_ROLL + 6f, line)

        // "This certificate is awarded to"
        fill.textAlign = Align.CENTER
        fill.typeface = SERIF_ITALIC
        fill.textSize = 38f
        c.drawText("This certificate is awarded to", BODY_CX, Y_AWARDED, fill)

        // Student Name in stately navy serif
        fill.typeface = SERIF_BOLD
        fill.color = NAVY
        fitTextSize(fill, cert.studentName, 1000f, 90f, 40f)
        c.drawText(cert.studentName, BODY_CX, Y_NAME, fill)

        // Ornamental gold accent line under Student Name with central diamond
        val ruleHalf = 420f
        line.color = GOLD
        line.strokeWidth = 2.5f
        c.drawLine(BODY_CX - ruleHalf, Y_NAME_RULE, BODY_CX - 18f, Y_NAME_RULE, line)
        c.drawLine(BODY_CX + 18f, Y_NAME_RULE, BODY_CX + ruleHalf, Y_NAME_RULE, line)
        fill.color = GOLD
        val nameDiamond = Path().apply {
            moveTo(BODY_CX, Y_NAME_RULE - 7f)
            lineTo(BODY_CX + 8f, Y_NAME_RULE)
            lineTo(BODY_CX, Y_NAME_RULE + 7f)
            lineTo(BODY_CX - 8f, Y_NAME_RULE)
            close()
        }
        c.drawPath(nameDiamond, fill)

        // Guardian Line (D/O or S/O ...)
        fill.typeface = SERIF_ITALIC
        fill.textSize = 32f
        val guardianText = when {
            cert.guardian.isBlank() -> ""
            cert.guardian.startsWith("S/O", ignoreCase = true) ||
            cert.guardian.startsWith("D/O", ignoreCase = true) ||
            cert.guardian.startsWith("W/O", ignoreCase = true) -> cert.guardian
            else -> "S/O or D/O ${cert.guardian}"
        }
        if (guardianText.isNotBlank()) {
            c.drawText(guardianText, BODY_CX, Y_GUARDIAN, fill)
            val gW = fill.measureText(guardianText)
            line.color = GOLD_LIGHT
            line.strokeWidth = 1.5f
            c.drawLine(BODY_CX - gW / 2f, Y_GUARDIAN + 6f, BODY_CX + gW / 2f, Y_GUARDIAN + 6f, line)
        }

        // Recognition Subtitle
        fill.typeface = SERIF
        fill.textSize = 34f
        c.drawText("In recognition of successful completion of", BODY_CX, Y_RECOGNITION, fill)

        // Course Title flanked by symmetrical gold flourishes
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fitTextSize(fill, cert.course, 1100f, 78f, 32f)
        val courseW = fill.measureText(cert.course)
        c.drawText(cert.course, BODY_CX, Y_COURSE, fill)

        val flourishY = Y_COURSE - 20f
        drawCourseFlourish(c, line, BODY_CX - courseW / 2f - 60f, flourishY, -1)
        drawCourseFlourish(c, line, BODY_CX + courseW / 2f + 60f, flourishY, 1)

        // Institution Endorsement
        fill.color = NAVY
        fill.typeface = SERIF_BOLD
        fill.textSize = 30f
        val instText = "AT LAKSHMI GROUP OF EDUCATION SOCIETY"
        val instW = fill.measureText(instText)
        c.drawText(instText, BODY_CX, Y_INSTITUTION, fill)

        line.color = GOLD
        line.strokeWidth = 2.5f
        val dashY = Y_INSTITUTION - 9f
        c.drawLine(BODY_CX - instW / 2f - 140f, dashY, BODY_CX - instW / 2f - 30f, dashY, line)
        c.drawLine(BODY_CX + instW / 2f + 30f, dashY, BODY_CX + instW / 2f + 140f, dashY, line)
    }

    /** Symmetrical vector flourish flanking the course name. dir = -1 left, +1 right. */
    private fun drawCourseFlourish(c: Canvas, line: Paint, x: Float, y: Float, dir: Int) {
        line.color = GOLD
        line.strokeWidth = 3f
        val d = dir.toFloat()

        val swash = Path().apply {
            moveTo(x + d * 4f, y)
            cubicTo(x + d * 30f, y - 22f, x + d * 56f, y - 14f, x + d * 64f, y + 4f)
            cubicTo(x + d * 56f, y + 20f, x + d * 30f, y + 18f, x + d * 12f, y + 6f)
        }
        c.drawPath(swash, line)

        val leaf = Path().apply {
            moveTo(x + d * 6f, y + 24f)
            quadTo(x + d * 32f, y + 14f, x + d * 52f, y + 30f)
            quadTo(x + d * 30f, y + 40f, x + d * 6f, y + 24f)
            close()
        }
        c.drawPath(leaf, line)
    }

    // ==================================================================
    // SESSION & GRADE ROW (clean, no parchment box)
    // ==================================================================
    private fun drawSessionGradeRow(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        val y = Y_SESSION_ROW
        val midX = BODY_CX

        // Left half: [Calendar Icon] Session: ...
        val sessionText = "Session: ${cert.session}"
        fill.typeface = SERIF_BOLD
        fill.textSize = 30f
        fill.color = NAVY
        fill.textAlign = Align.LEFT
        val sessionW = fill.measureText(sessionText)

        val sessionBlockLeft = midX - sessionW - 80f
        drawCalendarIcon(c, fill, line, sessionBlockLeft + 15f, y - 8f, 22f, GOLD)
        c.drawText(sessionText, sessionBlockLeft + 52f, y, fill)

        // Center vertical divider rule
        line.color = GOLD_LIGHT
        line.strokeWidth = 2f
        c.drawLine(midX, y - 30f, midX, y + 10f, line)

        // Right half: [Star Icon] Performance Grade: ...
        val gradeText = "Performance Grade: ${cert.grade}"
        val gradeBlockLeft = midX + 35f
        drawStarIcon(c, fill, gradeBlockLeft + 15f, y - 8f, 22f, GOLD)
        c.drawText(gradeText, gradeBlockLeft + 52f, y, fill)
    }

    // ==================================================================
    // DETAILS TABLE (5-row card with golden border and vector icons)
    // ==================================================================
    private fun drawDetailsGrid(c: Canvas, fill: Paint, line: Paint, cert: CertificateData) {
        val gx1 = BODY_LEFT
        val gx2 = GRID_RIGHT
        val gy1 = Y_GRID_TOP
        val gy2 = Y_GRID_BOTTOM

        val cardRect = RectF(gx1, gy1, gx2, gy2)

        // Card border in warm Gold with rounded corners
        line.color = GOLD
        line.strokeWidth = 3f
        c.drawRoundRect(cardRect, 16f, 16f, line)

        val rows = listOf(
            Triple(IconKind.USER, "Run By", cert.runBy),
            Triple(IconKind.CLOCK, "Course Duration", cert.duration),
            Triple(IconKind.CALENDAR, "Date of Issue", cert.dateOfIssue),
            Triple(IconKind.PIN, "Place of Issue", cert.placeOfIssue),
            Triple(IconKind.GLOBE, "Website", cert.website),
        )

        val rowH = (gy2 - gy1) / rows.size
        val iconColRight = gx1 + 78f
        val labelColRight = gx1 + 400f
        val colonX = gx1 + 418f
        val valueX = gx1 + 448f
        val iconCx = (gx1 + iconColRight) / 2f
        val iconR = 20f

        for ((i, row) in rows.withIndex()) {
            val top = gy1 + i * rowH
            val midY = top + rowH / 2f

            // Horizontal row divider
            if (i > 0) {
                line.color = GOLD_LIGHT
                line.strokeWidth = 1.5f
                c.drawLine(gx1, top, gx2, top, line)
            }

            // Draw clean line icon
            drawRowIcon(c, fill, line, row.first, iconCx, midY, iconR)

            // Label
            fill.color = INK
            fill.typeface = SERIF_BOLD
            fill.textAlign = Align.LEFT
            fill.textSize = 28f
            c.drawText(row.second, iconColRight + 18f, midY + 10f, fill)

            // Colon separator
            fill.typeface = SERIF
            c.drawText(":", colonX, midY + 10f, fill)

            // Value text
            fitTextSize(fill, row.third, gx2 - valueX - 20f, 28f, 18f)
            c.drawText(row.third, valueX, midY + 10f, fill)
        }
    }

    // ==================================================================
    // VECTOR ICONS
    // ==================================================================
    private enum class IconKind { USER, CLOCK, CALENDAR, PIN, GLOBE }

    private fun drawRowIcon(c: Canvas, fill: Paint, line: Paint, kind: IconKind, cx: Float, cy: Float, r: Float) {
        val iconColor = NAVY
        when (kind) {
            IconKind.USER -> drawUserIcon(c, line, cx, cy, r, iconColor)
            IconKind.CLOCK -> drawClockIcon(c, line, cx, cy, r, iconColor)
            IconKind.CALENDAR -> drawCalendarIcon(c, fill, line, cx, cy, r, iconColor)
            IconKind.PIN -> drawPinIcon(c, line, cx, cy, r, iconColor)
            IconKind.GLOBE -> drawGlobeIcon(c, line, cx, cy, r, iconColor)
        }
    }

    private fun drawUserIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 2.5f
        c.drawCircle(cx, cy - r * 0.35f, r * 0.35f, line)
        val shoulders = RectF(cx - r * 0.70f, cy + r * 0.05f, cx + r * 0.70f, cy + r * 1.05f)
        c.drawArc(shoulders, 180f, 180f, false, line)
    }

    private fun drawClockIcon(c: Canvas, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 2.5f
        c.drawCircle(cx, cy, r * 0.85f, line)
        c.drawLine(cx, cy, cx, cy - r * 0.45f, line)
        c.drawLine(cx, cy, cx + r * 0.40f, cy + r * 0.15f, line)
    }

    private fun drawCalendarIcon(c: Canvas, fill: Paint, line: Paint, cx: Float, cy: Float, r: Float, color: Int) {
        line.color = color
        line.strokeWidth = 2.5f
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
        line.strokeWidth = 2.5f
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
        line.strokeWidth = 2.5f
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
    // DYNAMIC QR CODE
    // ==================================================================
    private fun drawQr(c: Canvas, fill: Paint, line: Paint, qr: Bitmap?) {
        val qrLeft = 1760
        val qrTop = 1170
        val qrSize = 250
        val box = Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)

        // Clean white background card for high-contrast scanning
        fill.color = WHITE
        c.drawRect(box, fill)

        if (qr != null) {
            val inset = 12
            val qrDst = Rect(box.left + inset, box.top + inset, box.right - inset, box.bottom - inset)
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            c.drawBitmap(qr, null, qrDst, bitmapPaint)
        }

        // Delicate golden framing line
        line.color = GOLD_LIGHT
        line.strokeWidth = 2.5f
        c.drawRect(RectF(box), line)

        // Note: No "Scan to verify" text as per the reference certificate design.
    }
}

