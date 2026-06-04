package com.fazenda.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("plantId")]
)
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val plantId: Long? = null,
    val actionType: String,  // Примітка, Підживлення, Обприскування, Заміна, Інше
    val photoPath: String? = null,
    val comment: String? = null,
    val aiDiagnosis: String? = null
)

object LogActionTypes {
    const val NOTE = "Примітка"
    const val FEEDING = "Підживлення"
    const val SPRAYING = "Обприскування"
    const val REPLACEMENT = "Заміна"
    const val OTHER = "Інше"
    const val PLANT_ADDED = "Додано рослину"
    const val PLANT_DELETED = "Видалено рослину"

    val ALL = listOf(NOTE, FEEDING, SPRAYING, REPLACEMENT, OTHER, PLANT_ADDED, PLANT_DELETED)
}
