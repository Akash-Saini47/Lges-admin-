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
import com.example.util.CertificateConfig
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * CertificateDrawer
 *
 * Uses ONLY:
 *
 * assets/Reference_certificate.pdf
 *
 * The PDF provides the static certificate artwork.
 * Dynamic student/certificate information is drawn over it.
 *
 * Current certificate layout and coordinates are intentionally preserved.
 *
 * Certificate dimensions:
 * 2400 x 1600
 * Aspect ratio: 3:2
 *
 * Multiple certificates are supported.
 * Every Certificate object is rendered independently.
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

    // ================================================================
    // CERTIFICATE CANVAS
    // ================================================================

    private const val W = 2400
    private const val H = 1600

    /**
     * Increment this whenever Reference_certificate.pdf is replaced.
     *
     * The cache file is refreshed every time anyway, but keeping the
     * version in the filename makes stale-cache problems much less likely.
     */
    private const val TEMPLATE_VERSION = "v2"

    private const val TEMPLATE_FILE = "Reference_certificate.pdf"

    // ================================================================
    // TYPEFACES
    // ================================================================

    private val SERIF_BOLD =
        Typeface.create(
            Typeface.SERIF,
            Typeface.BOLD
        )

    private val SERIF =
        Typeface.create(
            Typeface.SERIF,
            Typeface.NORMAL
        )

    // ================================================================
    // COLORS
    // ================================================================

    private val NAVY =
        0xFF0B1B3D.toInt()

    private val INK =
        0xFF1E293B.toInt()

    // ================================================================
    // PUBLIC API
    // ================================================================

    /**
     * Converts a database Certificate into CertificateData.
     *
     * Each certificate is handled independently.
     *
     * This is important when the same student has:
     *
     * - Course certificate
     * - Internship certificate
     * - Multiple course certificates
     * - Certificates with different roll numbers
     */
    fun drawCertificate(
        context: Context,
        cert: Certificate,
        qrBitmap: Bitmap?
    ): Bitmap {

        val normalizedRollNo =
            cert.rollNo
                .trim()
                .ifBlank {
                    cert.certificateId.trim()
                }

        val normalizedWebsite =
            CertificateConfig.DEFAULT_INSTITUTE_WEBSITE
                .trim()
                .removeSuffix("/")

        val data =
            CertificateData(
                rollNo = normalizedRollNo,

                studentName =
                    cert.studentName.trim(),

                guardian =
                    cert.fatherName.trim(),

                course =
                    cert.courseName.trim(),

                session =
                    cert.sessionRange.trim(),

                grade =
                    cert.grade.trim(),

                runBy =
                    "Lakshmi Group of Education Society",

                duration =
                    cert.duration.trim(),

                dateOfIssue =
                    cert.dateOfIssue.trim(),

                placeOfIssue =
                    cert.placeOfIssue
                        .trim()
                        .ifBlank {
                            "CHAMBA"
                        },

                website =
                    normalizedWebsite
            )

        return draw(
            context = context,
            cert = data,
            qr = qrBitmap
        )
    }

    /**
     * Main rendering function.
     *
     * The static artwork is loaded from Reference_certificate.pdf.
     * Dynamic information is then drawn using the existing coordinates.
     */
    fun draw(
        context: Context,
        cert: CertificateData,
        qr: Bitmap?
    ): Bitmap {

        val template =
            loadReferenceCertificate(context)
                ?: throw IllegalStateException(
                    """
                    Certificate template could not be loaded.
                    
                    Required asset:
                    assets/$TEMPLATE_FILE
                    
                    No fallback certificate template is used.
                    """.trimIndent()
                )

        val output =
            Bitmap.createBitmap(
                W,
                H,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(output)

        // ------------------------------------------------------------
        // STATIC TEMPLATE
        // ------------------------------------------------------------

        val bitmapPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        canvas.drawBitmap(
            template,
            null,
            Rect(
                0,
                0,
                W,
                H
            ),
            bitmapPaint
        )

        // ------------------------------------------------------------
        // DYNAMIC TEXT
        // ------------------------------------------------------------

        drawDynamicData(
            canvas = canvas,
            cert = cert
        )

        // ------------------------------------------------------------
        // DYNAMIC QR
        // ------------------------------------------------------------

        drawDynamicQr(
            canvas = canvas,
            qr = qr
        )

        /*
         * The rendered template bitmap is no longer required after
         * being drawn onto the final canvas.
         *
         * recycle() is deliberately NOT called because bitmap ownership
         * is managed by the Android runtime and premature recycling can
         * cause rendering crashes.
         */

        return output
    }

    // ================================================================
    // MASTER PDF LOADER
    // ================================================================

    private fun loadReferenceCertificate(
        context: Context
    ): Bitmap? {

        return try {

            val cacheFile =
                File(
                    context.cacheDir,
                    "Reference_certificate_$TEMPLATE_VERSION.pdf"
                )

            /*
             * Always copy the current asset into cache.
             *
             * This prevents an old PDF from remaining after the asset
             * has been replaced.
             */
            context.assets
                .open(TEMPLATE_FILE)
                .use { input ->

                    FileOutputStream(cacheFile)
                        .use { output ->

                            input.copyTo(output)
                        }
                }

            if (!cacheFile.exists() ||
                cacheFile.length() <= 0L
            ) {
                throw IllegalStateException(
                    "Reference certificate PDF is empty."
                )
            }

            ParcelFileDescriptor
                .open(
                    cacheFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                .use { descriptor ->

                    PdfRenderer(descriptor)
                        .use { renderer ->

                            if (renderer.pageCount <= 0) {
                                throw IllegalStateException(
                                    "Reference_certificate.pdf contains no pages."
                                )
                            }

                            renderer
                                .openPage(0)
                                .use { page ->

                                    val bitmap =
                                        Bitmap.createBitmap(
                                            W,
                                            H,
                                            Bitmap.Config.ARGB_8888
                                        )

                                    /*
                                     * Render the first page directly into
                                     * the final 2400 x 1600 certificate bitmap.
                                     */
                                    page.render(
                                        bitmap,
                                        null,
                                        null,
                                        PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                                    )

                                    bitmap
                                }
                        }
                }

        } catch (e: Exception) {

            e.printStackTrace()

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

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        paint.typeface =
            SERIF_BOLD

        paint.color =
            INK

        paint.textAlign =
            Paint.Align.CENTER

        // ------------------------------------------------------------
        // Student Name
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Father / Guardian
        // Existing coordinate preserved.
        // ------------------------------------------------------------

        paint.textSize =
            30f

        val guardianText =
            normalizeGuardian(
                cert.guardian
            )

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

        // ------------------------------------------------------------
        // Course
        // Existing coordinate preserved.
        // ------------------------------------------------------------

        paint.color =
            NAVY

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

        // ------------------------------------------------------------
        // Session
        // Existing coordinate preserved.
        // ------------------------------------------------------------

        paint.textAlign =
            Paint.Align.LEFT

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

        // ------------------------------------------------------------
        // Performance Grade
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Run By
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Duration
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Date
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Place
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Website
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Roll / Registration Number
        // Existing coordinate preserved.
        // ------------------------------------------------------------

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

    // ================================================================
    // GUARDIAN NORMALIZATION
    // ================================================================

    private fun normalizeGuardian(
        guardian: String
    ): String {

        val clean =
            guardian
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        if (clean.isBlank()) {
            return ""
        }

        /*
         * Preserve an explicitly supplied relationship.
         */
        if (
            clean.startsWith(
                "S/O ",
                ignoreCase = true
            ) ||
            clean.startsWith(
                "D/O ",
                ignoreCase = true
            ) ||
            clean.startsWith(
                "W/O ",
                ignoreCase = true
            ) ||
            clean.startsWith(
                "C/O ",
                ignoreCase = true
            )
        ) {
            return clean
        }

        /*
         * Existing application behavior defaults to S/O.
         */
        return "S/O $clean"
    }

    // ================================================================
    // WEBSITE NORMALIZATION
    // ================================================================

    private fun cleanWebsiteForDisplay(
        website: String
    ): String {

        val clean =
            website
                .trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removeSuffix("/")

        return clean
    }

    // ================================================================
    // DYNAMIC QR
    // ================================================================

    private fun drawDynamicQr(
        canvas: Canvas,
        qr: Bitmap?
    ) {

        if (
            qr == null ||
            qr.isRecycled ||
            qr.width <= 0 ||
            qr.height <= 0
        ) {
            return
        }

        /*
         * Existing QR position preserved.
         *
         * LEFT = 1545
         * TOP  = 1275
         * SIZE = 175
         */
        val left =
            1545

        val top =
            1275

        val size =
            175

        /*
         * Existing 6px inset preserved.
         */
        val destination =
            Rect(
                left + 6,
                top + 6,
                left + size - 6,
                top + size - 6
            )

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
            )

        canvas.drawBitmap(
            qr,
            null,
            destination,
            paint
        )
    }

    // ================================================================
    // CENTERED TEXT FITTING
    // ================================================================

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

        val cleanText =
            text
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        if (cleanText.isBlank()) {
            return
        }

        var size =
            preferredSize

        paint.textAlign =
            Paint.Align.CENTER

        paint.textSize =
            size

        while (
            size > minimumSize &&
            paint.measureText(cleanText) > maxWidth
        ) {

            size -= 1f

            paint.textSize =
                size
        }

        /*
         * If the text still does not fit at the minimum size,
         * use a safely truncated version rather than allowing it
         * to overwrite neighboring certificate elements.
         */
        val finalText =
            fitTextWithEllipsis(
                text = cleanText,
                paint = paint,
                maxWidth = maxWidth
            )

        canvas.drawText(
            finalText,
            centerX,
            baselineY,
            paint
        )
    }

    // ================================================================
    // LEFT-ALIGNED TEXT FITTING
    // ================================================================

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

        val cleanText =
            text
                .trim()
                .replace(
                    Regex("\\s+"),
                    " "
                )

        if (cleanText.isBlank()) {
            return
        }

        var size =
            preferredSize

        paint.textAlign =
            Paint.Align.LEFT

        paint.textSize =
            size

        while (
            size > minimumSize &&
            paint.measureText(cleanText) > maxWidth
        ) {

            size -= 1f

            paint.textSize =
                size
        }

        val finalText =
            fitTextWithEllipsis(
                text = cleanText,
                paint = paint,
                maxWidth = maxWidth
            )

        canvas.drawText(
            finalText,
            x,
            baselineY,
            paint
        )
    }

    // ================================================================
    // ELLIPSIS PROTECTION
    // ================================================================

    private fun fitTextWithEllipsis(
        text: String,
        paint: Paint,
        maxWidth: Float
    ): String {

        if (
            text.isBlank() ||
            paint.measureText(text) <= maxWidth
        ) {
            return text
        }

        val ellipsis =
            "..."

        if (
            paint.measureText(ellipsis) > maxWidth
        ) {
            return ""
        }

        var end =
            text.length

        while (end > 0) {

            val candidate =
                text.substring(
                    0,
                    end
                ).trimEnd() + ellipsis

            if (
                paint.measureText(candidate) <= maxWidth
            ) {
                return candidate
            }

            end--
        }

        return ellipsis
    }
}