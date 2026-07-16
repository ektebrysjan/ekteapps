package com.ektebrysjan.notes.util

import java.text.DateFormat
import java.util.Date

object DateUtils {
    /** Short, locale-aware date + time for a note's last-edited timestamp (epoch millis). */
    fun prettyDateTime(epochMillis: Long): String {
        if (epochMillis <= 0) return ""
        val fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return fmt.format(Date(epochMillis))
    }
}
