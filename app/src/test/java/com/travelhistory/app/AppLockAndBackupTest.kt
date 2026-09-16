package com.travelhistory.app

import com.travelhistory.app.data.backup.BackupCrypto
import com.travelhistory.app.data.backup.BackupLocationItem
import com.travelhistory.app.data.backup.BackupManager
import com.travelhistory.app.data.backup.BackupPayload
import com.travelhistory.app.data.backup.BackupSettingsItem
import com.travelhistory.app.data.backup.BackupTripItem
import com.travelhistory.app.data.backup.BackupValidationResult
import com.travelhistory.app.data.backup.CURRENT_BACKUP_VERSION
import com.travelhistory.app.data.backup.RestoreResult
import com.travelhistory.app.data.backup.RestoreStrategy
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.LocationRecordDao
import com.travelhistory.app.data.db.TripRecord
import com.travelhistory.app.data.db.TripRecordDao
import com.travelhistory.app.data.repository.LocationRepository
import com.travelhistory.app.security.AppLockManager
import com.travelhistory.app.security.BiometricAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit test suite verifying:
 * - App Lock settings and biometric availability handling
 * - Backup JSON serialization & deserialization
 * - Version compatibility & invalid backup handling
 * - AES-256-GCM encryption & decryption
 * - Duplicate detection and Merge vs Replace restoration
 * - Empty & corrupted file validation
 */
class AppLockAndBackupTest {

    // -------------------------------------------------------------
    // 1. Biometric Availability & App Lock
    // -------------------------------------------------------------

    @Test
    fun testBiometricAvailabilityResponses() {
        val available: BiometricAvailability = BiometricAvailability.Available
        assertEquals(BiometricAvailability.Available, available)

        val noHw = BiometricAvailability.Unavailable("This device does not have biometric hardware.")
        assertTrue(noHw.reason.contains("hardware"))

        val noneEnrolled = BiometricAvailability.Unavailable("No biometric credentials are enrolled.")
        assertTrue(noneEnrolled.reason.contains("enrolled"))
    }

    @Test
    fun testAppLockManagerSessionState() {
        // Test lock and unlock session transitions
        AppLockManager.unlock()
        assertFalse(AppLockManager.isAppLocked.value)

        // Lock suppression test
        AppLockManager.temporarilySuppressLock(5000L)
        // Ensure unlock works cleanly
        AppLockManager.unlock()
        assertFalse(AppLockManager.isAppLocked.value)
    }

    // -------------------------------------------------------------
    // 2. Backup Payload Serialization & Deserialization
    // -------------------------------------------------------------

    @Test
    fun testBackupPayloadJsonRoundTrip() {
        val payload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = 1726410000000L,
            locations = listOf(
                BackupLocationItem(latitude = 11.234567, longitude = 77.123456, accuracy = 8.0f, timestamp = 1726400000000L),
                BackupLocationItem(latitude = 11.236789, longitude = 77.125678, accuracy = 7.5f, timestamp = 1726400060000L)
            ),
            trips = listOf(
                BackupTripItem(
                    startTime = 1726400000000L,
                    endTime = 1726400060000L,
                    startLatitude = 11.234567,
                    startLongitude = 77.123456,
                    endLatitude = 11.236789,
                    endLongitude = 77.125678,
                    distanceMeters = 345.5,
                    durationSeconds = 60L,
                    pointCount = 2
                )
            ),
            settings = BackupSettingsItem(
                intervalMinutes = 5,
                isCustomInterval = false,
                resumeAfterReboot = true
            )
        )

        val jsonString = BackupManager.serializeToJson(payload)
        val deserialized = BackupManager.deserializeFromJson(jsonString)

        assertEquals(payload.backupVersion, deserialized.backupVersion)
        assertEquals(payload.createdAt, deserialized.createdAt)
        assertEquals(2, deserialized.locations.size)
        assertEquals(1, deserialized.trips.size)

