package com.ektebrysjan.workout.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single logged workout.
 *
 * [type] is a free-text label (the editor offers common suggestions). [durationMinutes] is the
 * length in whole minutes. [intensity] is one of [WorkoutMeta.INTENSITIES] ("Low"/"Medium"/"High").
 * [date] is epoch millis for the day the workout happened (used for the weekly/monthly summaries).
 */
@Entity(tableName = "workouts")
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val durationMinutes: Int,
    val intensity: String,
    val note: String = "",
    val date: Long,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/** Static suggestions shared by the editor and used as sensible defaults. */
object WorkoutMeta {
    val TYPES = listOf("Run", "Walk", "Cycling", "Strength", "Yoga", "Swim", "HIIT", "Other")
    const val LOW = "Low"
    const val MEDIUM = "Medium"
    const val HIGH = "High"
    val INTENSITIES = listOf(LOW, MEDIUM, HIGH)
}
