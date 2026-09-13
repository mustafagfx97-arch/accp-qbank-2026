package com.mustafanabeel.accpqbank2026.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.mustafanabeel.accpqbank2026.data.local.AppDatabase
import com.mustafanabeel.accpqbank2026.data.local.entity.AttemptEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.BookmarkEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.ChapterEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.SessionEntity
import com.mustafanabeel.accpqbank2026.data.model.QBankJsonRoot
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

data class SessionConfig(
    val mode: String = "instant",
    val questionType: String = "all",
    val selectedChapterIds: Set<String> = emptySet(),
    val statusFilter: String = "all",
    val questionCount: Int = 20,
    val timedMode: Boolean = false,
    val timeLimitMinutes: Int = 30,
    val shuffleQuestions: Boolean = false
)

class QBankRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val chapterDao = db.chapterDao()
    private val questionDao = db.questionDao()
    private val attemptDao = db.attemptDao()
    private val bookmarkDao = db.bookmarkDao()
    private val sessionDao = db.sessionDao()

    private val seedPreferences = context.getSharedPreferences(
        SEED_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    val allChapters: Flow<List<ChapterEntity>> = chapterDao.getAllChapters()
    val usableQuestions: Flow<List<QuestionEntity>> = questionDao.getUsableQuestions()
    val allAttempts: Flow<List<AttemptEntity>> = attemptDao.getAllAttempts()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    suspend fun initializeDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val jsonString = readBundledQuestionJson()
            requireExpectedAsset(jsonString)

            val currentCount = questionDao.getTotalQuestionCount()
            val chapterCount = chapterDao.getChapterCount()
            val installedHash = seedPreferences.getString(INSTALLED_HASH_KEY, null)

            if (
                currentCount != EXPECTED_QUESTION_COUNT ||
                chapterCount != EXPECTED_CHAPTER_COUNT ||
                installedHash != EXPECTED_JSON_SHA256
            ) {
                replaceQuestionsFromJson(jsonString)
            }
        } catch (e: Exception) {
            Log.e("QBankRepository", "Question-bank initialization failed", e)
        }
    }

    suspend fun reloadQuestionsFromAssets() = withContext(Dispatchers.IO) {
        try {
            replaceQuestionsFromJson(readBundledQuestionJson())
        } catch (e: Exception) {
            Log.e("QBankRepository", "Failed to load questions from assets", e)
        }
    }

    private suspend fun replaceQuestionsFromJson(jsonString: String) {
        requireExpectedAsset(jsonString)

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(QBankJsonRoot::class.java)
        val root = requireNotNull(adapter.fromJson(jsonString)) {
            "Unable to parse the bundled ACCP question bank"
        }

        require(root.chapters.size == EXPECTED_CHAPTER_COUNT) {
            "Expected $EXPECTED_CHAPTER_COUNT chapters, found ${root.chapters.size}"
        }
        require(root.questions.size == EXPECTED_QUESTION_COUNT) {
            "Expected $EXPECTED_QUESTION_COUNT questions, found ${root.questions.size}"
        }
        require(root.questions.map { it.id }.distinct().size == EXPECTED_QUESTION_COUNT) {
            "Question IDs are not unique"
        }

        val chapterIds = root.chapters.map { it.id }.toSet()
        require(root.questions.all { it.chapterId in chapterIds }) {
            "At least one question references a non-ACCP chapter"
        }
        require(root.questions.count { it.type == "assessment" } == EXPECTED_ASSESSMENT_COUNT)
        require(root.questions.count { it.type == "case" } == EXPECTED_CASE_COUNT)
        require(root.questions.count { it.ocrStatus != "source_missing" } == EXPECTED_USABLE_COUNT)
        require(root.questions.all { it.answer in setOf("A", "B", "C", "D") })
        require(root.questions.all { it.options.keys == setOf("A", "B", "C", "D") })

        val chapterEntities = root.chapters.map { chapter ->
            val start = chapter.pageRange?.getOrNull(0) ?: 1
            val end = chapter.pageRange?.getOrNull(1) ?: start
            ChapterEntity(
                id = chapter.id,
                title = chapter.title,
                caseLabel = chapter.caseLabel ?: "Patient Case",
                pageStart = start,
                pageEnd = end,
                assessmentCount = chapter.assessmentCount,
                caseCount = chapter.caseCount
            )
        }

        val questionEntities = root.questions.map { question ->
            QuestionEntity(
                id = question.id,
                chapterId = question.chapterId,
                type = question.type,
                number = question.number,
                stem = question.stem,
                optionA = question.options["A"] ?: "",
                optionB = question.options["B"] ?: "",
                optionC = question.options["C"] ?: "",
                optionD = question.options["D"] ?: "",
                answer = question.answer,
                explanation = question.explanation,
                caseContext = question.caseContext ?: "",
                questionPage = question.questionPage,
                answerPage = question.answerPage,
                ocrStatus = question.ocrStatus,
                sourceIssue = question.sourceIssue
            )
        }

        db.withTransaction {
            questionDao.deleteAll()
            chapterDao.deleteAll()
            chapterDao.insertAll(chapterEntities)
            questionDao.insertAll(questionEntities)
            attemptDao.deleteOrphanedAttempts()
            bookmarkDao.deleteOrphanedBookmarks()
        }

        check(
            seedPreferences.edit()
                .putString(INSTALLED_HASH_KEY, EXPECTED_JSON_SHA256)
                .commit()
        ) { "Could not record the installed question-bank version" }

        Log.d(
            "QBankRepository",
            "Installed ${chapterEntities.size} chapters and ${questionEntities.size} trusted questions"
        )
    }

    private fun readBundledQuestionJson(): String {
        val packagedJson = runCatching {
            context.assets.open(PACKAGED_ASSET_NAME)
        }.getOrNull()

        if (packagedJson != null) {
            return packagedJson.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        return GZIPInputStream(context.assets.open(COMPRESSED_ASSET_NAME))
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun requireExpectedAsset(jsonString: String) {
        val actualHash = MessageDigest.getInstance("SHA-256")
            .digest(jsonString.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
        require(actualHash == EXPECTED_JSON_SHA256) {
            "The bundled question bank failed integrity validation"
        }
    }

    suspend fun getQuestionsForSession(config: SessionConfig): List<QuestionEntity> =
        withContext(Dispatchers.IO) {
            // getUsableQuestions() is ordered by rowid, which is the same order
            // as the repaired ACCP source JSON. Keep it untouched unless the
            // caller explicitly asks for a random Quick Session.
            var pool = questionDao.getUsableQuestions().first()

            if (config.selectedChapterIds.isNotEmpty()) {
                pool = pool.filter { it.chapterId in config.selectedChapterIds }
            }

            pool = when (config.questionType) {
                "assessment" -> pool.filter { it.type == "assessment" }
                "case" -> pool.filter { it.type == "case" }
                else -> pool
            }

            val attempts = attemptDao.getAllAttempts().first()
            val latestAttempts = attempts.groupBy { it.questionId }
                .mapValues { it.value.first() }
            val bookmarks = bookmarkDao.getAllBookmarks().first()
                .map { it.questionId }
                .toSet()

            pool = when (config.statusFilter) {
                "unanswered" -> pool.filter { it.id !in latestAttempts }
                "incorrect" -> pool.filter { latestAttempts[it.id]?.isCorrect == false }
                "bookmarked" -> pool.filter { it.id in bookmarks }
                else -> pool
            }

            val orderedPool = if (config.shuffleQuestions) pool.shuffled() else pool

            if (config.questionCount > 0 && config.questionCount < orderedPool.size) {
                orderedPool.take(config.questionCount)
            } else {
                orderedPool
            }
        }

    suspend fun getQuestionById(id: String): QuestionEntity? = withContext(Dispatchers.IO) {
        questionDao.getQuestionById(id)
    }

    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity> =
        withContext(Dispatchers.IO) {
            questionDao.getQuestionsByIds(ids)
        }

    suspend fun toggleBookmark(questionId: String) = withContext(Dispatchers.IO) {
        if (bookmarkDao.isBookmarkedSync(questionId)) {
            bookmarkDao.removeBookmark(questionId)
        } else {
            bookmarkDao.addBookmark(BookmarkEntity(questionId = questionId))
        }
    }

    fun isBookmarked(questionId: String): Flow<Boolean> = bookmarkDao.isBookmarked(questionId)

    suspend fun recordAttempt(
        questionId: String,
        chapterId: String,
        selectedOption: String,
        isCorrect: Boolean,
        mode: String
    ) = withContext(Dispatchers.IO) {
        attemptDao.insertAttempt(
            AttemptEntity(
                questionId = questionId,
                chapterId = chapterId,
                selectedOption = selectedOption,
                isCorrect = isCorrect,
                mode = mode
            )
        )
    }

    suspend fun saveSession(session: SessionEntity) = withContext(Dispatchers.IO) {
        sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: SessionEntity) = withContext(Dispatchers.IO) {
        sessionDao.updateSession(session)
    }

    suspend fun getSessionById(sessionId: String): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)
    }

    suspend fun getLatestIncompleteSession(): SessionEntity? = withContext(Dispatchers.IO) {
        sessionDao.getLatestIncompleteSession()
    }

    suspend fun resetProgress() = withContext(Dispatchers.IO) {
        attemptDao.clearAllAttempts()
    }

    private companion object {
        const val EXPECTED_CHAPTER_COUNT = 22
        const val EXPECTED_QUESTION_COUNT = 533
        const val EXPECTED_USABLE_COUNT = 531
        const val EXPECTED_ASSESSMENT_COUNT = 258
        const val EXPECTED_CASE_COUNT = 275
        const val EXPECTED_JSON_SHA256 =
            "d285391e4ec2debe39a1ca6be969cf265c53d41c0cdf70003fb3631fa35cfb75"
        const val PACKAGED_ASSET_NAME = "questions.json"
        const val COMPRESSED_ASSET_NAME = "questions.json.gz"
        const val SEED_PREFERENCES_NAME = "accp_qbank_seed"
        const val INSTALLED_HASH_KEY = "installed_json_sha256"
    }
}
