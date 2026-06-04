package com.fazenda.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phaseTime: String,
    val targetCategory: String,
    val recipe: String,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val isCompleted: Boolean = false
)
