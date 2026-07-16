package com.ektebrysjan.workout.data

import android.content.Context
import com.ektebrysjan.workout.util.WorkoutExport
import kotlinx.coroutines.flow.Flow

/** Single source of truth for workout entries. */
class WorkoutRepository private constructor(private val dao: WorkoutDao) {

    fun observeAll(): Flow<List<WorkoutEntry>> = dao.observeAll()

    suspend fun getById(id: Long): WorkoutEntry? = dao.getById(id)

    /** Insert or update; stamps createdAt/updatedAt. Returns the row id. */
    suspend fun save(entry: WorkoutEntry): Long {
        val stamped = if (entry.id == 0L) {
            entry.copy(createdAt = now(), updatedAt = now())
        } else {
            entry.copy(updatedAt = now())
        }
        return dao.upsert(stamped)
    }

    suspend fun delete(entry: WorkoutEntry) = dao.delete(entry)

    // --- Export / import ---

    suspend fun exportRows(): List<WorkoutExport.Row> = dao.getAllOnce().map {
        WorkoutExport.Row(
            date = it.date,
            type = it.type,
            durationMinutes = it.durationMinutes,
            intensity = it.intensity,
            note = it.note
        )
    }

    /** Insert imported rows as fresh entries. Returns the count added. */
    suspend fun importRows(rows: List<WorkoutExport.Row>): Int {
        rows.forEach { r ->
            dao.upsert(
                WorkoutEntry(
                    id = 0,
                    type = r.type,
                    durationMinutes = r.durationMinutes,
                    intensity = r.intensity,
                    note = r.note,
                    date = if (r.date > 0) r.date else now(),
                    createdAt = now(),
                    updatedAt = now()
                )
            )
        }
        return rows.size
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        @Volatile
        private var INSTANCE: WorkoutRepository? = null

        fun get(context: Context): WorkoutRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WorkoutRepository(AppDatabase.get(context).workoutDao())
                    .also { INSTANCE = it }
            }
    }
}
