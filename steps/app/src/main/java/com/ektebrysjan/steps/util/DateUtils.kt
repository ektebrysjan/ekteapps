package com.ektebrysjan.steps.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Date helpers. Dates are stored in the database as ISO strings ("yyyy-MM-dd") so they
 * sort correctly and are timezone-independent (they represent a calendar day, not an instant).
 */
object DateUtils {

    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Today's calendar date as "yyyy-MM-dd" in the device's local timezone. */
    fun today(): String = LocalDate.now().format(ISO)

    /**
     * The last [count] calendar dates ending today, oldest first, as "yyyy-MM-dd" strings.
     * e.g. count = 7 → [6 days ago … today].
     */
    fun lastDates(count: Int): List<String> {
        val today = LocalDate.now()
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()).format(ISO) }
    }

    /** Short weekday label for a stored date, e.g. "Mon". */
    fun weekdayLabel(isoDate: String): String =
        LocalDate.parse(isoDate, ISO)
            .dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())

    /** Human-friendly date, e.g. "Mon, 14 Jul". */
    fun prettyDate(isoDate: String): String {
        val date = LocalDate.parse(isoDate, ISO)
        val fmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
        return date.format(fmt)
    }
}
