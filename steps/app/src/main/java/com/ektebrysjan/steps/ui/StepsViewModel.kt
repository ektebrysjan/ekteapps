package com.ektebrysjan.steps.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ektebrysjan.steps.R
import com.ektebrysjan.steps.data.DailyStep
import com.ektebrysjan.steps.data.StepRepository
import com.ektebrysjan.steps.util.DateUtils
import com.ektebrysjan.steps.util.StepsExport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One day rendered on screen. */
data class DayUi(
    val date: String,
    val label: String,
    val steps: Int,
    val isToday: Boolean
)

/** Aggregate figures for the statistics screen. */
data class Stats(
    val totalSteps: Long,
    val dailyAverage: Int,
    val bestDaySteps: Int,
    val daysTracked: Int
)

class StepsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = StepRepository.get(app)

    /** Live count for today. */
    val todaySteps: StateFlow<Int> = repository.observeToday()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** The last 7 calendar days (oldest first), always exactly 7 entries even if some are empty. */
    val week: StateFlow<List<DayUi>> = run {
        val dates = DateUtils.lastDates(7)
        val today = DateUtils.today()
        repository.observeForDates(dates)
            .map { rows ->
                val byDate = rows.associateBy { it.date }
                dates.map { date ->
                    DayUi(
                        date = date,
                        label = DateUtils.weekdayLabel(date),
                        steps = byDate[date]?.steps ?: 0,
                        isToday = date == today
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    /** Full recorded history, newest first. */
    val history: StateFlow<List<DailyStep>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Aggregate statistics derived from the full history. */
    val stats: StateFlow<Stats> = repository.observeAll()
        .map { rows ->
            if (rows.isEmpty()) {
                Stats(0, 0, 0, 0)
            } else {
                val total = rows.sumOf { it.steps.toLong() }
                Stats(
                    totalSteps = total,
                    dailyAverage = (total / rows.size).toInt(),
                    bestDaySteps = rows.maxOf { it.steps },
                    daysTracked = rows.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats(0, 0, 0, 0))

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    /** One-off user messages (export/import results), resolved to display strings. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = StepsExport.export(getApplication(), uri, repository.getAllOnce())
            emit(if (ok) R.string.export_ok else R.string.export_failed)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val days = StepsExport.import(getApplication(), uri)
            if (days == null) {
                emit(R.string.import_failed)
            } else {
                repository.importDays(days)
                _messages.tryEmit(getApplication<Application>().getString(R.string.import_ok, days.size))
            }
        }
    }

    private fun emit(resId: Int) {
        _messages.tryEmit(getApplication<Application>().getString(resId))
    }
}
