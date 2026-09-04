package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.database.Certificate
import com.example.util.CertificateConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private const val CERTIFICATE_FOLDER = "LGES_Certificates"
    private const val SHARED_CERTIFICATE_FOLDER = "shared_certificates"
    private const val SHARED_REPORT_FOLDER = "shared_reports"

    // Certificate reference ratio: 3:2
    private const val CERTIFICATE_ASPECT_RATIO = 1.5f

    // Prevent accidental huge QR bitmap allocations.
    private const val MIN_QR_SIZE = 64
    private const val MAX_QR_SIZE = 2048

    // ============================================================
    // FILE NAME
    // ============================================================

    fun sanitizeFileName(rawName: String): String {

        return rawName
            .trim()
            .replace(
                Regex("""[\\/:*?"<>|\s]+"""),
                "_"
            )
            .trim('_')
            .ifBlank {
                "LGES_Certificate"
            }
    }

    private fun uniqueFileName(
        directory: File,
        baseName: String,
        extension: String
    ): File {

        var index = 0

        while (true) {

            val suffix =
                if (index == 0) {
                    ""
                } else {
                    "_$index"
                }

            val file =
                File(
                    directory,
                    "$baseName$suffix.$extension"
                )

            if (!file.exists()) {
                return file
            }

            index++
        }
    }

    private fun uniqueMediaStoreFileName(
        context: Context,
        baseName: String,
        extension: String
    ): String {

        val safeBaseName =
            sanitizeFileName(baseName)

        val resolver =
            context.contentResolver

        var index = 0

        while (true) {

            val suffix =
                if (index == 0) {
                    ""
                } else {
                    "_$index"
                }

            val candidate =
                "$safeBaseName$suffix.$extension"

            val projection =
                arrayOf(
                    MediaStore.MediaColumns._ID
                )

            val selection =
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"

            val selectionArgs =
                arrayOf(candidate)

            val collection =
                if (extension.equals("png", ignoreCase = true)) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

            val exists =
                try {
                    resolver.query(
                        collection,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                    } == true
                } catch (_: Exception) {
                    false
                }

            if (!exists) {
                return candidate
            }

            index++
        }
    }

    private fun ensureDirectory(
        directory: File
    ) {

        if (directory.exists()) {

            if (!directory.isDirectory) {
                throw IllegalStateException(
                    "Path exists but is not a directory: ${directory.absolutePath}"
                )
            }

            return
        }

        if (!directory.mkdirs() && !directory.exists()) {
            throw IllegalStateException(
                "Unable to create directory: ${directory.absolutePath}"
            )
        }
    }

    // ============================================================
    // QR CODE
    // ============================================================

    fun generateQrCode(
        text: String,
        size: Int = 400
    ): Bitmap? {

        val cleanText =
            text.trim()

        if (cleanText.isEmpty()) {
            return null
        }

        val safeSize =
            size.coerceIn(
                MIN_QR_SIZE,
                MAX_QR_SIZE
            )

        return try {

            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(
                    cleanText,
                    BarcodeFormat.QR_CODE,
                    safeSize,
                    safeSize
                )

            val width =
                bitMatrix.width

            val height =
                bitMatrix.height

            val pixels =
                IntArray(width * height)

            for (y in 0 until height) {

                val offset =
                    y * width

                for (x in 0 until width) {

                    pixels[offset + x] =
                        if (bitMatrix.get(x, y)) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                }
            }

            Bitmap.createBitmap(
                pixels,
                0,
                width,
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }

    // ============================================================
    // BITMAP VALIDATION
    // ============================================================

    private fun isValidBitmap(
        bitmap: Bitmap?
    ): Boolean {

        return bitmap != null &&
                !bitmap.isRecycled &&
                bitmap.width > 0 &&
                bitmap.height > 0
    }

    // ============================================================
    // SAVE PNG
    // ============================================================

    fun saveBitmapToDevice(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {

        if (!isValidBitmap(bitmap)) {

            Toast.makeText(
                context,
                "Cannot save certificate: invalid image.",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        val safeBaseName =
            sanitizeFileName(fileName)

        var uri: Uri? = null
        var outputStream: OutputStream? = null

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val safeName =
                    uniqueMediaStoreFileName(
                        context = context,
                        baseName = safeBaseName,
                        extension = "png"
                    )

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            safeName
                        )

                        put(
                            MediaStore.Images.Media.MIME_TYPE,
                            "image/png"
                        )

                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES +
                                    "/$CERTIFICATE_FOLDER"
                        )

                        put(
                            MediaStore.Images.Media.IS_PENDING,
                            1
                        )
                    }

                val resolver =
                    context.contentResolver

                uri =
                    resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                        ?: throw IllegalStateException(
                            "Unable to create image entry."
                        )

                outputStream =
                    resolver.openOutputStream(uri)
                        ?: throw IllegalStateException(
                            "Unable to open image output stream."
                        )

                val compressed =
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        outputStream
                    )

                if (!compressed) {
                    throw IllegalStateException(
                        "Unable to compress certificate image."
                    )
                }

                outputStream.flush()
                outputStream.close()
                outputStream = null

                val completedValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Images.Media.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    completedValues,
                    null,
                    null
                )

            } else {

                val directory =
                    File(
                        Environment
                            .getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES
                            ),
                        CERTIFICATE_FOLDER
                    )

                ensureDirectory(directory)

                val file =
                    uniqueFileName(
                        directory,
                        safeBaseName,
                        "png"
                    )

                outputStream =
                    FileOutputStream(file)

                val compressed =
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        outputStream
                    )

                if (!compressed) {
                    throw IllegalStateException(
                        "Unable to compress certificate image."
                    )
                }

                outputStream.flush()
                outputStream.close()
                outputStream = null

                /*
                 * This URI is suitable for returning to the caller.
                 *
                 * If the returned URI is later shared on legacy Android,
                 * use a FileProvider instead of directly putting this
                 * file:// URI into an Intent.
                 */
                uri =
                    Uri.fromFile(file)
            }

            Toast.makeText(
                context,
                "Certificate image saved to Pictures/$CERTIFICATE_FOLDER",
                Toast.LENGTH_SHORT
            ).show()

            return uri

        } catch (e: Exception) {

            e.printStackTrace()

            if (uri != null) {

                try {
                    context.contentResolver.delete(
                        uri,
                        null,
                        null
                    )
                } catch (_: Exception) {
                }
            }

            Toast.makeText(
                context,
                "Failed to save image: " +
                        (
                            e.localizedMessage
                                ?: "Unknown error"
                        ),
                Toast.LENGTH_LONG
            ).show()

            return null

        } finally {

            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    // ============================================================
    // PDF CREATION
    // ============================================================

    private fun createCertificatePdf(
        bitmap: Bitmap,
        outputStream: OutputStream
    ) {

        if (!isValidBitmap(bitmap)) {

            throw IllegalArgumentException(
                "Invalid certificate bitmap."
            )
        }

        val pdfDocument =
            PdfDocument()

        try {

            /*
             * Stable 3:2 certificate PDF page.
             */
            val pageWidth =
                1200

            val pageHeight =
                800

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    1
                ).create()

            val page =
                pdfDocument.startPage(pageInfo)

            try {

                val canvas =
                    page.canvas

                val paint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                    )

                val sourceRatio =
                    bitmap.width.toFloat() /
                            bitmap.height.toFloat()

                val targetRatio =
                    CERTIFICATE_ASPECT_RATIO

                /*
                 * Preserve the complete certificate.
                 * No cropping is performed.
                 */
                val destination =
                    if (sourceRatio > targetRatio) {

                        val height =
                            pageWidth / sourceRatio

                        val top =
                            (pageHeight - height) / 2f

                        android.graphics.RectF(
                            0f,
                            top,
                            pageWidth.toFloat(),
                            top + height
                        )

                    } else {

                        val width =
                            pageHeight * sourceRatio

                        val left =
                            (pageWidth - width) / 2f

                        android.graphics.RectF(
                            left,
                            0f,
                            left + width,
                            pageHeight.toFloat()
                        )
                    }

                canvas.drawBitmap(
                    bitmap,
                    null,
                    destination,
                    paint
                )

            } finally {

                pdfDocument.finishPage(page)
            }

            pdfDocument.writeTo(
                outputStream
            )

        } finally {

            pdfDocument.close()
        }
    }

    // ============================================================
    // SAVE PDF
    // ============================================================

    fun savePdfToDevice(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): Uri? {

        if (!isValidBitmap(bitmap)) {

            Toast.makeText(
                context,
                "Cannot create PDF: invalid certificate image.",
                Toast.LENGTH_LONG
            ).show()

            return null
        }

        val safeBaseName =
            sanitizeFileName(fileName)

        var uri: Uri? = null
        var outputStream: OutputStream? = null

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val safeName =
                    uniqueMediaStoreFileName(
                        context = context,
                        baseName = safeBaseName,
                        extension = "pdf"
                    )

                val values =
                    ContentValues().apply {

                        put(
                            MediaStore.Downloads.DISPLAY_NAME,
                            safeName
                        )

                        put(
                            MediaStore.Downloads.MIME_TYPE,
                            "application/pdf"
                        )

                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS +
                                    "/$CERTIFICATE_FOLDER"
                        )

                        put(
                            MediaStore.Downloads.IS_PENDING,
                            1
                        )
                    }

                val resolver =
                    context.contentResolver

                uri =
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )
                        ?: throw IllegalStateException(
                            "Unable to create PDF entry."
                        )

                outputStream =
                    resolver.openOutputStream(uri)
                        ?: throw IllegalStateException(
                            "Unable to open PDF output stream."
                        )

                createCertificatePdf(
                    bitmap,
                    outputStream
                )

                outputStream.flush()
                outputStream.close()
                outputStream = null

                val completedValues =
                    ContentValues().apply {
                        put(
                            MediaStore.Downloads.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    completedValues,
                    null,
                    null
                )

            } else {

                val directory =
                    File(
                        Environment
                            .getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS
                            ),
                        CERTIFICATE_FOLDER
                    )

                ensureDirectory(directory)

                val file =
                    uniqueFileName(
                        directory,
                        safeBaseName,
                        "pdf"
                    )

                outputStream =
                    FileOutputStream(file)

                createCertificatePdf(
                    bitmap,
                    outputStream
                )

                outputStream.flush()
                outputStream.close()
                outputStream = null

                uri =
                    Uri.fromFile(file)
            }

            Toast.makeText(
                context,
                "Certificate PDF saved to Downloads/$CERTIFICATE_FOLDER",
                Toast.LENGTH_SHORT
            ).show()

            return uri

        } catch (e: Exception) {

            e.printStackTrace()

            if (uri != null) {

                try {
                    context.contentResolver.delete(
                        uri,
                        null,
                        null
                    )
                } catch (_: Exception) {
                }
            }

            Toast.makeText(
                context,
                "Failed to save PDF: " +
                        (
                            e.localizedMessage
                                ?: "Unknown error"
                        ),
                Toast.LENGTH_LONG
            ).show()

            return null

        } finally {

            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    // ============================================================
    // SHARE PDF
    // ============================================================

    fun sharePdf(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ) {

        if (!isValidBitmap(bitmap)) {

            Toast.makeText(
                context,
                "Cannot share: invalid certificate image.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val safeBaseName =
            sanitizeFileName(fileName)

        var outputStream: FileOutputStream? = null
        var pdfFile: File? = null

        try {

            val cachePath =
                File(
                    context.cacheDir,
                    SHARED_CERTIFICATE_FOLDER
                )

            ensureDirectory(cachePath)

            pdfFile =
                uniqueFileName(
                    cachePath,
                    safeBaseName,
                    "pdf"
                )

            outputStream =
                FileOutputStream(pdfFile)

            createCertificatePdf(
                bitmap,
                outputStream
            )

            outputStream.flush()
            outputStream.close()
            outputStream = null

            val authority =
                "${context.packageName}.fileprovider"

            val contentUri =
                FileProvider.getUriForFile(
                    context,
                    authority,
                    pdfFile
                )

            val shareIntent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        "application/pdf"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        contentUri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "LGES Student Certificate: $fileName"
                    )

                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Attached is the verified certificate of completion from LGES."
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            val chooser =
                Intent.createChooser(
                    shareIntent,
                    "Share Certificate PDF via"
                ).apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            context.startActivity(
                chooser
            )

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                context,
                "Failed to share PDF: " +
                        (
                            e.localizedMessage
                                ?: "Unknown error"
                        ),
                Toast.LENGTH_LONG
            ).show()

        } finally {

            try {
                outputStream?.close()
            } catch (_: Exception) {
            }
        }
    }

    // ============================================================
    // CSV EXPORT
    // ============================================================

    fun shareCsvRegistry(
        context: Context,
        certificates: List<Certificate>
    ) {

        if (certificates.isEmpty()) {

            Toast.makeText(
                context,
                "No saved certificates in registry to export.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            val csvBuilder =
                StringBuilder()

            /*
             * UTF-8 BOM improves compatibility with Microsoft Excel.
             */
            csvBuilder.append('\uFEFF')

            val headers =
                listOf(
                    "Certificate ID",
                    "Roll No",
                    "Student Name",
                    "Father / Guardian",
                    "Course Name",
                    "Certificate Type",
                    "Session Range",
                    "Duration",
                    "Grade",
                    "Place Of Issue",
                    "Date Of Issue",
                    "Created Timestamp",
                    "Cloud Synced"
                )

            csvBuilder
                .append(
                    headers.joinToString(",") {
                        CertificateConfig.escapeCsv(it)
                    }
                )
                .append("\r\n")

            val dateFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                )

            certificates.forEach { cert ->

                val timeStr =
                    dateFormat.format(
                        Date(cert.timestamp)
                    )

                val row =
                    listOf(
                        cert.certificateId,
                        cert.rollNo,
                        cert.studentName,
                        cert.fatherName,
                        cert.courseName,
                        cert.certType,
                        cert.sessionRange,
                        cert.duration,
                        cert.grade,
                        cert.placeOfIssue,
                        cert.dateOfIssue,
                        timeStr,
                        if (cert.isSynced) {
                            "YES"
                        } else {
                            "NO"
                        }
                    )

                csvBuilder
                    .append(
                        row.joinToString(",") {
                            CertificateConfig.escapeCsv(it)
                        }
                    )
                    .append("\r\n")
            }

            val cachePath =
                File(
                    context.cacheDir,
                    SHARED_REPORT_FOLDER
                )

            ensureDirectory(cachePath)

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())

            val csvFile =
                uniqueFileName(
                    cachePath,
                    "LGES_Certificates_Registry_$timestamp",
                    "csv"
                )

            csvFile.writeText(
                csvBuilder.toString(),
                Charsets.UTF_8
            )

            val authority =
                "${context.packageName}.fileprovider"

            val contentUri =
                FileProvider.getUriForFile(
                    context,
                    authority,
                    csvFile
                )

            val shareIntent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        "text/csv"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        contentUri
                    )

                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "LGES Certificates Registry Export"
                    )

                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Attached is the CSV export of ${certificates.size} certificates from LGES Registry."
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            val chooser =
                Intent.createChooser(
                    shareIntent,
                    "Share Certificate Registry CSV via"
                ).apply {

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            context.startActivity(
                chooser
            )

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                context,
                "Failed to export CSV: " +
                        (
                            e.localizedMessage
                                ?: "Unknown error"
                        ),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}