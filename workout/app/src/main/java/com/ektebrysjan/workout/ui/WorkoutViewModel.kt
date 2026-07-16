package com.ektebrysjan.workout.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ektebrysjan.workout.R
import com.ektebrysjan.workout.data.WorkoutEntry
import com.ektebrysjan.workout.data.WorkoutRepository
import com.ektebrysjan.workout.util.DateUtils
import com.ektebrysjan.workout.util.WorkoutExport
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

/** Per-type totals for the monthly breakdown. */
data class TypeStat(val type: String, val sessions: Int, val minutes: Int)

/** Aggregate figures for the summary screen. */
data class Summary(
    val weekSessions: Int = 0,
    val weekMinutes: Int = 0,
    val monthSessions: Int = 0,
    val monthMinutes: Int = 0,
    val monthByType: List<TypeStat> = emptyList()
)

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WorkoutRepository.get(app)

    private val allEntries: StateFlow<List<WorkoutEntry>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedType = MutableStateFlow<String?>(null)

    /** Entries shown on the log, filtered by the selected type. */
    val entries: StateFlow<List<WorkoutEntry>> =
        combine(allEntries, selectedType) { all, type ->
            if (type == null) all else all.filter { it.type.equals(type, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Distinct workout types present in the log (for the filter chips). */
    val types: StateFlow<List<String>> = allEntries
        .map { list -> list.map { it.type }.distinct().sortedBy { it.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Weekly + monthly rollups, recomputed whenever the log changes. */
    val summary: StateFlow<Summary> = allEntries
        .map { computeSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Summary())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private fun computeSummary(all: List<WorkoutEntry>): Summary {
        val now = System.currentTimeMillis()
        val weekStart = DateUtils.startOfWeek(now)
        val monthStart = DateUtils.startOfMonth(now)
        val week = all.filter { it.date >= weekStart }
        val month = all.filter { it.date >= monthStart }
        val byType = month.groupBy { it.type }
            .map { (type, list) -> TypeStat(type, list.size, list.sumOf { it.durationMinutes }) }
            .sortedByDescending { it.minutes }
        return Summary(
            weekSessions = week.size,
            weekMinutes = week.sumOf { it.durationMinutes },
            monthSessions = month.size,
            monthMinutes = month.sumOf { it.durationMinutes },
            monthByType = byType
        )
    }

    fun selectType(type: String?) { selectedType.value = type }

    suspend fun getEntry(id: Long): WorkoutEntry? = repository.getById(id)

    fun saveWorkout(
        id: Long,
        type: String,
        durationMinutes: Int,
        intensity: String,
        note: String,
        date: Long
    ) {
        if (type.isBlank() || durationMinutes <= 0) return
        viewModelScope.launch {
            val base = if (id != 0L) repository.getById(id) else null
            val entry = base?.copy(
                type = type.trim(),
                durationMinutes = durationMinutes,
                intensity = intensity,
                note = note.trim(),
                date = date
            ) ?: WorkoutEntry(
                type = type.trim(),
                durationMinutes = durationMinutes,
                intensity = intensity,
                note = note.trim(),
                date = date
            )
            repository.save(entry)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch { repository.getById(id)?.let { repository.delete(it) } }
    }

    // --- Export / import ---
    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = WorkoutExport.export(getApplication(), uri, repository.exportRows())
            emit(if (ok) R.string.export_ok else R.string.export_failed)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val rows = WorkoutExport.import(getApplication(), uri)
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
