package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.database.Certificate
import com.example.ui.theme.COLOR_LGES_GOLD_ARGB
import com.example.ui.theme.COLOR_LGES_INK_ARGB
import com.example.ui.theme.COLOR_LGES_NAVY_ARGB
import com.example.util.AppLogger
import com.example.util.CertificateConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Master coordinate system and layout constants for the 2400x1600 certificate canvas.
 * Derived from the visual reference certificate (Reference_certificate.png).
 */
object CertificateLayout {
    const val WIDTH = 2400f
    const val HEIGHT = 1600f

    // Content center shifted right from physical canvas center due to left sidebar
    const val CONTENT_CENTER_X = 1330f

    // 1. Roll No (Top-Left of Content Area)
    const val ROLL_X = 650f
    const val ROLL_Y = 540f
    const val ROLL_MAX_WIDTH = 420f
    const val ROLL_PREFERRED_SIZE = 26f
    const val ROLL_MIN_SIZE = 14f

    // 2. Student Name
    const val NAME_CENTER_X = 1330f
    const val NAME_Y = 665f
    const val NAME_MAX_WIDTH = 1050f
    const val NAME_PREFERRED_SIZE = 78f
    const val NAME_MIN_SIZE = 32f

    // 3. Father / Guardian Name
    const val FATHER_CENTER_X = 1330f
    const val FATHER_Y = 715f
    const val FATHER_MAX_WIDTH = 900f
    const val FATHER_PREFERRED_SIZE = 30f
    const val FATHER_MIN_SIZE = 18f

    // 4. Course Name
    const val COURSE_CENTER_X = 1330f
    const val COURSE_START_Y = 905f
    const val COURSE_SECOND_LINE_Y = 950f
    const val COURSE_MAX_WIDTH = 1100f
    const val COURSE_PREFERRED_SIZE = 70f
    const val COURSE_MIN_SINGLE_LINE_SIZE = 40f
    const val COURSE_PREFERRED_2LINE_SIZE = 48f
    const val COURSE_MIN_SIZE = 24f

    // 5. Session Range
    const val SESSION_X = 1100f
    const val SESSION_Y = 1110f
    const val SESSION_MAX_WIDTH = 220f
    const val SESSION_PREFERRED_SIZE = 28f
    const val SESSION_MIN_SIZE = 16f

    // 6. Performance Grade
    const val GRADE_X = 1765f
    const val GRADE_Y = 1110f
    const val GRADE_MAX_WIDTH = 200f
    const val GRADE_PREFERRED_SIZE = 28f
    const val GRADE_MIN_SIZE = 16f

    // 7. Lower Information Box
    const val INFO_BOX_X = 885f
    const val INFO_BOX_MAX_WIDTH = 630f
    const val RUN_BY_Y = 1211f
    const val DURATION_Y = 1275f
    const val DATE_OF_ISSUE_Y = 1339f
    const val PLACE_OF_ISSUE_Y = 1405f
    const val WEBSITE_Y = 1470f
    const val INFO_BOX_PREFERRED_SIZE = 24f
    const val INFO_BOX_MIN_SIZE = 15f
    const val WEBSITE_PREFERRED_SIZE = 22f
    const val WEBSITE_MIN_SIZE = 14f

    // 8. Dynamic QR Code Box Bounds
    const val QR_LEFT = 1535f
    const val QR_TOP = 1255f
    const val QR_SIZE = 185f
    const val QR_BITMAP_PADDING = 8f
    const val QR_BITMAP_LEFT = QR_LEFT + QR_BITMAP_PADDING
    const val QR_BITMAP_TOP = QR_TOP + QR_BITMAP_PADDING
    const val QR_BITMAP_SIZE = QR_SIZE - (QR_BITMAP_PADDING * 2f)

    // 9. Bottom-Right Certificate No / Reg No
    const val CERT_NO_X = 2100f
    const val CERT_NO_Y = 1475f
    const val CERT_NO_MAX_WIDTH = 230f
    const val CERT_NO_PREFERRED_SIZE = 24f
    const val CERT_NO_MIN_SIZE = 12f
}

