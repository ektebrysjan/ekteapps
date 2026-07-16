package com.ektebrysjan.steps.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StepDao {

    /** All recorded days, newest first. Observed by the statistics screen. */
    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    abstract fun observeAll(): Flow<List<DailyStep>>

    /** One-shot snapshot of all days, for export. */
    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    abstract suspend fun getAllOnce(): List<DailyStep>

    /** Rows for the given dates (used by the home screen for the last 7 days). */
    @Query("SELECT * FROM daily_steps WHERE date IN (:dates)")
    abstract fun observeForDates(dates: List<String>): Flow<List<DailyStep>>

    /** Live step count for a single day. */
    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    abstract fun observeSteps(date: String): Flow<Int?>

    @Query("SELECT steps FROM daily_steps WHERE date = :date")
    abstract suspend fun getSteps(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(day: DailyStep)

    /**
     * Add [delta] steps to [date], creating the row if it does not exist yet.
     *
     * Implemented as read-modify-write inside a transaction rather than a SQL UPSERT, because the
     * `ON CONFLICT ... DO UPDATE` syntax requires SQLite 3.24+ which is not available on the
     * SQLite bundled with Android below API 30. Steps are written by a single writer (the
     * service), so there is no concurrent-writer contention.
     */
    @Transaction
    open suspend fun addSteps(date: String, delta: Int) {
        val current = getSteps(date) ?: 0
        upsert(DailyStep(date = date, steps = current + delta))
    }

    @Query("DELETE FROM daily_steps")
    abstract suspend fun clearAll()
}
