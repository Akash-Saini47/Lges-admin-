package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.database.Certificate
import com.example.util.AppLogger
import com.example.util.CertificateConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Data structure for rendering a single certificate.
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

    // Palette
    private val NAVY = 0xFF0B1B3D.toInt()
    private val INK = 0xFF1E293B.toInt()

    /**
     * Converts a database Certificate into CertificateData for rendering.
     */
    fun buildCertificateData(
        cert: Certificate,
        instituteName: String = CertificateConfig.DEFAULT_INSTITUTE_NAME,
        websiteUrl: String = CertificateConfig.DEFAULT_INSTITUTE_WEBSITE
    ): CertificateData {
        val normalizedRollNo = cert.rollNo.trim().ifBlank { cert.certificateId.trim() }
        val normalizedWebsite = websiteUrl.trim().removeSuffix("/")

        return CertificateData(
            rollNo = normalizedRollNo,
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
            color = INK
            textAlign = Paint.Align.CENTER
        }

        // Student Name
        drawFittedText(
            canvas = canvas,
            text = cert.studentName,
            centerX = 1480f,
            baselineY = 668f,
            maxWidth = 1050f,
            preferredSize = 78f,
            minimumSize = 35f,
            paint = paint
        )

        // Guardian
        paint.textSize = 30f
        val guardianText = normalizeGuardian(cert.guardian)
        if (guardianText.isNotBlank()) {
            drawFittedText(
                canvas = canvas,
                text = guardianText,
                centerX = 1480f,
                baselineY = 800f,
                maxWidth = 900f,
                preferredSize = 30f,
                minimumSize = 20f,
                paint = paint
            )
        }

        // Course Name
        paint.color = NAVY
        drawFittedText(
            canvas = canvas,
            text = cert.course,
            centerX = 1480f,
            baselineY = 968f,
            maxWidth = 1100f,
            preferredSize = 70f,
            minimumSize = 28f,
            paint = paint
        )

        // Session Range
        paint.textAlign = Paint.Align.LEFT
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.session,
            x = 1100f,
            baselineY = 1125f,
            maxWidth = 500f,
            preferredSize = 30f,
            minimumSize = 18f,
            paint = paint
        )

        // Performance Grade
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.grade,
            x = 1740f,
            baselineY = 1125f,
            maxWidth = 300f,
            preferredSize = 30f,
            minimumSize = 18f,
            paint = paint
        )

        // Run By (Institute)
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.runBy,
            x = 900f,
            baselineY = 1238f,
            maxWidth = 550f,
            preferredSize = 24f,
            minimumSize = 16f,
            paint = paint
        )

        // Duration
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.duration,
            x = 900f,
            baselineY = 1300f,
            maxWidth = 550f,
            preferredSize = 24f,
            minimumSize = 16f,
            paint = paint
        )

        // Date of Issue
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.dateOfIssue,
            x = 900f,
            baselineY = 1362f,
            maxWidth = 550f,
            preferredSize = 24f,
            minimumSize = 16f,
            paint = paint
        )

        // Place of Issue
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.placeOfIssue,
            x = 900f,
            baselineY = 1422f,
            maxWidth = 550f,
            preferredSize = 24f,
            minimumSize = 16f,
            paint = paint
        )

        // Website
        drawFittedTextLeft(
            canvas = canvas,
            text = cleanWebsiteForDisplay(cert.website),
            x = 900f,
            baselineY = 1482f,
            maxWidth = 700f,
            preferredSize = 22f,
            minimumSize = 14f,
            paint = paint
        )

        // Roll / Registration Number
        drawFittedTextLeft(
            canvas = canvas,
            text = cert.rollNo,
            x = 2080f,
            baselineY = 1485f,
            maxWidth = 300f,
            preferredSize = 25f,
            minimumSize = 15f,
            paint = paint
        )
    }

    private fun normalizeGuardian(guardian: String): String {
        val clean = guardian.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return ""

        if (clean.startsWith("S/O ", ignoreCase = true) ||
            clean.startsWith("D/O ", ignoreCase = true) ||
            clean.startsWith("W/O ", ignoreCase = true) ||
            clean.startsWith("C/O ", ignoreCase = true)
        ) {
            return clean
        }
        return "S/O $clean"
    }

    private fun cleanWebsiteForDisplay(website: String): String {
        return website.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
    }

    private fun drawDynamicQr(canvas: Canvas, qr: Bitmap?) {
        if (qr == null || qr.isRecycled || qr.width <= 0 || qr.height <= 0) return

        val left = 1545
        val top = 1275
        val size = 175

        val destination = Rect(left + 6, top + 6, left + size - 6, top + size - 6)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(qr, null, destination, paint)
    }

    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float,
        paint: Paint
    ) {
        val cleanText = text.trim().replace(Regex("\\s+"), " ")
        if (cleanText.isBlank()) return

        var size = preferredSize
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size

        while (size > minimumSize && paint.measureText(cleanText) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }

        val finalText = fitTextWithEllipsis(cleanText, paint, maxWidth)
        canvas.drawText(finalText, centerX, baselineY, paint)
    }

    private fun drawFittedTextLeft(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        preferredSize: Float,
        minimumSize: Float,
        paint: Paint
    ) {
        val cleanText = text.trim().replace(Regex("\\s+"), " ")
        if (cleanText.isBlank()) return

        var size = preferredSize
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = size

        while (size > minimumSize && paint.measureText(cleanText) > maxWidth) {
            size -= 1f
            paint.textSize = size
        }

        val finalText = fitTextWithEllipsis(cleanText, paint, maxWidth)
        canvas.drawText(finalText, x, baselineY, paint)
    }

    private fun fitTextWithEllipsis(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isBlank() || paint.measureText(text) <= maxWidth) return text

        val ellipsis = "..."
        if (paint.measureText(ellipsis) > maxWidth) return ""

        var end = text.length
        while (end > 0) {
            val candidate = text.substring(0, end).trimEnd() + ellipsis
            if (paint.measureText(candidate) <= maxWidth) {
                return candidate
            }
            end--
        }
        return ellipsis
    }
}