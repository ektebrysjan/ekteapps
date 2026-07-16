package com.ektebrysjan.steps.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day. [date] is an ISO "yyyy-MM-dd" string (primary key),
 * [steps] is the number of steps recorded on that day.
 */
@Entity(tableName = "daily_steps")
data class DailyStep(
    @PrimaryKey val date: String,
    val steps: Int
)
