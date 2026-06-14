package com.fazenda.app.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fazenda.app.R
import com.fazenda.app.data.dao.CategoryDao
import com.fazenda.app.data.dao.ChemicalDao
import com.fazenda.app.data.dao.LogDao
import com.fazenda.app.data.dao.PlantDao
import com.fazenda.app.data.dao.PlantPhotoDao
import com.fazenda.app.data.dao.ScheduleDao
import com.fazenda.app.data.dao.ZoneDao
import com.fazenda.app.data.entity.CategoryEntity
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
import com.fazenda.app.data.entity.LogChemicalCrossRef

@Database(
    entities = [
        PlantEntity::class,
        PlantPhotoEntity::class,
        ChemicalEntity::class,
        ScheduleEntity::class,
        LogEntity::class,
        ZoneEntity::class,
        CategoryEntity::class,
        LogChemicalCrossRef::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun plantPhotoDao(): PlantPhotoDao
    abstract fun chemicalDao(): ChemicalDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun logDao(): LogDao
    abstract fun zoneDao(): ZoneDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // plants
                db.execSQL("ALTER TABLE plants RENAME TO plants_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `plants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `categoryId` INTEGER, 
                        `name` TEXT NOT NULL, 
                        `zoneId` INTEGER, 
                        `row` REAL, 
                        `position` REAL, 
                        `comment` TEXT, 
                        `photoPath` TEXT, 
                        `latitude` REAL, 
                        `longitude` REAL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`zoneId`) REFERENCES `zones`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO plants (id, categoryId, name, zoneId, row, position, comment, photoPath, latitude, longitude)
                    SELECT id, categoryId, name, zoneId, row, position, comment, photoPath, latitude, longitude FROM plants_old
                """.trimIndent())
                db.execSQL("DROP TABLE plants_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plants_categoryId` ON `plants` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plants_zoneId` ON `plants` (`zoneId`)")

                // logs
                db.execSQL("ALTER TABLE logs RENAME TO logs_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `plantId` INTEGER, 
                        `chemicalId` INTEGER, 
                        `actionType` TEXT NOT NULL, 
                        `photoPath` TEXT, 
                        `comment` TEXT, 
                        `aiDiagnosis` TEXT,
                        FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`chemicalId`) REFERENCES `chemicals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO logs (id, date, plantId, chemicalId, actionType, photoPath, comment, aiDiagnosis)
                    SELECT id, date, plantId, NULL, actionType, photoPath, comment, aiDiagnosis FROM logs_old
                """.trimIndent())
                db.execSQL("DROP TABLE logs_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_plantId` ON `logs` (`plantId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_chemicalId` ON `logs` (`chemicalId`)")

                // schedules
                db.execSQL("ALTER TABLE schedules RENAME TO schedules_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `phaseTime` TEXT NOT NULL, 
                        `categoryId` INTEGER, 
                        `recipe` TEXT NOT NULL, 
                        `startDate` INTEGER NOT NULL, 
                        `endDate` INTEGER NOT NULL, 
                        `isCompleted` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO schedules (id, phaseTime, categoryId, recipe, startDate, endDate, isCompleted)
                    SELECT s.id, s.phaseTime, c.id, s.recipe, s.startDate, s.endDate, s.isCompleted 
                    FROM schedules_old s
                    LEFT JOIN categories c ON c.name = s.targetCategory
                """.trimIndent())
                db.execSQL("DROP TABLE schedules_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedules_categoryId` ON `schedules` (`categoryId`)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create log_chemicals cross-reference table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `log_chemicals` (
                        `logId` INTEGER NOT NULL, 
                        `chemicalId` INTEGER NOT NULL, 
                        PRIMARY KEY(`logId`, `chemicalId`), 
                        FOREIGN KEY(`logId`) REFERENCES `logs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`chemicalId`) REFERENCES `chemicals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_log_chemicals_chemicalId` ON `log_chemicals` (`chemicalId`)")

                // 2. Add chemicalGroup to chemicals table
                db.execSQL("ALTER TABLE `chemicals` ADD COLUMN `chemicalGroup` TEXT NOT NULL DEFAULT 'Інше'")

                // 3. Migrate logs table: drop chemicalId and add zoneId, categoryId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` INTEGER NOT NULL, 
                        `plantId` INTEGER, 
                        `zoneId` INTEGER, 
                        `categoryId` INTEGER, 
                        `actionType` TEXT NOT NULL, 
                        `photoPath` TEXT, 
                        `comment` TEXT, 
                        `aiDiagnosis` TEXT,
                        FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`zoneId`) REFERENCES `zones`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT INTO logs_new (id, date, plantId, zoneId, categoryId, actionType, photoPath, comment, aiDiagnosis)
                    SELECT id, date, plantId, NULL, NULL, actionType, photoPath, comment, aiDiagnosis FROM logs
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO log_chemicals (logId, chemicalId)
                    SELECT id, chemicalId FROM logs WHERE chemicalId IS NOT NULL
                """.trimIndent())

                db.execSQL("DROP TABLE logs")
                db.execSQL("ALTER TABLE logs_new RENAME TO logs")
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_plantId` ON `logs` (`plantId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_zoneId` ON `logs` (`zoneId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_categoryId` ON `logs` (`categoryId`)")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "fazenda_db")
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration(false)
                .addCallback(SeedDatabaseCallback(context))
                .build()
        }
    }

    private class SeedDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {

        private val photoExtensions = mapOf(
            "10" to "png",
            "12" to "png"
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
                syncMissingPlantPhotos(db)
            }
        }

        private fun syncMissingPlantPhotos(db: SupportSQLiteDatabase) {
            try {
                db.execSQL(
                    """
                    INSERT INTO plant_photos (plantId, photoPath, `order`)
                    SELECT id, photoPath, 0 FROM plants
                    WHERE photoPath IS NOT NULL 
                      AND photoPath != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM plant_photos 
                          WHERE plant_photos.plantId = plants.id 
                            AND plant_photos.photoPath = plants.photoPath
                      )
                    """.trimIndent()
                )
                Log.i(TAG, "Successfully synced missing plant photos to plant_photos table")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync missing plant photos", e)
            }
        }

        private suspend fun seedDataWithDb(db: SupportSQLiteDatabase) {
            try {
                if (hasAnySeedData(db)) return

                db.beginTransaction()
                try {
                    seedZonesFromRawDb(context, db)
                    seedCategoriesFromRawDb(context, db)
                    seedPlantsFromRawDb(context, db)
                    seedChemicalsFromRawDb(context, db)
                    seedSchedulesFromRawDb(context, db)
                    seedLogsFromRawDb(context, db)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed database", e)
            }
        }

        private fun hasAnySeedData(db: SupportSQLiteDatabase): Boolean {
            val tables = listOf("plants", "plant_photos", "chemicals", "schedules", "logs", "zones", "categories")
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
            categoryId: Long?,
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
                INSERT INTO plants (categoryId, name, zoneId, row, position, comment, photoPath, latitude, longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(categoryId, name, zoneId, row, position, comment, photoPath, latitude, longitude)
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

        private suspend fun seedCategoriesFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            val categoryNames = mutableSetOf<String>()
            context.resources.openRawResource(R.raw.plants).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 3) {
                                val category = parts[1].trim()
                                if (category.isNotBlank()) {
                                    categoryNames.add(category)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            val sortedCategories = categoryNames.sorted()
            for (name in sortedCategories) {
                db.execSQL("INSERT OR IGNORE INTO categories (name) VALUES (?)", arrayOf(name))
            }
        }

        private suspend fun seedPlantsFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            val zoneMap = mutableMapOf<String, Long>()
            db.query("SELECT id, name FROM zones").use { cursor ->
                while (cursor.moveToNext()) {
                    zoneMap[cursor.getString(1)] = cursor.getLong(0)
                }
            }
            val categoryMap = mutableMapOf<String, Long>()
            db.query("SELECT id, name FROM categories").use { cursor ->
                while (cursor.moveToNext()) {
                    categoryMap[cursor.getString(1)] = cursor.getLong(0)
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
                                    val categoryId = categoryMap[category]

                                    val plantId = insertPlant(
                                        db = db,
                                        categoryId = categoryId,
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
                                    val categoryId = categoryMap[category]

                                    val plantId = insertPlant(
                                        db = db,
                                        categoryId = categoryId,
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
                            Log.e(TAG, "Failed to seed plant", e)
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
                            Log.e(TAG, "Failed to seed plant", e)
                        }
                    }
                }
            }
        }

        private suspend fun seedSchedulesFromRawDb(context: Context, db: SupportSQLiteDatabase) {
            val categoryMap = mutableMapOf<String, Long>()
            db.query("SELECT id, name FROM categories").use { cursor ->
                while (cursor.moveToNext()) {
                    categoryMap[cursor.getString(1)] = cursor.getLong(0)
                }
            }

            context.resources.openRawResource(R.raw.schedules).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLines().filter { it.isNotBlank() }.drop(1).forEach { line ->
                        try {
                            val parts = CsvParser.parseLine(line)
                            if (parts.size >= 4) {
                                val categoryName = parts[2].trim()
                                val categoryId = categoryMap[categoryName]
                                db.execSQL(
                                    "INSERT INTO schedules (phaseTime, categoryId, recipe, startDate, endDate, isCompleted) VALUES (?, ?, ?, 0, 0, 0)",
                                    arrayOf(parts[1], categoryId, parts[3])
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to seed schedule", e)
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
                            Log.e(TAG, "Failed to seed plant", e)
                        }
                    }
                }
            }
        }
    }
}
