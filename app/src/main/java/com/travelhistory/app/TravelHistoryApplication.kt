package com.travelhistory.app

import android.app.Application
import com.travelhistory.app.data.db.AppDatabase
import com.travelhistory.app.data.network.NetworkMonitor
import com.travelhistory.app.data.repository.LocationRepository
import org.maplibre.android.MapLibre

/**
 * Custom Application class that initializes the local Room database,
 * MapLibre Native SDK, and NetworkMonitor.
 */
class TravelHistoryApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val locationRepository: LocationRepository by lazy {
        LocationRepository(database.locationRecordDao(), database.tripRecordDao())
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre Native Android SDK
        MapLibre.getInstance(this)
    }
}
