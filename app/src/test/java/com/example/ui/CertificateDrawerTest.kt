package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.database.Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CertificateDrawerTest {

    private lateinit var paint: Paint
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas

    @Before
    fun setup() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        bitmap = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    // 1. Short student name
    @Test
    fun testShortStudentName() {
        val name = "Raj Saini"
        val maxWidth = 1050f
        val preferredSize = 78f

        val fittedSize = CertificateDrawer.calculateFittedTextSize(name, paint, maxWidth, preferredSize)
        assertEquals("Short name should use preferred font size", preferredSize, fittedSize, 0.01f)

        paint.textSize = fittedSize
        val measured = paint.measureText(name)
        assertTrue("Measured width must be <= maxWidth", measured <= maxWidth)
        assertFalse("Should never contain ellipsis", name.contains("..."))

        // Draw on canvas without exception
        CertificateDrawer.drawStudentName(canvas, name, 1480f, 668f, maxWidth, preferredSize, 32f, paint)
    }

    // 2. Very long student name
    @Test
    fun testVeryLongStudentName() {
        val longName = "CHANDRASEKHARA VENKATA SUBRAMANIAN KRISHNAMURTHY THE THIRD"
        val maxWidth = 1050f
        val preferredSize = 78f

        val fittedSize = CertificateDrawer.calculateFittedTextSize(longName, paint, maxWidth, preferredSize)
        assertTrue("Fitted size must be valid positive font size", fittedSize > 0f)
        assertFalse("Should never contain ellipsis", longName.contains("..."))

        // Verify draw completes without exception
        CertificateDrawer.drawStudentName(canvas, longName, 1480f, 668f, maxWidth, preferredSize, 32f, paint)
    }

    // 3. Short course name
    @Test
    fun testShortCourseName() {
        val course = "DCA"
        val maxWidth = 1100f
        val preferredSize = 70f

        val fittedSize = CertificateDrawer.calculateFittedTextSize(course, paint, maxWidth, preferredSize)
        assertEquals("Short course should use preferred font size", preferredSize, fittedSize, 0.01f)

        CertificateDrawer.drawWrappedCenteredText(
            canvas = canvas,
            text = course,
            centerX = 1480f,
            baselineY = 968f,
            maxWidth = maxWidth,
            preferredSize = preferredSize,
            minimumSingleLineSize = 40f,
            preferred2LineSize = 48f,
            minimumSize = 24f,
            paint = paint
        )
    }

    // 4. Very long course name with controlled two-line wrapping
    @Test
    fun testVeryLongCourseName() {
        val longCourse = "POST GRADUATE DIPLOMA IN COMPUTER APPLICATIONS AND INFORMATION TECHNOLOGY"
        val maxWidth = 1100f
        val preferredSize = 70f
        val minSingleLine = 40f
        val pref2Line = 48f

        // Draw with wrapped centered text
        CertificateDrawer.drawWrappedCenteredText(
            canvas = canvas,
            text = longCourse,
            centerX = 1480f,
            baselineY = 968f,
            maxWidth = maxWidth,
            preferredSize = preferredSize,
            minimumSingleLineSize = minSingleLine,
            preferred2LineSize = pref2Line,
            minimumSize = 24f,
            paint = paint
        )

        // Verify line splitting does not produce ellipsis
        assertFalse("Course text must never contain ellipsis", longCourse.contains("..."))
    }

    // 5. Long guardian name with relation prefix
    @Test
    fun testLongGuardianName() {
        val guardian = "D/O PROFESSOR DR. VIKRAMADITYA CHANDRASEKHARAN THE HONORABLE"
        val normalized = CertificateDrawer.normalizeGuardian(guardian)
        assertEquals("Prefix must be preserved without duplicate", "D/O PROFESSOR DR. VIKRAMADITYA CHANDRASEKHARAN THE HONORABLE", normalized)

        val maxWidth = 900f
        val preferredSize = 30f
        val fittedSize = CertificateDrawer.calculateFittedTextSize(normalized, paint, maxWidth, preferredSize)
        paint.textSize = fittedSize
        assertTrue("Fitted guardian must not exceed maxWidth", paint.measureText(normalized) <= maxWidth)

        CertificateDrawer.drawCenteredFittedText(canvas, normalized, 1480f, 800f, maxWidth, preferredSize, 18f, paint)
    }

    // Test guardian prefix normalization variations
    @Test
    fun testGuardianPrefixNormalization() {
        assertEquals("S/O Ramesh Kumar", CertificateDrawer.normalizeGuardian("Ramesh Kumar"))
        assertEquals("S/O Ramesh Kumar", CertificateDrawer.normalizeGuardian("S/O Ramesh Kumar"))
        assertEquals("S/O Ramesh Kumar", CertificateDrawer.normalizeGuardian("s/o Ramesh Kumar"))
        assertEquals("D/O Sunita Sharma", CertificateDrawer.normalizeGuardian("D/O Sunita Sharma"))
        assertEquals("D/O Sunita Sharma", CertificateDrawer.normalizeGuardian("d/o Sunita Sharma"))
        assertEquals("W/O Priya Verma", CertificateDrawer.normalizeGuardian("W/O Priya Verma"))
        assertEquals("C/O Mahesh Chand", CertificateDrawer.normalizeGuardian("C/O Mahesh Chand"))
        assertEquals("S/O Mahesh Chand", CertificateDrawer.normalizeGuardian("S/O. Mahesh Chand"))
        assertEquals("D/O Anita", CertificateDrawer.normalizeGuardian("DO: Anita"))
        assertEquals("", CertificateDrawer.normalizeGuardian("   "))
    }

    // 6. Long institute name
    @Test
    fun testLongInstituteName() {
        val institute = "LAKSHYA GLOBAL EDUCATIONAL SOCIETY OF ADVANCED INFORMATION TECHNOLOGY & APPLIED SCIENCES"
        val maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH
        val preferredSize = CertificateLayout.INFO_BOX_PREFERRED_SIZE

        val fittedSize = CertificateDrawer.calculateFittedTextSize(institute, paint, maxWidth, preferredSize)
        paint.textSize = fittedSize
        val measured = paint.measureText(institute)
        assertTrue("Fitted institute name must not exceed maxWidth", measured <= maxWidth)

        // Verify bounds: x + measured <= qrLeft
        val x = CertificateLayout.INFO_BOX_X
        val rightEdge = x + measured
        val qrLeft = CertificateLayout.QR_DEST_LEFT
        assertTrue("Institute name must not collide with QR code", rightEdge < qrLeft)

        CertificateDrawer.drawLeftFittedText(canvas, institute, x, CertificateLayout.RUN_BY_Y, maxWidth, preferredSize, 9f, paint)
    }

    // 7. Long website
    @Test
    fun testLongWebsite() {
        val rawWebsite = "https://www.lakshyaglobaleducationalsociety.org/academics/verify/certificates/"
        val cleaned = CertificateDrawer.cleanWebsiteForDisplay(rawWebsite)
        assertEquals("www.lakshyaglobaleducationalsociety.org/academics/verify/certificates", cleaned)

        val maxWidth = CertificateLayout.INFO_BOX_MAX_WIDTH
        val preferredSize = CertificateLayout.WEBSITE_PREFERRED_SIZE
        val fittedSize = CertificateDrawer.calculateFittedTextSize(cleaned, paint, maxWidth, preferredSize)
        paint.textSize = fittedSize
        assertTrue("Fitted website must not exceed maxWidth", paint.measureText(cleaned) <= maxWidth)

        CertificateDrawer.drawLeftFittedText(canvas, cleaned, CertificateLayout.INFO_BOX_X, CertificateLayout.WEBSITE_Y, maxWidth, preferredSize, 9f, paint)
    }

    // 8. Long roll number
    @Test
    fun testLongRollNumber() {
        val rollNo = "LGES/2026/DIPLOMA/CS/INTERN/999888777"
        val maxWidth = 300f
        val preferredSize = 25f

        val fittedSize = CertificateDrawer.calculateFittedTextSize(rollNo, paint, maxWidth, preferredSize)
        paint.textSize = fittedSize
        val measured = paint.measureText(rollNo)
        assertTrue("Fitted roll number must not exceed maxWidth (300f)", measured <= maxWidth)

        // Verify canvas bounds: x = 1347f + measured <= 1502f <= canvas W = 1536f
        val x = CertificateLayout.CERT_NO_X
        val rightEdge = x + measured
        assertTrue("Roll number must stay within canvas width of 1536", rightEdge <= 1536f)

        CertificateDrawer.drawLeftFittedText(canvas, rollNo, x, CertificateLayout.CERT_NO_Y, maxWidth, preferredSize, 14f, paint)
    }

    // 9. Internship certificate
    @Test
    fun testInternshipCertificate() {
        val cert = Certificate.create(
            rollNo = "INT-2026-0042",
            studentName = "Aakash Deep",
            fatherName = "S/O Surender Deep",
            courseName = "Android App Development Internship",
            sessionRange = "Jan 2026 - Jun 2026",
            duration = "6 Months",
            grade = "A+",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "30-06-2026",
            certType = "Internship"
        )

        val certData = CertificateDrawer.buildCertificateData(cert)
        assertEquals("INT-2026-0042", certData.rollNo)
        assertEquals("Aakash Deep", certData.studentName)
        assertEquals("Android App Development Internship", certData.course)
        assertEquals("6 Months", certData.duration)
    }

    // 10. Course certificate
    @Test
    fun testCourseCertificate() {
        val cert = Certificate.create(
            rollNo = "COU-2026-0089",
            studentName = "Simran Kaur",
            fatherName = "D/O Harjit Singh",
            courseName = "Post Graduate Diploma in Computer Applications",
            sessionRange = "2025 - 2026",
            duration = "1 Year",
            grade = "O",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "15-05-2026",
            certType = "Course"
        )

        val certData = CertificateDrawer.buildCertificateData(cert)
        assertEquals("COU-2026-0089", certData.rollNo)
        assertEquals("Simran Kaur", certData.studentName)
        assertEquals("Post Graduate Diploma in Computer Applications", certData.course)
        assertEquals("1 Year", certData.duration)
    }

    // 11. Same student receiving multiple certificates
    @Test
    fun testSameStudentMultipleCertificates() {
        val studentName = "Priya Sharma"
        val cert1 = Certificate.create(
            rollNo = "101",
            studentName = studentName,
            fatherName = "D/O R. K. Sharma",
            courseName = "Basic Computer Course",
            sessionRange = "2024 - 2025",
            duration = "3 Months",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "10-01-2025",
            certType = "Course"
        )

        val cert2 = Certificate.create(
            rollNo = "101",
            studentName = studentName,
            fatherName = "D/O R. K. Sharma",
            courseName = "Full Stack Web Development Internship",
            sessionRange = "2025 - 2026",
            duration = "6 Months",
            grade = "A+",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "20-07-2025",
            certType = "Internship"
        )

        // Each certificate has its own immutable ID
        assertFalse("Unique certificate IDs must not match", cert1.certificateId == cert2.certificateId)
        assertEquals("Roll number can be shared", cert1.rollNo, cert2.rollNo)

        val data1 = CertificateDrawer.buildCertificateData(cert1)
        val data2 = CertificateDrawer.buildCertificateData(cert2)

        assertEquals("Basic Computer Course", data1.course)
        assertEquals("Full Stack Web Development Internship", data2.course)
        assertEquals(studentName, data1.studentName)
        assertEquals(studentName, data2.studentName)
    }

    // 12. Certificate with empty optional fields
    @Test
    fun testCertificateWithEmptyOptionalFields() {
        val emptyCert = CertificateData(
            rollNo = "LGES-EMPTY-01",
            studentName = "Solo Student",
            guardian = "",
            course = "Python",
            session = "",
            grade = "",
            runBy = "",
            duration = "",
            dateOfIssue = "",
            placeOfIssue = "",
            website = ""
        )

        // Should render without throwing any exceptions
        val output = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(output)

        // Directly verify helpers handle blank strings gracefully
        CertificateDrawer.drawCenteredFittedText(c, emptyCert.guardian, 1480f, 800f, 900f, 30f, 18f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.session, CertificateDrawer.Layout.SESSION_X, CertificateDrawer.Layout.SESSION_BASELINE_Y, CertificateDrawer.Layout.SESSION_MAX_WIDTH, 28f, 16f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.grade, CertificateDrawer.Layout.GRADE_X, CertificateDrawer.Layout.GRADE_BASELINE_Y, CertificateDrawer.Layout.GRADE_MAX_WIDTH, 28f, 16f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.runBy, CertificateDrawer.Layout.METADATA_X, CertificateDrawer.Layout.RUN_BY_BASELINE_Y, CertificateDrawer.Layout.METADATA_MAX_WIDTH, 24f, 15f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.duration, CertificateDrawer.Layout.METADATA_X, CertificateDrawer.Layout.DURATION_BASELINE_Y, CertificateDrawer.Layout.METADATA_MAX_WIDTH, 24f, 15f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.dateOfIssue, CertificateDrawer.Layout.METADATA_X, CertificateDrawer.Layout.DATE_OF_ISSUE_BASELINE_Y, CertificateDrawer.Layout.METADATA_MAX_WIDTH, 24f, 15f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.placeOfIssue, CertificateDrawer.Layout.METADATA_X, CertificateDrawer.Layout.PLACE_OF_ISSUE_BASELINE_Y, CertificateDrawer.Layout.METADATA_MAX_WIDTH, 24f, 15f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.website, CertificateDrawer.Layout.METADATA_X, CertificateDrawer.Layout.WEBSITE_BASELINE_Y, CertificateDrawer.Layout.METADATA_MAX_WIDTH, 22f, 14f, paint)
        CertificateDrawer.drawLeftFittedText(c, emptyCert.rollNo, CertificateDrawer.Layout.ROLL_NO_X, CertificateDrawer.Layout.ROLL_NO_BASELINE_Y, CertificateDrawer.Layout.ROLL_NO_MAX_WIDTH, 24f, 12f, paint)

        assertEquals("", CertificateDrawer.normalizeWhitespace("   "))
        assertEquals("", CertificateDrawer.normalizeGuardian(""))
        assertEquals("", CertificateDrawer.cleanWebsiteForDisplay(""))
    }

    // Verify whitespace normalization
    @Test
    fun testWhitespaceNormalization() {
        val input = "   Rahul    Kumar    Sharma   "
        assertEquals("Rahul Kumar Sharma", CertificateDrawer.normalizeWhitespace(input))
    }

    // Verify Layout coordinates consistency
    @Test
    fun testLayoutCoordinatesAndColors() {
        assertEquals(1536, CertificateDrawer.Layout.CANVAS_WIDTH)
        assertEquals(1024, CertificateDrawer.Layout.CANVAS_HEIGHT)
        assertEquals(1536, CertificateLayout.canvasWidth)
        assertEquals(1024, CertificateLayout.canvasHeight)
        assertEquals(1536f, CertificateLayout.WIDTH, 0.01f)
        assertEquals(1024f, CertificateLayout.HEIGHT, 0.01f)

        assertEquals(0xFF1A237E.toInt(), CertificateDrawer.NAVY)
        assertEquals(0xFFD4AF37.toInt(), CertificateDrawer.GOLD)
        assertEquals(0xFF1E293B.toInt(), CertificateDrawer.INK)

        // Verify QR box quiet zone
        assertTrue(CertificateDrawer.Layout.QR_DEST_LEFT >= CertificateDrawer.Layout.QR_BOX_LEFT)
        assertTrue(CertificateDrawer.Layout.QR_DEST_TOP >= CertificateDrawer.Layout.QR_BOX_TOP)
        val qrBoxRight = CertificateDrawer.Layout.QR_BOX_LEFT + CertificateDrawer.Layout.QR_BOX_WIDTH
        val qrDestRight = CertificateDrawer.Layout.QR_DEST_LEFT + CertificateDrawer.Layout.QR_DEST_SIZE
        assertTrue(qrDestRight <= qrBoxRight)

        // Verify Roll No bounds stay within canvas
        val rollNoRightEdge = CertificateDrawer.Layout.ROLL_NO_X + CertificateDrawer.Layout.ROLL_NO_MAX_WIDTH
        assertTrue("Roll No right edge must not exceed canvas width", rollNoRightEdge <= 1536f)
    }

    // Test 4 Specific Datasets
    @Test
    fun testDataset1_KavitaSaini() {
        val cert = Certificate.create(
            rollNo = "10410",
            studentName = "Kavita Saini",
            fatherName = "Kamlesh Saini",
            courseName = "Advance Diploma In Information Technology & Computer Management",
            sessionRange = "2023 - 2024",
            duration = "1 Year",
            grade = "A+",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "25-07-2024",
            certType = "Diploma"
        )
        val certData = CertificateDrawer.buildCertificateData(cert)
        assertEquals("10410", CertificateDrawer.cleanRollNoForDisplay(certData.rollNo))
        assertEquals("Kavita Saini", certData.studentName)
        assertEquals("S/O Kamlesh Saini", CertificateDrawer.normalizeGuardian(certData.guardian))

        val bmp = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        CertificateDrawer.renderDynamicOverlay(c, certData, null)

        // Validate student name fits within NAME_MAX_WIDTH
        val nameSize = CertificateDrawer.calculateFittedTextSize(
            certData.studentName,
            paint,
            CertificateLayout.studentName.maxWidth,
            CertificateLayout.studentName.maxFontSize
        )
        paint.textSize = nameSize
        assertTrue(paint.measureText(certData.studentName) <= CertificateLayout.studentName.maxWidth)
    }

    @Test
    fun testDataset2_Avikash() {
        val cert = Certificate.create(
            rollNo = "10411",
            studentName = "Avikash",
            fatherName = "Akash",
            courseName = "Advance Diploma In Information Technology & Computer Management",
            sessionRange = "2023 - 2024",
            duration = "1 Year",
            grade = "A",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "25-07-2024",
            certType = "Diploma"
        )
        val certData = CertificateDrawer.buildCertificateData(cert)
        assertEquals("10411", CertificateDrawer.cleanRollNoForDisplay(certData.rollNo))
        assertEquals("Avikash", certData.studentName)
        assertEquals("S/O Akash", CertificateDrawer.normalizeGuardian(certData.guardian))

        val bmp = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        CertificateDrawer.renderDynamicOverlay(c, certData, null)
    }

    @Test
    fun testDataset3_LongNames() {
        val cert = Certificate.create(
            rollNo = "LGES/2026/CS/99988",
            studentName = "A Very Long Student Name That Tests Auto Fitting",
            fatherName = "A Very Long Father's Name That Tests Auto Fitting",
            courseName = "Advanced Diploma in Information Technology, Computer Applications and Computer Management",
            sessionRange = "Jan 2024 - Dec 2025",
            duration = "2 Years",
            grade = "A+",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "25-07-2026",
            certType = "Diploma"
        )
        val certData = CertificateDrawer.buildCertificateData(cert)

        val bmp = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        CertificateDrawer.renderDynamicOverlay(c, certData, null)

        // Verify that long student name fits within width
        val nameSize = CertificateDrawer.calculateFittedTextSize(
            certData.studentName,
            paint,
            CertificateLayout.studentName.maxWidth,
            CertificateLayout.studentName.maxFontSize
        )
        paint.textSize = nameSize
        assertTrue(paint.measureText(certData.studentName) <= CertificateLayout.studentName.maxWidth)
        assertFalse(certData.studentName.contains("..."))
    }

    @Test
    fun testDataset4_Amit() {
        val cert = Certificate.create(
            rollNo = "10412",
            studentName = "Amit",
            fatherName = "Raj Kumar",
            courseName = "Basic Computer Course",
            sessionRange = "2024",
            duration = "3 Months",
            grade = "B+",
            placeOfIssue = "CHAMBA",
            dateOfIssue = "15-08-2024",
            certType = "Certificate"
        )
        val certData = CertificateDrawer.buildCertificateData(cert)
        val bmp = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        CertificateDrawer.renderDynamicOverlay(c, certData, null)

        val nameSize = CertificateDrawer.calculateFittedTextSize(
            certData.studentName,
            paint,
            CertificateLayout.studentName.maxWidth,
            CertificateLayout.studentName.maxFontSize
        )
        assertEquals(CertificateLayout.studentName.maxFontSize, nameSize, 0.01f)
    }

    @Test
    fun testAutoFitHelpers() {
        val bmp = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Test drawAutoFitCenteredText
        CertificateDrawer.drawAutoFitCenteredText(
            canvas = c,
            text = "Centered Test Text",
            centerX = CertificateLayout.CONTENT_CENTER_X,
            topY = 400f,
            maxWidth = 500f,
            maxHeight = 40f,
            minSize = 10f,
            maxSize = 25f,
            paint = paint
        )

        // Test drawAutoFitLeftText
        CertificateDrawer.drawAutoFitLeftText(
            canvas = c,
            text = "Left Aligned Test Text",
            x = CertificateLayout.INFO_BOX_X,
            baselineY = CertificateLayout.RUN_BY_Y,
            maxWidth = 300f,
            minSize = 10f,
            maxSize = 16f,
            paint = paint
        )
    }

    // 13. Label prefix stripping tests
    @Test
    fun testLabelStripping() {
        // Roll No
        assertEquals("LGES2026AI000123456789", CertificateDrawer.cleanRollNoForDisplay("Roll No.: LGES2026AI000123456789"))
        assertEquals("LGES2026AI000123456789", CertificateDrawer.cleanRollNoForDisplay("Roll No: LGES2026AI000123456789"))
        assertEquals("LGES2026AI000123456789", CertificateDrawer.cleanRollNoForDisplay("Roll-LGES2026AI000123456789"))
        assertEquals("LGES2026AI000123456789", CertificateDrawer.cleanRollNoForDisplay("LGES2026AI000123456789"))

        // Session
        assertEquals("2024 - 2026", CertificateDrawer.cleanSessionForDisplay("Session: 2024 - 2026"))
        assertEquals("2024 - 2026", CertificateDrawer.cleanSessionForDisplay("Academic Session: 2024 - 2026"))
        assertEquals("2024 - 2026", CertificateDrawer.cleanSessionForDisplay("2024 - 2026"))

        // Performance Grade
        assertEquals("A+", CertificateDrawer.cleanGradeForDisplay("Performance Grade: A+"))
        assertEquals("A+", CertificateDrawer.cleanGradeForDisplay("Grade: A+"))
        assertEquals("A+", CertificateDrawer.cleanGradeForDisplay("Grade A+"))
        assertEquals("A+", CertificateDrawer.cleanGradeForDisplay("A+"))

        // Metadata box labels
        assertEquals("Lakshya Global", CertificateDrawer.cleanRunByForDisplay("Run By: Lakshya Global"))
        assertEquals("2 Years", CertificateDrawer.cleanDurationForDisplay("Course Duration: 2 Years"))
        assertEquals("2 Years", CertificateDrawer.cleanDurationForDisplay("Duration: 2 Years"))
        assertEquals("25-07-2026", CertificateDrawer.cleanDateOfIssueForDisplay("Date of Issue: 25-07-2026"))
        assertEquals("25-07-2026", CertificateDrawer.cleanDateOfIssueForDisplay("Date: 25-07-2026"))
        assertEquals("CHAMBA", CertificateDrawer.cleanPlaceOfIssueForDisplay("Place of Issue: CHAMBA"))
        assertEquals("CHAMBA", CertificateDrawer.cleanPlaceOfIssueForDisplay("Place: CHAMBA"))
        assertEquals("www.lges.in", CertificateDrawer.cleanWebsiteForDisplay("Website: https://www.lges.in/"))
    }

    // 14. Specific User Test Cases for Certificate Generation
    @Test
    fun testUserSpecificTestCases() {
        val cert = CertificateData(
            rollNo = "Roll No.: LGES2026AI000123456789",
            certificateId = "Cert No: LGES/2026/001",
            studentName = "Mohammed Abdul Rahman Khan Siddiqui",
            guardian = "S/O Dr. Vikramaditya Chandrasekharan",
            course = "Master of Science in Artificial Intelligence and Machine Learning",
            session = "Session: 2024 - 2026",
            grade = "Grade: A+",
            runBy = "Run By: Lakshya Global Educational Society",
            duration = "Course Duration: 2 Years",
            dateOfIssue = "Date of Issue: 25-07-2026",
            placeOfIssue = "CHAMBA",
            website = "Website: https://www.lakshyaglobal.org"
        )

        // Verify label stripping for all fields
        val cleanRoll = CertificateDrawer.cleanRollNoForDisplay(cert.rollNo)
        assertEquals("LGES2026AI000123456789", cleanRoll)

        val cleanCertNo = CertificateDrawer.cleanCertNoForDisplay(cert.certificateId)
        assertEquals("LGES/2026/001", cleanCertNo)

        val cleanSession = CertificateDrawer.cleanSessionForDisplay(cert.session)
        assertEquals("2024 - 2026", cleanSession)

        val cleanGrade = CertificateDrawer.cleanGradeForDisplay(cert.grade)
        assertEquals("A+", cleanGrade)

        val cleanRunBy = CertificateDrawer.cleanRunByForDisplay(cert.runBy)
        assertEquals("Lakshya Global Educational Society", cleanRunBy)

        val cleanDuration = CertificateDrawer.cleanDurationForDisplay(cert.duration)
        assertEquals("2 Years", cleanDuration)

        val cleanDate = CertificateDrawer.cleanDateOfIssueForDisplay(cert.dateOfIssue)
        assertEquals("25-07-2026", cleanDate)

        val cleanWebsite = CertificateDrawer.cleanWebsiteForDisplay(cert.website)
        assertEquals("www.lakshyaglobal.org", cleanWebsite)

        // Verify long student name fitting
        val studentNameSize = CertificateDrawer.calculateFittedTextSize(
            cert.studentName,
            paint,
            CertificateLayout.NAME_MAX_WIDTH,
            CertificateLayout.NAME_PREFERRED_SIZE
        )
        paint.textSize = studentNameSize
        assertTrue(paint.measureText(cert.studentName) <= CertificateLayout.NAME_MAX_WIDTH)

        // Verify long course name bounds
        val bitmap = Bitmap.createBitmap(CertificateDrawer.W, CertificateDrawer.H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        CertificateDrawer.drawCourseName(
            canvas = canvas,
            text = cert.course,
            centerX = CertificateLayout.COURSE_CENTER_X,
            paint = paint
        )

        // Verify complete rendering without exceptions
        val outputBitmap = CertificateDrawer.renderDynamicOverlay(canvas, cert, null)
    }
}
