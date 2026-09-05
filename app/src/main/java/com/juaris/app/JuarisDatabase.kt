package com.juaris.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SecurityLogEntity::class], version = 1, exportSchema = false)
abstract class JuarisDatabase : RoomDatabase() {
    abstract fun securityLogDao(): SecurityLogDao

    companion object {
        @Volatile
        private var INSTANCE: JuarisDatabase? = null

        fun getDatabase(context: Context): JuarisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JuarisDatabase::class.java,
                    "juaris_secure_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
