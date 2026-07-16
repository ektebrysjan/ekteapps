package com.ektebrysjan.workout.util

import android.content.Context
import android.net.Uri

/**
 * Manual export/import of the full workout log to a plain **CSV** file.
 *
 * Columns: `Date, Type, DurationMin, Intensity, Note`. `Date` is `yyyy-MM-dd`. The file opens
 * cleanly in any spreadsheet and imports back in the same format. The caller supplies a Storage
 * Access Framework [Uri], so no storage permission is needed. Only Android's own APIs are used.
 */
object WorkoutExport {

    data class Row(
        val date: Long,
        val type: String,
        val durationMinutes: Int,
        val intensity: String,
        val note: String
    )

    private const val HEADER = "Date,Type,DurationMin,Intensity,Note"

    fun export(context: Context, uri: Uri, rows: List<Row>): Boolean {
        val sb = StringBuilder()
        sb.append(HEADER).append("\r\n")
        for (r in rows) {
            sb.append(esc(DateUtils.isoDate(r.date))).append(',')
                .append(esc(r.type)).append(',')
                .append(r.durationMinutes).append(',')
                .append(esc(r.intensity)).append(',')
                .append(esc(r.note)).append("\r\n")
        }
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Parse a CSV in the exported format. Returns null if unreadable or empty. */
    fun import(context: Context, uri: Uri): List<Row>? {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        } ?: return null

        val rows = parse(text)
        if (rows.isEmpty()) return null

        val out = ArrayList<Row>()
        rows.forEachIndexed { index, cols ->
            if (index == 0 && cols.getOrNull(0)?.trim().equals("Date", true) &&
                cols.getOrNull(1)?.trim().equals("Type", true)
            ) return@forEachIndexed

            val type = cols.getOrNull(1)?.trim().orEmpty()
            val duration = cols.getOrNull(2)?.trim()?.toIntOrNull()
            if (type.isEmpty() || duration == null) return@forEachIndexed // skip blank/invalid lines

            out += Row(
                date = DateUtils.parseIso(cols.getOrNull(0).orEmpty()) ?: 0L,
                type = type,
                durationMinutes = duration,
                intensity = cols.getOrNull(3)?.trim().orEmpty(),
                note = cols.getOrNull(4).orEmpty()
            )
        }
        return out.ifEmpty { null }
    }

    private fun esc(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Minimal RFC-4180-ish CSV parser: handles quoted fields with commas, quotes and newlines. */
    private fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { cur.add(field.toString()); field.setLength(0) }
                '\n' -> { cur.add(field.toString()); field.setLength(0); rows.add(cur); cur = ArrayList() }
                '\r' -> { /* handled with the following \n */ }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || cur.isNotEmpty()) { cur.add(field.toString()); rows.add(cur) }
        return rows
    }
}
