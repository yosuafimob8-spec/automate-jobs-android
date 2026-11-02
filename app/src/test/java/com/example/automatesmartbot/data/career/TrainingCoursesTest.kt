package com.example.automatesmartbot.data.career

import org.junit.Assert.*
import org.junit.Test

class TrainingCoursesTest {
    @Test fun coursesHaveValidLessonsAndQuestions() {
        assertTrue(TrainingCourses.all.isNotEmpty())
        assertEquals(TrainingCourses.all.size, TrainingCourses.all.map { it.id }.distinct().size)
        TrainingCourses.all.forEach { course ->
            assertTrue(course.title.isNotBlank())
            assertTrue(course.lessons.isNotEmpty())
            assertTrue(course.questions.isNotEmpty())
            course.questions.forEach { question ->
                assertTrue(question.options.size >= 2)
                assertTrue(question.correct in question.options.indices)
            }
        }
    }

    @Test fun perfectScorePassesAndWrongAnswersFail() {
        TrainingCourses.all.forEach { course ->
            val correct = course.questions.map { it.correct }
            assertEquals(100, TrainingCourses.score(correct, course.questions))
            val wrong = course.questions.map { (it.correct + 1) % it.options.size }
            assertEquals(0, TrainingCourses.score(wrong, course.questions))
        }
    }
}
