package com.fazenda.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chemicals")
data class ChemicalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val purpose: String,
    val waitingPeriodDays: Int = 0
)
