/**
 * Google Apps Script - doPost Web App Endpoint (with API Key Protection)
 * 
 * Instructions to Deploy:
 * 1. Open Google Sheets and create a new blank spreadsheet.
 * 2. In the top menu, go to "Extensions" -> "Apps Script".
 * 3. Delete any default code in the editor and paste this code.
 * 4. (Optional Security) Set API_KEY below to a custom secret string (e.g. "MySecretKey123").
 *    If set, you must also enter this key in the Settings tab of the Android app.
 * 5. Save the script (disk icon).
 * 6. Click "Deploy" -> "New deployment" in the top-right.
 * 7. Under "Select type" (gear icon), choose "Web app".
 * 8. Set "Execute as" to: "Me (your-email@gmail.com)".
 * 9. Set "Who has access" to: "Anyone".
 * 10. Click "Deploy". Authorize any permissions requested.
 * 11. Copy the generated "Web app URL" (ends in "/exec") and paste it in the Setup tab of the Android app!
 */

// OPTIONAL SECURITY KEY (Shared Secret)
// Set a secret key below to restrict access to authorized apps only.
// If set, incoming POST requests must match this key in their JSON body ("apiKey": "YOUR_SECRET_KEY").
// Leave as empty string "" if you do not wish to require an API Key.
var API_KEY = "";

function doPost(e) {
  // Set CORS and return headers
  var headers = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  };

  try {
    // Check if post data is empty
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({
        "status": "error",
        "message": "Empty POST body or payload received."
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }

    // Parse the incoming JSON parameters sent from the Android Admin app
    var data = JSON.parse(e.postData.contents);

    // Security Check: Verify API Key if configured
    if (API_KEY && API_KEY.trim() !== "") {
      if (!data.apiKey || data.apiKey !== API_KEY) {
        return ContentService.createTextOutput(JSON.stringify({
          "status": "error",
          "message": "Unauthorized: Invalid or missing API Security Key."
        }))
        .setMimeType(ContentService.MimeType.JSON);
      }
    }
    
    // Obtain the active spreadsheet and the primary sheet tab
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    var timestamp = new Date();
    var rollNo = data.rollNo || "";
    var studentName = data.studentName || "";
    var fatherName = data.fatherName || "";
    var courseName = data.courseName || "";
    var certType = data.certType || ""; // "Course" or "Internship"
    var sessionRange = data.sessionRange || "";
    var duration = data.duration || "";
    var grade = data.grade || "";
    var placeOfIssue = data.placeOfIssue || "";
    var dateOfIssue = data.dateOfIssue || "";

    // Data Validation Guard
    if (!rollNo || !studentName) {
      return ContentService.createTextOutput(JSON.stringify({
        "status": "error",
        "message": "Validation Error: Roll Number and Student Name are required fields."
      }))
      .setMimeType(ContentService.MimeType.JSON);
    }
    
    // Construct the verification Netlify link
    var verificationUrl = "https://lges-computer-classes.netlify.app/verify.html?certNo=" + encodeURIComponent(rollNo);
    
    // Append the values as a new row in the spreadsheet
    sheet.appendRow([
      timestamp,          // Column A: Date & Time of upload
      rollNo,             // Column B: Roll No. / Certificate No.
      studentName,        // Column C: Student Name
      fatherName,         // Column D: Father's Name (Empty if Internship)
      courseName,         // Column E: Course / Internship Title
      certType,           // Column F: Certificate Type
      sessionRange,       // Column G: Session / Date Range
      duration,           // Column H: Duration
      grade,              // Column I: Grade
      placeOfIssue,       // Column J: Institute Address / Place of Issue
      dateOfIssue,        // Column K: Date of Issue
      verificationUrl     // Column L: Netlify Verification Link
    ]);
    
    // Return successful JSON response
    return ContentService.createTextOutput(JSON.stringify({
      "status": "success",
      "message": "Successfully synchronized certificate for student '" + studentName + "' to the cloud!",
      "verificationUrl": verificationUrl
    }))
    .setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    // Return error JSON response
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": "Script Error: " + error.toString()
    }))
    .setMimeType(ContentService.MimeType.JSON);
  }
}

// Handle preflight OPTIONS requests gracefully
function doOptions(e) {
  return ContentService.createTextOutput("")
    .setMimeType(ContentService.MimeType.TEXT);
}