/**
 * Data structure for rendering a single certificate.
 */
data class CertificateData(
    val rollNo: String,
    val certificateId: String = rollNo,
    val studentName: String,
    val guardian: String,
    val course: String,
    val session: String,
    val grade: String,
    val runBy: String,
    val duration: String,
    val dateOfIssue: String,
    val placeOfIssue: String,
    val website: String
)

object CertificateDrawer {

    private const val TAG = "CertificateDrawer"

    // Master certificate canvas dimensions
    const val W = 2400
    const val H = 1600

    private const val TEMPLATE_VERSION = "v2"
    private const val TEMPLATE_FILE = "Reference_certificate.pdf"

    // In-memory cache for the static artwork to prevent repeated PDF extraction
    @Volatile
    private var cachedTemplateBitmap: Bitmap? = null
    private val templateLock = Any()

    // Typefaces
    private val SERIF_BOLD: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    // Palette (Centralized brand colors)
    val NAVY = COLOR_LGES_NAVY_ARGB // #1A237E Deep Navy Primary
    val GOLD = COLOR_LGES_GOLD_ARGB // #D4AF37 Institutional Gold
    val INK = COLOR_LGES_INK_ARGB   // #1E293B Dark Charcoal/Ink

    /**
     * Backward-compatible layout alias forwarding to CertificateLayout.
     */
    object Layout {
        const val CANVAS_WIDTH = 2400
        const val CANVAS_HEIGHT = 1600

        const val STUDENT_NAME_CENTER_X = CertificateLayout.NAME_CENTER_X
        const val STUDENT_NAME_BASELINE_Y = CertificateLayout.NAME_Y
        const val STUDENT_NAME_MAX_WIDTH = CertificateLayout.NAME_MAX_WIDTH
        const val STUDENT_NAME_PREFERRED_SIZE = CertificateLayout.NAME_PREFERRED_SIZE
        const val STUDENT_NAME_MIN_SIZE = CertificateLayout.NAME_MIN_SIZE

        const val GUARDIAN_CENTER_X = CertificateLayout.FATHER_CENTER_X
        const val GUARDIAN_BASELINE_Y = CertificateLayout.FATHER_Y
        const val GUARDIAN_MAX_WIDTH = CertificateLayout.FATHER_MAX_WIDTH
        const val GUARDIAN_PREFERRED_SIZE = CertificateLayout.FATHER_PREFERRED_SIZE
        const val GUARDIAN_MIN_SIZE = CertificateLayout.FATHER_MIN_SIZE

        const val COURSE_CENTER_X = CertificateLayout.COURSE_CENTER_X
        const val COURSE_BASELINE_Y = CertificateLayout.COURSE_START_Y
        const val COURSE_MAX_WIDTH = CertificateLayout.COURSE_MAX_WIDTH
        const val COURSE_PREFERRED_SIZE = CertificateLayout.COURSE_PREFERRED_SIZE
        const val COURSE_MIN_SINGLE_LINE_SIZE = CertificateLayout.COURSE_MIN_SINGLE_LINE_SIZE
        const val COURSE_PREFERRED_2LINE_SIZE = CertificateLayout.COURSE_PREFERRED_2LINE_SIZE
        const val COURSE_MIN_SIZE = CertificateLayout.COURSE_MIN_SIZE

        const val SESSION_X = CertificateLayout.SESSION_X
        const val SESSION_BASELINE_Y = CertificateLayout.SESSION_Y
        const val SESSION_MAX_WIDTH = CertificateLayout.SESSION_MAX_WIDTH
        const val SESSION_PREFERRED_SIZE = CertificateLayout.SESSION_PREFERRED_SIZE
        const val SESSION_MIN_SIZE = CertificateLayout.SESSION_MIN_SIZE

