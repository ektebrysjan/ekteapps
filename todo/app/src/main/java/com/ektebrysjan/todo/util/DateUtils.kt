package com.ektebrysjan.todo.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    // Stable machine format used in the CSV export (sortable, locale-independent).
    private val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Human-friendly due date for the UI (e.g. "Wed, 3 Sep 2026"). Empty if null. */
    fun prettyDate(epochMillis: Long?): String {
        if (epochMillis == null) return ""
        return SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }

    /** ISO yyyy-MM-dd for CSV. Empty string if null. */
    fun isoDate(epochMillis: Long?): String =
        if (epochMillis == null) "" else iso.format(Date(epochMillis))

    /** Parse an ISO yyyy-MM-dd back to epoch millis, or null if blank/invalid. */
    fun parseIso(text: String): Long? =
        text.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { iso.parse(it)?.time }.getOrNull()
        }
}
