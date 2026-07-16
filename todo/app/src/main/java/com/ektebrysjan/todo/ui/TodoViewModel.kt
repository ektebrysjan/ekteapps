package com.ektebrysjan.todo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ektebrysjan.todo.R
import com.ektebrysjan.todo.data.TaskItem
import com.ektebrysjan.todo.data.TaskList
import com.ektebrysjan.todo.data.TodoRepository
import com.ektebrysjan.todo.util.TodoExport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TodoRepository.get(app)

    val lists: StateFlow<List<TaskList>> = repository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    /** Tasks in the selected list (all states; the UI groups them into active/done/archived). */
    val tasks: StateFlow<List<TaskItem>> = _selectedListId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeTasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            repository.ensureDefaultList(app.getString(R.string.default_list_name))
        }
        // Keep a valid list selected as lists are added/removed.
        viewModelScope.launch {
            lists.collect { ls ->
                val cur = _selectedListId.value
                _selectedListId.value = when {
                    ls.isEmpty() -> null
                    cur == null || ls.none { it.id == cur } -> ls.first().id
                    else -> cur
                }
            }
        }
    }

    fun selectList(id: Long) { _selectedListId.value = id }

    // --- Lists ---
    fun addList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { _selectedListId.value = repository.addList(name) }
    }

    fun renameList(list: TaskList, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repository.renameList(list, newName) }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch { repository.deleteList(id) }
    }

    // --- Tasks ---
    suspend fun getTask(id: Long): TaskItem? = repository.getTask(id)

    fun saveTask(taskId: Long, listId: Long, title: String, note: String, dueDate: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val base = if (taskId != 0L) repository.getTask(taskId) else null
            val task = base?.copy(title = title.trim(), note = note.trim(), dueDate = dueDate)
                ?: TaskItem(listId = listId, title = title.trim(), note = note.trim(), dueDate = dueDate)
            repository.saveTask(task)
        }
    }

    fun toggleDone(task: TaskItem) {
        viewModelScope.launch { repository.setDone(task, !task.isDone) }
    }

    fun setArchived(task: TaskItem, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(task, archived) }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun deleteTaskById(id: Long) {
        viewModelScope.launch { repository.getTask(id)?.let { repository.deleteTask(it) } }
    }

    // --- Export / import ---
    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = TodoExport.export(getApplication(), uri, repository.exportRows())
            emit(if (ok) R.string.export_ok else R.string.export_failed)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val rows = TodoExport.import(getApplication(), uri)
            if (rows == null) {
                emit(R.string.import_failed)
            } else {
                val count = repository.importRows(rows)
                _messages.tryEmit(getApplication<Application>().getString(R.string.import_ok, count))
            }
        }
    }

    private fun emit(resId: Int) {
        _messages.tryEmit(getApplication<Application>().getString(resId))
    }
}
