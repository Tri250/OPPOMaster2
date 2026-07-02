package com.silas.omaster.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 数据库
 * 版本1：初始版本，包含 presets 表
 */
@Database(
    entities = [PresetEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PresetConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "omaster_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
