package com.mustafanabeel.accpqbank2026.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QBankJsonRoot(
    @Json(name = "meta") val meta: QBankMetaJson?,
    @Json(name = "chapters") val chapters: List<ChapterJson>,
    @Json(name = "questions") val questions: List<QuestionJson>
)

@JsonClass(generateAdapter = true)
data class QBankMetaJson(
    @Json(name = "source_title") val sourceTitle: String? = null,
    @Json(name = "edition") val edition: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "total_chapters") val totalChapters: Int? = null,
    @Json(name = "total_questions") val totalQuestions: Int? = null,
    @Json(name = "usable_questions") val usableQuestions: Int? = null,
    @Json(name = "missing_source_questions") val missingSourceQuestions: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChapterJson(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "page_range") val pageRange: List<Int>? = null,
    @Json(name = "case_label") val caseLabel: String? = "Patient Case",
    @Json(name = "assessment_count") val assessmentCount: Int = 0,
    @Json(name = "case_count") val caseCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class QuestionJson(
    @Json(name = "id") val id: String,
    @Json(name = "chapter_id") val chapterId: String,
    @Json(name = "type") val type: String,
    @Json(name = "number") val number: Int,
    @Json(name = "stem") val stem: String,
    @Json(name = "options") val options: Map<String, String>,
    @Json(name = "case_context") val caseContext: String? = "",
    @Json(name = "question_page") val questionPage: Int? = null,
    @Json(name = "answer_page") val answerPage: Int? = null,
    @Json(name = "answer") val answer: String,
    @Json(name = "explanation") val explanation: String,
    @Json(name = "ocr_status") val ocrStatus: String = "parsed",
    @Json(name = "ocr_warnings") val ocrWarnings: List<String>? = emptyList(),
    @Json(name = "source_issue") val sourceIssue: String? = null
)
