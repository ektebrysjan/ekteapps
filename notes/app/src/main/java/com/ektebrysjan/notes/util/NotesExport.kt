package com.ektebrysjan.notes.util

import android.content.Context
import android.net.Uri
import com.ektebrysjan.notes.data.Note
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manual backup: writes all notes to a ZIP archive containing a single `notes.json`, and reads that
 * archive back. Uses only Android's bundled `org.json` and `java.util.zip` — no third-party libs —
 * and the caller supplies a Storage Access Framework [Uri], so no storage permission is needed.
 *
 * The archive is plain and unencrypted (disclosed in the UI); it includes private notes, so the
 * caller must ensure private notes are unlocked before exporting.
 */
object NotesExport {

    private const val ENTRY_NAME = "notes.json"
    private const val FORMAT_VERSION = 1

    /** Serialise [notes] into a ZIP at [uri]. Returns true on success. */
    fun export(context: Context, uri: Uri, notes: List<Note>): Boolean {
        val root = JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("app", "ekte-notes")
            put("notes", JSONArray().apply { notes.forEach { put(it.toJson()) } })
        }
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry(ENTRY_NAME))
                    zip.write(root.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Read notes back from the ZIP at [uri]. Returns null if the archive is not a valid export. */
    fun import(context: Context, uri: Uri): List<Note>? = try {
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                var text: String? = null
                while (entry != null) {
                    if (entry.name == ENTRY_NAME) {
                        text = zip.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    entry = zip.nextEntry
                }
                text
            }
        }
        json?.let { parseNotes(it) }
    } catch (e: Exception) {
        null
    }

    private fun parseNotes(text: String): List<Note> {
        val arr = JSONObject(text).getJSONArray("notes")
        return (0 until arr.length()).map { arr.getJSONObject(it).toNote() }
    }

    private fun Note.toJson(): JSONObject = JSONObject().apply {
        // id is intentionally omitted so import always inserts fresh rows (no id collisions).
        put("title", title)
        put("body", body)
        put("tags", tags)
        put("isPrivate", isPrivate)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toNote(): Note = Note(
        id = 0,
        title = optString("title"),
        body = optString("body"),
        tags = optString("tags"),
        isPrivate = optBoolean("isPrivate", false),
        createdAt = optLong("createdAt", 0),
        updatedAt = optLong("updatedAt", 0)
    )
}
