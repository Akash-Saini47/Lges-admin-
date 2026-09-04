/**
 * LGES Certificate Cloud Synchronization Engine
 * Google Apps Script - Web App Endpoint (CRUD + Deduplication + API Key Protection)
 * 
 * Instructions to Deploy:
 * 1. Open your Google Sheet for LGES Certificates.
 * 2. Go to "Extensions" -> "Apps Script".
 * 3. Replace all existing code with this updated script.
 * 4. (Optional Security Key) Set API_KEY below to a secret passphrase.
 *    If set, you must also save this key in the Settings tab of the Android app.
 * 5. Save (disk icon).
 * 6. Click "Deploy" -> "Manage deployments" -> Edit (pencil) -> New version -> "Deploy".
 *    (Or "Deploy" -> "New deployment" -> Type: Web app, Execute as: "Me", Access: "Anyone").
 * 7. Ensure Web App URL ends with "/exec" and copy it into the LGES Admin Android App.
 */

var API_KEY = ""; // Set secret passphrase or leave blank ""
var VERIFICATION_BASE_URL = "https://lges-computer-classes.netlify.app/verify.html";

var HEADERS = [
  "Upload Timestamp",       // Col 1 (A)
  "Certificate ID",         // Col 2 (B)
  "Roll No",                // Col 3 (C)
  "Student Name",           // Col 4 (D)
  "Father / Guardian Name", // Col 5 (E)
  "Course / Internship",    // Col 6 (F)
  "Certificate Type",       // Col 7 (G)
  "Session / Date Range",   // Col 8 (H)
  "Duration",               // Col 9 (I)
  "Grade",                  // Col 10 (J)
  "Place of Issue",         // Col 11 (K)
  "Date of Issue",          // Col 12 (L)
  "Verification Link"       // Col 13 (M)
];

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({
        status: "error",
        message: "Invalid request: Empty payload received."
      });
    }

    var data;
    try {
      data = JSON.parse(e.postData.contents);
    } catch (parseErr) {
      return jsonResponse({
        status: "error",
        message: "Malformed JSON payload."
      });
    }

    // 1. Authenticate API Key if configured
    if (API_KEY && API_KEY.trim() !== "") {
      var clientKey = data.apiKey || "";
      if (clientKey !== API_KEY) {
        return jsonResponse({
          status: "error",
          message: "Unauthorized: Invalid or missing API Security Key."
        });
      }
    }

    var action = (data.action || "save").toLowerCase().trim();
    var sheet = getOrCreateSheet();

    // 2. Handle DELETE action
    if (action === "delete") {
      var targetId = (data.certificateId || data.rollNo || "").trim();
      if (!targetId) {
        return jsonResponse({
          status: "error",
          message: "Delete failed: Certificate ID is required."
        });
      }

      var rowIndex = findCertificateRowById(sheet, targetId);
      if (rowIndex > 0) {
        sheet.deleteRow(rowIndex);
        return jsonResponse({
          status: "success",
          action: "deleted",
          certificateId: targetId,
          message: "Certificate '" + targetId + "' deleted from cloud registry."
        });
      } else {
        return jsonResponse({
          status: "success",
          action: "deleted",
          certificateId: targetId,
          message: "Certificate '" + targetId + "' not found in cloud registry (already deleted)."
        });
      }
    }

    // 3. Handle PING / TEST action
    if (action === "ping" || action === "test") {
      return jsonResponse({
        status: "success",
        action: "ping",
        message: "Connection verified! LGES Cloud Sync Web App is active."
      });
    }

    // 4. Handle CREATE / UPDATE (Upsert using certificateId as idempotency key)
    var rollNo = (data.rollNo || data.regdNo || "").trim();
    var certificateId = (data.certificateId || "").trim();
    if (!certificateId && rollNo) {
      certificateId = rollNo.toUpperCase().indexOf("LGES-") === 0 ? rollNo.toUpperCase() : ("LGES-" + rollNo);
    }

    var studentName = (data.studentName || data.name || "").trim();
    var fatherName = (data.fatherName || "").trim();
    var courseName = (data.courseName || data.course || "").trim();
    var certType = (data.certType || "Course").trim();
    var sessionRange = (data.sessionRange || "").trim();
    var duration = (data.duration || "").trim();
    var grade = (data.grade || "A").trim();
    var placeOfIssue = (data.placeOfIssue || "").trim();
    var dateOfIssue = (data.dateOfIssue || data.issueDate || "").trim();

    // Server-side field validation
    if (!certificateId || !rollNo) {
      return jsonResponse({
        status: "error",
        message: "Validation Error: Certificate ID and Roll Number are required."
      });
    }
    if (!studentName) {
      return jsonResponse({
        status: "error",
        message: "Validation Error: Student Name is required."
      });
    }
    if (!courseName) {
      return jsonResponse({
        status: "error",
        message: "Validation Error: Course or Internship title is required."
      });
    }

    var verificationUrl = (data.verificationUrl || "").trim();
    if (!verificationUrl) {
      verificationUrl = VERIFICATION_BASE_URL + "?certNo=" + encodeURIComponent(certificateId);
    }
    var timestampStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");

    var rowValues = [
      timestampStr,
      certificateId,
      rollNo,
      studentName,
      fatherName,
      courseName,
      certType,
      sessionRange,
      duration,
      grade,
      placeOfIssue,
      dateOfIssue,
      verificationUrl
    ];

    // Search strictly by Certificate ID to preserve multiple certificates per roll number
    var existingRow = findCertificateRowById(sheet, certificateId);

    if (existingRow > 0) {
      // UPDATE existing row in place (idempotent retry)
      var range = sheet.getRange(existingRow, 1, 1, rowValues.length);
      range.setValues([rowValues]);
      return jsonResponse({
        status: "success",
        action: "updated",
        certificateId: certificateId,
        message: "Successfully updated cloud record for " + studentName + " (" + certificateId + ").",
        verificationUrl: verificationUrl
      });
    } else {
      // CREATE new row
      sheet.appendRow(rowValues);
      return jsonResponse({
        status: "success",
        action: "created",
        certificateId: certificateId,
        message: "Successfully synchronized new certificate for " + studentName + " (" + certificateId + ") to cloud!",
        verificationUrl: verificationUrl
      });
    }

  } catch (err) {
    return jsonResponse({
      status: "error",
      message: "Server Execution Error: " + (err.message || err.toString())
    });
  }
}

