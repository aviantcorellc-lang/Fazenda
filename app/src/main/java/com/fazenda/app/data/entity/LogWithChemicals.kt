package com.fazenda.app.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class LogWithChemicals(
    @Embedded val log: LogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = LogChemicalCrossRef::class,
            parentColumn = "logId",
            entityColumn = "chemicalId"
        )
    )
    val chemicals: List<ChemicalEntity>
)