        const val GRADE_X = CertificateLayout.GRADE_X
        const val GRADE_BASELINE_Y = CertificateLayout.GRADE_Y
        const val GRADE_MAX_WIDTH = CertificateLayout.GRADE_MAX_WIDTH
        const val GRADE_PREFERRED_SIZE = CertificateLayout.GRADE_PREFERRED_SIZE
        const val GRADE_MIN_SIZE = CertificateLayout.GRADE_MIN_SIZE

        const val METADATA_X = CertificateLayout.INFO_BOX_X
        const val METADATA_MAX_WIDTH = CertificateLayout.INFO_BOX_MAX_WIDTH
        const val METADATA_PREFERRED_SIZE = CertificateLayout.INFO_BOX_PREFERRED_SIZE
        const val METADATA_MIN_SIZE = CertificateLayout.INFO_BOX_MIN_SIZE

        const val RUN_BY_BASELINE_Y = CertificateLayout.RUN_BY_Y
        const val DURATION_BASELINE_Y = CertificateLayout.DURATION_Y
        const val DATE_OF_ISSUE_BASELINE_Y = CertificateLayout.DATE_OF_ISSUE_Y
        const val PLACE_OF_ISSUE_BASELINE_Y = CertificateLayout.PLACE_OF_ISSUE_Y
        const val WEBSITE_BASELINE_Y = CertificateLayout.WEBSITE_Y
        const val WEBSITE_PREFERRED_SIZE = CertificateLayout.WEBSITE_PREFERRED_SIZE
        const val WEBSITE_MIN_SIZE = CertificateLayout.WEBSITE_MIN_SIZE

        const val ROLL_NO_X = CertificateLayout.CERT_NO_X
        const val ROLL_NO_BASELINE_Y = CertificateLayout.CERT_NO_Y
        const val ROLL_NO_MAX_WIDTH = CertificateLayout.CERT_NO_MAX_WIDTH
        const val ROLL_NO_PREFERRED_SIZE = CertificateLayout.CERT_NO_PREFERRED_SIZE
        const val ROLL_NO_MIN_SIZE = CertificateLayout.CERT_NO_MIN_SIZE

        const val QR_BOX_LEFT = 1535
        const val QR_BOX_TOP = 1255
        const val QR_BOX_WIDTH = 185
        const val QR_BOX_HEIGHT = 185

        const val QR_DEST_LEFT = 1543
        const val QR_DEST_TOP = 1263
        const val QR_DEST_SIZE = 169
    }

    /**
     * Converts a database Certificate into CertificateData for rendering.
     */
    fun buildCertificateData(
        cert: Certificate,
        instituteName: String = CertificateConfig.DEFAULT_INSTITUTE_NAME,
        websiteUrl: String = CertificateConfig.DEFAULT_INSTITUTE_WEBSITE
    ): CertificateData {
        val normalizedRollNo = cert.rollNo.trim()
        val normalizedCertId = cert.certificateId.trim().ifBlank { normalizedRollNo }
        val normalizedWebsite = cleanWebsiteForDisplay(websiteUrl)

        return CertificateData(
            rollNo = normalizedRollNo,
            certificateId = normalizedCertId,
            studentName = cert.studentName.trim(),
            guardian = cert.fatherName.trim(),
            course = cert.courseName.trim(),
            session = cert.sessionRange.trim(),
            grade = cert.grade.trim(),
            runBy = instituteName.trim().ifBlank { CertificateConfig.DEFAULT_INSTITUTE_NAME },
            duration = cert.duration.trim(),
            dateOfIssue = cert.dateOfIssue.trim(),
            placeOfIssue = cert.placeOfIssue.trim().ifBlank { "CHAMBA" },
            website = normalizedWebsite
        )
    }

    /**
     * Full-resolution render (2400 x 1600). Used for export and high-fidelity output.
     */
    fun drawCertificate(
        context: Context,
        cert: Certificate,
        qrBitmap: Bitmap?,
        instituteName: String = CertificateConfig.DEFAULT_INSTITUTE_NAME,
        websiteUrl: String = CertificateConfig.DEFAULT_INSTITUTE_WEBSITE
    ): Bitmap {
        val data = buildCertificateData(cert, instituteName, websiteUrl)
        return draw(context = context, cert = data, qr = qrBitmap)
    }

