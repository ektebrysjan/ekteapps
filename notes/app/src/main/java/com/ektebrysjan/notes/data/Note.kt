package com.ektebrysjan.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note.
 *
 * [tags] is a simple comma-separated string (e.g. "work, ideas"); this keeps the schema flat and
 * matches the app's "straightforward" charter without a separate tags table. [isPrivate] notes are
 * hidden from the list until the user unlocks with their PIN. [createdAt]/[updatedAt] are epoch
 * milliseconds.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val tags: String = "",
    val isPrivate: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    /** Split, trimmed, non-empty tag tokens. */
    fun tagList(): List<String> =
        tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
