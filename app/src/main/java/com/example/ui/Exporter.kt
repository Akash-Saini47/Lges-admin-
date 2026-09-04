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

    /**
     * Sanitizes file names by replacing unsafe filesystem characters.
     */
    fun sanitizeFileName(rawName: String): String {
        return rawName.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
    }

    /**
     * Generates QR Code bitmap using ZXing.
     * Returns null if input is blank or encoding fails.
     */
    fun generateQrCode(text: String, size: Int = 400): Bitmap? {
        val cleanText = text.trim()
        if (cleanText.isEmpty() || size <= 0) return null
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                cleanText,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves Bitmap as PNG to device Pictures storage.
     */
    fun saveBitmapToDevice(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val safeName = sanitizeFileName(fileName).ifBlank { "LGES_Certificate" } + ".png"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LGES_Certificates")
                }
                val contentResolver = context.contentResolver
                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "LGES_Certificates")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, safeName)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                Toast.makeText(context, "Saved Certificate Image to Pictures/LGES_Certificates!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save Image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
        }
        return uri
    }

    /**
     * Converts Bitmap to PDF and saves to Downloads storage.
     */
    fun savePdfToDevice(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val safeName = sanitizeFileName(fileName).ifBlank { "LGES_Certificate" } + ".pdf"
        var outputStream: OutputStream? = null
        var uri: Uri? = null
        var pdfDocument: PdfDocument? = null

        try {
            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            pdfDocument.finishPage(page)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LGES_Certificates")
                }
                val contentResolver = context.contentResolver
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = contentResolver.openOutputStream(uri)
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LGES_Certificates")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, safeName)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                Toast.makeText(context, "Saved Certificate PDF to Downloads/LGES_Certificates!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                pdfDocument?.close()
            } catch (_: Exception) {}
        }
        return uri
    }

    /**
     * Shares PDF via Intent with dynamic FileProvider authority.
     */
    fun sharePdf(context: Context, bitmap: Bitmap, fileName: String) {
        val safeName = sanitizeFileName(fileName).ifBlank { "LGES_Certificate" } + ".pdf"
        var pdfDocument: PdfDocument? = null
        var outputStream: FileOutputStream? = null

        try {
            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            pdfDocument.finishPage(page)

            val cachePath = File(context.cacheDir, "shared_certificates")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val pdfFile = File(cachePath, safeName)
            outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            outputStream = null

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "LGES Student Certificate: $fileName")
                putExtra(Intent.EXTRA_TEXT, "Attached is the verified certificate of completion from LGES.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Certificate PDF via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                pdfDocument?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Exports and shares CSV of certificate registry with strict RFC 4180 escaping.
     * Uses real certificate timestamps and dynamic FileProvider authority.
     */
    fun shareCsvRegistry(context: Context, certificates: List<Certificate>) {
        if (certificates.isEmpty()) {
            Toast.makeText(context, "No saved certificates in registry to export.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvBuilder = StringBuilder()
            val headers = listOf(
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
            csvBuilder.append(headers.joinToString(",") { CertificateConfig.escapeCsv(it) }).append("\n")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            certificates.forEach { cert ->
                val timeStr = dateFormat.format(Date(cert.timestamp))
                val row = listOf(
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
                    if (cert.isSynced) "YES" else "NO"
                )
                csvBuilder.append(row.joinToString(",") { CertificateConfig.escapeCsv(it) }).append("\n")
            }

            val cachePath = File(context.cacheDir, "shared_reports")
            if (!cachePath.exists()) cachePath.mkdirs()
            val csvFile = File(cachePath, "LGES_Certificates_Registry.csv")
            csvFile.writeText(csvBuilder.toString(), Charsets.UTF_8)

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, csvFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "LGES Certificates Registry Export")
                putExtra(Intent.EXTRA_TEXT, "Attached is the CSV export of ${certificates.size} certificates from LGES Registry.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Certificate Registry CSV via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
