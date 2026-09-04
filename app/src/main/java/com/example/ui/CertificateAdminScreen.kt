package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.database.Certificate
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun SafeInstituteLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val painter = remember(context) {
        try {
            val drawable = ContextCompat.getDrawable(context, R.drawable.img_lges_logo)
            drawable?.let {
                val w = if (it.intrinsicWidth > 0) it.intrinsicWidth else 512
                val h = if (it.intrinsicHeight > 0) it.intrinsicHeight else 512
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                it.setBounds(0, 0, w, h)
                it.draw(canvas)
                BitmapPainter(bitmap.asImageBitmap())
            }
        } catch (e: Exception) {
            null
        }
    }

    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = "LGES Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(LgesNavy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "LGES Logo",
                tint = Color.White,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

/**
 * Reusable Formal Document Card with Deep Navy & Gold double-line border style
 */
@Composable
fun DocumentCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, LgesNavy),
        shadowElevation = 2.dp,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, LgesGold, RoundedCornerShape(7.dp))
                .padding(12.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateAdminScreen(
    viewModel: CertificateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Certificates, 2: Settings

    // Form state bindings
    val rollNo by viewModel.rollNo.collectAsStateWithLifecycle()
    val studentName by viewModel.studentName.collectAsStateWithLifecycle()
    val fatherName by viewModel.fatherName.collectAsStateWithLifecycle()
    val courseName by viewModel.courseName.collectAsStateWithLifecycle()
    val sessionRange by viewModel.sessionRange.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val grade by viewModel.grade.collectAsStateWithLifecycle()
    val placeOfIssue by viewModel.placeOfIssue.collectAsStateWithLifecycle()
    val dateOfIssue by viewModel.dateOfIssue.collectAsStateWithLifecycle()
    val certType by viewModel.certType.collectAsStateWithLifecycle()
    val webAppUrl by viewModel.webAppUrl.collectAsStateWithLifecycle()

    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()
    val certificates by viewModel.allCertificates.collectAsStateWithLifecycle()

    // Asynchronous certificate drawing state
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    LaunchedEffect(
        rollNo, studentName, fatherName, courseName,
        sessionRange, duration, grade, placeOfIssue, dateOfIssue, certType
    ) {
        isGeneratingPreview = true
        delay(300)
        withContext(Dispatchers.Default) {
            val cert = viewModel.getAsCertificate()
            val qrLink = "https://lges-computer-classes.netlify.app/verify.html?certNo=${cert.rollNo}"
            val qr = Exporter.generateQrCode(qrLink, 250)
            val bmp = CertificateDrawer.drawCertificate(context, cert, qr)
            withContext(Dispatchers.Main) {
                previewBitmap = bmp
                isGeneratingPreview = false
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        SafeInstituteLogo(
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "LGES Credentials",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy,
                                fontSize = 21.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.clearForm()
                                Toast.makeText(context, "Form cleared!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("reset_form_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = LgesNavy)
                        }

                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LgesNavy)
                                .border(1.5.dp, LgesGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Admin Profile",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, Color(0xFFE0E3E6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple(0, "Dashboard", Icons.Default.Dashboard),
                        Triple(1, "Certificates", Icons.Default.WorkspacePremium)
                    )

                    tabs.forEach { (index, title, icon) ->
                        val isSelected = selectedTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedTab = index }
                                .padding(top = 4.dp)
                                .testTag("nav_tab_$index")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = LgesNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = LgesNavy
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(2.dp)
                                        .background(LgesGold)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    onNavigateToGenerate = { selectedTab = 1 },
                    previewBitmap = previewBitmap,
                    isGeneratingPreview = isGeneratingPreview,
                    certificates = certificates
                )
                1 -> CertificatesTab(
                    viewModel = viewModel,
                    certificates = certificates,
                    previewBitmap = previewBitmap,
                    isGeneratingPreview = isGeneratingPreview,
                    isUploading = isUploading,
                    uploadStatus = uploadStatus,
                    uploadError = uploadError,
                    webAppUrlConfigured = webAppUrl.isNotEmpty()
                )
            }
        }
    }
}

/**
 * Main Dashboard Tab with real database statistics and actual recent activity
 */
