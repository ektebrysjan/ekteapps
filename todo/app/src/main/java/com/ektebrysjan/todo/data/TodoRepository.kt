package com.ektebrysjan.todo.data

import android.content.Context
import com.ektebrysjan.todo.util.TodoExport
import kotlinx.coroutines.flow.Flow

/** Single source of truth for lists and tasks. */
class TodoRepository private constructor(private val dao: TodoDao) {

    fun observeLists(): Flow<List<TaskList>> = dao.observeLists()
    fun observeTasks(listId: Long): Flow<List<TaskItem>> = dao.observeTasks(listId)

    /** Create a starter list the first time the app runs, so there's somewhere to add tasks. */
    suspend fun ensureDefaultList(defaultName: String) {
        if (dao.listCount() == 0) {
            dao.insertList(TaskList(name = defaultName, createdAt = now()))
        }
    }

    suspend fun addList(name: String): Long =
        dao.insertList(TaskList(name = name.trim(), createdAt = now()))

    suspend fun renameList(list: TaskList, newName: String) =
        dao.updateList(list.copy(name = newName.trim()))

    suspend fun deleteList(id: Long) = dao.deleteList(id)

    suspend fun getTask(id: Long): TaskItem? = dao.getTask(id)

    /** Insert or update a task; returns its row id. */
    suspend fun saveTask(task: TaskItem): Long {
        val stamped = if (task.id == 0L) {
            task.copy(createdAt = now(), updatedAt = now())
        } else {
            task.copy(updatedAt = now())
        }
        return dao.upsertTask(stamped)
    }

    suspend fun setDone(task: TaskItem, done: Boolean) =
        dao.upsertTask(task.copy(isDone = done, updatedAt = now()))

    suspend fun setArchived(task: TaskItem, archived: Boolean) =
        dao.upsertTask(task.copy(isArchived = archived, updatedAt = now()))

    suspend fun deleteTask(task: TaskItem) = dao.deleteTask(task)

    // --- Export / import ---

    /** All tasks paired with their list name, for CSV export. */
    suspend fun exportRows(): List<TodoExport.Row> {
        val listName = dao.getListsOnce().associate { it.id to it.name }
        return dao.getAllTasksOnce().map { t ->
            TodoExport.Row(
                listName = listName[t.listId] ?: "",
                title = t.title,
                note = t.note,
                dueDate = t.dueDate,
                isDone = t.isDone,
                isArchived = t.isArchived,
                createdAt = t.createdAt,
                updatedAt = t.updatedAt
            )
        }
    }

    /** Insert imported rows, creating lists by name as needed. Returns the task count added. */
    suspend fun importRows(rows: List<TodoExport.Row>): Int {
        val cache = HashMap<String, Long>()
        var count = 0
        for (r in rows) {
            val name = r.listName.ifBlank { "Imported" }
            val listId = cache[name] ?: run {
                val existing = dao.findListByName(name)?.id
                    ?: dao.insertList(TaskList(name = name, createdAt = now()))
                cache[name] = existing
                existing
            }
            dao.upsertTask(
                TaskItem(
                    id = 0,
                    listId = listId,
                    title = r.title,
                    note = r.note,
                    dueDate = r.dueDate,
                    isDone = r.isDone,
                    isArchived = r.isArchived,
                    createdAt = if (r.createdAt > 0) r.createdAt else now(),
                    updatedAt = if (r.updatedAt > 0) r.updatedAt else now()
                )
            )
            count++
        }
        return count
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        @Volatile
        private var INSTANCE: TodoRepository? = null

        fun get(context: Context): TodoRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TodoRepository(AppDatabase.get(context).todoDao()).also { INSTANCE = it }
            }
    }
}
