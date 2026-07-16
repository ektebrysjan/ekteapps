package com.ektebrysjan.workout.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Human-friendly date for the UI (e.g. "Wed, 3 Sep 2026"). Empty if <= 0. */
    fun prettyDate(epochMillis: Long): String =
        if (epochMillis <= 0) "" else
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

    /** ISO yyyy-MM-dd for CSV. Empty if <= 0. */
    fun isoDate(epochMillis: Long): String =
        if (epochMillis <= 0) "" else iso.format(Date(epochMillis))

    /** Parse an ISO yyyy-MM-dd back to epoch millis, or null if blank/invalid. */
    fun parseIso(text: String): Long? =
        text.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { iso.parse(it)?.time }.getOrNull()
        }

    /** Midnight at the start of the current calendar week (Monday). */
    fun startOfWeek(nowMillis: Long): Long {
        val cal = midnight(nowMillis)
        // Roll back to Monday regardless of locale's first-day-of-week.
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return cal.timeInMillis
    }

    /** Midnight on the first day of the current calendar month. */
    fun startOfMonth(nowMillis: Long): Long {
        val cal = midnight(nowMillis)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    private fun midnight(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
