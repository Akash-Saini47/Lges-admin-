package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.certificates.CertificatesTab
import com.example.ui.components.SafeInstituteLogo
import com.example.ui.dashboard.DashboardTab
import com.example.ui.form.CertificateFormTab
import com.example.ui.settings.SettingsTab
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateAdminScreen(
    viewModel: CertificateViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val certificates by viewModel.allCertificates.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Surface(
                color = LgesNavy,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafeInstituteLogo(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LGES Admin Panel",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lakshmi Group of Education Society",
                            color = LgesGold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LgesNavy,
                    selectedTextColor = LgesNavy,
                    indicatorColor = LgesGold.copy(alpha = 0.25f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Generator") },
                    label = { Text("Create", fontSize = 11.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Registry") },
                    label = { Text("Registry", fontSize = 11.sp) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = navColors
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    certificates = certificates,
                    onNavigateToCreate = {
                        viewModel.clearForm()
                        selectedTab = 1
                    },
                    onNavigateToCertificates = { selectedTab = 2 },
                    onSyncAll = { viewModel.syncAllPending() },
                    onEditCertificate = { cert ->
                        viewModel.loadCertificateForEditing(cert)
                        selectedTab = 1
                    }
                )
                1 -> CertificateFormTab(viewModel = viewModel)
                2 -> CertificatesTab(
                    certificates = certificates,
                    onEditCertificate = { cert ->
                        viewModel.loadCertificateForEditing(cert)
                        selectedTab = 1
                    },
                    onDeleteCertificate = { certId ->
                        viewModel.deleteCertificate(certId)
                    }
                )
                3 -> SettingsTab(viewModel = viewModel)
            }
        }
    }
}