    /**
     * Lightweight scaled-down render (e.g. 1200 x 800 or 900 x 600) for live UI preview.
     * Conserves memory and keeps Compose UI buttery smooth.
     */
    fun drawPreviewCertificate(
        context: Context,
        cert: Certificate,
        qrBitmap: Bitmap?,
        previewWidth: Int = 1200,
        instituteName: String = CertificateConfig.DEFAULT_INSTITUTE_NAME,
        websiteUrl: String = CertificateConfig.DEFAULT_INSTITUTE_WEBSITE
    ): Bitmap {
        val fullBitmap = drawCertificate(context, cert, qrBitmap, instituteName, websiteUrl)
        if (fullBitmap.width == previewWidth) return fullBitmap

        val previewHeight = (previewWidth * (H.toFloat() / W.toFloat())).toInt()
        val scaled = Bitmap.createScaledBitmap(fullBitmap, previewWidth, previewHeight, true)
        if (scaled != fullBitmap) {
            fullBitmap.recycle()
        }
        return scaled
    }

    /**
     * Core drawing routine.
     */
    fun draw(
        context: Context,
        cert: CertificateData,
        qr: Bitmap?
    ): Bitmap {
        val template = getOrLoadTemplate(context)
            ?: throw IllegalStateException("Certificate master template could not be loaded from assets/$TEMPLATE_FILE")

        val output = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw static template
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(template, null, Rect(0, 0, W, H), bitmapPaint)

        // Draw dynamic text
        drawDynamicData(canvas = canvas, cert = cert)

        // Draw dynamic QR
        drawDynamicQr(canvas = canvas, qr = qr)

        return output
    }

    /**
     * Thread-safe in-memory cached template loader.
     */
    fun getOrLoadTemplate(context: Context): Bitmap? {
        cachedTemplateBitmap?.let {
            if (!it.isRecycled) return it
        }

        synchronized(templateLock) {
            cachedTemplateBitmap?.let {
                if (!it.isRecycled) return it
            }
            val loaded = loadReferenceCertificate(context)
            cachedTemplateBitmap = loaded
            return loaded
        }
    }

    /**
     * Clears template cache (call if asset changes or low memory event occurs).
     */
    fun clearCache() {
        synchronized(templateLock) {
            cachedTemplateBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
            cachedTemplateBitmap = null
        }
    }

