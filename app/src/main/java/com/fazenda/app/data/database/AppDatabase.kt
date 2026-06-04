package com.fazenda.app.data.database

import android.content.Context
import android.content.res.Resources
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fazenda.app.R
import com.fazenda.app.data.dao.ChemicalDao
import com.fazenda.app.data.dao.LogDao
import com.fazenda.app.data.dao.PlantDao
import com.fazenda.app.data.dao.PlantPhotoDao
import com.fazenda.app.data.dao.ScheduleDao
import com.fazenda.app.data.dao.ZoneDao
import com.fazenda.app.data.entity.ChemicalEntity
import com.fazenda.app.data.entity.LogEntity
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.data.entity.PlantPhotoEntity
import com.fazenda.app.data.entity.ScheduleEntity
import com.fazenda.app.data.entity.ZoneEntity
import com.fazenda.app.data.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        PlantEntity::class,
        PlantPhotoEntity::class,
        ChemicalEntity::class,
        ScheduleEntity::class,
        LogEntity::class,
        ZoneEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun plantPhotoDao(): PlantPhotoDao
    abstract fun chemicalDao(): ChemicalDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun logDao(): LogDao
    abstract fun zoneDao(): ZoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "fazenda_db")
                .fallbackToDestructiveMigration()
                .addCallback(SeedDatabaseCallback(context))
                .build()
        }
    }

    private class SeedDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        private val photoExtensions = mapOf(
            "1" to "png", "2" to "jpg", "3" to "png", "4" to "jpg",
            "5" to "png", "6" to "jpg", "7" to "png", "8" to "jpg",
            "9" to "jpg", "10" to "png", "11" to "jpg", "12" to "png",
            "13" to "png", "14" to "jpg", "15" to "png", "16" to "png",
            "17" to "png", "18" to "jpg", "19" to "png", "20" to "png",
            "21" to "png", "22" to "png", "23" to "jpg", "24" to "jpg",
            "25" to "png", "26" to "jpg"
        )

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            runBlocking(Dispatchers.IO) {
                seedDataWithDb(db)
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            runBlocking(Dispatchers.IO) {
                seedDataWithDb(db)
            }
        }

        private suspend fun seedDataWithDb(db: SupportSQLiteDatabase) {
            try {
                if (hasAnySeedData(db)) return

                db.beginTransaction()
                try {
                    seedZonesFromRawDb(context, db)
                    seedPlantsFromRawDb(context, db)
                    seedChemicalsFromRawDb(context, db)
                    seedSchedulesFromRawDb(context, db)
                    seedLogsFromRawDb(context, db)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun hasAnySeedData(db: SupportSQLiteDatabase): Boolean {
            val tables = listOf("plants", "plant_photos", "chemicals", "schedules", "logs", "zones")
            return tables.any { table ->
                db.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    cursor.moveToFirst()
                    cursor.getLong(0) > 0
                }
            }
        }

        private fun normalizePhotoPath(raw: String?): String? {
            val value = raw?.trim().orEmpty()
            if (value.isBlank()) return null
            return when {
                value.startsWith("assets/") -> value
                value.startsWith("image") -> "assets/images/$value"
                value.all { it.isDigit() } -> {
                    val ext = photoExtensions[value] ?: return null
                    "assets/images/image${value}.${ext}"
                }
                value.startsWith("photos/") -> value
                value.startsWith("file://") || value.startsWith("content://") -> value
                else -> value
            }
        }

        private fun insertPlant(
            db: SupportSQLiteDatabase,
            category: String,
            name: String,
            zoneId: Long?,
            row: Float?,
            position: Float?,
            comment: String?,
            photoPath: String?,
            latitude: Double?,
            longitude: Double?
        ): Long {
            db.execSQL(
                """
                INSERT INTO plants (category, name, zoneId, row, position, comment, photoPath, latitude, longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(category, name, zoneId, row, position, comment, photoPath, latitude, longitude)
            )

            db.query("SELECT id FROM plants ORDER BY id DESC LIMIT 1").use { cursor ->
                cursor.moveToFirst()
                return cursor.getLong(0)
            }
        }

        private fun insertPlantPhoto(
            db: SupportSQLiteDatabase,
            plantId: Long,
            photoPath: String,
            order: Int
        ) {
            db.execSQL(
                """
                INSERT INTO plant_photos (plantId, photoPath, `order`)
                VALUES (?, ?, ?)
                """.trimIndent(),
                arrayOf(plantId, photoPath, order)
            )
        }

        private suspend fun seedZonesFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            val zoneNames = mutableSetOf<String>()
            context.resources.openRawResource(R.raw.plants).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 4) {
                                val location = parts[3].trim()
                                if (location.isNotBlank()) {
                                    zoneNames.add(location)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            val sortedZones = zoneNames.sorted()
            for (name in sortedZones) {
                db.execSQL("INSERT INTO zones (name) VALUES (?)", arrayOf(name))
            }
        }

        private suspend fun seedPlantsFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            val zoneMap = mutableMapOf<String, Long>()
            db.query("SELECT id, name FROM zones").use { cursor ->
                while (cursor.moveToNext()) {
                    zoneMap[cursor.getString(1)] = cursor.getLong(0)
                }
            }

            context.resources.openRawResource(R.raw.plants).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            when {
                                parts.size >= 12 -> {
                                    val category = parts[1]
                                    val name = parts[2]
                                    val location = parts[3].trim()
                                    val row = parts[4].replace(',', '.').toFloatOrNull()
                                    val position = parts[5].replace(',', '.').toFloatOrNull()
                                    val comment = parts[7].ifBlank { null }
                                    val photoId1 = parts[8].ifBlank { null }
                                    val photoId2 = parts[9].ifBlank { null }
                                    val latitude = parts[10].replace(',', '.').toDoubleOrNull()
                                    val longitude = parts[11].replace(',', '.').toDoubleOrNull()
                                    val zoneId = zoneMap[location]

                                    val plantId = insertPlant(
                                        db = db,
                                        category = category,
                                        name = name,
                                        zoneId = zoneId,
                                        row = row,
                                        position = position,
                                        comment = comment,
                                        photoPath = normalizePhotoPath(photoId1),
                                        latitude = latitude,
                                        longitude = longitude
                                    )

                                    var order = 0
                                    normalizePhotoPath(photoId1)?.let { insertPlantPhoto(db, plantId, it, order++) }
                                    normalizePhotoPath(photoId2)?.let { insertPlantPhoto(db, plantId, it, order++) }
                                }

                                parts.size >= 6 -> {
                                    val category = parts[1]
                                    val name = parts[2]
                                    val location = parts[3].trim()
                                    val photoPath = normalizePhotoPath(parts[5])
                                    val row = extractNumber(location, "Ряд")
                                    val position = extractNumber(location, "Номер")
                                    val zoneId = if (location.isNotBlank()) zoneMap[location] else null

                                    val plantId = insertPlant(
                                        db = db,
                                        category = category,
                                        name = name,
                                        zoneId = zoneId,
                                        row = row,
                                        position = position,
                                        comment = null,
                                        photoPath = photoPath,
                                        latitude = null,
                                        longitude = null
                                    )

                                    photoPath?.let { insertPlantPhoto(db, plantId, it, 0) }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        private fun extractNumber(location: String?, label: String): Float? {
            if (location.isNullOrBlank()) return null
            val regex = Regex("""$label\s*([0-9]+(?:[.,][0-9]+)?)""")
            val match = regex.find(location) ?: return null
            return match.groupValues[1].replace(',', '.').toFloatOrNull()
        }

        private suspend fun seedChemicalsFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            context.resources.openRawResource(R.raw.chemicals).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 4) {
                                db.execSQL(
                                    "INSERT INTO chemicals (name, purpose, waitingPeriodDays) VALUES (?, ?, ?)",
                                    arrayOf(parts[1], parts[2], parts[3].toIntOrNull() ?: 0)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        private suspend fun seedSchedulesFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            context.resources.openRawResource(R.raw.schedules).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 4) {
                                db.execSQL(
                                    "INSERT INTO schedules (phaseTime, targetCategory, recipe) VALUES (?, ?, ?)",
                                    arrayOf(parts[1], parts[2], parts[3])
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        private suspend fun seedLogsFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            context.resources.openRawResource(R.raw.logs).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 7) {
                                val date = LocalDateTime.parse(
                                    parts[1],
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                val plantId = parts[2].toLongOrNull()
                                val actionType = parts[3]
                                val photoPath = normalizePhotoPath(parts[4])
                                val comment = parts[5].ifBlank { null }
                                val aiDiagnosis = parts[6].ifBlank { null }

                                db.execSQL(
                                    """
                                    INSERT INTO logs (date, plantId, actionType, photoPath, comment, aiDiagnosis)
                                    VALUES (?, ?, ?, ?, ?, ?)
                                    """.trimIndent(),
                                    arrayOf(date, plantId, actionType, photoPath, comment, aiDiagnosis)
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
