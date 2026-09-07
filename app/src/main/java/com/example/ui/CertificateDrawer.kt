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
 * Master coordinate system and layout constants for the 1536x1024 certificate canvas.
 * Derived from the visual reference certificate (Reference_certificate.png).
 */
data class FieldConfig(
    val anchorX: Float,
    val anchorY: Float,
    val maxWidth: Float,
    val maxHeight: Float = 0f,
    val alignment: Paint.Align = Paint.Align.LEFT,
    val minFontSize: Float = 8f,
    val maxFontSize: Float = 16f,
    val typeface: Typeface? = null,
    val color: Int = COLOR_LGES_INK_ARGB,
    val lineSpacing: Float = 1.1f,
    val letterSpacing: Float = 0f
)

object CertificateLayout {
    const val canvasWidth = 1536
    const val canvasHeight = 1024

    const val WIDTH = 1536f
    const val HEIGHT = 1024f

    const val CANVAS_WIDTH = 1536
    const val CANVAS_HEIGHT = 1024

    // Content center: aligned with template's "This certificate is awarded to", gold line, and "In recognition of"
    const val CONTENT_CENTER_X = 866f

    // 1. Roll No (Top-Left of Content Area)
    // Template 'Roll No.:' label ends at X = 508f with baseline Y = 345f.
    // Dynamic value starts after the label at X = 517f.
    const val ROLL_LABEL_END_X = 508f
    const val ROLL_X = 517f
    const val ROLL_VALUE_X = 517f
    const val ROLL_Y = 345f
    const val ROLL_MAX_WIDTH = 220f
    const val ROLL_PREFERRED_SIZE = 17f
    const val ROLL_MIN_SIZE = 9f

    // 2. Student Name
    // Centered at CONTENT_CENTER_X = 866f, baseline Y = 425f.
    const val NAME_CENTER_X = 866f
    const val NAME_Y = 425f
    const val NAME_MAX_WIDTH = 736f
    const val NAME_PREFERRED_SIZE = 38f
    const val NAME_MIN_SIZE = 18f

    // 3. Father / Guardian Name
    // Centered at CONTENT_CENTER_X = 866f, baseline Y = 458f.
    const val FATHER_CENTER_X = 866f
    const val FATHER_Y = 458f
    const val FATHER_MAX_WIDTH = 608f
    const val FATHER_PREFERRED_SIZE = 18f
    const val FATHER_MIN_SIZE = 11f

    // 4. Course Name (Strict vertical bounding between 528f and 608f)
    const val COURSE_CENTER_X = 866f
    const val COURSE_TOP = 528f
    const val COURSE_BOTTOM = 608f
    const val COURSE_START_Y = 560f
    const val COURSE_SECOND_LINE_Y = 588f
    const val COURSE_MAX_WIDTH = 832f
    const val COURSE_PREFERRED_SIZE = 33f
    const val COURSE_MIN_SINGLE_LINE_SIZE = 23f
    const val COURSE_PREFERRED_2LINE_SIZE = 24f
    const val COURSE_MIN_SIZE = 13f

    // 5. Session Range
    // Template 'Session:' label ends at X = 701f with baseline Y = 707f.
    const val SESSION_LABEL_END_X = 701f
    const val SESSION_X = 714f
    const val SESSION_Y = 707f
    const val SESSION_MAX_WIDTH = 166f
    const val SESSION_PREFERRED_SIZE = 17f
    const val SESSION_MIN_SIZE = 10f

    // 6. Performance Grade
    // Template 'Performance Grade:' label ends at X = 1122f with baseline Y = 707f.
    const val GRADE_LABEL_END_X = 1122f
    const val GRADE_X = 1136f
    const val GRADE_Y = 707f
    const val GRADE_MAX_WIDTH = 115f
    const val GRADE_PREFERRED_SIZE = 17f
    const val GRADE_MIN_SIZE = 10f

