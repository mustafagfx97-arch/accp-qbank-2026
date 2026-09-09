package com.mustafanabeel.accpqbank2026.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val title: String,
    val caseLabel: String,
    val pageStart: Int,
    val pageEnd: Int,
    val assessmentCount: Int,
    val caseCount: Int
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val type: String, // "assessment" or "case"
    val number: Int,
    val stem: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val answer: String, // "A", "B", "C", "D"
    val explanation: String,
    val caseContext: String,
    val questionPage: Int?,
    val answerPage: Int?,
    val ocrStatus: String,
    val sourceIssue: String?
)

@Entity(tableName = "user_attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val chapterId: String,
    val selectedOption: String,
    val isCorrect: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String // "instant", "exam"
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val questionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "quiz_sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String, // "instant", "exam"
    val totalQuestions: Int,
    val correctCount: Int,
    val questionIdsJson: String, // JSON array of question IDs
    val userAnswersJson: String, // JSON map of questionId -> selectedOption
    val isCompleted: Boolean = false,
    val timeSpentSeconds: Long = 0
)