    private fun loadReferenceCertificate(context: Context): Bitmap? {
        return try {
            val cacheFile = File(context.cacheDir, "Reference_certificate_$TEMPLATE_VERSION.pdf")

            // Only copy from assets if cache file doesn't exist or is empty
            if (!cacheFile.exists() || cacheFile.length() <= 0L) {
                context.assets.open(TEMPLATE_FILE).use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            if (!cacheFile.exists() || cacheFile.length() <= 0L) {
                AppLogger.e(TAG, "Reference certificate PDF is empty.")
                return null
            }

            ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount <= 0) {
                        AppLogger.e(TAG, "Reference_certificate.pdf contains no pages.")
                        return null
                    }

                    renderer.openPage(0).use { page ->
                        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to render PDF template: ${e.message}", e)
            null
        }
    }

    // ================================================================
    // DYNAMIC DATA
    // ================================================================

    private fun drawDynamicData(
        canvas: Canvas,
        cert: CertificateData
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = SERIF_BOLD
        }

        // 1. Roll Number (Top-Left of Content Area)
        // Left aligned at ROLL_X = 650f, baselineY = 540f
        if (cert.rollNo.isNotBlank()) {
            paint.color = INK
            drawLeftFittedText(
                canvas = canvas,
                text = cert.rollNo,
                x = CertificateLayout.ROLL_X,
                baselineY = CertificateLayout.ROLL_Y,
                maxWidth = CertificateLayout.ROLL_MAX_WIDTH,
                preferredSize = CertificateLayout.ROLL_PREFERRED_SIZE,
                minimumSize = CertificateLayout.ROLL_MIN_SIZE,
                paint = paint
            )
        }

        // 2. Student Name
        // Centered around NAME_CENTER_X = 1330f, baselineY = 665f, in LgesNavy
        paint.color = NAVY
        drawStudentName(
            canvas = canvas,
            text = cert.studentName,
            centerX = CertificateLayout.NAME_CENTER_X,
            baselineY = CertificateLayout.NAME_Y,
            maxWidth = CertificateLayout.NAME_MAX_WIDTH,
            preferredSize = CertificateLayout.NAME_PREFERRED_SIZE,
            minimumSize = CertificateLayout.NAME_MIN_SIZE,
            paint = paint
        )

        // 3. Father / Guardian Name
        // Centered around FATHER_CENTER_X = 1330f, baselineY = 715f, in INK
        paint.color = INK
        val guardianText = normalizeGuardian(cert.guardian)
        if (guardianText.isNotBlank()) {
            drawCenteredFittedText(
                canvas = canvas,
                text = guardianText,
                centerX = CertificateLayout.FATHER_CENTER_X,
                baselineY = CertificateLayout.FATHER_Y,
                maxWidth = CertificateLayout.FATHER_MAX_WIDTH,
                preferredSize = CertificateLayout.FATHER_PREFERRED_SIZE,
                minimumSize = CertificateLayout.FATHER_MIN_SIZE,
                paint = paint
            )
        }

        // 4. Course Name (Prominent NAVY, 2-line balanced wrapping if long)
        // Centered around COURSE_CENTER_X = 1330f, START_Y = 905f, SECOND_LINE_Y = 950f
        paint.color = NAVY
        drawCenteredMultilineFittedText(
            canvas = canvas,
            text = cert.course,
            centerX = CertificateLayout.COURSE_CENTER_X,
            baselineY = CertificateLayout.COURSE_START_Y,
            secondLineY = CertificateLayout.COURSE_SECOND_LINE_Y,
            maxWidth = CertificateLayout.COURSE_MAX_WIDTH,
            preferredSize = CertificateLayout.COURSE_PREFERRED_SIZE,
            minimumSingleLineSize = CertificateLayout.COURSE_MIN_SINGLE_LINE_SIZE,
            preferred2LineSize = CertificateLayout.COURSE_PREFERRED_2LINE_SIZE,
            minimumSize = CertificateLayout.COURSE_MIN_SIZE,
            paint = paint
        )

        // Reset color to INK for remaining metadata fields
        paint.color = INK

        // 5. Session Range
        // Left aligned after template 'Session:' label at SESSION_X = 1100f, baselineY = 1110f
        if (cert.session.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.session,
                x = CertificateLayout.SESSION_X,
                baselineY = CertificateLayout.SESSION_Y,
                maxWidth = CertificateLayout.SESSION_MAX_WIDTH,
                preferredSize = CertificateLayout.SESSION_PREFERRED_SIZE,
                minimumSize = CertificateLayout.SESSION_MIN_SIZE,
                paint = paint
            )
        }