    // 7. Lower Information Box
    // Template colons are all at X = 582f.
    // Dynamic values start at X = 592f, left aligned.
    const val INFO_BOX_X = 592f
    const val INFO_BOX_MAX_WIDTH = 374f
    const val RUN_BY_Y = 816f
    const val DURATION_Y = 856f
    const val DATE_OF_ISSUE_Y = 893f
    const val PLACE_OF_ISSUE_Y = 918f
    const val WEBSITE_Y = 941f
    const val INFO_BOX_PREFERRED_SIZE = 15.5f
    const val INFO_BOX_MIN_SIZE = 9f
    const val WEBSITE_PREFERRED_SIZE = 14f
    const val WEBSITE_MIN_SIZE = 9f

    // 8. Dynamic QR Code Box Bounds
    // QR Gold Frame: Left=985, Top=805, Right=1100, Bottom=922 (Width=115, Height=117)
    const val QR_FRAME_LEFT = 985f
    const val QR_FRAME_TOP = 805f
    const val QR_FRAME_RIGHT = 1100f
    const val QR_FRAME_BOTTOM = 922f

    const val QR_CLEAR_LEFT = 988f
    const val QR_CLEAR_TOP = 808f
    const val QR_CLEAR_RIGHT = 1097f
    const val QR_CLEAR_BOTTOM = 919f

    const val QR_LEFT = 985f
    const val QR_TOP = 805f
    const val QR_SIZE = 115f

    const val QR_DEST_LEFT = 994f
    const val QR_DEST_TOP = 815f
    const val QR_DEST_SIZE = 96f

    const val QR_BITMAP_LEFT = 994f
    const val QR_BITMAP_TOP = 815f
    const val QR_BITMAP_SIZE = 96f

    // 9. Bottom-Right Certificate No / Reg No
    // Template 'Certificate No/Reg No.:' ends at X = 1328f with baseline Y = 944f.
    const val CERT_NO_LABEL_END_X = 1328f
    const val CERT_NO_X = 1347f
    const val CERT_NO_Y = 944f
    const val CERT_NO_MAX_WIDTH = 155f
    const val CERT_NO_PREFERRED_SIZE = 15.5f
    const val CERT_NO_MIN_SIZE = 8f

