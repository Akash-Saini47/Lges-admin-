package com.example.ui.form

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CertificateViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.preview.CertificatePreviewCard
import com.example.ui.theme.LgesNavy
import com.example.util.CertificateValidator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateFormTab(
    viewModel: CertificateViewModel,
    modifier: Modifier = Modifier
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

    val errors by viewModel.validationErrors.collectAsStateWithLifecycle()
    val duplicateNote by viewModel.duplicateNote.collectAsStateWithLifecycle()
    val editingId by viewModel.currentEditingId.collectAsStateWithLifecycle()

    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()

    // Dropdown states
    var gradeDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var relationDropdownExpanded by remember { mutableStateOf(false) }

    val currentCertificate = viewModel.getAsCertificate()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header / Edit banner
            if (editingId != null) {
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Editing Certificate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1976D2))
                            Text("ID: $editingId", fontSize = 12.sp, color = Color(0xFF0D47A1))
                        }
                        OutlinedButton(onClick = { viewModel.clearForm() }) {
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "New Certificate Generator",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LgesNavy
                )
            }
        }

        // Form Fields
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text("Student Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LgesNavy)
                Spacer(modifier = Modifier.height(8.dp))

                // Roll No
                OutlinedTextField(
                    value = rollNo,
                    onValueChange = {
                        viewModel.rollNo.value = it
                        viewModel.checkForDuplicateRollNo(it)
                    },
                    label = { Text("Roll No. / Regd No. *") },
                    isError = errors.rollNoError != null,
                    supportingText = {
                        errors.rollNoError?.let { Text(it, color = Color.Red) }
                            ?: duplicateNote?.let { Text(it, color = Color(0xFFE65100)) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Student Name
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { viewModel.studentName.value = it },
                    label = { Text("Student Name *") },
                    isError = errors.studentNameError != null,
                    supportingText = errors.studentNameError?.let { { Text(it, color = Color.Red) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Father / Guardian with prefix dropdown
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = relationDropdownExpanded,
                        onExpandedChange = { relationDropdownExpanded = !relationDropdownExpanded },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = relationPrefix,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rel") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = relationDropdownExpanded,
                            onDismissRequest = { relationDropdownExpanded = false }
                        ) {
                            listOf("S/O", "D/O", "W/O", "C/O").forEach { prefix ->
                                DropdownMenuItem(
                                    text = { Text(prefix) },
                                    onClick = {
                                        viewModel.relationPrefix.value = prefix
                                        relationDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { viewModel.fatherName.value = it },
                        label = { Text("Father / Guardian Name *") },
                        isError = errors.fatherNameError != null,
                        supportingText = errors.fatherNameError?.let { { Text(it, color = Color.Red) } },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Course & Assessment Details
        item {
            DocumentCard(modifier = Modifier.fillMaxWidth()) {
                Text("Course & Assessment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LgesNavy)
                Spacer(modifier = Modifier.height(8.dp))

                // Course Name
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { viewModel.courseName.value = it },
                    label = { Text("Course / Internship Title *") },
                    isError = errors.courseNameError != null,
                    supportingText = errors.courseNameError?.let { { Text(it, color = Color.Red) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Certificate Type & Grade
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = certType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            CertificateValidator.SUPPORTED_CERT_TYPES.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        viewModel.certType.value = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = gradeDropdownExpanded,
                        onExpandedChange = { gradeDropdownExpanded = !gradeDropdownExpanded },
                        modifier = Modifier.width(110.dp)
                    ) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeDropdownExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = gradeDropdownExpanded,
                            onDismissRequest = { gradeDropdownExpanded = false }
                        ) {
                            CertificateValidator.SUPPORTED_GRADES.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = {
                                        viewModel.grade.value = g
                                        gradeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Session Range & Duration
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sessionRange,
                        onValueChange = { viewModel.sessionRange.value = it },
                        label = { Text("Session (e.g. 2024-2025)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { viewModel.duration.value = it },
                        label = { Text("Duration (e.g. 1 Year)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Place of Issue & Date of Issue
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = placeOfIssue,
                        onValueChange = { viewModel.placeOfIssue.value = it },
                        label = { Text("Place of Issue") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = dateOfIssue,
                        onValueChange = { viewModel.dateOfIssue.value = it },
                        label = { Text("Date of Issue *") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val dayStr = day.toString().padStart(2, '0')
                                        val monthStr = (month + 1).toString().padStart(2, '0')
                                        viewModel.dateOfIssue.value = "$dayStr-$monthStr-$year"
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveCertificateLocally(
                            onSuccess = { isUpdate ->
                                val msg = if (isUpdate) "Certificate updated!" else "Certificate created and saved!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LgesNavy),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (editingId != null) "Update Certificate" else "Save & Sync", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearForm() },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        // Upload Status feedback
        if (isUploading || uploadStatus != null || uploadError != null) {
            item {
                Surface(
                    color = if (uploadError != null) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchronizing with Google Sheets...", fontSize = 13.sp)
                        } else if (uploadStatus != null) {
                            Text(uploadStatus!!, color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        } else if (uploadError != null) {
                            Text(uploadError!!, color = Color(0xFFC62828), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Embedded Preview
        item {
            CertificatePreviewCard(certificate = currentCertificate)
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
