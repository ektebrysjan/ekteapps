package com.ektebrysjan.notes.data

import android.content.Context
import com.ektebrysjan.notes.util.PinManager
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for notes and the private-notes PIN.
 */
class NoteRepository private constructor(
    private val dao: NoteDao,
    val pin: PinManager
) {
    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    suspend fun getById(id: Long): Note? = dao.getById(id)

    suspend fun getAllOnce(): List<Note> = dao.getAllOnce()

    /** Insert or update a note; returns its row id. */
    suspend fun save(note: Note): Long = dao.upsert(note)

    suspend fun delete(note: Note) = dao.delete(note)

    /** Bulk insert imported notes as fresh rows (ids are assigned by Room). */
    suspend fun importNotes(notes: List<Note>) {
        notes.forEach { dao.upsert(it.copy(id = 0)) }
    }

    companion object {
        @Volatile
        private var INSTANCE: NoteRepository? = null

        fun get(context: Context): NoteRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteRepository(
                    AppDatabase.get(context).noteDao(),
                    PinManager(context.applicationContext)
                ).also { INSTANCE = it }
            }
    }
}
