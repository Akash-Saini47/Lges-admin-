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
import java.io.File
import java.io.FileOutputStream

/**
 * CertificateDrawer
 *
 * IMPORTANT:
 * The attached Reference_certificate.pdf is the ONLY certificate
 * template used by this renderer.
 *
 * No old certificate template.
 * No Modern Institutional template.
 * No Canvas-based fallback certificate.
 *
 * Architecture:
 *
 * Reference_certificate.pdf
 *          +
 * Dynamic certificate data
 *          +
 * Dynamic QR
 *          =
 * Final certificate
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

    /*
     * Keep the certificate in the same 3:2 ratio as the supplied
     * reference certificate.
     */
    private const val W = 2400
    private const val H = 1600

    /*
     * IMPORTANT:
     * Change this number whenever the master PDF is replaced.
     * This prevents an older cached certificate from being reused.
     */
    private const val TEMPLATE_VERSION = "v2"

    private const val TEMPLATE_FILE = "Reference_certificate.pdf"

    private val SERIF_BOLD =
        Typeface.create(Typeface.SERIF, Typeface.BOLD)

    private val SERIF =
        Typeface.create(Typeface.SERIF, Typeface.NORMAL)

    private val NAVY = 0xFF0B1B3D.toInt()
    private val INK = 0xFF1E293B.toInt()

    // ================================================================
    // PUBLIC API
    // ================================================================

    fun drawCertificate(
        context: Context,
        cert: Certificate,
        qrBitmap: Bitmap?
    ): Bitmap {

        val data = CertificateData(
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

        return draw(
            context = context,
            cert = data,
            qr = qrBitmap
        )
    }

    fun draw(
        context: Context,
        cert: CertificateData,
        qr: Bitmap? = null
    ): Bitmap {

        /*
         * DO NOT create the old manually drawn certificate.
         *
         * If the reference PDF cannot be loaded, fail clearly instead
         * of silently displaying an old certificate.
         */
        val template = loadReferenceCertificate(context)
            ?: throw IllegalStateException(
                """
                Certificate template could not be loaded.

                Required asset:
                assets/Reference_certificate.pdf

                The application will NOT use an old certificate fallback.
                """.trimIndent()
            )

        val output = Bitmap.createBitmap(
            W,
            H,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)

        /*
         * Draw ONLY the supplied reference certificate.
         */
        val bitmapPaint = Paint(
            Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
        )

        canvas.drawBitmap(
            template,
            null,
            Rect(0, 0, W, H),
            bitmapPaint
        )

        /*
         * IMPORTANT:
         *
         * The reference PDF contains the complete static artwork,
         * including the Director Signature.
         *
         * We only add dynamic information on top.
         */
        drawDynamicData(
            canvas = canvas,
            cert = cert
        )

        /*
         * Add the dynamic verification QR.
         */
        drawDynamicQr(
            canvas = canvas,
            qr = qr
        )

        return output
    }

    // ================================================================
    // MASTER PDF LOADER
    // ================================================================

    private fun loadReferenceCertificate(
        context: Context
    ): Bitmap? {

        /*
         * NEVER use the old certificate_master_template.png.
         *
         * NEVER use certificate_master_template drawable.
         *
         * ALWAYS read the current Reference_certificate.pdf.
         */

        return try {

            val cacheFile = File(
                context.cacheDir,
                "Reference_certificate_$TEMPLATE_VERSION.pdf"
            )

            /*
             * Always refresh the cached template from assets.
             *
             * This prevents an old PDF from remaining in cache.
             */
            context.assets.open(TEMPLATE_FILE).use { input ->

                FileOutputStream(cacheFile).use { output ->

                    input.copyTo(output)
                }
            }

            ParcelFileDescriptor
                .open(
                    cacheFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                .use { descriptor ->

                    PdfRenderer(descriptor).use { renderer ->

                        if (renderer.pageCount <= 0) {
                            throw IllegalStateException(
                                "Reference_certificate.pdf has no pages."
                            )
                        }

                        renderer.openPage(0).use { page ->

                            val bitmap = Bitmap.createBitmap(
                                W,
                                H,
                                Bitmap.Config.ARGB_8888
                            )

                            /*
                             * Render the PDF at the exact certificate
                             * canvas dimensions.
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

        val paint = Paint(
            Paint.ANTI_ALIAS_FLAG
        )

        paint.typeface = SERIF_BOLD
        paint.color = INK
        paint.textAlign = Paint.Align.CENTER

        /*
         * IMPORTANT:
         *
         * These coordinates are the overlay coordinates.
         *
         * The static certificate design itself comes from
         * Reference_certificate.pdf.
         */

        // ------------------------------------------------------------
        // Student Name
        // ------------------------------------------------------------

        paint.textSize = 78f

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
        // Father / Guardian Name
        // ------------------------------------------------------------

        paint.textSize = 30f

        val guardianText =
            when {
                cert.guardian.isBlank() -> ""

                cert.guardian.startsWith(
                    "S/O",
                    ignoreCase = true
                ) ||
                cert.guardian.startsWith(
                    "D/O",
                    ignoreCase = true
                ) ||
                cert.guardian.startsWith(
                    "W/O",
                    ignoreCase = true
                ) ->
                    cert.guardian

                else ->
                    "S/O ${cert.guardian}"
            }

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
        // ------------------------------------------------------------

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

        // ------------------------------------------------------------
        // Session
        // ------------------------------------------------------------

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 30f

        canvas.drawText(
            cert.session,
            1100f,
            1125f,
            paint
        )

        // ------------------------------------------------------------
        // Performance Grade
        // ------------------------------------------------------------

        canvas.drawText(
            cert.grade,
            1740f,
            1125f,
            paint
        )

        // ------------------------------------------------------------
        // Run By
        // ------------------------------------------------------------

        paint.textSize = 24f

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
        // ------------------------------------------------------------

        canvas.drawText(
            cert.duration,
            900f,
            1300f,
            paint
        )

        // ------------------------------------------------------------
        // Date
        // ------------------------------------------------------------

        canvas.drawText(
            cert.dateOfIssue,
            900f,
            1362f,
            paint
        )

        // ------------------------------------------------------------
        // Place
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
        // ------------------------------------------------------------

        paint.textSize = 22f

        canvas.drawText(
            cert.website,
            900f,
            1482f,
            paint
        )

        // ------------------------------------------------------------
        // Roll / Registration Number
        // ------------------------------------------------------------

        paint.textSize = 25f

        canvas.drawText(
            cert.rollNo,
            2080f,
            1485f,
            paint
        )
    }

    // ================================================================
    // DYNAMIC QR
    // ================================================================

    private fun drawDynamicQr(
        canvas: Canvas,
        qr: Bitmap?
    ) {

        if (qr == null) {
            return
        }

        /*
         * Position of the QR on the supplied certificate.
         *
         * Adjust ONLY this position if necessary after checking
         * the actual Reference_certificate.pdf.
         */

        val left = 1545
        val top = 1275
        val size = 175

        val destination = Rect(
            left + 6,
            top + 6,
            left + size - 6,
            top + size - 6
        )

        val paint = Paint(
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
    // TEXT FITTING
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

        var size = preferredSize

        paint.textSize = size

        while (
            size > minimumSize &&
            paint.measureText(text) > maxWidth
        ) {
            size -= 1f
            paint.textSize = size
        }

        canvas.drawText(
            text,
            centerX,
            baselineY,
            paint
        )
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

        var size = preferredSize

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = size

        while (
            size > minimumSize &&
            paint.measureText(text) > maxWidth
        ) {
            size -= 1f
            paint.textSize = size
        }

        canvas.drawText(
            text,
            x,
            baselineY,
            paint
        )
    }
}