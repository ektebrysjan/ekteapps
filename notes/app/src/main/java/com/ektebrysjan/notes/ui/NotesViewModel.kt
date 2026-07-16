package com.ektebrysjan.notes.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ektebrysjan.notes.R
import com.ektebrysjan.notes.data.Note
import com.ektebrysjan.notes.data.NoteRepository
import com.ektebrysjan.notes.util.NotesExport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = NoteRepository.get(app)

    val searchQuery = MutableStateFlow("")
    val selectedTag = MutableStateFlow<String?>(null)

    private val _privateUnlocked = MutableStateFlow(false)
    val privateUnlocked: StateFlow<Boolean> = _privateUnlocked.asStateFlow()

    private val _hasPin = MutableStateFlow(repository.pin.hasPin())
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    /** One-off user messages (export/import results), resolved to display strings. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Notes matching search + tag filter, with private notes hidden unless unlocked. */
    val notes: StateFlow<List<Note>> =
        combine(
            repository.observeAll(),
            searchQuery,
            selectedTag,
            _privateUnlocked
        ) { all, query, tag, unlocked ->
            all.asSequence()
                .filter { unlocked || !it.isPrivate }
                .filter { tag == null || it.tagList().any { t -> t.equals(tag, ignoreCase = true) } }
                .filter { note -> query.isBlank() || note.matches(query) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Distinct tags across currently-visible notes (private tags appear only when unlocked). */
    val allTags: StateFlow<List<String>> =
        combine(repository.observeAll(), _privateUnlocked) { all, unlocked ->
            all.asSequence()
                .filter { unlocked || !it.isPrivate }
                .flatMap { it.tagList().asSequence() }
                .distinct()
                .sortedBy { it.lowercase() }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun Note.matches(query: String): Boolean {
        val q = query.trim()
        return title.contains(q, ignoreCase = true) ||
            body.contains(q, ignoreCase = true) ||
            tags.contains(q, ignoreCase = true)
    }

    // --- CRUD ---------------------------------------------------------------

    suspend fun getNote(id: Long): Note? = repository.getById(id)

    /** Create (id == 0) or update a note. Returns nothing; the list flow refreshes automatically. */
    fun saveNote(id: Long, title: String, body: String, tags: String, isPrivate: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val created = if (id == 0L) now else (repository.getById(id)?.createdAt ?: now)
            repository.save(
                Note(
                    id = id,
                    title = title.trim(),
                    body = body.trim(),
                    tags = normaliseTags(tags),
                    isPrivate = isPrivate,
                    createdAt = created,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.delete(note) }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch { repository.getById(id)?.let { repository.delete(it) } }
    }

    private fun normaliseTags(raw: String): String =
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    // --- PIN ----------------------------------------------------------------

    fun setPin(pin: String) {
        repository.pin.setPin(pin)
        _hasPin.value = true
        _privateUnlocked.value = true
    }

    /** Verify [pin]; on success, reveal private notes. Returns whether it matched. */
    fun unlock(pin: String): Boolean {
        val ok = repository.pin.verify(pin)
        if (ok) _privateUnlocked.value = true
        return ok
    }

    /** Remove the PIN after verifying the current one. Returns whether it matched. */
    fun removePin(currentPin: String): Boolean {
        if (!repository.pin.verify(currentPin)) return false
        repository.pin.clear()
        _hasPin.value = false
        _privateUnlocked.value = false
        return true
    }

    fun lock() {
        _privateUnlocked.value = false
    }

    // --- Export / import ----------------------------------------------------

    /** Whether exporting is currently allowed (private notes must be unlocked if a PIN is set). */
    fun canExport(): Boolean = !_hasPin.value || _privateUnlocked.value

    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = NotesExport.export(getApplication(), uri, repository.getAllOnce())
            emit(if (ok) R.string.export_ok else R.string.export_failed)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val imported = NotesExport.import(getApplication(), uri)
            if (imported == null) {
                emit(R.string.import_failed)
            } else {
                repository.importNotes(imported)
                _messages.tryEmit(getApplication<Application>().getString(R.string.import_ok, imported.size))
            }
        }
    }

    private fun emit(resId: Int) {
        _messages.tryEmit(getApplication<Application>().getString(resId))
    }
}
