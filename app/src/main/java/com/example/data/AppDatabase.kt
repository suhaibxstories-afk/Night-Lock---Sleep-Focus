package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LockSchedule::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lockScheduleDao(): LockScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "night_lock_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default "Sleep Time" schedule
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultSchedule = LockSchedule(
                                id = 1,
                                name = "Sleep Time",
                                startHour = 23, // 11:00 PM
                                startMinute = 0,
                                endHour = 7,    // 7:00 AM
                                endMinute = 0,
                                daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7), // Mon through Sun
                                isEnabled = true,
                                blockedPackages = listOf(
                                    "com.google.android.youtube",
                                    "com.instagram.android",
                                    "com.zhiliaoapp.musically",
                                    "com.facebook.katana",
                                    "com.twitter.android",
                                    "com.reddit.frontpage"
                                ),
                                allowedPackages = listOf(
                                    "com.google.android.dialer",
                                    "com.android.phone",
                                    "com.google.android.deskclock",
                                    "com.android.deskclock"
                                ),
                                isStrictSleepMode = false,
                                lockScreenOnly = false
                            )
                            INSTANCE?.lockScheduleDao()?.insert(defaultSchedule)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
