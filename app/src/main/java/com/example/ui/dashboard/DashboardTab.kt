package com.example.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Certificate
import com.example.database.SyncStatus
import com.example.ui.Exporter
import com.example.ui.components.DocumentCard
import com.example.ui.components.SafeInstituteLogo
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy

@Composable
fun DashboardTab(
    certificates: List<Certificate>,
    onNavigateToCreate: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onSyncAll: () -> Unit,
    onEditCertificate: (Certificate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val totalCount = certificates.size
    val syncedCount = certificates.count { it.syncStatus == SyncStatus.SYNCED }
    val pendingCount = certificates.count { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.SYNCING }
    val failedCount = certificates.count { it.syncStatus == SyncStatus.FAILED || it.syncStatus == SyncStatus.DELETE_FAILED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeInstituteLogo(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LGES Admin Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy
                    )
                    Text(
                        text = "Certificate Registry & Cloud Sync Control",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total",
                    count = totalCount.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = LgesNavy,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Synced",
                    count = syncedCount.toString(),
                    icon = Icons.Default.CloudUpload,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pending",
                    count = pendingCount.toString(),
                    icon = Icons.Default.Sync,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
                if (failedCount > 0) {
                    MetricCard(
                        title = "Failed",
                        count = failedCount.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFC62828),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LgesNavy
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = LgesNavy),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { Exporter.shareCsvRegistry(context, certificates) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = LgesNavy)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 13.sp, color = LgesNavy)
                    }

                    if (pendingCount > 0 || failedCount > 0) {
                        OutlinedButton(
                            onClick = onSyncAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync All", fontSize = 13.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }

        // Recent Certificates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Certificates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = LgesNavy
                )
                if (certificates.size > 5) {
                    OutlinedButton(onClick = onNavigateToCertificates) {
                        Text("View All (${certificates.size})", fontSize = 12.sp, color = LgesNavy)
                    }
                }
            }
        }

        if (certificates.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No certificates in database yet.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToCreate,
                            colors = ButtonDefaults.buttonColors(containerColor = LgesNavy)
                        ) {
                            Text("Create First Certificate")
                        }
                    }
                }
            }
        } else {
            items(certificates.take(5)) { cert ->
                RecentCertificateItem(
                    certificate = cert,
                    onClick = { onEditCertificate(cert) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun RecentCertificateItem(
    certificate: Certificate,
    onClick: () -> Unit
) {
    DocumentCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = certificate.studentName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LgesNavy
                )
                Text(
                    text = "${certificate.courseName} • Roll: ${certificate.rollNo}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "ID: ${certificate.certificateId}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            SyncStatusBadge(status = certificate.syncStatus)
        }
    }
}
