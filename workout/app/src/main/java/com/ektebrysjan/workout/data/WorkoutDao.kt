package com.ektebrysjan.workout.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    /** All workouts, most recent first. Filtering/summaries are computed in memory. */
    @Query("SELECT * FROM workouts ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workouts ORDER BY date DESC, id DESC")
    suspend fun getAllOnce(): List<WorkoutEntry>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntry?

    @Upsert
    suspend fun upsert(entry: WorkoutEntry): Long

    @Delete
    suspend fun delete(entry: WorkoutEntry)
}
