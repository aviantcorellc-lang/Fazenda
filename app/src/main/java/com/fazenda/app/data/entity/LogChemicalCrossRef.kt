package com.fazenda.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "log_chemicals",
    primaryKeys = ["logId", "chemicalId"],
    foreignKeys = [
        ForeignKey(
            entity = LogEntity::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChemicalEntity::class,
            parentColumns = ["id"],
            childColumns = ["chemicalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chemicalId")]
)
data class LogChemicalCrossRef(
    val logId: Long,
    val chemicalId: Long
)
