package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.database.CertificateDatabase
import com.example.ui.CertificateAdminScreen
import com.example.ui.CertificateViewModel
import com.example.ui.theme.LgesGold
import com.example.ui.theme.LgesNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppLogger

class MainActivity : ComponentActivity() {

    private var startupError: String? = null
    private var viewModelInstance: CertificateViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val factory = CertificateViewModel.Factory(application)
            viewModelInstance = ViewModelProvider(this, factory)[CertificateViewModel::class.java]
        } catch (t: Throwable) {
            AppLogger.e("MainActivity", "Fatal initialization error during startup", t)
            startupError = t.stackTraceToString()
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm = viewModelInstance
                    if (vm != null && startupError == null) {
                        CertificateAdminScreen(viewModel = vm)
                    } else {
                        StartupErrorRecoveryScreen(
                            errorDetails = startupError ?: "Unknown startup exception",
                            onResetAndRestart = {
                                resetAppDataAndRestart()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun resetAppDataAndRestart() {
        try {
            // Clear local database
            deleteDatabase("certificate_database")
            // Clear preferences
            getSharedPreferences("lges_admin_secure_prefs", Context.MODE_PRIVATE).edit().clear().commit()
            getSharedPreferences("lges_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        } catch (t: Throwable) {
            AppLogger.w("MainActivity", "Error resetting app data: ${t.message}")
        }

        // Restart activity
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun StartupErrorRecoveryScreen(
    errorDetails: String,
    onResetAndRestart: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Startup Warning",
            tint = Color(0xFFC62828),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LGES Admin",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LgesNavy
        )

        Text(
            text = "Startup Recovery",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFC62828)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The app encountered an issue during startup. You can view the details below or reset local app cache to recover.",
            fontSize = 13.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = errorDetails,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFB71C1C)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onResetAndRestart,
            colors = ButtonDefaults.buttonColors(containerColor = LgesNavy),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Reset App Cache & Restart", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("LGES Crash Log", errorDetails))
                Toast.makeText(context, "Error copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Copy Error Details", color = LgesNavy)
        }
    }
}
