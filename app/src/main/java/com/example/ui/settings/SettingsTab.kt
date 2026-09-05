package com.example.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CertificateViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.theme.LgesNavy

@Composable
fun SettingsTab(
    viewModel: CertificateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webAppUrl by viewModel.webAppUrl.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val verificationBaseUrl by viewModel.verificationBaseUrl.collectAsStateWithLifecycle()
    val isTestingConnection by viewModel.isTestingConnection.collectAsStateWithLifecycle()

    var showApiKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cloud & System Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LgesNavy
            )
            Text(
                text = "Configure Google Sheets Apps Script endpoint and security credentials.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Endpoint Configuration Card
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Google Sheets Web App Endpoint",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LgesNavy
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = webAppUrl,
                    onValueChange = { viewModel.updateWebAppUrl(it) },
                    label = { Text("Web App URL (/exec)") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // API Key with secure transformation
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Security Key (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Hide Key" else "Show Key"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    supportingText = { Text("Secured with Android Keystore AES-GCM encryption.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Public Verification URL Card
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Public Certificate Verification Base URL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LgesNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Base webpage URL encoded into student QR codes for verification.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = verificationBaseUrl,
                    onValueChange = { viewModel.updateVerificationBaseUrl(it) },
                    label = { Text("Verification URL") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Connection Test & Defaults
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        testResult = null
                        viewModel.testConnection { success, message ->
                            testResult = Pair(success, message)
                        }
                    },
                    enabled = !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgesNavy,
                        contentColor = Color.White,
                        disabledContainerColor = LgesNavy.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.weight(1.2f)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 13.sp, color = Color.White)
                    } else {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection", fontSize = 13.sp, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetSettingsToDefault()
                        testResult = null
                        Toast.makeText(context, "Settings reset to defaults.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Defaults", fontSize = 12.sp)
                }
            }
        }

        // Test Result Banner
        testResult?.let { (success, message) ->
            item {
                Surface(
                    color = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = if (success) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