        // Verify location record precision
        val loc1 = deserialized.locations[0]
        assertEquals(11.234567, loc1.latitude, 0.000001)
        assertEquals(77.123456, loc1.longitude, 0.000001)
        assertEquals(8.0f, loc1.accuracy, 0.01f)
        assertEquals(1726400000000L, loc1.timestamp)

        // Verify trip record
        val trip1 = deserialized.trips[0]
        assertEquals(345.5, trip1.distanceMeters, 0.01)
        assertEquals(60L, trip1.durationSeconds)
        assertEquals(2, trip1.pointCount)

        // Verify settings
        assertNotNull(deserialized.settings)
        assertEquals(5, deserialized.settings?.intervalMinutes)
        assertFalse(deserialized.settings!!.isCustomInterval)
        assertTrue(deserialized.settings!!.resumeAfterReboot)
    }

    // -------------------------------------------------------------
    // 3. Version Compatibility & Validation
    // -------------------------------------------------------------

    @Test
    fun testBackupVersionValidation() {
        val validPayload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = 1726410000000L,
            locations = emptyList(),
            trips = emptyList()
        )
        val validJson = BackupManager.serializeToJson(validPayload)
        val result = BackupManager.validateBackup(validJson.toByteArray(Charsets.UTF_8))
        assertTrue("Expected valid backup result", result is BackupValidationResult.Valid)

        // Test unsupported future version
        val futureJson = validJson.replace("\"backupVersion\": 1", "\"backupVersion\": 999")
        val futureResult = BackupManager.validateBackup(futureJson.toByteArray(Charsets.UTF_8))
        assertTrue("Expected invalid result for future backup version", futureResult is BackupValidationResult.Invalid)
        assertTrue((futureResult as BackupValidationResult.Invalid).reason.contains("Unsupported backup version"))
    }

    @Test
    fun testEmptyAndCorruptedBackupValidation() {
        // Empty bytes
        val emptyResult = BackupManager.validateBackup(ByteArray(0))
        assertTrue(emptyResult is BackupValidationResult.Invalid)
        assertEquals("Backup file is empty.", (emptyResult as BackupValidationResult.Invalid).reason)

        // Garbage non-JSON data
        val garbageBytes = "Not a json file at all".toByteArray(Charsets.UTF_8)
        val garbageResult = BackupManager.validateBackup(garbageBytes)
        assertTrue(garbageResult is BackupValidationResult.Invalid)

        // Missing required 'locations' array
        val missingLocJson = """{"backupVersion":1,"createdAt":1726410000000,"trips":[]}"""
        val missingLocResult = BackupManager.validateBackup(missingLocJson.toByteArray(Charsets.UTF_8))
        assertTrue(missingLocResult is BackupValidationResult.Invalid)
    }

    // -------------------------------------------------------------
    // 4. AES-256-GCM Encryption & Decryption
    // -------------------------------------------------------------

    @Test
    fun testAes256GcmEncryptionAndDecryption() {
        val plainText = """{"testKey":"sensitiveLocationData123"}"""
        val password = "StrongMasterPassword2026!".toCharArray()

        val encryptedBytes = BackupCrypto.encrypt(plainText, password)
        assertTrue(BackupCrypto.isEncrypted(encryptedBytes))
        assertFalse(String(encryptedBytes, Charsets.UTF_8).contains("sensitiveLocationData123"))

        val decryptedText = BackupCrypto.decrypt(encryptedBytes, password)
        assertEquals(plainText, decryptedText)
    }

    @Test
    fun testAes256GcmDecryptionWrongPasswordOrTampered() {
        val plainText = """{"secret":"travelHistory"}"""
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encryptedBytes = BackupCrypto.encrypt(plainText, password)

        // 1. Wrong password must throw SecurityException
        try {
            BackupCrypto.decrypt(encryptedBytes, wrongPassword)
            fail("Decryption with wrong password should fail.")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Incorrect password") == true)
        }

        // 2. Tampered ciphertext must fail authentication tag check
        val tamperedBytes = encryptedBytes.clone()
        tamperedBytes[tamperedBytes.size - 1] = (tamperedBytes[tamperedBytes.size - 1].toInt() xor 0xFF).toByte()
        try {
            BackupCrypto.decrypt(tamperedBytes, password)
            fail("Decryption with tampered ciphertext should fail.")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("corrupted") == true || e.message?.contains("Incorrect") == true)
        }
    }

    @Test
    fun testBackupValidationWithEncryptedFile() {
        val payload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = 1726410000000L,
            locations = listOf(
                BackupLocationItem(11.0, 77.0, 5.0f, 1000L)
            ),
            trips = emptyList()
        )

        val encryptedBytes = BackupManager.serializeBackup(payload, "MySecretKey")

        // Without password -> returns RequiresPassword
        val noPassResult = BackupManager.validateBackup(encryptedBytes)
        assertTrue(noPassResult is BackupValidationResult.RequiresPassword)

        // With wrong password -> returns Invalid
        val wrongPassResult = BackupManager.validateBackup(encryptedBytes, "WrongKey")
        assertTrue(wrongPassResult is BackupValidationResult.Invalid)

        // With correct password -> returns Valid
        val correctPassResult = BackupManager.validateBackup(encryptedBytes, "MySecretKey")
        assertTrue(correctPassResult is BackupValidationResult.Valid)
        val valid = correctPassResult as BackupValidationResult.Valid
        assertTrue(valid.isEncrypted)
        assertEquals(1, valid.locationCount)
    }

    // -------------------------------------------------------------
    // 5. Duplicate Detection & Merge vs Replace Behavior
    // -------------------------------------------------------------

    @Test
    fun testMergeStrategyWithDuplicatePrevention(): Unit = runBlocking {
        val fakeDao = FakeLocationRecordDao()
        val fakeTripDao = FakeTripRecordDao()
        val repository = LocationRepository(fakeDao, fakeTripDao)

        // Pre-populate database with existing records
        val existingLoc1 = LocationRecord(id = 1L, latitude = 11.1, longitude = 77.1, accuracy = 5f, timestamp = 1000L)
        val existingLoc2 = LocationRecord(id = 2L, latitude = 11.2, longitude = 77.2, accuracy = 5f, timestamp = 2000L)
        repository.insertAllRecords(listOf(existingLoc1, existingLoc2))

        val existingTrip1 = TripRecord(
            id = 1L, startTime = 1000L, endTime = 2000L,
            startLatitude = 11.1, startLongitude = 77.1, endLatitude = 11.2, endLongitude = 77.2,
            distanceMeters = 500.0, durationSeconds = 1000L, pointCount = 2
        )
        repository.insertAllTrips(listOf(existingTrip1))

        // Prepare backup payload containing:
        // - Loc with timestamp 2000L (duplicate of existingLoc2)
        // - Loc with timestamp 3000L (new!)
        // - Trip with startTime=1000L, endTime=2000L (duplicate of existingTrip1)
        // - Trip with startTime=2000L, endTime=3000L (new!)
        val backupPayload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = 1726410000000L,
            locations = listOf(
                BackupLocationItem(11.2, 77.2, 5f, 2000L), // duplicate
                BackupLocationItem(11.3, 77.3, 5f, 3000L)  // new
            ),
            trips = listOf(
                BackupTripItem(1000L, 2000L, 11.1, 77.1, 11.2, 77.2, 500.0, 1000L, 2), // duplicate
                BackupTripItem(2000L, 3000L, 11.2, 77.2, 11.3, 77.3, 600.0, 1000L, 2)  // new
            )
        )

        // Execute Restore with MERGE strategy
        val restoreResult = BackupManager.restoreBackup(
            payload = backupPayload,
            strategy = RestoreStrategy.MERGE,
            locationRepository = repository
        )

        assertTrue(restoreResult is RestoreResult.Success)
        val success = restoreResult as RestoreResult.Success
        assertEquals(1, success.locationsRestored)
        assertEquals(1, success.tripsRestored)
        assertEquals(2, success.duplicatesSkipped) // 1 duplicate location + 1 duplicate trip

        // Verify total records in database
        val allLocations = repository.getAllRecordsChronologicalList()
        assertEquals(3, allLocations.size) // 1000, 2000, 3000

        val allTrips = repository.getAllTripsList()
        assertEquals(2, allTrips.size) // trip1 and trip2
    }

    @Test
    fun testReplaceStrategyWipesAndRestores(): Unit = runBlocking {
        val fakeDao = FakeLocationRecordDao()
        val fakeTripDao = FakeTripRecordDao()
        val repository = LocationRepository(fakeDao, fakeTripDao)

        // Existing old record
        repository.insertAllRecords(listOf(LocationRecord(id = 1L, latitude = 10.0, longitude = 70.0, accuracy = 5f, timestamp = 999L)))
        repository.insertAllTrips(listOf(TripRecord(id = 1L, startTime = 900L, endTime = 999L, startLatitude = 10.0, startLongitude = 70.0, endLatitude = 10.0, endLongitude = 70.0, distanceMeters = 10.0, durationSeconds = 99L, pointCount = 1)))

        // Backup payload with completely fresh data
        val backupPayload = BackupPayload(
            backupVersion = CURRENT_BACKUP_VERSION,
            createdAt = 1726410000000L,
            locations = listOf(
                BackupLocationItem(20.0, 80.0, 4f, 5000L),
                BackupLocationItem(20.1, 80.1, 4f, 6000L)
            ),
            trips = listOf(
                BackupTripItem(5000L, 6000L, 20.0, 80.0, 20.1, 80.1, 800.0, 1000L, 2)
            )
        )

        // Execute Restore with REPLACE strategy
        val restoreResult = BackupManager.restoreBackup(
            payload = backupPayload,
            strategy = RestoreStrategy.REPLACE,
            locationRepository = repository
        )

        assertTrue(restoreResult is RestoreResult.Success)
        val success = restoreResult as RestoreResult.Success
        assertEquals(2, success.locationsRestored)
        assertEquals(1, success.tripsRestored)
        assertEquals(0, success.duplicatesSkipped)

        // Verify old record (timestamp 999L) was wiped and replaced
        val allLocations = repository.getAllRecordsChronologicalList()
        assertEquals(2, allLocations.size)
        assertFalse(allLocations.any { it.timestamp == 999L })
        assertTrue(allLocations.any { it.timestamp == 5000L })
    }

    // -------------------------------------------------------------
    // 6. Filename & Timestamp Formatting
    // -------------------------------------------------------------

    @Test
    fun testTimestampAndFilenameFormatting() {
        assertEquals("Never", BackupManager.formatLastBackupTimestamp(null))
        assertEquals("Never", BackupManager.formatLastBackupTimestamp(-1L))

        val formatted = BackupManager.formatLastBackupTimestamp(1726410000000L)
        assertFalse(formatted.isBlank())
        assertFalse(formatted == "Never")

        val plainName = BackupManager.generateSuggestedBackupFileName(isEncrypted = false, nowMillis = 1726410000000L)
        assertTrue(plainName.endsWith(".ttbackup.json"))

        val encName = BackupManager.generateSuggestedBackupFileName(isEncrypted = true, nowMillis = 1726410000000L)
        assertTrue(encName.endsWith(".ttbackup.enc"))
    }

    // -------------------------------------------------------------
    // In-memory Test Doubles
    // -------------------------------------------------------------

    private class FakeLocationRecordDao : LocationRecordDao {
        val records = mutableListOf<LocationRecord>()
        var nextId = 1L

        override suspend fun insert(record: LocationRecord): Long {
            val id = if (record.id != 0L) {
                nextId = maxOf(nextId, record.id + 1)
                record.id
            } else {
                nextId++
            }
            records.removeAll { it.id == id }
            records.add(record.copy(id = id))
            return id
        }

        override suspend fun insertAll(newRecords: List<LocationRecord>): List<Long> {
            return newRecords.map { insert(it) }
        }

        override fun getAllRecords(): Flow<List<LocationRecord>> = flowOf(records.sortedByDescending { it.timestamp })
        override suspend fun getAllRecordsList(): List<LocationRecord> = records.sortedByDescending { it.timestamp }
        override suspend fun getAllRecordsChronologicalList(): List<LocationRecord> = records.sortedBy { it.timestamp }
        override fun getRecordsBetween(startTime: Long, endTime: Long): Flow<List<LocationRecord>> =
            flowOf(records.filter { it.timestamp in startTime..endTime }.sortedBy { it.timestamp })
        override suspend fun getRecordsBetweenList(startTime: Long, endTime: Long): List<LocationRecord> =
            records.filter { it.timestamp in startTime..endTime }.sortedBy { it.timestamp }
        override suspend fun delete(record: LocationRecord): Int = if (records.removeAll { it.id == record.id }) 1 else 0
        override suspend fun deleteById(id: Long): Int = if (records.removeAll { it.id == id }) 1 else 0
        override suspend fun deleteRecordsBetween(startTime: Long, endTime: Long): Int =
            records.count { it.timestamp in startTime..endTime }.also { records.removeAll { r -> r.timestamp in startTime..endTime } }
        override suspend fun deleteAllRecords(): Int = records.size.also { records.clear() }
        override fun getRecordCount(): Flow<Int> = flowOf(records.size)
        override fun getRecordCountBetween(startTime: Long, endTime: Long): Flow<Int> =
            flowOf(records.count { it.timestamp in startTime..endTime })
        override fun getLatestRecord(): Flow<LocationRecord?> = flowOf(records.maxByOrNull { it.timestamp })
        override fun getFirstRecord(): Flow<LocationRecord?> = flowOf(records.minByOrNull { it.timestamp })
        override fun getTimestampsBetween(startTime: Long, endTime: Long): Flow<List<Long>> =
            flowOf(records.filter { it.timestamp in startTime..endTime }.map { it.timestamp }.sorted())
        override suspend fun getAllTimestampsList(): List<Long> = records.map { it.timestamp }
    }

    private class FakeTripRecordDao : TripRecordDao {
        val trips = mutableListOf<TripRecord>()
        var nextId = 1L

        override suspend fun insert(trip: TripRecord): Long {
            val id = if (trip.id != 0L) {
                nextId = maxOf(nextId, trip.id + 1)
                trip.id
            } else {
                nextId++
            }
            trips.removeAll { it.id == id }
            trips.add(trip.copy(id = id))
            return id
        }

        override suspend fun insertAll(newTrips: List<TripRecord>): List<Long> {
            return newTrips.map { insert(it) }
        }

        override fun getAllTrips(): Flow<List<TripRecord>> = flowOf(trips.sortedByDescending { it.startTime })
        override suspend fun getAllTripsList(): List<TripRecord> = trips.sortedBy { it.startTime }
        override fun getTripsBetween(startTime: Long, endTime: Long): Flow<List<TripRecord>> =
            flowOf(trips.filter { it.startTime in startTime..endTime }.sortedBy { it.startTime })
        override suspend fun getTripsBetweenList(startTime: Long, endTime: Long): List<TripRecord> =
            trips.filter { it.startTime in startTime..endTime }.sortedBy { it.startTime }
        override suspend fun deleteTripsBetween(startTime: Long, endTime: Long): Int =
            trips.count { it.startTime in startTime..endTime }.also { trips.removeAll { t -> t.startTime in startTime..endTime } }
        override suspend fun deleteAllTrips(): Int = trips.size.also { trips.clear() }
        override fun getTripCount(): Flow<Int> = flowOf(trips.size)
        override fun getTripCountBetween(startTime: Long, endTime: Long): Flow<Int> =
            flowOf(trips.count { it.startTime in startTime..endTime })
    }
}
