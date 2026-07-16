package com.ektebrysjan.todo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    // --- Lists ---
    @Query("SELECT * FROM lists ORDER BY createdAt ASC, id ASC")
    fun observeLists(): Flow<List<TaskList>>

    @Query("SELECT * FROM lists ORDER BY createdAt ASC, id ASC")
    suspend fun getListsOnce(): List<TaskList>

    @Query("SELECT * FROM lists WHERE name = :name LIMIT 1")
    suspend fun findListByName(name: String): TaskList?

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun listCount(): Int

    @Insert
    suspend fun insertList(list: TaskList): Long

    @Update
    suspend fun updateList(list: TaskList)

    // Tasks are removed via ON DELETE CASCADE.
    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    // --- Tasks ---
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY createdAt DESC")
    fun observeTasks(listId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks ORDER BY listId ASC, createdAt ASC")
    suspend fun getAllTasksOnce(): List<TaskItem>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskItem?

    @Upsert
    suspend fun upsertTask(task: TaskItem): Long

    @Delete
    suspend fun deleteTask(task: TaskItem)
}
