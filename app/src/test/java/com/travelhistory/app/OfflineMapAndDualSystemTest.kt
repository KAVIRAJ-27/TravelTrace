package com.travelhistory.app

import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.map.offline.OfflineMapRegion
import com.travelhistory.app.data.map.offline.OfflineRegionStatus
import com.travelhistory.app.data.map.offline.PREDEFINED_OFFLINE_REGIONS
import com.travelhistory.app.ui.map.MapUiState
import com.travelhistory.app.ui.map.MapViewModel
import com.travelhistory.app.ui.map.offline.OfflineMapsUiState
import com.travelhistory.app.ui.map.provider.MapProviderType
import com.travelhistory.app.ui.map.provider.MapSelectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapAndDualSystemTest {

    @Test
    fun testDualMapProviderResolutionAutoOnlineWithKey() {
        val provider = MapViewModel.resolveActiveProvider(
            mode = MapSelectionMode.AUTO,
            isOnline = true,
            isApiKeyConfigured = true
        )
        assertEquals(MapProviderType.GOOGLE_MAPS, provider)
    }

    @Test
    fun testDualMapProviderResolutionAutoOffline() {
        val provider = MapViewModel.resolveActiveProvider(
            mode = MapSelectionMode.AUTO,
            isOnline = false,
            isApiKeyConfigured = true
        )
        assertEquals(MapProviderType.MAPLIBRE_OFFLINE, provider)
    }

    @Test
    fun testDualMapProviderResolutionAutoMissingApiKeyFallsBackToMapLibre() {
        // When online but Google Maps API key is blank/missing, MUST fall back to MapLibre without crashing
        val provider = MapViewModel.resolveActiveProvider(
            mode = MapSelectionMode.AUTO,
            isOnline = true,
            isApiKeyConfigured = false
        )
        assertEquals(MapProviderType.MAPLIBRE_OFFLINE, provider)
    }

    @Test
    fun testDualMapProviderResolutionForceModes() {
        assertEquals(
            MapProviderType.GOOGLE_MAPS,
            MapViewModel.resolveActiveProvider(
                mode = MapSelectionMode.FORCE_GOOGLE_MAPS,
                isOnline = false,
                isApiKeyConfigured = false
            )
        )

        assertEquals(
            MapProviderType.MAPLIBRE_OFFLINE,
            MapViewModel.resolveActiveProvider(
                mode = MapSelectionMode.FORCE_MAPLIBRE,
                isOnline = true,
                isApiKeyConfigured = true
            )
        )
    }

    @Test
    fun testPredefinedRegionsCatalog() {
        assertTrue("Catalog must contain regions", PREDEFINED_OFFLINE_REGIONS.isNotEmpty())

        val tamilNadu = PREDEFINED_OFFLINE_REGIONS.find { it.name.contains("Tamil Nadu") }
        assertNotNull("Tamil Nadu must be present in catalog", tamilNadu)
        assertTrue(tamilNadu!!.minLatitude < tamilNadu.maxLatitude)
        assertTrue(tamilNadu.minLongitude < tamilNadu.maxLongitude)
        assertTrue(tamilNadu.sizeBytes > 0)
        assertEquals("245 MB", tamilNadu.formattedSize)

        val karnataka = PREDEFINED_OFFLINE_REGIONS.find { it.name.contains("Karnataka") }
        assertNotNull("Karnataka must be present in catalog", karnataka)
        assertEquals("210 MB", karnataka!!.formattedSize)

        val kerala = PREDEFINED_OFFLINE_REGIONS.find { it.name.contains("Kerala") }
        assertNotNull("Kerala must be present in catalog", kerala)
        assertEquals("160 MB", kerala!!.formattedSize)
    }

    @Test
    fun testOfflineMapRegionSizeFormatting() {
        val emptyRegion = OfflineMapRegion(
            id = 1, name = "Empty", description = "",
            minLatitude = 0.0, minLongitude = 0.0, maxLatitude = 1.0, maxLongitude = 1.0,
            sizeBytes = 0L
        )
        assertEquals("--", emptyRegion.formattedSize)

        val mbRegion = OfflineMapRegion(
            id = 2, name = "MB Region", description = "",
            minLatitude = 0.0, minLongitude = 0.0, maxLatitude = 1.0, maxLongitude = 1.0,
            sizeBytes = 100 * 1024 * 1024L
        )
        assertEquals("100 MB", mbRegion.formattedSize)

        val gbRegion = OfflineMapRegion(
            id = 3, name = "GB Region", description = "",
            minLatitude = 0.0, minLongitude = 0.0, maxLatitude = 1.0, maxLongitude = 1.0,
            sizeBytes = (1.5 * 1024 * 1024 * 1024L).toLong()
        )
        assertEquals("1.50 GB", gbRegion.formattedSize)
    }

    @Test
    fun testOfflineMapsUiStateStorageCalculation() {
        val r1 = OfflineMapRegion(
            id = 1, name = "R1", description = "",
            minLatitude = 0.0, minLongitude = 0.0, maxLatitude = 1.0, maxLongitude = 1.0,
            sizeBytes = 245 * 1024 * 1024L,
            status = OfflineRegionStatus.DOWNLOADED
        )
        val r2 = OfflineMapRegion(
            id = 2, name = "R2", description = "",
            minLatitude = 0.0, minLongitude = 0.0, maxLatitude = 1.0, maxLongitude = 1.0,
            sizeBytes = 160 * 1024 * 1024L,
            status = OfflineRegionStatus.DOWNLOADED
        )

        val state = OfflineMapsUiState(
            downloadedRegions = listOf(r1, r2),
            totalStorageBytes = r1.sizeBytes + r2.sizeBytes
        )

        assertTrue(state.hasDownloadedRegions)
        assertEquals("405 MB", state.totalStorageFormatted)
    }

    @Test
    fun testTripRouteFilteringVsDateBasedRouteFiltering() {
        val t0 = 1726400000000L
        val p1 = LocationRecord(id = 1, latitude = 11.0, longitude = 77.0, accuracy = 5f, timestamp = t0)
        val p2 = LocationRecord(id = 2, latitude = 11.1, longitude = 77.1, accuracy = 5f, timestamp = t0 + 600_000L) // in trip
        val p3 = LocationRecord(id = 3, latitude = 11.2, longitude = 77.2, accuracy = 5f, timestamp = t0 + 1200_000L) // in trip
        val p4 = LocationRecord(id = 4, latitude = 11.3, longitude = 77.3, accuracy = 5f, timestamp = t0 + 5000_000L) // outside trip

        val allRecords = listOf(p1, p2, p3, p4)

        val trip = TripRecord(
            id = 10,
            startTime = t0 + 500_000L,
            endTime = t0 + 1500_000L,
            startLatitude = 11.1, startLongitude = 77.1,
            endLatitude = 11.2, endLongitude = 77.2,
            distanceMeters = 15000.0,
            durationSeconds = 1000L,
            pointCount = 2
        )

        // Filter points for specific trip
        val tripPoints = allRecords.filter { it.timestamp in trip.startTime..trip.endTime }
        assertEquals(2, tripPoints.size)
        assertEquals(p2.id, tripPoints[0].id)
        assertEquals(p3.id, tripPoints[1].id)

        // Date-based viewing contains all 4 points
        assertEquals(4, allRecords.size)
    }

    @Test
    fun testMapUiStateTripRouteFlag() {
        val stateDefault = MapUiState()
        assertFalse(stateDefault.isTripRouteActive)

        val stateWithTrip = MapUiState(selectedTripId = 10L)
        assertTrue(stateWithTrip.isTripRouteActive)
    }
}
