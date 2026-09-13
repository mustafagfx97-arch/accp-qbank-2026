package com.mustafanabeel.accpqbank2026.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mustafanabeel.accpqbank2026.data.local.entity.AttemptEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.BookmarkEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.ChapterEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.SessionEntity
import com.mustafanabeel.accpqbank2026.data.repository.QBankRepository
import com.mustafanabeel.accpqbank2026.data.repository.SessionConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class QuizSessionState(
    val sessionId: String = "",
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val submittedInstant: Set<String> = emptySet(),
    val flaggedQuestionIds: Set<String> = emptySet(),
    val config: SessionConfig = SessionConfig(),
    val elapsedSeconds: Long = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false
)

data class OverallStats(
    val totalUsableQuestions: Int = 531,
    val totalAttempted: Int = 0,
    val totalCorrect: Int = 0,
    val accuracyPercentage: Int = 0,
    val bookmarkedCount: Int = 0,
    val chapterStats: Map<String, ChapterStat> = emptyMap()
)

data class ChapterStat(
    val total: Int = 0,
    val attempted: Int = 0,
    val correct: Int = 0,
    val accuracy: Int = 0
)

class QBankViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QBankRepository(application)

    val chapters: StateFlow<List<ChapterEntity>> = repository.allChapters.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val usableQuestions: StateFlow<List<QuestionEntity>> = repository.usableQuestions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val attempts: StateFlow<List<AttemptEntity>> = repository.allAttempts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sessions: StateFlow<List<SessionEntity>> = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    private var timerJob: Job? = null
    private val databaseInitializationJob: Job

    val stats: StateFlow<OverallStats> = combine(
        usableQuestions,
        attempts,
        bookmarks
    ) { questions, atts, bms ->
        val latestByQuestion = atts.groupBy { it.questionId }.mapValues { it.value.first() }
        val attemptedCount = latestByQuestion.size
        val correctCount = latestByQuestion.values.count { it.isCorrect }
        val accuracy = if (attemptedCount > 0) (correctCount * 100) / attemptedCount else 0

        val chStats = mutableMapOf<String, ChapterStat>()
        val questionsByChapter = questions.groupBy { it.chapterId }
        for ((chId, chQuestions) in questionsByChapter) {
            val chAttempted = chQuestions.count { latestByQuestion.containsKey(it.id) }
            val chCorrect = chQuestions.count { latestByQuestion[it.id]?.isCorrect == true }
            val chAcc = if (chAttempted > 0) (chCorrect * 100) / chAttempted else 0
            chStats[chId] = ChapterStat(
                total = chQuestions.size,
                attempted = chAttempted,
                correct = chCorrect,
                accuracy = chAcc
            )
        }

        OverallStats(
            totalUsableQuestions = questions.size,
            totalAttempted = attemptedCount,
            totalCorrect = correctCount,
            accuracyPercentage = accuracy,
            bookmarkedCount = bms.size,
            chapterStats = chStats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverallStats()
    )

    init {
        databaseInitializationJob = viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
        }
    }

    fun startSession(config: SessionConfig, onReady: () -> Unit) {
        viewModelScope.launch {
            _quizState.value = QuizSessionState(isLoading = true)
            databaseInitializationJob.join()
            val questions = repository.getQuestionsForSession(config)
            val sessionId = UUID.randomUUID().toString()

            _quizState.value = QuizSessionState(
                sessionId = sessionId,
                questions = questions,
                currentIndex = 0,
                selectedAnswers = emptyMap(),
                submittedInstant = emptySet(),
                flaggedQuestionIds = emptySet(),
                config = config,
                elapsedSeconds = 0,
                isCompleted = false,
                isLoading = false
            )
            startTimer()
            onReady()
        }
    }

    fun startChapterSession(chapterId: String, mode: String = "instant", onReady: () -> Unit) {
        startSession(
            SessionConfig(
                mode = mode,
                selectedChapterIds = setOf(chapterId),
                questionCount = 0,
                shuffleQuestions = false
            ),
            onReady = onReady
        )
    }

    fun startQuickSession(onReady: () -> Unit) {
        startSession(
            SessionConfig(
                mode = "instant",
                questionCount = 10,
                shuffleQuestions = true
            ),
            onReady = onReady
        )
    }

    fun startBookmarkSession(onReady: () -> Unit) {
        startSession(
            SessionConfig(
                mode = "instant",
                statusFilter = "bookmarked",
                questionCount = 0,
                shuffleQuestions = false
            ),
            onReady = onReady
        )
    }

    fun retryIncorrectQuestions(onReady: () -> Unit) {
        startSession(
            SessionConfig(
                mode = "instant",
                statusFilter = "incorrect",
                questionCount = 0,
                shuffleQuestions = false
            ),
            onReady = onReady
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (!_quizState.value.isCompleted) {
                delay(1000)
                _quizState.value = _quizState.value.copy(
                    elapsedSeconds = _quizState.value.elapsedSeconds + 1
                )
            }
        }
    }

    fun selectOption(question: QuestionEntity, option: String) {
        val current = _quizState.value
        if (current.isCompleted) return

        val isInstant = current.config.mode == "instant"
        val isAlreadySubmitted = current.submittedInstant.contains(question.id)
        if (isInstant && isAlreadySubmitted) return

        val newAnswers = current.selectedAnswers.toMutableMap()
        newAnswers[question.id] = option

        val newSubmitted = current.submittedInstant.toMutableSet()
        if (isInstant) {
            newSubmitted.add(question.id)
            val isCorrect = option.equals(question.answer, ignoreCase = true)
            viewModelScope.launch {
                repository.recordAttempt(
                    questionId = question.id,
                    chapterId = question.chapterId,
                    selectedOption = option,
                    isCorrect = isCorrect,
                    mode = current.config.mode
                )
            }
        }

        _quizState.value = current.copy(
            selectedAnswers = newAnswers,
            submittedInstant = newSubmitted
        )
    }

    fun toggleFlag(questionId: String) {
        val current = _quizState.value
        val flagged = current.flaggedQuestionIds.toMutableSet()
        if (flagged.contains(questionId)) {
            flagged.remove(questionId)
        } else {
            flagged.add(questionId)
        }
        _quizState.value = current.copy(flaggedQuestionIds = flagged)
    }

    fun toggleBookmark(questionId: String) {
        viewModelScope.launch {
            repository.toggleBookmark(questionId)
        }
    }

    fun isQuestionBookmarked(questionId: String): Boolean {
        return bookmarks.value.any { it.questionId == questionId }
    }

    fun goToNext() {
        val current = _quizState.value
        if (current.currentIndex < current.questions.size - 1) {
            _quizState.value = current.copy(currentIndex = current.currentIndex + 1)
        }
    }

    fun goToPrevious() {
        val current = _quizState.value
        if (current.currentIndex > 0) {
            _quizState.value = current.copy(currentIndex = current.currentIndex - 1)
        }
    }

    fun jumpToQuestion(index: Int) {
        val current = _quizState.value
        if (index in current.questions.indices) {
            _quizState.value = current.copy(currentIndex = index)
        }
    }

    fun finishSession(onFinished: () -> Unit) {
        timerJob?.cancel()
        val current = _quizState.value
        var correctCount = 0

        viewModelScope.launch {
            for (q in current.questions) {
                val selected = current.selectedAnswers[q.id]
                val isCorrect = selected != null && selected.equals(q.answer, ignoreCase = true)
                if (isCorrect) correctCount++

                if (selected != null) {
                    repository.recordAttempt(
                        questionId = q.id,
                        chapterId = q.chapterId,
                        selectedOption = selected,
                        isCorrect = isCorrect,
                        mode = current.config.mode
                    )
                }
            }

            val questionIdsList = current.questions.map { it.id }.joinToString(",")
            val answersMap = current.selectedAnswers.entries.joinToString(";") { "${it.key}:${it.value}" }

            val sessionEntity = SessionEntity(
                sessionId = current.sessionId,
                mode = current.config.mode,
                totalQuestions = current.questions.size,
                correctCount = correctCount,
                questionIdsJson = questionIdsList,
                userAnswersJson = answersMap,
                isCompleted = true,
                timeSpentSeconds = current.elapsedSeconds
            )
            repository.saveSession(sessionEntity)

            _quizState.value = current.copy(isCompleted = true)
            onFinished()
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetProgress()
        }
    }

    fun reloadDatabaseFromAssets() {
        viewModelScope.launch {
            repository.reloadQuestionsFromAssets()
        }
    }
}
