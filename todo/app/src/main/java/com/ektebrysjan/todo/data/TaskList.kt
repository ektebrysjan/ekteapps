package com.ektebrysjan.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named list that groups tasks (e.g. "Work", "Home", "Personal"). */
@Entity(tableName = "lists")
data class TaskList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = 0
)