    // Structured Field Configurations
    val rollNo = FieldConfig(
        anchorX = ROLL_VALUE_X,
        anchorY = ROLL_Y,
        maxWidth = ROLL_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = ROLL_MIN_SIZE,
        maxFontSize = ROLL_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val awardedTo = FieldConfig(
        anchorX = CONTENT_CENTER_X,
        anchorY = 253f,
        maxWidth = 600f,
        maxHeight = 30f,
        alignment = Paint.Align.CENTER,
        minFontSize = 14f,
        maxFontSize = 18f,
        color = COLOR_LGES_INK_ARGB
    )

    val studentName = FieldConfig(
        anchorX = CONTENT_CENTER_X,
        anchorY = NAME_Y,
        maxWidth = NAME_MAX_WIDTH,
        maxHeight = 60f,
        alignment = Paint.Align.CENTER,
        minFontSize = NAME_MIN_SIZE,
        maxFontSize = NAME_PREFERRED_SIZE,
        color = COLOR_LGES_NAVY_ARGB
    )

    val fatherName = FieldConfig(
        anchorX = CONTENT_CENTER_X,
        anchorY = FATHER_Y,
        maxWidth = FATHER_MAX_WIDTH,
        maxHeight = 30f,
        alignment = Paint.Align.CENTER,
        minFontSize = FATHER_MIN_SIZE,
        maxFontSize = FATHER_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val courseName = FieldConfig(
        anchorX = CONTENT_CENTER_X,
        anchorY = COURSE_START_Y,
        maxWidth = COURSE_MAX_WIDTH,
        maxHeight = COURSE_BOTTOM - COURSE_TOP,
        alignment = Paint.Align.CENTER,
        minFontSize = COURSE_MIN_SIZE,
        maxFontSize = COURSE_PREFERRED_SIZE,
        color = COLOR_LGES_NAVY_ARGB
    )

    val institute = FieldConfig(
        anchorX = CONTENT_CENTER_X,
        anchorY = 653f,
        maxWidth = 650f,
        maxHeight = 30f,
        alignment = Paint.Align.CENTER,
        minFontSize = 12f,
        maxFontSize = 18f,
        color = COLOR_LGES_INK_ARGB
    )

    val session = FieldConfig(
        anchorX = SESSION_X,
        anchorY = SESSION_Y,
        maxWidth = SESSION_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = SESSION_MIN_SIZE,
        maxFontSize = SESSION_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val performanceGrade = FieldConfig(
        anchorX = GRADE_X,
        anchorY = GRADE_Y,
        maxWidth = GRADE_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = GRADE_MIN_SIZE,
        maxFontSize = GRADE_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val runBy = FieldConfig(
        anchorX = INFO_BOX_X,
        anchorY = RUN_BY_Y,
        maxWidth = INFO_BOX_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = INFO_BOX_MIN_SIZE,
        maxFontSize = INFO_BOX_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val courseDuration = FieldConfig(
        anchorX = INFO_BOX_X,
        anchorY = DURATION_Y,
        maxWidth = INFO_BOX_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = INFO_BOX_MIN_SIZE,
        maxFontSize = INFO_BOX_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val dateOfIssue = FieldConfig(
        anchorX = INFO_BOX_X,
        anchorY = DATE_OF_ISSUE_Y,
        maxWidth = INFO_BOX_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = INFO_BOX_MIN_SIZE,
        maxFontSize = INFO_BOX_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val placeOfIssue = FieldConfig(
        anchorX = INFO_BOX_X,
        anchorY = PLACE_OF_ISSUE_Y,
        maxWidth = INFO_BOX_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = INFO_BOX_MIN_SIZE,
        maxFontSize = INFO_BOX_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val website = FieldConfig(
        anchorX = INFO_BOX_X,
        anchorY = WEBSITE_Y,
        maxWidth = INFO_BOX_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = WEBSITE_MIN_SIZE,
        maxFontSize = WEBSITE_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )

    val qrCode = FieldConfig(
        anchorX = QR_DEST_LEFT,
        anchorY = QR_DEST_TOP,
        maxWidth = QR_DEST_SIZE,
        maxHeight = QR_DEST_SIZE,
        minFontSize = 0f,
        maxFontSize = 0f
    )

    val directorSignature = FieldConfig(
        anchorX = 1295f,
        anchorY = 907f,
        maxWidth = 200f,
        maxHeight = 40f,
        alignment = Paint.Align.CENTER,
        minFontSize = 10f,
        maxFontSize = 16f,
        color = COLOR_LGES_INK_ARGB
    )

    val certificateNumber = FieldConfig(
        anchorX = CERT_NO_X,
        anchorY = CERT_NO_Y,
        maxWidth = CERT_NO_MAX_WIDTH,
        maxHeight = 25f,
        alignment = Paint.Align.LEFT,
        minFontSize = CERT_NO_MIN_SIZE,
        maxFontSize = CERT_NO_PREFERRED_SIZE,
        color = COLOR_LGES_INK_ARGB
    )
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
    const val BASE_WIDTH = 1536f
    const val BASE_HEIGHT = 1024f
    const val W = 1536
    const val H = 1024

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
        const val CANVAS_WIDTH = 1536
        const val CANVAS_HEIGHT = 1024

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

        const val QR_BOX_LEFT = 985
        const val QR_BOX_TOP = 805
        const val QR_BOX_WIDTH = 115
        const val QR_BOX_HEIGHT = 117

        const val QR_DEST_LEFT = 994
        const val QR_DEST_TOP = 815
        const val QR_DEST_SIZE = 96
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
     * Full-resolution render (1536 x 1024). Used for export and high-fidelity output.
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
     * Renders dynamic data and QR directly onto a canvas without requiring the PDF template.
     * Useful for overlays, preview compositing, and test verification.
     */
    fun renderDynamicOverlay(canvas: Canvas, cert: CertificateData, qr: Bitmap? = null) {
        drawDynamicData(canvas = canvas, cert = cert)
        drawDynamicQr(canvas = canvas, qr = qr)
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
        // 1. Try loading Reference_certificate.png directly from assets for lossless 1:1 pixel rendering
        try {
            context.assets.open("Reference_certificate.png").use { stream ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream, null, options)
                if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                    if (bitmap.width == W && bitmap.height == H) {
                        return bitmap
                    }
                    val scaled = Bitmap.createScaledBitmap(bitmap, W, H, true)
                    if (scaled != bitmap) {
                        bitmap.recycle()
                    }
                    return scaled
                }
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "PNG template load fallback to PDF: ${e.message}")
        }

        // 2. Fallback to PDF template
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
        // Left aligned after template 'Roll No.:' label at ROLL_VALUE_X = 808f, baseline = 540f
        val cleanRoll = cleanRollNoForDisplay(cert.rollNo)
        if (cleanRoll.isNotBlank()) {
            paint.color = INK
            drawLeftFittedText(
                canvas = canvas,
                text = cleanRoll,
                x = CertificateLayout.ROLL_VALUE_X,
                baselineY = CertificateLayout.ROLL_Y,
                maxWidth = CertificateLayout.ROLL_MAX_WIDTH,
                preferredSize = CertificateLayout.ROLL_PREFERRED_SIZE,
                minimumSize = CertificateLayout.ROLL_MIN_SIZE,
                paint = paint
            )
        }

        // 2. Student Name
        // Centered around NAME_CENTER_X = 1353f, baselineY = 665f, in NAVY
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
        // Centered around FATHER_CENTER_X = 1353f, baselineY = 715f, in INK
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
        // Strict boundaries between COURSE_TOP = 825f and COURSE_BOTTOM = 950f (never crossing gold line at 961f)
        paint.color = NAVY
        drawCourseName(
            canvas = canvas,
            text = cert.course,
            centerX = CertificateLayout.COURSE_CENTER_X,
            topBound = CertificateLayout.COURSE_TOP,
            bottomBound = CertificateLayout.COURSE_BOTTOM,
            maxWidth = CertificateLayout.COURSE_MAX_WIDTH,
            preferredSize = CertificateLayout.COURSE_PREFERRED_SIZE,
            minSingleLineSize = CertificateLayout.COURSE_MIN_SINGLE_LINE_SIZE,
            preferred2LineSize = CertificateLayout.COURSE_PREFERRED_2LINE_SIZE,
            minSize = CertificateLayout.COURSE_MIN_SIZE,
            paint = paint
        )

        // Reset color to INK for remaining metadata fields
        paint.color = INK

        // 5. Session Range
        // Left aligned after template 'Session:' label at SESSION_X = 1115f, baseline = 1105f
        val cleanSession = cleanSessionForDisplay(cert.session)
        if (cleanSession.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanSession,
                x = CertificateLayout.SESSION_X,
                baselineY = CertificateLayout.SESSION_Y,
                maxWidth = CertificateLayout.SESSION_MAX_WIDTH,
                preferredSize = CertificateLayout.SESSION_PREFERRED_SIZE,
                minimumSize = CertificateLayout.SESSION_MIN_SIZE,
                paint = paint
            )
        }

        // 6. Performance Grade
        // Left aligned after template 'Performance Grade:' label at GRADE_X = 1775f, baseline = 1105f
        val cleanGrade = cleanGradeForDisplay(cert.grade)
        if (cleanGrade.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanGrade,
                x = CertificateLayout.GRADE_X,
                baselineY = CertificateLayout.GRADE_Y,
                maxWidth = CertificateLayout.GRADE_MAX_WIDTH,
                preferredSize = CertificateLayout.GRADE_PREFERRED_SIZE,
                minimumSize = CertificateLayout.GRADE_MIN_SIZE,
                paint = paint
            )
        }

        // 7. Run By (Institute)
        // Left aligned after template 'Run By :' label at INFO_BOX_X = 925f, baseline = 1275f
        val cleanRunBy = cleanRunByForDisplay(cert.runBy)
        if (cleanRunBy.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanRunBy,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.RUN_BY_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 8. Duration
        // Left aligned after template 'Course Duration :' label at INFO_BOX_X = 925f, baseline = 1338f
        val cleanDuration = cleanDurationForDisplay(cert.duration)
        if (cleanDuration.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanDuration,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.DURATION_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 9. Date of Issue
        // Left aligned after template 'Date of Issue :' label at INFO_BOX_X = 925f, baseline = 1395f
        val cleanDate = cleanDateOfIssueForDisplay(cert.dateOfIssue)
        if (cleanDate.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanDate,
                x = CertificateLayout.INFO_BOX_X,
                baselineY = CertificateLayout.DATE_OF_ISSUE_Y,
                maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                paint = paint
            )
        }

        // 10. Place of Issue
        // Left aligned at INFO_BOX_X = 925f, baseline = 1435f
        val cleanPlace = cleanPlaceOfIssueForDisplay(cert.placeOfIssue)
        if (cleanPlace.isNotBlank() && !cleanPlace.equals("CHAMBA", ignoreCase = true) || cert.placeOfIssue.isNotBlank()) {
            if (cleanPlace.isNotBlank()) {
                drawLeftFittedText(
                    canvas = canvas,
                    text = cleanPlace,
                    x = CertificateLayout.INFO_BOX_X,
                    baselineY = CertificateLayout.PLACE_OF_ISSUE_Y,
                    maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH,
                    preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE,
                    minimumSize = CertificateLayout.INFO_BOX_MIN_SIZE,
                    paint = paint
                )
            }
        }

        // 11. Website
        // Left aligned after template 'Website :' label at INFO_BOX_X = 925f, baseline = 1470f
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
        // Left aligned after template 'Certificate No/Reg No.:' label at CERT_NO_X = 2105f, baseline = 1475f
        val rawCertNo = cert.certificateId.ifBlank { cert.rollNo }
        val cleanCertNo = cleanCertNoForDisplay(rawCertNo)
        if (cleanCertNo.isNotBlank()) {
            drawLeftFittedText(
                canvas = canvas,
                text = cleanCertNo,
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
     * Strips duplicate 'Roll No.:' or 'Roll No' prefix if entered by user.
     */
    fun cleanRollNoForDisplay(rollNo: String): String {
        val clean = normalizeWhitespace(rollNo)
        val prefixRegex = Regex("^(Roll\\s*No[.:\\s-]*|Roll[.:\\s-]+)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Session:' prefix if entered by user.
     */
    fun cleanSessionForDisplay(session: String): String {
        val clean = normalizeWhitespace(session)
        val prefixRegex = Regex("^(Session[.:\\s-]*|Academic\\s*Session[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Performance Grade:' or 'Grade:' prefix if entered by user.
     */
    fun cleanGradeForDisplay(grade: String): String {
        val clean = normalizeWhitespace(grade)
        val prefixRegex = Regex("^(Performance\\s*Grade[.:\\s-]*|Grade[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Certificate No/Reg No.:' or 'Cert No:' prefix if entered by user.
     */
    fun cleanCertNoForDisplay(certNo: String): String {
        val clean = normalizeWhitespace(certNo)
        val prefixRegex = Regex("^(Certificate\\s*No\\s*/\\s*Reg\\s*No[.:\\s-]*|Certificate\\s*No[.:\\s-]*|Cert\\s*No[.:\\s-]*|Reg\\s*No[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Run By:' prefix if entered by user.
     */
    fun cleanRunByForDisplay(runBy: String): String {
        val clean = normalizeWhitespace(runBy)
        val prefixRegex = Regex("^(Run\\s*By[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Course Duration:' or 'Duration:' prefix if entered by user.
     */
    fun cleanDurationForDisplay(duration: String): String {
        val clean = normalizeWhitespace(duration)
        val prefixRegex = Regex("^(Course\\s*Duration[.:\\s-]*|Duration[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Date of Issue:' or 'Date:' prefix if entered by user.
     */
    fun cleanDateOfIssueForDisplay(date: String): String {
        val clean = normalizeWhitespace(date)
        val prefixRegex = Regex("^(Date\\s*of\\s*Issue[.:\\s-]*|Date[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
    }

    /**
     * Strips duplicate 'Place of Issue:' or 'Place:' prefix if entered by user.
     */
    fun cleanPlaceOfIssueForDisplay(place: String): String {
        val clean = normalizeWhitespace(place)
        val prefixRegex = Regex("^(Place\\s*of\\s*Issue[.:\\s-]*|Place[.:\\s-]*)", RegexOption.IGNORE_CASE)
        return clean.replace(prefixRegex, "").trim()
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
     * Strips duplicate 'Website:' label, protocol and trailing slashes for clean aesthetic certificate display.
     */
    fun cleanWebsiteForDisplay(website: String): String {
        var clean = normalizeWhitespace(website)
        val prefixRegex = Regex("^(Website[.:\\s-]*)", RegexOption.IGNORE_CASE)
        clean = clean.replace(prefixRegex, "").trim()
        if (clean.startsWith("https://", ignoreCase = true)) {
            clean = clean.substring(8)
        } else if (clean.startsWith("http://", ignoreCase = true)) {
            clean = clean.substring(7)
        }
        return clean.trimEnd('/')
    }

    private fun drawDynamicQr(canvas: Canvas, qr: Bitmap?) {
        if (qr == null || qr.isRecycled || qr.width <= 0 || qr.height <= 0) return

        // 1. Clear the inner area of the gold QR frame with clean white
        val clearPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            CertificateLayout.QR_CLEAR_LEFT,
            CertificateLayout.QR_CLEAR_TOP,
            CertificateLayout.QR_CLEAR_RIGHT,
            CertificateLayout.QR_CLEAR_BOTTOM,
            clearPaint
        )

        // 2. Draw the QR bitmap centered with equal margins
        val destination = Rect(
            CertificateLayout.QR_DEST_LEFT.toInt(),
            CertificateLayout.QR_DEST_TOP.toInt(),
            (CertificateLayout.QR_DEST_LEFT + CertificateLayout.QR_DEST_SIZE).toInt(),
            (CertificateLayout.QR_DEST_TOP + CertificateLayout.QR_DEST_SIZE).toInt()
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
     * Reusable helper to draw auto-fit text centered within a designated field bounding box.
     */
    fun drawAutoFitCenteredText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        topY: Float,
        maxWidth: Float,
        maxHeight: Float,
        minSize: Float,
        maxSize: Float,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        val size = calculateFittedTextSize(clean, paint, maxWidth, maxSize, minSize)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size
        val fm = paint.fontMetrics
        val centerY = topY + (maxHeight / 2f)
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(clean, centerX, baseline, paint)
    }

    /**
     * Reusable helper to draw auto-fit left-aligned text within a designated field width.
     */
    fun drawAutoFitLeftText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        minSize: Float,
        maxSize: Float,
        paint: Paint
    ) {
        drawLeftFittedText(
            canvas = canvas,
            text = text,
            x = x,
            baselineY = baselineY,
            maxWidth = maxWidth,
            preferredSize = maxSize,
            minimumSize = minSize,
            paint = paint
        )
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
        // Prefer single-line reduction. Only in extreme cases (< 14f) with multiple words, wrap into 2 lines.
        if (singleLineSize >= 14f || !clean.contains(" ")) {
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
                minimumSingleLineSize = 14f,
                preferred2LineSize = 22f,
                minimumSize = minimumSize,
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
     * Renders course name strictly bounded between [topBound] and [bottomBound].
     * Guaranteed never to cross gold decorative line at 961f or touch text above at 790f.
     */
    fun drawCourseName(
        canvas: Canvas,
        text: String,
        centerX: Float,
        topBound: Float = CertificateLayout.COURSE_TOP,
        bottomBound: Float = CertificateLayout.COURSE_BOTTOM,
        maxWidth: Float = CertificateLayout.COURSE_MAX_WIDTH,
        preferredSize: Float = CertificateLayout.COURSE_PREFERRED_SIZE,
        minSingleLineSize: Float = CertificateLayout.COURSE_MIN_SINGLE_LINE_SIZE,
        preferred2LineSize: Float = CertificateLayout.COURSE_PREFERRED_2LINE_SIZE,
        minSize: Float = CertificateLayout.COURSE_MIN_SIZE,
        paint: Paint
    ) {
        val clean = normalizeWhitespace(text)
        if (clean.isBlank()) return

        val availableHeight = bottomBound - topBound
        val opticalCenterY = (topBound + bottomBound) / 2f

        // 1. Try single-line if it fits comfortably within maxWidth
        val singleLineSize = calculateFittedTextSize(clean, paint, maxWidth, preferredSize, minSize)
        if (singleLineSize >= minSingleLineSize) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = singleLineSize
            val fm = paint.fontMetrics
            val baseline = opticalCenterY - (fm.ascent + fm.descent) / 2f
            canvas.drawText(clean, centerX, baseline, paint)
            return
        }

        // 2. Try controlled 2-line wrapping
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
                val connectsWithSymbol = words.getOrNull(i)?.let { it == "&" || it.equals("and", ignoreCase = true) } == true
                val bonus = if (connectsWithSymbol && w1 <= maxWidth && w2 <= maxWidth) -150f else 0f
                val penalty = (overflow * 5f) + imbalance + bonus

                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestSplit = i
                }
            }

            val line1 = words.subList(0, bestSplit).joinToString(" ")
            val line2 = words.subList(bestSplit, words.size).joinToString(" ")

            val s1 = calculateFittedTextSize(line1, paint, maxWidth, preferred2LineSize, minSize)
            val s2 = calculateFittedTextSize(line2, paint, maxWidth, preferred2LineSize, minSize)
            var twoLineSize = min(s1, s2)

            // Ensure two lines fit within available vertical height
            paint.textSize = twoLineSize
            var fm = paint.fontMetrics
            val totalHeight = (fm.descent - fm.ascent) * 2.1f
            if (totalHeight > availableHeight) {
                val scale = availableHeight / totalHeight
                twoLineSize = max(minSize, twoLineSize * scale)
                paint.textSize = twoLineSize
                fm = paint.fontMetrics
            }

            val lineSpacing = (fm.descent - fm.ascent) * 1.1f
            val line1CenterY = opticalCenterY - (lineSpacing / 2f)
            val line2CenterY = opticalCenterY + (lineSpacing / 2f)

            val line1Baseline = line1CenterY - (fm.ascent + fm.descent) / 2f
            val line2Baseline = line2CenterY - (fm.ascent + fm.descent) / 2f

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(line1, centerX, line1Baseline, paint)
            canvas.drawText(line2, centerX, line2Baseline, paint)
            return
        }

        // Fallback for single very long word
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = singleLineSize
        val fm = paint.fontMetrics
        val baseline = opticalCenterY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(clean, centerX, baseline, paint)
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
