package com.ektebrysjan.steps.util

import android.content.Context
import android.net.Uri
import com.ektebrysjan.steps.data.DailyStep
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manual backup: writes the daily step history to a ZIP archive containing a single `steps.json`,
 * and reads it back. Uses only Android's bundled `org.json` and `java.util.zip` (no third-party
 * libs); the caller supplies a Storage Access Framework [Uri], so no storage permission is needed.
 */
object StepsExport {

    private const val ENTRY_NAME = "steps.json"
    private const val FORMAT_VERSION = 1

    /** Serialise [days] into a ZIP at [uri]. Returns true on success. */
    fun export(context: Context, uri: Uri, days: List<DailyStep>): Boolean {
        val root = JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("app", "ekte-steps")
            put("days", JSONArray().apply {
                days.forEach { put(JSONObject().put("date", it.date).put("steps", it.steps)) }
            })
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

    /** Read the history back from the ZIP at [uri]. Null if it is not a valid export. */
    fun import(context: Context, uri: Uri): List<DailyStep>? = try {
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
        json?.let { parseDays(it) }
    } catch (e: Exception) {
        null
    }

    private fun parseDays(text: String): List<DailyStep> {
        val arr = JSONObject(text).getJSONArray("days")
        return (0 until arr.length()).mapNotNull {
            val o = arr.getJSONObject(it)
            val date = o.optString("date")
            if (date.isEmpty()) null else DailyStep(date = date, steps = o.optInt("steps", 0))
        }
    }
}