@Composable
fun DashboardTab(
    viewModel: CertificateViewModel,
    onNavigateToGenerate: () -> Unit,
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    certificates: List<Certificate>
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Welcome Greeting Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome back, Admin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy,
                        fontSize = 28.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage institutional credentials and issue new secure certificates with precision and speed.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = LgesNavy.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToGenerate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgesNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LgesGold),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("generate_new_cert_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = LgesGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate New Certificate",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 2. Stats Cards (4 Structured Cards using Real Data)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Issued
                    DocumentCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOTAL ISSUED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${certificates.size}",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy
                            )
                            if (certificates.isNotEmpty()) {
                                Text(
                                    text = "Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // Pending Verification
                    DocumentCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PENDING VERIFICATION",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "0",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy
                            )
                            Icon(Icons.Outlined.HourglassEmpty, contentDescription = null, tint = LgesGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Templates Active
                    DocumentCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TEMPLATES ACTIVE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "2",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy
                            )
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = LgesNavy, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Verification Rate
                    DocumentCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VERIFICATION RATE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (certificates.isEmpty()) "0%" else "100%",
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy
                            )
                            Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = LgesGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 3. Recent Activity Section (Displaying real certificates)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy,
                            fontSize = 20.sp
                        )
                    )
                    if (certificates.isNotEmpty()) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = LgesNavy
                            ),
                            modifier = Modifier.clickable { onNavigateToGenerate() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (certificates.isEmpty()) {
                    DocumentCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = LgesNavy, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "No Activity Yet",
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = LgesNavy,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Generated certificates will appear here in real-time.",
                                    fontFamily = FontFamily.Serif,
                                    color = LgesNavy.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        certificates.take(5).forEach { cert ->
                            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(LgesNavy.copy(alpha = 0.08f))
                                                .border(1.dp, LgesGold, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CardMembership, contentDescription = null, tint = LgesNavy)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = cert.studentName,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LgesNavy
                                                )
                                            )
                                            Text(
                                                text = "${cert.courseName} • ${cert.dateOfIssue}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    color = LgesNavy.copy(alpha = 0.7f),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(LgesNavy)
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "Verified",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Preview Card (Deep Navy Background, Gold Headers, Image 3 Thumbnail)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LgesNavy),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, LgesGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Live Preview",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = LgesGold,
                                    fontSize = 22.sp
                                )
                            )
                            Text(
                                text = "Template: Modern Institutional",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    color = LgesGold.copy(alpha = 0.85f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        IconButton(onClick = onNavigateToGenerate) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full View", tint = LgesGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Certificate Thumbnail Canvas Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(2.dp, LgesGold, RoundedCornerShape(6.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Certificate Thumbnail Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = LgesNavy, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Rendering Certificate...", color = LgesNavy, fontSize = 12.sp, fontFamily = FontFamily.Serif)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom-Right ISO 9001-2015 Seal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Transparent,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, LgesGold),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = LgesGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ISO 9001-2015 CERTIFIED",
                                    color = LgesGold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick Action Bento Grid (4 Real Working Actions)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DocumentCard(
                        modifier = Modifier.weight(1f).testTag("quick_action_new_cert"),
                        onClick = {
                            viewModel.clearForm()
                            Toast.makeText(context, "Ready to issue new certificate", Toast.LENGTH_SHORT).show()
                            onNavigateToGenerate()
                        }
                    ) {
                        Icon(Icons.Outlined.NoteAdd, contentDescription = "New Certificate", tint = LgesNavy, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("New Certificate", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy, fontSize = 13.sp)
                        Text("Open Form", fontFamily = FontFamily.Serif, color = LgesNavy.copy(alpha = 0.6f), fontSize = 11.sp)
                    }

                    DocumentCard(
                        modifier = Modifier.weight(1f).testTag("quick_action_cloud_sync"),
                        onClick = {
                            viewModel.uploadCertificateToSheets()
                            Toast.makeText(context, "Syncing with Google Sheets...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.CloudSync, contentDescription = "Cloud Sync", tint = LgesNavy, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Cloud Sync", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy, fontSize = 13.sp)
                        Text("Sync Sheets", fontFamily = FontFamily.Serif, color = LgesNavy.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DocumentCard(
                        modifier = Modifier.weight(1f).testTag("quick_action_export_csv"),
                        onClick = {
                            Exporter.shareCsvRegistry(context, certificates)
                        }
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Export CSV", tint = LgesNavy, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Export CSV", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy, fontSize = 13.sp)
                        Text("Share Registry", fontFamily = FontFamily.Serif, color = LgesNavy.copy(alpha = 0.6f), fontSize = 11.sp)
                    }

                    DocumentCard(
                        modifier = Modifier.weight(1f).testTag("quick_action_reset_form"),
                        onClick = {
                            viewModel.clearForm()
                            Toast.makeText(context, "Form fields reset", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.CleaningServices, contentDescription = "Reset Form", tint = LgesNavy, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reset Form", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy, fontSize = 13.sp)
                        Text("Clear Inputs", fontFamily = FontFamily.Serif, color = LgesNavy.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Certificate Generator & Database Registry Tab
 */
@Composable
fun CertificatesTab(
    viewModel: CertificateViewModel,
    certificates: List<Certificate>,
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    isUploading: Boolean,
    uploadStatus: String?,
    uploadError: String?,
    webAppUrlConfigured: Boolean
) {
    val context = LocalContext.current
    val rollNo by viewModel.rollNo.collectAsStateWithLifecycle()
    val studentName by viewModel.studentName.collectAsStateWithLifecycle()
    val relationPrefix by viewModel.relationPrefix.collectAsStateWithLifecycle()
    val fatherName by viewModel.fatherName.collectAsStateWithLifecycle()
    val courseName by viewModel.courseName.collectAsStateWithLifecycle()
    val sessionRange by viewModel.sessionRange.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val grade by viewModel.grade.collectAsStateWithLifecycle()
    val placeOfIssue by viewModel.placeOfIssue.collectAsStateWithLifecycle()
    val dateOfIssue by viewModel.dateOfIssue.collectAsStateWithLifecycle()
    val certType by viewModel.certType.collectAsStateWithLifecycle()

    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    val grades = listOf("S", "A", "C", "D", "F")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Preview Pane Header
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Live Canvas Preview",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LivePreviewPane(
                    previewBitmap = previewBitmap,
                    isGeneratingPreview = isGeneratingPreview,
                    rollNo = rollNo,
                    certType = certType
                )
            }
        }

        // Certificate Form Parameters
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Certificate Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                FormFieldsSection(
                    viewModel = viewModel,
                    rollNo = rollNo,
                    studentName = studentName,
                    relationPrefix = relationPrefix,
                    fatherName = fatherName,
                    courseName = courseName,
                    sessionRange = sessionRange,
                    duration = duration,
                    grade = grade,
                    placeOfIssue = placeOfIssue,
                    dateOfIssue = dateOfIssue,
                    certType = certType,
                    gradeDropdownExpanded = gradeDropdownExpanded,
                    onGradeDropdownToggle = { gradeDropdownExpanded = it },
                    grades = grades
                )
            }
        }

        // Export Controls
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export & Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExportControlsSection(
                    viewModel = viewModel,
                    isUploading = isUploading,
                    uploadStatus = uploadStatus,
                    uploadError = uploadError,
                    onExportPdf = {
                        previewBitmap?.let { bmp ->
                            val name = if (studentName.isNotEmpty()) studentName.replace(" ", "_") else "Certificate"
                            Exporter.savePdfToDevice(context, bmp, "${rollNo}_$name")
                        }
                    },
                    onExportPng = {
                        previewBitmap?.let { bmp ->
                            val name = if (studentName.isNotEmpty()) studentName.replace(" ", "_") else "Certificate"
                            Exporter.saveBitmapToDevice(context, bmp, "${rollNo}_$name")
                        }
                    },
                    onSharePdf = {
                        previewBitmap?.let { bmp ->
                            val name = if (studentName.isNotEmpty()) studentName.replace(" ", "_") else "Certificate"
                            Exporter.sharePdf(context, bmp, "${rollNo}_$name")
                        }
                    },
                    webAppUrlConfigured = webAppUrlConfigured
                )
            }
        }

        // Saved Certificates Registry List
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                HistoryTab(
                    viewModel = viewModel,
                    certificates = certificates,
                    onSelectCertificate = { cert ->
                        viewModel.loadCertificateForEditing(cert)
                        Toast.makeText(context, "Loaded into editor!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun FormFieldsSection(
    viewModel: CertificateViewModel,
    rollNo: String,
    studentName: String,
    relationPrefix: String,
    fatherName: String,
    courseName: String,
    sessionRange: String,
    duration: String,
    grade: String,
    placeOfIssue: String,
    dateOfIssue: String,
    certType: String,
    gradeDropdownExpanded: Boolean,
    onGradeDropdownToggle: (Boolean) -> Unit,
    grades: List<String>
) {
    val context = LocalContext.current
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LgesNavy,
        unfocusedBorderColor = LgesNavy.copy(alpha = 0.3f),
        focusedLabelColor = LgesNavy,
        unfocusedLabelColor = LgesNavy.copy(alpha = 0.6f),
        focusedTextColor = LgesNavy,
        unfocusedTextColor = LgesNavy,
        focusedLeadingIconColor = LgesNavy,
        unfocusedLeadingIconColor = LgesNavy.copy(alpha = 0.4f),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Toggle Course / Internship Type
        Column {
            Text(
                text = "Certificate Template Type",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.certType.value = "Course" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (certType == "Course") LgesNavy else Color.White,
                        contentColor = if (certType == "Course") Color.White else LgesNavy
                    ),
                    border = BorderStroke(1.dp, LgesNavy),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("type_course_button")
                ) {
                    Text("Course", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.certType.value = "Internship" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (certType == "Internship") LgesNavy else Color.White,
                        contentColor = if (certType == "Internship") Color.White else LgesNavy
                    ),
                    border = BorderStroke(1.dp, LgesNavy),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("type_internship_button")
                ) {
                    Text("Internship", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Roll No / Certificate No
        OutlinedTextField(
            value = rollNo,
            onValueChange = { viewModel.rollNo.value = it },
            label = { Text("Roll No. / Certificate No.", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("roll_no_input")
        )

        // Student Name
        OutlinedTextField(
            value = studentName,
            onValueChange = { viewModel.studentName.value = it },
            label = { Text("Student Name", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("student_name_input")
        )

        // Father / Parent Relation & Name Selection
        AnimatedVisibility(visible = certType == "Course") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Relation Type (S/O, D/O, W/O)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("S/O", "D/O", "W/O").forEach { prefix ->
                        val isSelected = relationPrefix == prefix
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) LgesNavy else Color.White)
                                .border(1.dp, LgesNavy, RoundedCornerShape(6.dp))
                                .clickable { viewModel.relationPrefix.value = prefix }
                                .testTag("relation_${prefix.lowercase().replace("/", "_")}_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (prefix) {
                                    "S/O" -> "S/O (Son of)"
                                    "D/O" -> "D/O (Daughter of)"
                                    else -> "W/O (Wife of)"
                                },
                                color = if (isSelected) Color.White else LgesNavy,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { viewModel.fatherName.value = it },
                    label = { Text("$relationPrefix Name / Father's Name", fontFamily = FontFamily.Serif) },
                    leadingIcon = { Icon(Icons.Default.SupervisorAccount, contentDescription = null) },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("father_name_input")
                )
            }
        }

        // Course Name
        OutlinedTextField(
            value = courseName,
            onValueChange = { viewModel.courseName.value = it },
            label = { Text(if (certType == "Course") "Course Name" else "Internship Name", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("course_name_input")
        )

        // Session
        OutlinedTextField(
            value = sessionRange,
            onValueChange = { viewModel.sessionRange.value = it },
            label = { Text("Academic Session / Date Range", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("session_range_input")
        )

        // Duration
        OutlinedTextField(
            value = duration,
            onValueChange = { viewModel.duration.value = it },
            label = { Text("Duration", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("duration_input")
        )

        // Performance Grade Selection
        Column {
            Text("Performance Grade", fontSize = 12.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grades.forEach { gradeOption ->
                    val isSelected = grade == gradeOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) LgesNavy else Color.White)
                            .border(1.dp, LgesNavy, RoundedCornerShape(6.dp))
                            .clickable { viewModel.grade.value = gradeOption },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gradeOption,
                            color = if (isSelected) Color.White else LgesNavy,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Place of Issue
        OutlinedTextField(
            value = placeOfIssue,
            onValueChange = { viewModel.placeOfIssue.value = it },
            label = { Text("Place of Issue", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("place_of_issue_input")
        )

        // Date of Issue
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dateOfIssue,
                onValueChange = { viewModel.dateOfIssue.value = it },
                label = { Text("Date of Issue", fontFamily = FontFamily.Serif) },
                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                singleLine = true,
                colors = textFieldColors,
                modifier = Modifier.weight(1f).testTag("date_of_issue_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        val mm = if (m + 1 < 10) "0${m + 1}" else "${m + 1}"
                        val dd = if (d < 10) "0$d" else "$d"
                        viewModel.dateOfIssue.value = "$dd-$mm-$y"
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = LgesNavy, contentColor = Color.White),
                border = BorderStroke(1.dp, LgesGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(52.dp).padding(top = 6.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = LgesGold)
            }
        }
    }
}

@Composable
fun LivePreviewPane(
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    rollNo: String,
    certType: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(2.dp, LgesGold, RoundedCornerShape(8.dp))
            .background(LgesNavy),
        contentAlignment = Alignment.Center
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Live Canvas Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
            if (isGeneratingPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LgesGold, modifier = Modifier.size(24.dp))
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = LgesGold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Generating High-Resolution Canvas...", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Serif)
            }
        }
    }
}

@Composable
fun ExportControlsSection(
    viewModel: CertificateViewModel,
    isUploading: Boolean,
    uploadStatus: String?,
    uploadError: String?,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onSharePdf: () -> Unit,
    webAppUrlConfigured: Boolean
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (uploadStatus != null) {
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uploadStatus, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }

        if (uploadError != null) {
            Surface(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFC62828))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uploadError, color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = LgesNavy, contentColor = Color.White),
                border = BorderStroke(1.dp, LgesGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(46.dp).testTag("export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = LgesGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }

            Button(
                onClick = onExportPng,
                colors = ButtonDefaults.buttonColors(containerColor = LgesNavy, contentColor = Color.White),
                border = BorderStroke(1.dp, LgesGold),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(46.dp).testTag("export_png_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = LgesGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSharePdf,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LgesNavy),
                border = BorderStroke(1.dp, LgesNavy),
                modifier = Modifier.weight(1f).height(46.dp).testTag("share_pdf_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }

            Button(
                onClick = {
                    viewModel.saveCertificateLocally(
                        onSuccess = { Toast.makeText(context, "Saved to database!", Toast.LENGTH_SHORT).show() },
                        onError = { err -> Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show() }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LgesGold, contentColor = LgesNavy),
                border = BorderStroke(1.dp, LgesNavy),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).height(46.dp).testTag("save_local_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate & Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            }
        }

        if (isUploading) {
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LgesNavy, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing to Google Sheet...", color = LgesNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    viewModel: CertificateViewModel,
    certificates: List<Certificate>,
    onSelectCertificate: (Certificate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCertificates = certificates.filter {
        it.studentName.contains(searchQuery, ignoreCase = true) ||
                it.rollNo.contains(searchQuery, ignoreCase = true) ||
                it.courseName.contains(searchQuery, ignoreCase = true)
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LgesNavy,
        unfocusedBorderColor = LgesNavy.copy(alpha = 0.3f),
        focusedLabelColor = LgesNavy,
        unfocusedLabelColor = LgesNavy.copy(alpha = 0.6f),
        focusedTextColor = LgesNavy,
        unfocusedTextColor = LgesNavy,
        focusedLeadingIconColor = LgesNavy,
        unfocusedLeadingIconColor = LgesNavy.copy(alpha = 0.4f),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Student, Roll No, Course...", fontFamily = FontFamily.Serif) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LgesNavy) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = LgesNavy)
                    }
                }
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth().testTag("search_history_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredCertificates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = LgesNavy.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Saved Certificates Found", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LgesNavy)
                }
            }
        } else {
            Text(
                "Certificate Registry (${filteredCertificates.size} entries)",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCertificates.forEach { cert ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LgesNavy.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LgesNavy)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(cert.certType.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                                }

                                Row {
                                    IconButton(
                                        onClick = { onSelectCertificate(cert) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = LgesNavy)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCertificate(cert.rollNo) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFC62828))
                                    }
                                }
                            }

                            Text(cert.studentName, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LgesNavy)
                            Text("Course: ${cert.courseName}", fontFamily = FontFamily.Serif, fontSize = 12.sp, color = LgesNavy.copy(alpha = 0.8f))
                            Text("Roll No: ${cert.rollNo} | Grade: ${cert.grade}", fontFamily = FontFamily.Serif, fontSize = 11.sp, color = LgesNavy.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}
