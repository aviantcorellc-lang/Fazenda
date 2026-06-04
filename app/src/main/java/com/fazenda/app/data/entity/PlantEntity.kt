package com.fazenda.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null,
    val name: String,
    val zoneId: Long? = null,
    val row: Float? = null,
    val position: Float? = null,
    val comment: String? = null,
    val photoPath: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
