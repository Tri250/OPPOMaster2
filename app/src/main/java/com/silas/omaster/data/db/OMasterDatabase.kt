package com.silas.omaster.data.db

import android.content.Context
import androidx.room.*
import com.silas.omaster.data.db.converter.Converters

/**
 * OMaster Room 数据库
 * 统一管理编辑会话和配方历史
 */
@Database(
    entities = [
        EditSession::class,
        RecipeHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OMasterDatabase : RoomDatabase() {
    abstract fun editSessionDao(): EditSessionDao
    abstract fun recipeHistoryDao(): RecipeHistoryDao

    companion object {
        private const val DATABASE_NAME = "omaster_database"

        @Volatile
        private var INSTANCE: OMasterDatabase? = null

        fun getInstance(context: Context): OMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): OMasterDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                OMasterDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
