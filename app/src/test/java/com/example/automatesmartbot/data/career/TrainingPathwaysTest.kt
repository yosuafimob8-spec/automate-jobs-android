package com.example.automatesmartbot.data.career

import org.junit.Assert.*
import org.junit.Test

class TrainingPathwaysTest {
    @Test fun allPositionsHaveThreeDistinctLevels() {
        assertEquals(15, TrainingPositions.groups.size)
        assertEquals(78, TrainingPositions.count)
        val ids = mutableSetOf<String>()
        TrainingPositions.groups.forEach { group ->
            group.positions.forEach { position ->
                TrainingLevel.entries.forEach { level ->
                    val course = TrainingPathways.courseFor(position, level)
                    assertTrue("Duplicate course id: ${course.id}", ids.add(course.id))
                    assertTrue(course.title.contains(position))
                    assertTrue(course.title.contains(level.label))
                    assertEquals(5, course.lessons.size)
                    assertEquals(5, course.questions.size)
                    assertTrue(course.lessons.all { it.second.contains(position) })
                    assertEquals(100, TrainingCourses.score(course.questions.map { it.correct }, course.questions))
                    assertEquals(0, TrainingCourses.score(course.questions.map { (it.correct + 1) % it.options.size }, course.questions))
                }
            }
        }
        assertEquals(234, ids.size)
    }

    @Test fun practiceRequiresSubstantiveResponse() {
        assertFalse(TrainingPractice.assess("Saya bekerja").accepted)
        assertFalse(TrainingPractice.assess("a ".repeat(70)).accepted)
        val response = "Saya akan periksa dokumen dan kondisi pekerjaan terlebih dahulu, kemudian mencatat hasil pemeriksaan secara rinci. Setelah itu saya melakukan verifikasi bersama penanggung jawab dan melaporkan setiap perbedaan agar dapat ditindaklanjuti sesuai prosedur perusahaan."
        assertTrue(TrainingPractice.assess(response).accepted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownPositionRejected() {
        TrainingPathways.courseFor("Posisi yang tidak terdaftar", TrainingLevel.BASIC)
    }
}
