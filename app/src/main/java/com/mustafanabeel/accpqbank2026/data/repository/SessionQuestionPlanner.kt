package com.mustafanabeel.accpqbank2026.data.repository

import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import kotlin.random.Random

object SessionQuestionOrder {
    const val BOOK = "book"
    const val RANDOM = "random"
}

/**
 * Builds quiz sessions without separating questions that share one Patient Case vignette.
 *
 * The canonical JSON already stores the original chapter/type/number and repeats a non-empty
 * caseContext for every question belonging to the same vignette. Assessment questions and case
 * questions without a shared vignette remain individual blocks.
 */
internal object SessionQuestionPlanner {
    fun expandMatchedCaseGroups(
        matchedQuestions: List<QuestionEntity>,
        availableQuestions: List<QuestionEntity>
    ): List<QuestionEntity> {
        val matchedIds = matchedQuestions.mapTo(mutableSetOf()) { it.id }
        val matchedCaseKeys = matchedQuestions.mapNotNullTo(mutableSetOf(), ::sharedCaseKey)

        if (matchedCaseKeys.isEmpty()) return matchedQuestions

        return availableQuestions.filter { question ->
            val caseKey = sharedCaseKey(question)
            question.id in matchedIds || (caseKey != null && caseKey in matchedCaseKeys)
        }
    }

    fun plan(
        questions: List<QuestionEntity>,
        chapterOrder: Map<String, Int>,
        requestedCount: Int,
        order: String,
        random: Random = Random.Default
    ): List<QuestionEntity> {
        if (questions.isEmpty()) return emptyList()

        val sourceOrdered = questions.sortedWith(
            compareBy<QuestionEntity> { chapterOrder[it.chapterId] ?: Int.MAX_VALUE }
                .thenBy { if (it.type == "assessment") 0 else 1 }
                .thenBy(QuestionEntity::number)
                .thenBy { it.questionPage ?: Int.MAX_VALUE }
                .thenBy(QuestionEntity::id)
        )

        val blocks = toWholeCaseBlocks(sourceOrdered)
        val orderedBlocks = if (order == SessionQuestionOrder.RANDOM) {
            blocks.shuffled(random)
        } else {
            blocks
        }

        return takeWholeBlocks(orderedBlocks, requestedCount)
    }

    fun sharedCaseQuestions(
        questions: List<QuestionEntity>,
        currentQuestion: QuestionEntity
    ): List<QuestionEntity> {
        val key = sharedCaseKey(currentQuestion) ?: return listOf(currentQuestion)
        return questions
            .filter { sharedCaseKey(it) == key }
            .sortedBy(QuestionEntity::number)
    }

    private fun toWholeCaseBlocks(
        sourceOrdered: List<QuestionEntity>
    ): List<List<QuestionEntity>> {
        val sharedGroups = sourceOrdered
            .mapNotNull { question -> sharedCaseKey(question)?.let { it to question } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        val emittedSharedGroups = mutableSetOf<String>()

        return buildList {
            sourceOrdered.forEach { question ->
                val key = sharedCaseKey(question)
                if (key == null) {
                    add(listOf(question))
                } else if (emittedSharedGroups.add(key)) {
                    add(sharedGroups.getValue(key).sortedBy(QuestionEntity::number))
                }
            }
        }
    }

    private fun takeWholeBlocks(
        blocks: List<List<QuestionEntity>>,
        requestedCount: Int
    ): List<QuestionEntity> {
        if (requestedCount <= 0) return blocks.flatten()

        return buildList {
            for (block in blocks) {
                if (size >= requestedCount) break
                addAll(block)
            }
        }
    }

    private fun sharedCaseKey(question: QuestionEntity): String? {
        if (question.type != "case") return null
        val normalizedContext = question.caseContext
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
        if (normalizedContext.isBlank()) return null
        return "${question.chapterId}\u0000$normalizedContext"
    }
}
