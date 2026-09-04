package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.database.Certificate
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy
import com.example.util.CertificateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

private val ScreenBackground = Color(0xFFF8F9FA)
private val BorderLight = Color(0xFFE0E3E6)
private val SuccessGreen = Color(0xFF2E7D32)
private val ErrorRed = Color(0xFFC62828)

private val Grades = listOf("S", "A", "C", "D", "F")

/* -------------------------------------------------------------------------- */
/* LOGO                                                                       */
/* -------------------------------------------------------------------------- */

@Composable
fun SafeInstituteLogo(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val painter = remember(context) {
        try {
            val drawable =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.img_lges_logo
                )

            drawable?.let {
                val width =
                    if (it.intrinsicWidth > 0) it.intrinsicWidth else 512

                val height =
                    if (it.intrinsicHeight > 0) it.intrinsicHeight else 512

                val bitmap =
                    Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                    )

                val canvas = Canvas(bitmap)

                it.setBounds(
                    0,
                    0,
                    width,
                    height
                )

                it.draw(canvas)

                BitmapPainter(bitmap.asImageBitmap())
            }
        } catch (_: Exception) {
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
                imageVector = Icons.Default.Person,
                contentDescription = "LGES Logo",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* COMMON CARD                                                                */
/* -------------------------------------------------------------------------- */

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
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = LgesGold,
                    shape = RoundedCornerShape(7.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* MAIN SCREEN                                                                */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateAdminScreen(
    viewModel: CertificateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

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

    val webAppUrl by viewModel.webAppUrl.collectAsStateWithLifecycle()
    val verificationBaseUrl by
        viewModel.verificationBaseUrl.collectAsStateWithLifecycle()

    val certificates by
        viewModel.allCertificates.collectAsStateWithLifecycle()

    val isUploading by
        viewModel.isUploading.collectAsStateWithLifecycle()

    val uploadStatus by
        viewModel.uploadStatus.collectAsStateWithLifecycle()

    val uploadError by
        viewModel.uploadError.collectAsStateWithLifecycle()

    /*
     * Keep the preview outside the ViewModel.
     * The certificate itself is always generated from the current
     * ViewModel state.
     */
    var previewBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var isGeneratingPreview by remember {
        mutableStateOf(false)
    }

    /*
     * Debounced preview generation.
     *
     * This fixes the original problem where a complete 2400x1600
     * certificate was rendered on every single text-field keystroke.
     */
    LaunchedEffect(
        rollNo,
        studentName,
        relationPrefix,
        fatherName,
        courseName,
        sessionRange,
        duration,
        grade,
        placeOfIssue,
        dateOfIssue,
        certType,
        verificationBaseUrl
    ) {
        isGeneratingPreview = true

        delay(350)

        val generatedBitmap =
            withContext(Dispatchers.Default) {
                try {
                    val certificate =
                        viewModel.getAsCertificate()

                    if (certificate.rollNo.isBlank()) {
                        /*
                         * Still render the template so the user can
                         * see the certificate structure before entering
                         * a roll number.
                         */
                    }

                    val verificationUrl =
                        CertificateConfig.buildVerificationUrl(
                            certificate.certificateId,
                            verificationBaseUrl
                        )

                    val qr =
                        if (certificate.certificateId.isNotBlank()) {
                            Exporter.generateQrCode(
                                verificationUrl,
                                250
                            )
                        } else {
                            null
                        }

                    CertificateDrawer.drawCertificate(
                        context,
                        certificate,
                        qr
                    )
                } catch (error: Exception) {
                    error.printStackTrace()
                    null
                }
            }

        previewBitmap = generatedBitmap
        isGeneratingPreview = false
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,

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
                        .padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafeInstituteLogo(
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "LGES Credentials",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Certificate Administration",
                            fontFamily = FontFamily.Serif,
                            color = LgesNavy.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearForm()

                            Toast.makeText(
                                context,
                                "Form cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag(
                            "reset_form_button"
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset form",
                            tint = LgesNavy
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LgesNavy)
                            .border(
                                1.5.dp,
                                LgesGold,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Admin Profile",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },

        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    0.5.dp,
                    BorderLight
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminNavigationItem(
                        index = 0,
                        title = "Dashboard",
                        icon = Icons.Default.Dashboard,
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                        }
                    )

                    AdminNavigationItem(
                        index = 1,
                        title = "Certificates",
                        icon = Icons.Default.WorkspacePremium,
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                        }
                    )

                    AdminNavigationItem(
                        index = 2,
                        title = "Settings",
                        icon = Icons.Default.Settings,
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBackground)
        ) {
            when (selectedTab) {
                0 -> {
                    DashboardTab(
                        viewModel = viewModel,
                        certificates = certificates,
                        previewBitmap = previewBitmap,
                        isGeneratingPreview = isGeneratingPreview,
                        onNavigateToGenerate = {
                            selectedTab = 1
                        }
                    )
                }

                1 -> {
                    CertificatesTab(
                        viewModel = viewModel,
                        certificates = certificates,
                        previewBitmap = previewBitmap,
                        isGeneratingPreview = isGeneratingPreview,
                        isUploading = isUploading,
                        uploadStatus = uploadStatus,
                        uploadError = uploadError,
                        webAppUrlConfigured =
                            webAppUrl.isNotBlank()
                    )
                }

                2 -> {
                    SettingsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* BOTTOM NAVIGATION                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun AdminNavigationItem(
    index: Int,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag("nav_tab_$index"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint =
                if (selected) {
                    LgesNavy
                } else {
                    LgesNavy.copy(alpha = 0.45f)
                },
            modifier = Modifier.size(23.dp)
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            color =
                if (selected) {
                    LgesNavy
                } else {
                    LgesNavy.copy(alpha = 0.55f)
                },
            fontSize = 11.sp
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Box(
            modifier = Modifier
                .width(
                    if (selected) 22.dp else 0.dp
                )
                .height(2.dp)
                .background(
                    if (selected) {
                        LgesGold
                    } else {
                        Color.Transparent
                    }
                )
        )
    }
}

/* -------------------------------------------------------------------------- */
/* DASHBOARD                                                                  */
/* -------------------------------------------------------------------------- */

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
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Welcome back, Admin",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 27.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Manage institutional credentials and issue secure certificates.",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Button(
                    onClick = {
                        viewModel.clearForm()
                        onNavigateToGenerate()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgesNavy,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        LgesGold
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag(
                            "generate_new_cert_button"
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = LgesGold
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Generate New Certificate",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            DashboardStats(
                certificates = certificates
            )
        }

        item {
            RecentActivity(
                certificates = certificates,
                onViewAll = onNavigateToGenerate
            )
        }

        item {
            DashboardPreview(
                previewBitmap = previewBitmap,
                isGeneratingPreview = isGeneratingPreview,
                onOpen = onNavigateToGenerate
            )
        }

        item {
            QuickActions(
                viewModel = viewModel,
                certificates = certificates,
                onGenerate = onNavigateToGenerate
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* DASHBOARD STATS                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun DashboardStats(
    certificates: List<Certificate>
) {
    val synced =
        certificates.count { it.isSynced }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "TOTAL ISSUED",
                value = certificates.size.toString(),
                trailing = if (certificates.isNotEmpty()) {
                    "Active"
                } else {
                    null
                }
            )

            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "SYNCED",
                value = "${synced}/${certificates.size}",
                icon =
                    if (
                        certificates.isNotEmpty() &&
                        synced == certificates.size
                    ) {
                        Icons.Outlined.CloudDone
                    } else {
                        Icons.Outlined.CloudQueue
                    }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "ACTIVE TEMPLATE",
                value = "1",
                icon = Icons.Outlined.Description
            )

            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "VERIFICATION",
                value = "N/A",
                trailing = "Unmetered"
            )
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    trailing: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    DocumentCard(
        modifier = modifier
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LgesNavy.copy(alpha = 0.55f),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy,
                fontSize = 21.sp
            )

            when {
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LgesGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                trailing != null -> {
                    Text(
                        text = trailing,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* RECENT ACTIVITY                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun RecentActivity(
    certificates: List<Certificate>,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Activity",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy,
                fontSize = 20.sp
            )

            if (certificates.isNotEmpty()) {
                Text(
                    text = "View All",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(
                        onClick = onViewAll
                    )
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (certificates.isEmpty()) {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = LgesNavy,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Column {
                        Text(
                            text = "No Activity Yet",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy,
                            fontSize = 14.sp
                        )

                        Text(
                            text =
                                "Generated certificates will appear here.",
                            fontFamily = FontFamily.Serif,
                            color = LgesNavy.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                certificates
                    .take(5)
                    .forEach { certificate ->
                        RecentCertificateRow(
                            certificate = certificate
                        )
                    }
            }
        }
    }
}

@Composable
private fun RecentCertificateRow(
    certificate: Certificate
) {
    DocumentCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        LgesNavy.copy(alpha = 0.07f)
                    )
                    .border(
                        1.dp,
                        LgesGold,
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardMembership,
                    contentDescription = null,
                    tint = LgesNavy
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = certificate.studentName,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "${certificate.courseName} • ${certificate.dateOfIssue}",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.65f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            StatusBadge(
                text =
                    if (certificate.isSynced) {
                        "Synced"
                    } else {
                        "Local"
                    },
                success = certificate.isSynced
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* DASHBOARD PREVIEW                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun DashboardPreview(
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    onOpen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = LgesNavy
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.5.dp,
            LgesGold
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Live Preview",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesGold,
                        fontSize = 21.sp
                    )

                    Text(
                        text = "LGES Reference Certificate",
                        fontFamily = FontFamily.Serif,
                        color = LgesGold.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onOpen
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Open certificate generator",
                        tint = LgesGold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            CertificatePreviewBox(
                previewBitmap = previewBitmap,
                isGeneratingPreview = isGeneratingPreview,
                height = 190.dp
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* QUICK ACTIONS                                                              */
/* -------------------------------------------------------------------------- */

@Composable
private fun QuickActions(
    viewModel: CertificateViewModel,
    certificates: List<Certificate>,
    onGenerate: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Quick Actions",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LgesNavy,
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                tag = "quick_action_new_cert",
                icon = Icons.Outlined.NoteAdd,
                title = "New Certificate",
                subtitle = "Open Form",
                onClick = {
                    viewModel.clearForm()
                    onGenerate()
                }
            )

            QuickActionCard(
                modifier = Modifier.weight(1f),
                tag = "quick_action_cloud_sync",
                icon = Icons.Outlined.CloudSync,
                title = "Cloud Sync",
                subtitle = "Sync Sheets",
                onClick = {
                    viewModel.uploadCertificateToSheets()

                    Toast.makeText(
                        context,
                        "Starting cloud sync...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                tag = "quick_action_export_csv",
                icon = Icons.Outlined.FileDownload,
                title = "Export CSV",
                subtitle = "Share Registry",
                onClick = {
                    if (certificates.isEmpty()) {
                        Toast.makeText(
                            context,
                            "No certificates to export.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Exporter.shareCsvRegistry(
                            context,
                            certificates
                        )
                    }
                }
            )

            QuickActionCard(
                modifier = Modifier.weight(1f),
                tag = "quick_action_reset_form",
                icon = Icons.Outlined.CleaningServices,
                title = "Reset Form",
                subtitle = "Clear Inputs",
                onClick = {
                    viewModel.clearForm()

                    Toast.makeText(
                        context,
                        "Form reset.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    DocumentCard(
        modifier = modifier.testTag(tag),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = LgesNavy,
            modifier = Modifier.size(25.dp)
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LgesNavy,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            fontFamily = FontFamily.Serif,
            color = LgesNavy.copy(alpha = 0.6f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* -------------------------------------------------------------------------- */
/* CERTIFICATES TAB                                                           */
/* -------------------------------------------------------------------------- */

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

    val validationErrors by
        viewModel.validationErrors.collectAsStateWithLifecycle()

    val duplicateNote by
        viewModel.duplicateNote.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Certificate Preview",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 17.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                CertificatePreviewBox(
                    previewBitmap = previewBitmap,
                    isGeneratingPreview = isGeneratingPreview,
                    height = 220.dp
                )
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title = "Certificate Details"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

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
                    validationErrors = validationErrors,
                    duplicateNote = duplicateNote
                )
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title = "Export & Actions"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ExportControlsSection(
                    viewModel = viewModel,
                    isUploading = isUploading,
                    uploadStatus = uploadStatus,
                    uploadError = uploadError,
                    onExportPdf = {
                        previewBitmap?.let { bitmap ->
                            val fileName =
                                certificateFileName(
                                    rollNo,
                                    studentName
                                )

                            Exporter.savePdfToDevice(
                                context,
                                bitmap,
                                fileName
                            )
                        } ?: showNoPreviewToast(context)
                    },
                    onExportPng = {
                        previewBitmap?.let { bitmap ->
                            val fileName =
                                certificateFileName(
                                    rollNo,
                                    studentName
                                )

                            Exporter.saveBitmapToDevice(
                                context,
                                bitmap,
                                fileName
                            )
                        } ?: showNoPreviewToast(context)
                    },
                    onSharePdf = {
                        previewBitmap?.let { bitmap ->
                            val fileName =
                                certificateFileName(
                                    rollNo,
                                    studentName
                                )

                            Exporter.sharePdf(
                                context,
                                bitmap,
                                fileName
                            )
                        } ?: showNoPreviewToast(context)
                    },
                    webAppUrlConfigured =
                        webAppUrlConfigured
                )
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title = "Certificate Registry"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                HistoryTab(
                    viewModel = viewModel,
                    certificates = certificates,
                    onSelectCertificate = { certificate ->
                        viewModel.loadCertificateForEditing(
                            certificate
                        )

                        Toast.makeText(
                            context,
                            "Certificate loaded into editor.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* FORM                                                                       */
/* -------------------------------------------------------------------------- */

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
    validationErrors: FormValidationErrors,
    duplicateNote: String?
) {
    val context = LocalContext.current

    val textFieldColors =
        remember {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LgesNavy,
                unfocusedBorderColor =
                    LgesNavy.copy(alpha = 0.28f),
                focusedLabelColor = LgesNavy,
                unfocusedLabelColor =
                    LgesNavy.copy(alpha = 0.6f),
                focusedTextColor = LgesNavy,
                unfocusedTextColor = LgesNavy,
                focusedLeadingIconColor = LgesNavy,
                unfocusedLeadingIconColor =
                    LgesNavy.copy(alpha = 0.4f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed
            )
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Certificate Category",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LgesNavy,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryButton(
                modifier = Modifier.weight(1f),
                title = "Course",
                selected = certType.equals(
                    "Course",
                    ignoreCase = true
                ),
                onClick = {
                    viewModel.certType.value = "Course"
                },
                tag = "type_course_button"
            )

            CategoryButton(
                modifier = Modifier.weight(1f),
                title = "Internship",
                selected = certType.equals(
                    "Internship",
                    ignoreCase = true
                ),
                onClick = {
                    viewModel.certType.value = "Internship"
                },
                tag = "type_internship_button"
            )
        }

        if (duplicateNote != null) {
            InfoBanner(
                text = duplicateNote
            )
        }

        OutlinedTextField(
            value = rollNo,
            onValueChange = { value ->
                viewModel.rollNo.value = value
                viewModel.checkForDuplicateRollNo(
                    value
                )
            },
            label = {
                Text(
                    "Roll No. / Certificate No.",
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null
                )
            },
            isError =
                validationErrors.rollNoError != null,
            supportingText = {
                validationErrors.rollNoError?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                }
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("roll_no_input")
        )

        OutlinedTextField(
            value = studentName,
            onValueChange = {
                viewModel.studentName.value = it
            },
            label = {
                Text(
                    "Student Name",
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            isError =
                validationErrors.studentNameError != null,
            supportingText = {
                validationErrors.studentNameError?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                }
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("student_name_input")
        )

        AnimatedVisibility(
            visible = certType.equals(
                "Course",
                ignoreCase = true
            )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "Relation Type",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {
                    RelationButton(
                        modifier = Modifier.weight(1f),
                        title = "S/O",
                        selected =
                            relationPrefix == "S/O",
                        onClick = {
                            viewModel.relationPrefix.value =
                                "S/O"
                        },
                        tag = "relation_s_o_button"
                    )

                    RelationButton(
                        modifier = Modifier.weight(1f),
                        title = "D/O",
                        selected =
                            relationPrefix == "D/O",
                        onClick = {
                            viewModel.relationPrefix.value =
                                "D/O"
                        },
                        tag = "relation_d_o_button"
                    )

                    RelationButton(
                        modifier = Modifier.weight(1f),
                        title = "W/O",
                        selected =
                            relationPrefix == "W/O",
                        onClick = {
                            viewModel.relationPrefix.value =
                                "W/O"
                        },
                        tag = "relation_w_o_button"
                    )
                }

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = {
                        viewModel.fatherName.value = it
                    },
                    label = {
                        Text(
                            "$relationPrefix Name / Father's Name",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector =
                                Icons.Default.SupervisorAccount,
                            contentDescription = null
                        )
                    },
                    isError =
                        validationErrors.fatherNameError != null,
                    supportingText = {
                        validationErrors.fatherNameError?.let {
                            Text(
                                text = it,
                                color = ErrorRed,
                                fontSize = 11.sp
                            )
                        }
                    },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("father_name_input")
                )
            }
        }

        OutlinedTextField(
            value = courseName,
            onValueChange = {
                viewModel.courseName.value = it
            },
            label = {
                Text(
                    if (
                        certType.equals(
                            "Course",
                            ignoreCase = true
                        )
                    ) {
                        "Course Name"
                    } else {
                        "Internship Name"
                    },
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null
                )
            },
            isError =
                validationErrors.courseNameError != null,
            supportingText = {
                validationErrors.courseNameError?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                }
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("course_name_input")
        )

        OutlinedTextField(
            value = sessionRange,
            onValueChange = {
                viewModel.sessionRange.value = it
            },
            label = {
                Text(
                    "Academic Session / Date Range",
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector =
                        Icons.Default.CalendarToday,
                    contentDescription = null
                )
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("session_range_input")
        )

        OutlinedTextField(
            value = duration,
            onValueChange = {
                viewModel.duration.value = it
            },
            label = {
                Text(
                    "Duration",
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null
                )
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("duration_input")
        )

        Text(
            text = "Performance Grade",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = LgesNavy,
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Grades.forEach { gradeOption ->
                GradeButton(
                    modifier = Modifier.weight(1f),
                    grade = gradeOption,
                    selected =
                        grade.equals(
                            gradeOption,
                            ignoreCase = true
                        ),
                    onClick = {
                        viewModel.grade.value =
                            gradeOption
                    }
                )
            }
        }

        OutlinedTextField(
            value = placeOfIssue,
            onValueChange = {
                viewModel.placeOfIssue.value = it
            },
            label = {
                Text(
                    "Place of Issue",
                    fontFamily = FontFamily.Serif
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null
                )
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("place_of_issue_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = dateOfIssue,
                onValueChange = {
                    viewModel.dateOfIssue.value = it
                },
                label = {
                    Text(
                        "Date of Issue",
                        fontFamily = FontFamily.Serif
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null
                    )
                },
                isError =
                    validationErrors.dateOfIssueError != null,
                supportingText = {
                    validationErrors.dateOfIssueError?.let {
                        Text(
                            text = it,
                            color = ErrorRed,
                            fontSize = 11.sp
                        )
                    }
                },
                singleLine = true,
                colors = textFieldColors,
                modifier = Modifier
                    .weight(1f)
                    .testTag("date_of_issue_input")
            )

            Button(
                onClick = {
                    val calendar =
                        Calendar.getInstance()

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val formattedMonth =
                                (month + 1)
                                    .toString()
                                    .padStart(2, '0')

                            val formattedDay =
                                day
                                    .toString()
                                    .padStart(2, '0')

                            viewModel.dateOfIssue.value =
                                "$formattedDay-$formattedMonth-$year"
                        },
                        calendar.get(
                            Calendar.YEAR
                        ),
                        calendar.get(
                            Calendar.MONTH
                        ),
                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        )
                    ).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LgesNavy,
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    LgesGold
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Pick date",
                    tint = LgesGold
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* FORM BUTTONS                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun CategoryButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (selected) {
                    LgesNavy
                } else {
                    Color.White
                },
            contentColor =
                if (selected) {
                    Color.White
                } else {
                    LgesNavy
                }
        ),
        border = BorderStroke(
            1.dp,
            LgesNavy
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .height(42.dp)
            .testTag(tag)
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RelationButton(
    modifier: Modifier,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) {
                    LgesNavy
                } else {
                    Color.White
                }
            )
            .border(
                1.dp,
                LgesNavy,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color =
                if (selected) {
                    Color.White
                } else {
                    LgesNavy
                },
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun GradeButton(
    modifier: Modifier,
    grade: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (selected) {
                    LgesNavy
                } else {
                    Color.White
                }
            )
            .border(
                1.dp,
                LgesNavy,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade,
            color =
                if (selected) {
                    Color.White
                } else {
                    LgesNavy
                },
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/* -------------------------------------------------------------------------- */
/* PREVIEW                                                                    */
/* -------------------------------------------------------------------------- */

@Composable
private fun CertificatePreviewBox(
    previewBitmap: Bitmap?,
    isGeneratingPreview: Boolean,
    height: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .border(
                2.dp,
                LgesGold,
                RoundedCornerShape(8.dp)
            )
            .background(
                if (previewBitmap != null) {
                    Color.White
                } else {
                    LgesNavy
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Certificate preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            )

            if (isGeneratingPreview) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = 0.35f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = LgesGold,
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = LgesGold,
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 3.dp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Generating Certificate...",
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* EXPORT CONTROLS                                                            */
/* -------------------------------------------------------------------------- */

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

    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        if (uploadStatus != null) {
            StatusBanner(
                text = uploadStatus,
                success = true
            )
        }

        if (uploadError != null) {
            StatusBanner(
                text = uploadError,
                success = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Export PDF",
                icon = Icons.Default.PictureAsPdf,
                onClick = onExportPdf,
                enabled = true,
                primary = true,
                tag = "export_pdf_button"
            )

            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Save PNG",
                icon = Icons.Default.Image,
                onClick = onExportPng,
                enabled = true,
                primary = true,
                tag = "export_png_button"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Share PDF",
                icon = Icons.Default.Share,
                onClick = onSharePdf,
                enabled = true,
                primary = false,
                tag = "share_pdf_button"
            )

            ActionButton(
                modifier = Modifier.weight(1f),
                text = "Generate & Save",
                icon = Icons.Default.Save,
                onClick = {
                    viewModel.saveCertificateLocally(
                        onSuccess = { isUpdate ->
                            Toast.makeText(
                                context,
                                if (isUpdate) {
                                    "Certificate updated successfully."
                                } else {
                                    "Certificate saved successfully."
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { error ->
                            Toast.makeText(
                                context,
                                error,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                },
                enabled = !isUploading,
                primary = false,
                gold = true,
                tag = "save_local_button"
            )
        }

        Button(
            onClick = {
                viewModel.uploadCertificateToSheets()
            },
            enabled =
                !isUploading &&
                        webAppUrlConfigured,
            colors = ButtonDefaults.buttonColors(
                containerColor = LgesNavy,
                contentColor = Color.White,
                disabledContainerColor =
                    LgesNavy.copy(alpha = 0.35f),
                disabledContentColor =
                    Color.White.copy(alpha = 0.65f)
            ),
            border = BorderStroke(
                1.dp,
                LgesGold
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("sync_sheets_button")
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    color = LgesGold,
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = LgesGold
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )
            }

            Text(
                text =
                    if (isUploading) {
                        "Syncing..."
                    } else if (!webAppUrlConfigured) {
                        "Configure Sheets URL First"
                    } else {
                        "Sync Certificate to Google Sheets"
                    },
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isUploading) {
            StatusBanner(
                text = "Uploading certificate to Google Sheets...",
                success = true,
                progress = true
            )
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    gold: Boolean = false,
    tag: String
) {
    if (primary || gold) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (gold) {
                        LgesGold
                    } else {
                        LgesNavy
                    },
                contentColor =
                    if (gold) {
                        LgesNavy
                    } else {
                        Color.White
                    }
            ),
            border = BorderStroke(
                1.dp,
                if (gold) {
                    LgesNavy
                } else {
                    LgesGold
                }
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = modifier
                .height(46.dp)
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (gold) {
                        LgesNavy
                    } else {
                        LgesGold
                    },
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(5.dp)
            )

            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LgesNavy
            ),
            border = BorderStroke(
                1.dp,
                LgesNavy
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = modifier
                .height(46.dp)
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(5.dp)
            )

            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* HISTORY                                                                    */
/* -------------------------------------------------------------------------- */

@Composable
fun HistoryTab(
    viewModel: CertificateViewModel,
    certificates: List<Certificate>,
    onSelectCertificate: (Certificate) -> Unit
) {
    val context = LocalContext.current

    var searchQuery by remember {
        mutableStateOf("")
    }

    var certificateToDelete by remember {
        mutableStateOf<Certificate?>(null)
    }

    if (certificateToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                certificateToDelete = null
            },
            title = {
                Text(
                    text = "Delete Certificate",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy
                )
            },
            text = {
                Text(
                    text =
                        "Delete the certificate for " +
                                "${certificateToDelete?.studentName} " +
                                "(${certificateToDelete?.certificateId})?\n\n" +
                                "This action cannot be undone.",
                    fontFamily = FontFamily.Serif
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        certificateToDelete?.let {
                            certificate ->
                            viewModel.deleteCertificate(
                                certificate.certificateId,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Certificate deleted.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { error ->
                                    Toast.makeText(
                                        context,
                                        error,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }

                        certificateToDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        certificateToDelete = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = LgesNavy
                    )
                }
            }
        )
    }

    val query =
        searchQuery.trim()

    val filteredCertificates =
        if (query.isBlank()) {
            certificates
        } else {
            certificates.filter { certificate ->
                certificate.studentName.contains(
                    query,
                    ignoreCase = true
                ) ||
                        certificate.rollNo.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        certificate.certificateId.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        certificate.courseName.contains(
                            query,
                            ignoreCase = true
                        )
            }
        }

    val textFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LgesNavy,
            unfocusedBorderColor =
                LgesNavy.copy(alpha = 0.28f),
            focusedLabelColor = LgesNavy,
            unfocusedLabelColor =
                LgesNavy.copy(alpha = 0.6f),
            focusedTextColor = LgesNavy,
            unfocusedTextColor = LgesNavy,
            focusedLeadingIconColor = LgesNavy,
            unfocusedLeadingIconColor =
                LgesNavy.copy(alpha = 0.4f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            placeholder = {
                Text(
                    text =
                        "Search Student, Roll No, ID, Course...",
                    fontFamily = FontFamily.Serif,
                    fontSize = 12.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = LgesNavy
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = LgesNavy
                        )
                    }
                }
            },
            singleLine = true,
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input")
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (filteredCertificates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        tint =
                            LgesNavy.copy(alpha = 0.35f),
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Text(
                        text =
                            if (query.isBlank()) {
                                "No Saved Certificates"
                            } else {
                                "No Matching Certificates"
                            },
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = LgesNavy,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Text(
                text =
                    "Certificate Registry (${filteredCertificates.size})",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                filteredCertificates.forEach { certificate ->
                    CertificateRegistryItem(
                        certificate = certificate,
                        onEdit = {
                            onSelectCertificate(
                                certificate
                            )
                        },
                        onDelete = {
                            certificateToDelete =
                                certificate
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateRegistryItem(
    certificate: Certificate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(
            1.dp,
            LgesNavy.copy(alpha = 0.22f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text =
                        certificate.certType.uppercase(),
                    success = false
                )

                Spacer(
                    modifier = Modifier.width(5.dp)
                )

                StatusBadge(
                    text =
                        if (certificate.isSynced) {
                            "Synced"
                        } else {
                            "Local"
                        },
                    success = certificate.isSynced
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit certificate",
                        tint = LgesNavy,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete certificate",
                        tint = ErrorRed,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = certificate.studentName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = LgesNavy,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text =
                    "${certificate.courseName} • " +
                            "${certificate.dateOfIssue}",
                fontFamily = FontFamily.Serif,
                color = LgesNavy.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text =
                    "ID: ${certificate.certificateId} • " +
                            "Roll: ${certificate.rollNo} • " +
                            "Grade: ${certificate.grade}",
                fontFamily = FontFamily.Serif,
                color = LgesNavy.copy(alpha = 0.55f),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* SETTINGS                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun SettingsTab(
    viewModel: CertificateViewModel
) {
    val context = LocalContext.current

    val webAppUrl by
        viewModel.webAppUrl.collectAsStateWithLifecycle()

    val apiKey by
        viewModel.apiKey.collectAsStateWithLifecycle()

    val verificationBaseUrl by
        viewModel.verificationBaseUrl.collectAsStateWithLifecycle()

    val isTestingConnection by
        viewModel.isTestingConnection.collectAsStateWithLifecycle()

    var inputUrl by remember(webAppUrl) {
        mutableStateOf(webAppUrl)
    }

    var inputApiKey by remember(apiKey) {
        mutableStateOf(apiKey)
    }

    var inputVerificationUrl by
        remember(verificationBaseUrl) {
            mutableStateOf(
                verificationBaseUrl
            )
        }

    var connectionResult by remember {
        mutableStateOf<Pair<Boolean, String>?>(null)
    }

    val textFieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LgesNavy,
            unfocusedBorderColor =
                LgesNavy.copy(alpha = 0.28f),
            focusedLabelColor = LgesNavy,
            unfocusedLabelColor =
                LgesNavy.copy(alpha = 0.6f),
            focusedTextColor = LgesNavy,
            unfocusedTextColor = LgesNavy,
            focusedLeadingIconColor = LgesNavy,
            unfocusedLeadingIconColor =
                LgesNavy.copy(alpha = 0.4f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "System & Cloud Settings",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy,
                    fontSize = 24.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Configure Google Sheets synchronization, API security, and public certificate verification.",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title = "Google Sheets Web App URL"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Use the deployed Apps Script endpoint ending with /exec.",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        connectionResult = null
                    },
                    label = {
                        Text(
                            "Web App Endpoint URL",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null
                        )
                    },
                    singleLine = false,
                    maxLines = 3,
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            "settings_web_app_url_input"
                        )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateWebAppUrl(
                                inputUrl
                            )

                            Toast.makeText(
                                context,
                                "Web App URL saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LgesNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Save URL",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateWebAppUrl(
                                inputUrl
                            )

                            viewModel.testConnection { success, message ->
                                connectionResult =
                                    success to message
                            }
                        },
                        enabled = !isTestingConnection,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LgesNavy
                        ),
                        border = BorderStroke(
                            1.dp,
                            LgesNavy
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = LgesNavy,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector =
                                    Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            if (isTestingConnection) {
                                "Testing..."
                            } else {
                                "Test"
                            },
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                connectionResult?.let { result ->
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    StatusBanner(
                        text = result.second,
                        success = result.first
                    )
                }
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title =
                        "API Security Passphrase"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Optional key used by your Google Apps Script endpoint.",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = inputApiKey,
                    onValueChange = {
                        inputApiKey = it
                    },
                    label = {
                        Text(
                            "Security Passphrase",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            "settings_api_key_input"
                        )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        viewModel.updateApiKey(
                            inputApiKey
                        )

                        Toast.makeText(
                            context,
                            "API key updated.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgesNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Save Passphrase",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            DocumentCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionTitle(
                    title =
                        "Online Verification Portal"
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "This base URL is embedded into certificate QR codes.",
                    fontFamily = FontFamily.Serif,
                    color = LgesNavy.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = inputVerificationUrl,
                    onValueChange = {
                        inputVerificationUrl = it
                    },
                    label = {
                        Text(
                            "Verification Base URL",
                            fontFamily = FontFamily.Serif
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null
                        )
                    },
                    singleLine = false,
                    maxLines = 2,
                    colors = textFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(
                            "settings_verification_url_input"
                        )
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateVerificationBaseUrl(
                                inputVerificationUrl
                            )

                            Toast.makeText(
                                context,
                                "Verification URL saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LgesNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Save Portal URL",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetSettingsToDefault()

                            inputUrl =
                                viewModel.webAppUrl.value

                            inputApiKey =
                                viewModel.apiKey.value

                            inputVerificationUrl =
                                viewModel.verificationBaseUrl.value

                            connectionResult = null

                            Toast.makeText(
                                context,
                                "Settings restored to defaults.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        ),
                        border = BorderStroke(
                            1.dp,
                            ErrorRed
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Reset Defaults",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Surface(
                color = Color(0xFFE8EAF6),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    LgesNavy.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(13.dp)
                ) {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Info,
                            contentDescription = null,
                            tint = LgesNavy,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Text(
                            text =
                                "Deployment & Verification Architecture",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = LgesNavy,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "1. Deploy Google Apps Script as a Web App.\n" +
                                    "2. The certificate ID is standardized as LGES-{rollNo}.\n" +
                                    "3. Existing records are updated rather than duplicated.\n" +
                                    "4. QR codes point to the configured verification portal.",
                        fontFamily = FontFamily.Serif,
                        color =
                            LgesNavy.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* COMMON UI                                                                  */
/* -------------------------------------------------------------------------- */

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = LgesNavy,
        fontSize = 16.sp
    )
}

@Composable
private fun InfoBanner(
    text: String
) {
    Surface(
        color = Color(0xFFFFF8E1),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            1.dp,
            LgesGold
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = LgesNavy,
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                color = LgesNavy,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    success: Boolean,
    progress: Boolean = false
) {
    val background =
        if (success) {
            Color(0xFFE8F5E9)
        } else {
            Color(0xFFFFEBEE)
        }

    val foreground =
        if (success) {
            SuccessGreen
        } else {
            ErrorRed
        }

    Surface(
        color = background,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (progress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = foreground,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector =
                        if (success) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Error
                        },
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(7.dp)
            )

            Text(
                text = text,
                color = foreground,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    success: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (success) {
                    SuccessGreen
                } else {
                    LgesNavy
                }
            )
            .padding(
                horizontal = 7.dp,
                vertical = 3.dp
            )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

/* -------------------------------------------------------------------------- */
/* HELPERS                                                                    */
/* -------------------------------------------------------------------------- */

private fun certificateFileName(
    rollNo: String,
    studentName: String
): String {
    val safeRoll =
        rollNo.trim().ifBlank {
            "Certificate"
        }

    val safeName =
        studentName
            .trim()
            .ifBlank {
                "Student"
            }
            .replace(
                Regex("\\s+"),
                "_"
            )

    return "${safeRoll}_$safeName"
}

private fun showNoPreviewToast(
    context: Context
) {
    Toast.makeText(
        context,
        "Certificate preview is not ready yet.",
        Toast.LENGTH_SHORT
    ).show()
}