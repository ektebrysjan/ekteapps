package com.ektebrysjan.todo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single task belonging to a [TaskList].
 *
 * [dueDate] is epoch millis or null (optional). [isDone] marks completion; [isArchived] hides it
 * from the active view while keeping it for export/history. Deleting a task removes the row entirely.
 * Tasks are removed automatically (CASCADE) when their parent list is deleted.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val note: String = "",
    val dueDate: Long? = null,
    val isDone: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
