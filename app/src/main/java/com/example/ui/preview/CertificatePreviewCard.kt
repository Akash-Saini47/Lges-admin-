package com.example.ui.preview

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.database.Certificate
import com.example.ui.CertificateDrawer
import com.example.ui.Exporter
import com.example.ui.components.DocumentCard
import com.example.ui.theme.LgesNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun CertificatePreviewCard(
    certificate: Certificate,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }

    // Debounced asynchronous preview generation (300ms) to keep typing fluid
    LaunchedEffect(
        certificate.rollNo,
        certificate.studentName,
        certificate.fatherName,
        certificate.courseName,
        certificate.sessionRange,
        certificate.duration,
        certificate.grade,
        certificate.placeOfIssue,
        certificate.dateOfIssue,
        certificate.certType
    ) {
        isRendering = true
        delay(300) // Debounce rapid keystrokes

        withContext(Dispatchers.Default) {
            try {
                val qr = Exporter.generateQrCode(certificate.certificateId, 200)
                val bitmap = CertificateDrawer.drawPreviewCertificate(
                    context = context,
                    cert = certificate,
                    qrBitmap = qr,
                    previewWidth = 1200
                )
                withContext(Dispatchers.Main) {
                    previewBitmap = bitmap
                    isRendering = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isRendering = false
                }
            }
        }
    }

    DocumentCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Live Certificate Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = LgesNavy
            )
            if (previewBitmap != null) {
                IconButton(onClick = { showFullscreen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = LgesNavy)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preview Box (3:2 Aspect Ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "Certificate Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isRendering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Export Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val qr = Exporter.generateQrCode(certificate.certificateId, 300)
                    val fullBitmap = CertificateDrawer.drawCertificate(context, certificate, qr)
                    Exporter.saveBitmapToDevice(context, fullBitmap, "${certificate.rollNo}_${certificate.studentName}")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = LgesNavy)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Image", fontSize = 12.sp, color = LgesNavy)
            }

            Button(
                onClick = {
                    val qr = Exporter.generateQrCode(certificate.certificateId, 300)
                    val fullBitmap = CertificateDrawer.drawCertificate(context, certificate, qr)
                    Exporter.savePdfToDevice(context, fullBitmap, "${certificate.rollNo}_${certificate.studentName}")
                },
                colors = ButtonDefaults.buttonColors(containerColor = LgesNavy),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    val qr = Exporter.generateQrCode(certificate.certificateId, 300)
                    val fullBitmap = CertificateDrawer.drawCertificate(context, certificate, qr)
                    Exporter.sharePdf(context, fullBitmap, "${certificate.rollNo}_${certificate.studentName}")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = LgesNavy)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp, color = LgesNavy)
            }
        }
    }

    // Fullscreen Dialog
    if (showFullscreen && previewBitmap != null) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.9f)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Fullscreen Certificate Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    IconButton(
                        onClick = { showFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}
