package com.ektebrysjan.todo.util

import android.content.Context
import android.net.Uri

/**
 * Manual export/import of every task (including completed and archived) to a plain **CSV** file.
 *
 * Columns: `List, Title, Note, Due, Done, Archived`. `Due` is `yyyy-MM-dd` (or blank); `Done` and
 * `Archived` are `yes`/`no`. The file opens cleanly in any spreadsheet and imports back in the same
 * format. The caller supplies a Storage Access Framework [Uri], so no storage permission is needed.
 */
object TodoExport {

    /** Interchange row between the database and a CSV line (createdAt/updatedAt aren't in the CSV). */
    data class Row(
        val listName: String,
        val title: String,
        val note: String,
        val dueDate: Long?,
        val isDone: Boolean,
        val isArchived: Boolean,
        val createdAt: Long = 0,
        val updatedAt: Long = 0
    )

    private const val HEADER = "List,Title,Note,Due,Done,Archived"

    fun export(context: Context, uri: Uri, rows: List<Row>): Boolean {
        val sb = StringBuilder()
        sb.append(HEADER).append("\r\n")
        for (r in rows) {
            sb.append(esc(r.listName)).append(',')
                .append(esc(r.title)).append(',')
                .append(esc(r.note)).append(',')
                .append(esc(DateUtils.isoDate(r.dueDate))).append(',')
                .append(if (r.isDone) "yes" else "no").append(',')
                .append(if (r.isArchived) "yes" else "no").append("\r\n")
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

    /** Parse a CSV in the exported format. Returns null if the file can't be read or has no rows. */
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
            // Skip the header row if present.
            if (index == 0 && cols.getOrNull(0)?.trim().equals("List", true) &&
                cols.getOrNull(1)?.trim().equals("Title", true)
            ) return@forEachIndexed

            val title = cols.getOrNull(1)?.trim().orEmpty()
            if (title.isEmpty()) return@forEachIndexed // ignore blank lines / listless rows

            out += Row(
                listName = cols.getOrNull(0)?.trim().orEmpty(),
                title = title,
                note = cols.getOrNull(2).orEmpty(),
                dueDate = DateUtils.parseIso(cols.getOrNull(3).orEmpty()),
                isDone = truthy(cols.getOrNull(4)),
                isArchived = truthy(cols.getOrNull(5))
            )
        }
        return out.ifEmpty { null }
    }

    private fun truthy(v: String?): Boolean {
        val s = v?.trim()?.lowercase() ?: return false
        return s == "yes" || s == "true" || s == "1" || s == "done" || s == "y"
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
