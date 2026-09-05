package com.example.ui.certificates

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Certificate
import com.example.database.SyncStatus
import com.example.ui.CertificateDrawer
import com.example.ui.Exporter
import com.example.ui.components.DocumentCard
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.LgesNavy

@Composable
fun CertificatesTab(
    certificates: List<Certificate>,
    onEditCertificate: (Certificate) -> Unit,
    onDeleteCertificate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<SyncStatus?>(null) }
    var certificateToDelete by remember { mutableStateOf<Certificate?>(null) }

    val filteredCertificates = remember(certificates, searchQuery, selectedFilter) {
        certificates.filter { cert ->
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                cert.studentName.lowercase().contains(q) ||
                        cert.rollNo.lowercase().contains(q) ||
                        cert.certificateId.lowercase().contains(q) ||
                        cert.courseName.lowercase().contains(q)
            }
            val matchesFilter = selectedFilter == null || cert.syncStatus == selectedFilter
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, roll no, or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${certificates.size})") }
            )
            FilterChip(
                selected = selectedFilter == SyncStatus.SYNCED,
                onClick = {
                    selectedFilter = if (selectedFilter == SyncStatus.SYNCED) null else SyncStatus.SYNCED
                },
                label = { Text("Synced") }
            )
            FilterChip(
                selected = selectedFilter == SyncStatus.PENDING,
                onClick = {
                    selectedFilter = if (selectedFilter == SyncStatus.PENDING) null else SyncStatus.PENDING
                },
                label = { Text("Pending") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredCertificates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No certificates match '$searchQuery'" else "No certificates found",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCertificates, key = { it.certificateId }) { cert ->
                    CertificateItemCard(
                        certificate = cert,
                        onEdit = { onEditCertificate(cert) },
                        onDelete = { certificateToDelete = cert },
                        onExportPdf = {
                            val qr = Exporter.generateQrCode(cert.certificateId, 250)
                            val bitmap = CertificateDrawer.drawCertificate(context, cert, qr)
                            Exporter.savePdfToDevice(context, bitmap, "${cert.rollNo}_${cert.studentName}")
                        },
                        onShare = {
                            val qr = Exporter.generateQrCode(cert.certificateId, 250)
                            val bitmap = CertificateDrawer.drawCertificate(context, cert, qr)
                            Exporter.sharePdf(context, bitmap, "${cert.rollNo}_${cert.studentName}")
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    certificateToDelete?.let { cert ->
        AlertDialog(
            onDismissRequest = { certificateToDelete = null },
            title = { Text("Delete Certificate") },
            text = {
                Text("Are you sure you want to delete certificate for ${cert.studentName} (${cert.certificateId})? If connected, it will also be deleted from Google Sheets.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCertificate(cert.certificateId)
                        certificateToDelete = null
                        Toast.makeText(context, "Certificate deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { certificateToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CertificateItemCard(
    certificate: Certificate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit
) {
    DocumentCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = certificate.studentName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LgesNavy
                )
                Text(
                    text = "${certificate.courseName} • Grade: ${certificate.grade}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Roll No: ${certificate.rollNo} • Session: ${certificate.sessionRange}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "ID: ${certificate.certificateId}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                if (!certificate.lastSyncError.isNullOrBlank()) {
                    Text(
                        text = "Sync error: ${certificate.lastSyncError}",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }
            SyncStatusBadge(status = certificate.syncStatus)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExportPdf, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = LgesNavy)
            }
            IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = LgesNavy)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = LgesNavy)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
            }
        }
    }
}