        // 6. Performance Grade
        // Left aligned after template 'Performance Grade:' label at GRADE_X = 1765f, baselineY = 1110f
        if (cert.grade.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.grade,
                x = CertificateLayout.GRADE_X,
                baselineY = CertificateLayout.GRADE_Y,
                maxWidth = CertificateLayout.GRADE_MAX_WIDTH,
                preferredSize = CertificateLayout.GRADE_PREFERRED_SIZE,
                minimumSize = CertificateLayout.GRADE_MIN_SIZE,
                paint = paint
            )
        }

        // 7. Run By (Institute)
        // Left aligned at INFO_BOX_X = 885f, baselineY = 1211f
        if (cert.runBy.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.runBy,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.RUN_BY_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 8. Duration
        // Left aligned at INFO_BOX_X = 885f, baselineY = 1275f
        if (cert.duration.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.duration,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.DURATION_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 9. Date of Issue
        // Left aligned at INFO_BOX_X = 885f, baselineY = 1339f
        if (cert.dateOfIssue.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.dateOfIssue,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.DATE_OF_ISSUE_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 10. Place of Issue
        // Left aligned at INFO_BOX_X = 885f, baselineY = 1405f
        if (cert.placeOfIssue.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cert.placeOfIssue,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.PLACE_OF_ISSUE_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 11. Website
        // Left aligned at INFO_BOX_X = 885f, baselineY = 1470f
        val cleanWebsite = cleanWebsiteForDisplay(cert.website)
        if (cleanWebsite.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanWebsite,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.WEBSITE_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.WEBSITE_PREFERRED_SIZE,
                minimumSize = CertificateLayout.WEBSITE_MIN_SIZE,
                paint = paint
            )
        }

        // 12. Roll No / Certificate / Registration Number (Bottom-Right)
        // Left aligned after template 'ROLL NO/REG NO.:' label at CERT_NO_X = 2100f, baselineY = 1475f
        val certNoToDraw = cert.certificateId.ifBlank { cert.rollNo }
        if (certNoToDraw.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = certNoToDraw,
                x = CertificateLayout.CERT_NO_X,
                baselineY = CertificateLayout.CERT_NO_Y,
                maxWidth = CertificateLayout.CERT_NO_MAX_WIDTH,
                preferredSize = CertificateLayout.CERT_NO_PREFERRED_SIZE,
                minimumSize = CertificateLayout.CERT_NO_MIN_SIZE,
                paint = paint
            )
        }
    }

    /**
     * Normalizes consecutive whitespace into single spaces and trims ends.
     */
    fun normalizeWhitespace(text: String): String {
        return text.trim().replace(Regex("\\s+"), " ")
    }

    /**
     * Normalizes guardian relation prefix (S/O, D/O, W/O, C/O) without duplication.
     */
    fun normalizeGuardian(guardian: String): String {
        val clean = normalizeWhitespace(guardian)
        if (clean.isBlank()) return ""

        val prefixMatch = Regex("^(S/O|D/O|W/O|C/O|SO|DO|WO|CO)[.:\\s-]*\\s*", RegexOption.IGNORE_CASE).find(clean)
        if (prefixMatch != null) {
            val rawPrefix = prefixMatch.groupValues[1].uppercase()
            val normalizedPrefix = when (rawPrefix) {
                "SO" -> "S/O"
                "DO" -> "D/O"
                "WO" -> "W/O"
                "CO" -> "C/O"
                else -> rawPrefix
            }
            val rest = clean.substring(prefixMatch.range.last + 1).trim()
            return if (rest.isNotEmpty()) "$normalizedPrefix $rest" else normalizedPrefix
        }
        return "S/O $clean"
    }

    /**
     * Strips protocol and trailing slashes for clean aesthetic certificate display.
     */
    fun cleanWebsiteForDisplay(website: String): String {
        var clean = website.trim()
        if (clean.startsWith("https://", ignoreCase = true)) {
            clean = clean.substring(8)
        } else if (clean.startsWith("http://", ignoreCase = true)) {
            clean = clean.substring(7)
        }
        return clean.trimEnd('/')
    }

    private fun drawDynamicQr(canvas: Canvas, qr: Bitmap?) {
        if (qr == null || qr.isRecycled || qr.width <= 0 || qr.height <= 0) return

        val destination = Rect(
            CertificateLayout.QR_BITMAP_LEFT.toInt(),
            CertificateLayout.QR_BITMAP_TOP.toInt(),
            (CertificateLayout.QR_BITMAP_LEFT + CertificateLayout.QR_BITMAP_SIZE).toInt(),
            (CertificateLayout.QR_BITMAP_TOP + CertificateLayout.QR_BITMAP_SIZE).toInt()
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(qr, null, destination, paint)
    }

    /**
     * Calculates the maximum font size (up to preferredSize) that guarantees the text fits within maxWidth.
     * Uses high-precision binary search to find the optimal size without truncation.
     */
    fun calculateFittedTextSize(
        text: String,
        paint: Paint,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float = 8f
    ): Float {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return preferredSize

        paint.textSize = preferredSize
        if (paint.measureText(clean) <= maxWidth) {
            return preferredSize
        }

        // Binary search for highest font size where measureText <= maxWidth
        var low = 1f
        var high = preferredSize
        var bestSize = 1f

        for (i in 0 until 24) {
            val mid = (low + high) / 2f
            paint.textSize = mid
            if (paint.measureText(clean) <= maxWidth) {
                bestSize = mid
                low = mid
            } else {
                high = mid
            }
        }

        // Final safety check to strictly guarantee width <= maxWidth
        paint.textSize = bestSize
        while (paint.measureText(clean) > maxWidth && bestSize > 1f) {
            bestSize -= 0.1f
            paint.textSize = bestSize
        }

        paint.textSize = bestSize
        return bestSize
    }

    /**
     * Renders student name centered around centerX.
     * Prefers reducing font size on a single line so the name looks prominent and dignified.
     * Never truncates and never displays "...".
     */
    fun drawStudentName(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        val singleLineSize = calculateFittedTextSize(clean, paint, maxWidth, preferredSize)
        // Prefer single-line reduction. Only in the most extreme cases (< 22f) with multiple words, wrap into 2 lines.
        if (singleLineSize >= 22f) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = singleLineSize
            canvas.drawText(clean, centerX, baselineY, paint)
            return
        }

        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.size >= 2) {
            drawWrappedCenteredText(
                canvas = canvas,
                text = clean,
                centerX = centerX,
                baselineY = baselineY,
                maxWidth = maxWidth,
                preferredSize = preferredSize,
                minimumSingleLineSize = 22f,
                preferred2LineSize = 36f,
                minimumSize = 16f,
                paint = paint
            )
        } else {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = singleLineSize
            canvas.drawText(clean, centerX, baselineY, paint)
        }
    }

    /**
     * Renders single-line text centered around centerX, scaling font size down to fit within maxWidth.
     * Never truncates and never displays "...".
     */
    fun drawCenteredFittedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float = 8f,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        val size = calculateFittedTextSize(clean, paint, maxWidth, preferredSize, minimumSize)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        canvas.drawText(clean, centerX, baselineY, paint)
    }

    /**
     * Renders single-line text left-aligned at x, scaling font size down to fit within maxWidth.
     * Never truncates and never displays "...".
     */
    fun drawLeftFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float = 8f,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        val size = calculateFittedTextSize(clean, paint, maxWidth, preferredSize, minimumSize)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = size
        canvas.drawText(clean, x, baselineY, paint)
    }

    /**
     * Renders centered text across 1 or 2 lines.
     * If single line fits within maxWidth at or above minimumSingleLineSize, centers between baselineY and secondLineY.
     * Otherwise splits across two lines with balanced word distribution.
     */
    fun drawCenteredMultilineFittedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        secondLineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSingleLineSize: Float,
        preferred2LineSize: Float,
        minimumSize: Float = 8f,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        // 1. Try single-line dynamic font reduction first
        val singleLineSize = calculateFittedTextSize(clean, paint, maxWidth, preferredSize)
        if (singleLineSize >= minimumSingleLineSize) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = singleLineSize
            val singleBaseline = (baselineY + secondLineY) / 2f
            canvas.drawText(clean, centerX, singleBaseline, paint)
            return
        }

        // 2. Try controlled two-line wrapping if text contains multiple words
        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.size >= 2) {
            var bestSplit = 1
            var bestPenalty = Float.MAX_VALUE
            paint.textSize = preferred2LineSize

            for (i in 1 until words.size) {
                val l1 = words.subList(0, i).joinToString(" ")
                val l2 = words.subList(i, words.size).joinToString(" ")
                val w1 = paint.measureText(l1)
                val w2 = paint.measureText(l2)

                val overflow = max(0f, w1 - maxWidth) + max(0f, w2 - maxWidth)
                val imbalance = abs(w1 - w2)
                val penalty = (overflow * 5f) + imbalance

                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestSplit = i
                }
            }

            val line1 = words.subList(0, bestSplit).joinToString(" ")
            val line2 = words.subList(bestSplit, words.size).joinToString(" ")

            val s1 = calculateFittedTextSize(line1, paint, maxWidth, preferred2LineSize, minimumSize)
            val s2 = calculateFittedTextSize(line2, paint, maxWidth, preferred2LineSize, minimumSize)
            val twoLineSize = min(s1, s2)

            paint.textSize = twoLineSize
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(line1, centerX, baselineY, paint)
            canvas.drawText(line2, centerX, secondLineY, paint)
            return
        }

        // Fallback for single long word
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = singleLineSize
        val singleBaseline = (baselineY + secondLineY) / 2f
        canvas.drawText(clean, centerX, singleBaseline, paint)
    }

    /**
     * Renders centered text, first attempting single-line scaling down to minimumSingleLineSize.
     * If the text exceeds maxWidth at that size, wraps cleanly into two balanced lines centered
     * vertically around the intended field area.
     * Never truncates and never displays "...".
     */
    fun drawWrappedCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSingleLineSize: Float,
        preferred2LineSize: Float,
        minimumSize: Float = 8f,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        // 1. Try single-line dynamic font reduction first
        val singleLineSize = calculateFittedTextSize(clean, paint, maxWidth, preferredSize)
        if (singleLineSize >= minimumSingleLineSize) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = singleLineSize
            canvas.drawText(clean, centerX, baselineY, paint)
            return
        }

        // 2. Try controlled two-line wrapping if text contains multiple words
        val words = clean.split(" ").filter { it.isNotBlank() }
        if (words.size >= 2) {
            // Find the optimal balanced word split that minimizes length imbalance and overflow
            var bestSplit = 1
            var bestPenalty = Float.MAX_VALUE
            paint.textSize = preferred2LineSize

            for (i in 1 until words.size) {
                val l1 = words.subList(0, i).joinToString(" ")
                val l2 = words.subList(i, words.size).joinToString(" ")
                val w1 = paint.measureText(l1)
                val w2 = paint.measureText(l2)

                val overflow = max(0f, w1 - maxWidth) + max(0f, w2 - maxWidth)
                val imbalance = abs(w1 - w2)
                val penalty = (overflow * 5f) + imbalance

                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestSplit = i
                }
            }

            val line1 = words.subList(0, bestSplit).joinToString(" ")
            val line2 = words.subList(bestSplit, words.size).joinToString(" ")

            val s1 = calculateFittedTextSize(line1, paint, maxWidth, preferred2LineSize, minimumSize)
            val s2 = calculateFittedTextSize(line2, paint, maxWidth, preferred2LineSize, minimumSize)
            val twoLineSize = min(s1, s2)

            // Compute optical vertical center of single-line preferred size at baselineY
            paint.textSize = preferredSize
            val refFm = paint.fontMetrics
            val opticalCenterY = baselineY + (refFm.ascent + refFm.descent) / 2f

            // Position both lines centered vertically around opticalCenterY
            paint.textSize = twoLineSize
            val fm = paint.fontMetrics
            val lineSpacing = (fm.descent - fm.ascent) * 1.15f

            val line1CenterY = opticalCenterY - (lineSpacing / 2f)
            val line2CenterY = opticalCenterY + (lineSpacing / 2f)

            val line1Baseline = line1CenterY - (fm.ascent + fm.descent) / 2f
            val line2Baseline = line2CenterY - (fm.ascent + fm.descent) / 2f

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(line1, centerX, line1Baseline, paint)
            canvas.drawText(line2, centerX, line2Baseline, paint)
            return
        }

        // Single word fallback (cannot be word-wrapped without hyphenation): scale to fit
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = singleLineSize
        canvas.drawText(clean, centerX, baselineY, paint)
    }
}
