package com.example.automatesmartbot.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CvValidatorTest {
    private val pdfHeader = "%PDF-".toByteArray()

    @Test
    fun acceptsValidPdf() {
        val result = CvValidator.validate(
            displayName = "cv-joko.pdf",
            mimeType = "application/pdf",
            sizeBytes = 512_000L,
            headerBytes = pdfHeader
        )
        assertTrue(result.isValid)
    }

    @Test
    fun acceptsGenericMimeWhenExtensionAndHeaderArePdf() {
        assertTrue(
            CvValidator.validate("cv.pdf", "application/octet-stream", 1024L, pdfHeader).isValid
        )
        assertTrue(
            CvValidator.validate("cv.pdf", null, 1024L, pdfHeader).isValid
        )
    }

    @Test
    fun rejectsNonPdfExtension() {
        val result = CvValidator.validate("cv.docx", "application/pdf", 1024L, pdfHeader)
        assertFalse(result.isValid)
        assertEquals("CV harus berupa file PDF.", result.message)
    }

    @Test
    fun rejectsUnsupportedMimeType() {
        val result = CvValidator.validate("cv.pdf", "image/png", 1024L, pdfHeader)
        assertFalse(result.isValid)
        assertEquals("CV harus berupa file PDF.", result.message)
    }

    @Test
    fun rejectsFilesOverTwoMegabytes() {
        val result = CvValidator.validate(
            displayName = "cv.pdf",
            mimeType = "application/pdf",
            sizeBytes = CvValidator.MAX_SIZE_BYTES + 1L,
            headerBytes = pdfHeader
        )
        assertFalse(result.isValid)
        assertEquals("Ukuran CV maksimal 2 MB.", result.message)
    }

    @Test
    fun acceptsExactlyTwoMegabytes() {
        assertTrue(
            CvValidator.validate("cv.pdf", "application/pdf", CvValidator.MAX_SIZE_BYTES, pdfHeader).isValid
        )
    }

    @Test
    fun rejectsMissingSizeAndHeader() {
        assertFalse(
            CvValidator.validate("cv.pdf", "application/pdf", null, pdfHeader).isValid
        )
        assertFalse(
            CvValidator.validate("cv.pdf", "application/pdf", 1024L, "not-pdf".toByteArray()).isValid
        )
    }

    @Test
    fun rejectsMissingName() {
        assertFalse(
            CvValidator.validate(null, "application/pdf", 1024L, pdfHeader).isValid
        )
    }

    @Test
    fun formatsFileSizes() {
        assertEquals("900 B", CvValidator.formatSize(900L))
        assertEquals("1.5 KB", CvValidator.formatSize(1536L))
        assertEquals("2.0 MB", CvValidator.formatSize(2L * 1024L * 1024L))
    }
}
