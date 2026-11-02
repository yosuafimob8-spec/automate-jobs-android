package com.example.automatesmartbot.data.career

import org.junit.Assert.*
import org.junit.Test

class TrainingPathwaysTest {
    @Test fun allPositionsHaveThreeDistinctLevels() {
        assertEquals(15, TrainingPositions.groups.size)
        assertEquals(TrainingPositions.groups.map { it.category }.toSet(), TrainingDomainContent.categories())
        assertEquals(78, TrainingPositions.count)
        assertEquals(TrainingPositions.groups.flatMap { it.positions }.toSet(), TrainingRoleSpecialization.positions())
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
                    val specialization = TrainingRoleSpecialization.forPosition(position)
                    assertEquals(5, specialization.tasks.size)
                    assertEquals(5, specialization.deliverables.size)
                    specialization.tasks.forEachIndexed { index, task ->
                        assertTrue(course.lessons[index].second.contains(task))
                    }
                    val domain = TrainingDomainContent.forCategory(group.category)
                    assertTrue(course.lessons[0].second.contains(domain.tool))
                    assertTrue(course.lessons[1].second.contains(domain.workflow))
                    assertTrue(course.lessons[2].second.contains(domain.record))
                    assertTrue(course.lessons[3].second.contains(domain.incident))
                    assertTrue(course.lessons[4].second.contains(domain.metric))
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
