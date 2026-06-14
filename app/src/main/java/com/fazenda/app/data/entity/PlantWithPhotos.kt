package com.fazenda.app.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlantWithPhotos(
    @Embedded val plant: PlantEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "plantId",
        entity = PlantPhotoEntity::class
    )
    val photos: List<PlantPhotoEntity>
) {
    /**
     * Повертає всі шляхи до фото:
     * - Спочатку photoPath рослини (головне фото)
     * - Потім усі фото з таблиці plant_photos (відфільтровуємо дублікат головного)
     */
    fun allPhotoPathsSorted(): List<String> {
        val result = mutableListOf<String>()
        plant.photoPath?.let { result.add(it) }
        val sortedExtra = photos
            .filter { it.photoPath != null && it.photoPath != plant.photoPath }
            .sortedBy { it.order }
        sortedExtra.forEach { it.photoPath?.let { p -> result.add(p) } }
        return result
    }

    val totalPhotoCount: Int
        get() = allPhotoPathsSorted().size
}