function doGet(e) {
  var certId = "";
  if (e && e.parameter) {
    certId = e.parameter.certNo || e.parameter.certificateId || e.parameter.rollNo || "";
  }

  if (!certId) {
    return jsonResponse({
      status: "success",
      service: "LGES Certificate Sync API",
      version: "2.1",
      active: true
    });
  }

  var sheet = getOrCreateSheet();
  var row = findCertificateRowById(sheet, certId);
  if (row > 0) {
    var data = sheet.getRange(row, 1, 1, HEADERS.length).getValues()[0];
    return jsonResponse({
      status: "success",
      action: "found",
      certificate: {
        timestamp: data[0],
        certificateId: data[1],
        rollNo: data[2],
        studentName: data[3],
        fatherName: data[4],
        courseName: data[5],
        certType: data[6],
        sessionRange: data[7],
        duration: data[8],
        grade: data[9],
        placeOfIssue: data[10],
        dateOfIssue: data[11],
        verificationUrl: data[12]
      }
    });
  } else {
    return jsonResponse({
      status: "error",
      message: "Certificate '" + certId + "' not found."
    });
  }
}

/**
 * Searches for a certificate row strictly by Certificate ID (Col B).
 * Ensures multiple certificates for the same student roll number do not overwrite each other.
 */
function findCertificateRowById(sheet, certificateId) {
  var lastRow = sheet.getLastRow();
  if (lastRow <= 1) return -1;

  var target = (certificateId || "").trim().toUpperCase();
  if (!target) return -1;

  // Read columns B (Cert ID)
  var certIdRange = sheet.getRange(2, 2, lastRow - 1, 1).getValues();

  for (var i = 0; i < certIdRange.length; i++) {
    var rowCertId = (certIdRange[i][0] || "").toString().trim().toUpperCase();
    if (rowCertId === target) {
      return i + 2; // 1-indexed row number
    }
  }

  // Fallback for legacy records where Col B might contain roll number
  var rollRange = sheet.getRange(2, 3, lastRow - 1, 1).getValues();
  for (var j = 0; j < rollRange.length; j++) {
    var rowRoll = (rollRange[j][0] || "").toString().trim().toUpperCase();
    if (rowRoll === target || ("LGES-" + rowRoll) === target) {
      return j + 2;
    }
  }

  return -1;
}

function getOrCreateSheet() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getActiveSheet();
  if (!sheet) {
    sheet = ss.insertSheet("Certificates");
  }

  // Ensure header row exists
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(HEADERS);
    sheet.getRange(1, 1, 1, HEADERS.length).setFontWeight("bold").setBackground("#0B1B3D").setFontColor("#FFFFFF");
  }
  return sheet;
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
