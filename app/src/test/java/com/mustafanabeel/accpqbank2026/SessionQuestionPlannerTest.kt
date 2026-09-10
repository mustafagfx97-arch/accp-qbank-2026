package com.mustafanabeel.accpqbank2026

import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.data.repository.SessionQuestionOrder
import com.mustafanabeel.accpqbank2026.data.repository.SessionQuestionPlanner
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionQuestionPlannerTest {
    @Test
    fun bookOrderMatchesChapterAssessmentThenCaseSequence() {
        val questions = listOf(
            question("b-case-2", "b", "case", 2),
            question("a-case-2", "a", "case", 2, SHARED_CASE),
            question("a-assessment-2", "a", "assessment", 2),
            question("b-assessment-1", "b", "assessment", 1),
            question("a-case-1", "a", "case", 1, SHARED_CASE),
            question("a-assessment-1", "a", "assessment", 1)
        )

        val planned = SessionQuestionPlanner.plan(
            questions = questions,
            chapterOrder = mapOf("a" to 0, "b" to 1),
            requestedCount = 0,
            order = SessionQuestionOrder.BOOK
        )

        assertEquals(
            listOf(
                "a-assessment-1",
                "a-assessment-2",
                "a-case-1",
                "a-case-2",
                "b-assessment-1",
                "b-case-2"
            ),
            planned.map { it.id }
        )
    }

    @Test
    fun randomOrderNeverSeparatesQuestionsFromOnePatientCase() {
        val questions = listOf(
            question("assessment-1", "a", "assessment", 1),
            question("case-2", "a", "case", 2, SHARED_CASE),
            question("standalone-case", "a", "case", 3),
            question("case-1", "a", "case", 1, SHARED_CASE)
        )

        val planned = SessionQuestionPlanner.plan(
            questions = questions,
            chapterOrder = mapOf("a" to 0),
            requestedCount = 0,
            order = SessionQuestionOrder.RANDOM,
            random = Random(7)
        )

        val firstIndex = planned.indexOfFirst { it.id == "case-1" }
        val secondIndex = planned.indexOfFirst { it.id == "case-2" }
        assertEquals(firstIndex + 1, secondIndex)
    }

    @Test
    fun questionLimitDoesNotCutAClinicalCaseInHalf() {
        val questions = listOf(
            question("case-1", "a", "case", 1, SHARED_CASE),
            question("case-2", "a", "case", 2, SHARED_CASE),
            question("case-3", "a", "case", 3)
        )

        val planned = SessionQuestionPlanner.plan(
            questions = questions,
            chapterOrder = mapOf("a" to 0),
            requestedCount = 1,
            order = SessionQuestionOrder.BOOK
        )

        assertEquals(listOf("case-1", "case-2"), planned.map { it.id })
    }

    @Test
    fun statusFilterMatchRestoresTheCompleteSharedCase() {
        val caseOne = question("case-1", "a", "case", 1, SHARED_CASE)
        val caseTwo = question("case-2", "a", "case", 2, SHARED_CASE)
        val unrelated = question("case-3", "a", "case", 3, "Different patient")

        val expanded = SessionQuestionPlanner.expandMatchedCaseGroups(
            matchedQuestions = listOf(caseTwo),
            availableQuestions = listOf(caseOne, caseTwo, unrelated)
        )

        assertEquals(setOf("case-1", "case-2"), expanded.map { it.id }.toSet())
        assertTrue("case-3" !in expanded.map { it.id })
    }

    private fun question(
        id: String,
        chapterId: String,
        type: String,
        number: Int,
        caseContext: String = ""
    ) = QuestionEntity(
        id = id,
        chapterId = chapterId,
        type = type,
        number = number,
        stem = "Question $number",
        optionA = "A",
        optionB = "B",
        optionC = "C",
        optionD = "D",
        answer = "A",
        explanation = "Explanation",
        caseContext = caseContext,
        questionPage = number,
        answerPage = number,
        ocrStatus = "parsed",
        sourceIssue = null
    )

    private companion object {
        const val SHARED_CASE = "A patient vignette shared by two questions."
    }
}
