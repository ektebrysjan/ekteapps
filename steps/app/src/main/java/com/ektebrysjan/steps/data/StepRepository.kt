package com.ektebrysjan.steps.data

import android.content.Context
import com.ektebrysjan.steps.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for step data.
 *
 * The hardware TYPE_STEP_COUNTER sensor reports a cumulative count since the last reboot and
 * resets to 0 when the device restarts. This repository converts that raw running total into
 * per-day deltas and stores daily totals in Room.
 */
class StepRepository private constructor(
    private val dao: StepDao,
    context: Context
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Serialises the baseline read-modify-write so overlapping sensor events (which may run
    // concurrently on Dispatchers.Default) can never read the same baseline and double-count.
    private val mutex = Mutex()

    /** All recorded days, newest first. */
    fun observeAll(): Flow<List<DailyStep>> = dao.observeAll()

    /** Rows for the given calendar dates. */
    fun observeForDates(dates: List<String>): Flow<List<DailyStep>> = dao.observeForDates(dates)

    /** Live step count for today. */
    fun observeToday(): Flow<Int?> = dao.observeSteps(DateUtils.today())

    /**
     * Record a new raw cumulative value from the step-counter sensor and attribute the delta
     * to today's total.
     *
     * - First reading ever → establish the baseline, count no steps.
     * - Value decreased (device rebooted, counter reset) → re-establish baseline, count no steps.
     * - Otherwise → add (value - lastValue) to today.
     */
    suspend fun recordCounterValue(rawValue: Long) = mutex.withLock {
        val last = prefs.getLong(KEY_LAST_VALUE, NONE)

        if (last == NONE || rawValue < last) {
            // Baseline (first run) or reboot detected — set the baseline, attribute nothing.
            prefs.edit().putLong(KEY_LAST_VALUE, rawValue).apply()
            return@withLock
        }
        val delta = (rawValue - last).toInt()
        if (delta > 0) {
            dao.addSteps(DateUtils.today(), delta)
        }
        // Only advance the baseline after the steps are persisted, still inside the lock.
        prefs.edit().putLong(KEY_LAST_VALUE, rawValue).apply()
    }

    /** Wipe all history and reset the sensor baseline so counting resumes cleanly. */
    suspend fun clearHistory() = mutex.withLock {
        dao.clearAll()
        prefs.edit().remove(KEY_LAST_VALUE).apply()
    }

    /** One-shot snapshot of the full history, for export. */
    suspend fun getAllOnce(): List<DailyStep> = dao.getAllOnce()

    /** Restore days from a backup; imported values overwrite existing rows for the same date. */
    suspend fun importDays(days: List<DailyStep>) = mutex.withLock {
        days.forEach { dao.upsert(it) }
    }

    companion object {
        private const val PREFS = "steps_prefs"
        private const val KEY_LAST_VALUE = "last_counter_value"
        private const val NONE = -1L

        @Volatile
        private var INSTANCE: StepRepository? = null

        fun get(context: Context): StepRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepRepository(
                    AppDatabase.get(context).stepDao(),
                    context.applicationContext
                ).also { INSTANCE = it }
            }
    }
}
