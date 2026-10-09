package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LockScheduleDao {
    @Query("SELECT * FROM lock_schedules ORDER BY id ASC")
    fun getAllSchedules(): Flow<List<LockSchedule>>

    @Query("SELECT * FROM lock_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): LockSchedule?

    @Query("SELECT * FROM lock_schedules WHERE isEnabled = 1")
    fun getEnabledSchedules(): Flow<List<LockSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: LockSchedule): Long

    @Update
    suspend fun update(schedule: LockSchedule)

    @Delete
    suspend fun delete(schedule: LockSchedule)

    @Query("DELETE FROM lock_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE lock_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM lock_schedules")
    suspend fun clearAll()
}
