package com.travelhistory.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database for Travel History.
 * Operates purely on local SQLite storage on the device.
 */
@Database(
    entities = [
        LocationRecord::class,
        TripRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationRecordDao(): LocationRecordDao
    abstract fun tripRecordDao(): TripRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trips` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `startLatitude` REAL NOT NULL,
                        `startLongitude` REAL NOT NULL,
                        `endLatitude` REAL NOT NULL,
                        `endLongitude` REAL NOT NULL,
                        `distanceMeters` REAL NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `pointCount` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trips_startTime` ON `trips` (`startTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trips_endTime` ON `trips` (`endTime`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_history.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
