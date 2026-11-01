package com.example.automatesmartbot.data.cv

import org.junit.Assert.*
import org.junit.Test

class SharedCvParserTest {
    private val text = "Dina Pratama\r\nProfil: Administrasi dokumen dan laporan mingguan dengan Excel.\r\nPengalaman: Memeriksa data pengiriman.\r\nSkills: Excel, Pengarsipan"

    @Test fun analyzerUsesTheSameLiteralSectionsAsTheSharedParser() {
        val parsed = SharedCvParser.parseText(text)
        assertEquals(parsed.content, PdfCvAnalyzer.analyzeText(text, "fixture").cv)
        assertEquals("Excel, Pengarsipan", parsed.content.skills.single().items.single())
        assertTrue(parsed.content.education.isEmpty())
        assertFalse(parsed.text.contains('\r'))
    }

    @Test fun interviewContextRetainsProjectsAndOmitsPrivateContactFields() {
        val cv = CvContent(contact = CvContact(fullName = "Private Person", email = "private@example.com", phone = "+12125551234"),
            summary = "Administrasi dokumen dan laporan mingguan dengan Excel.",
            projects = listOf(CvProject(name = "Proyek Arsip", bullets = listOf("Menyusun pengarsipan laporan nyata."))),
            certifications = listOf(CvCertification(name = "Pelatihan Excel")))
        val context = SharedCvParser.interviewContext(cv)
        listOf("Private Person", "private@example.com", "+12125551234").forEach { assertFalse(context.contains(it)) }
        assertTrue(context.contains("Proyek Arsip")); assertTrue(context.contains("Pelatihan Excel"))
        val parsed = SharedCvParser.parseText(SharedCvParser.renderContent(cv))
        assertTrue(parsed.content.projects.isNotEmpty()); assertTrue(parsed.content.certifications.isNotEmpty())
    }

    @Test fun emptyOrOversizedTextCannotProduceAnAnalysis() {
        listOf("", "short", "x".repeat(250_001)).forEach { input ->
            assertThrows(IllegalArgumentException::class.java) { SharedCvParser.parseText(input) }
            assertThrows(IllegalArgumentException::class.java) { PdfCvAnalyzer.analyzeText(input, "fixture") }
        }
    }
}
