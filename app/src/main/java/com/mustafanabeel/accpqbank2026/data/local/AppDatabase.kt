package com.mustafanabeel.accpqbank2026.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mustafanabeel.accpqbank2026.data.local.dao.AttemptDao
import com.mustafanabeel.accpqbank2026.data.local.dao.BookmarkDao
import com.mustafanabeel.accpqbank2026.data.local.dao.ChapterDao
import com.mustafanabeel.accpqbank2026.data.local.dao.QuestionDao
import com.mustafanabeel.accpqbank2026.data.local.dao.SessionDao
import com.mustafanabeel.accpqbank2026.data.local.entity.AttemptEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.BookmarkEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.ChapterEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.QuestionEntity
import com.mustafanabeel.accpqbank2026.data.local.entity.SessionEntity

@Database(
    entities = [
        ChapterEntity::class,
        QuestionEntity::class,
        AttemptEntity::class,
        BookmarkEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao
    abstract fun questionDao(): QuestionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "accp_qbank_2026.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
