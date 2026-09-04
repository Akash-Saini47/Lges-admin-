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
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object Exporter {

    // 1. Generate QR Code programmatically using ZXing Core
    fun generateQrCode(text: String, size: Int = 400): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 2. Save Bitmap as PNG using MediaStore (Modern API)
    fun saveBitmapToDevice(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val nameWithExtension = "$fileName.png"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nameWithExtension)
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
                val file = File(directory, nameWithExtension)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                Toast.makeText(context, "Saved Certificate Image to Pictures!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            outputStream?.close()
        }
        return uri
    }

    // 3. Convert Bitmap to PDF and save to device using MediaStore
    fun savePdfToDevice(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val nameWithExtension = "$fileName.pdf"
        var outputStream: OutputStream? = null
        var uri: Uri? = null

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        pdfDocument.finishPage(page)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nameWithExtension)
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
                val file = File(directory, nameWithExtension)
                uri = Uri.fromFile(file)
                outputStream = FileOutputStream(file)
            }

            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                Toast.makeText(context, "Saved Certificate PDF to Downloads!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            outputStream?.close()
            pdfDocument.close()
        }
        return uri
    }

    // 4. Share PDF File directly via Intent using temporary cache FileProvider
    fun sharePdf(context: Context, bitmap: Bitmap, fileName: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        pdfDocument.finishPage(page)

        try {
            val cachePath = File(context.cacheDir, "shared_certificates")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val pdfFile = File(cachePath, "$fileName.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.lgesadmin.kypwzm.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "LGES Student Certificate: $fileName")
                putExtra(Intent.EXTRA_TEXT, "Attached is the verified certificate of completion from LGES.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Certificate PDF via"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }

    // 5. Share CSV of all saved certificates in registry
    fun shareCsvRegistry(context: Context, certificates: List<com.example.database.Certificate>) {
        if (certificates.isEmpty()) {
            Toast.makeText(context, "No saved certificates in registry to export.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("Roll No,Student Name,Father Name,Course,Issue Date,Grade,Timestamp,Cert Type,Session,Duration,Place Of Issue\n")
            val timeStamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            certificates.forEach { cert ->
                csvBuilder.append("\"${cert.rollNo}\",\"${cert.studentName}\",\"${cert.fatherName}\",\"${cert.courseName}\",\"${cert.dateOfIssue}\",\"${cert.grade}\",\"$timeStamp\",\"${cert.certType}\",\"${cert.sessionRange}\",\"${cert.duration}\",\"${cert.placeOfIssue}\"\n")
            }

            val cachePath = File(context.cacheDir, "shared_reports")
            if (!cachePath.exists()) cachePath.mkdirs()
            val csvFile = File(cachePath, "LGES_Certificates_Registry.csv")
            csvFile.writeText(csvBuilder.toString())

            val contentUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.lgesadmin.kypwzm.fileprovider",
                csvFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "LGES Certificates Registry Export")
                putExtra(Intent.EXTRA_TEXT, "Attached is the CSV export of ${certificates.size} certificates from LGES Registry.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Certificate Registry CSV via"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
