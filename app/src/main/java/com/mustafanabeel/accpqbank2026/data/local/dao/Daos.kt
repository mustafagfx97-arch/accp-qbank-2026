package com.mustafanabeel.accpqbank2026.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mustafanabeel.accpqbank2026.data.local.entity.AttemptEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.BookmarkEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.ChapterEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters ORDER BY rowid ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getChapterCount(): Int
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY rowid ASC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE ocrStatus != 'source_missing' ORDER BY rowid ASC")
    fun getUsableQuestions(): Flow<List<QuestionEntity>>

    // number resets between Assessment and Case Study, so sorting only by
    // number interleaves the two sections. rowid preserves the exact trusted
    // source order written from the repaired ACCP JSON.
    @Query("SELECT * FROM questions WHERE chapterId = :chapterId AND ocrStatus != 'source_missing' ORDER BY rowid ASC")
    fun getQuestionsByChapter(chapterId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE chapterId = :chapterId ORDER BY rowid ASC")
    fun getAllQuestionsInChapter(chapterId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE id IN (:ids) ORDER BY rowid ASC")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE ocrStatus != 'source_missing'")
    suspend fun getUsableQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT * FROM questions WHERE stem LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%' ORDER BY rowid ASC")
    fun searchQuestions(query: String): Flow<List<QuestionEntity>>
}

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM user_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM user_attempts WHERE questionId = :questionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAttemptForQuestion(questionId: String): AttemptEntity?

    @Query("DELETE FROM user_attempts")
    suspend fun clearAllAttempts()

    @Query("DELETE FROM user_attempts WHERE questionId NOT IN (SELECT id FROM questions)")
    suspend fun deleteOrphanedAttempts()
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: String)

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    fun isBookmarked(questionId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE questionId = :questionId)")
    suspend fun isBookmarkedSync(questionId: String): Boolean

    @Query("DELETE FROM bookmarks WHERE questionId NOT IN (SELECT id FROM questions)")
    suspend fun deleteOrphanedBookmarks()
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM quiz_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM quiz_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM quiz_sessions WHERE isCompleted = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestIncompleteSession(): SessionEntity?
}
