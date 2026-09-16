# TravelTrace

> **Privacy-First Native Android Location Recording, Travel Analytics & Dual-Map Tracking System**

[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-brightgreen.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin_2.0-blue.svg)](https://kotlinlang.org)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Database-Room_SQLite-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Encryption](https://img.shields.io/badge/Encryption-AES--256--GCM_·_PBKDF2-red.svg)](https://developer.android.com)
[![Tests](https://img.shields.io/badge/Tests-74_Passed_·_0_Failed-success.svg)]()

---

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Key Features](#2-key-features)
- [3. Technology Stack](#3-technology-stack)
- [4. System Architecture](#4-system-architecture)
- [5. Location Tracking Engine](#5-location-tracking-engine)
- [6. Dual-Map System (Online & Offline)](#6-dual-map-system-online--offline)
- [7. Travel Analytics & Trip Detection](#7-travel-analytics--trip-detection)
- [8. Security & Privacy](#8-security--privacy)
- [9. Data Export (CSV & JSON)](#9-data-export-csv--json)
- [10. Backup & Restore](#10-backup--restore)
- [11. Battery Optimization & Reliability](#11-battery-optimization--reliability)
- [12. Installation](#12-installation)
- [13. Build Instructions](#13-build-instructions)
- [14. Testing & Quality Assurance](#14-testing--quality-assurance)
- [15. Project Structure](#15-project-structure)
- [16. Screenshots](#16-screenshots)
- [17. Future Improvements](#17-future-improvements)
- [18. License](#18-license)
- [19. Author & Contact](#19-author--contact)

---

## 1. Project Overview

**TravelTrace** (published under the application label **Travel History**) is an offline-first native Android application engineered for automated, privacy-centric GPS location recording, comprehensive travel analytics, and intelligent dual-engine map visualization.

Traditional travel trackers often rely on third-party cloud backends that ingest sensitive coordinate histories. TravelTrace adopts a strict **local-first paradigm**: all geographic waypoints, velocity logs, and trip statistics reside solely in an encrypted local database on the physical device. 

The application features a robust background foreground service that survives process death and system reboots, automatic stationary detection to minimize battery consumption, dual-engine map rendering (Google Maps when online and MapLibre with pre-downloaded offline vector/raster packs when disconnected), hardware-backed biometric access gates, and military-grade AES-256-GCM encrypted backup archives.

---

## 2. Key Features

- **Periodic GPS Location Recording**: Automated background location acquisition using Google Play Services Fused Location Provider.
- **Customizable Recording Intervals**: Flexible intervals (15 seconds, 30 seconds, 1 minute, 2 minutes, 5 minutes, 15 minutes, 30 minutes, 1 hour) tailored to transit modes (walking, cycling, driving).
- **Background GPS Tracking**: Persistent tracking executed via an Android Foreground Service with continuous status notifications.
- **Foreground Service Architecture**: Operates under strict Android 14 location foreground service requirements (`FOREGROUND_SERVICE_LOCATION`).
- **Smart Dashboard**: Centralized dashboard presenting live tracking status, active interval, today's summary counters, quick interval selection, and tracking health indicators.
- **Automated Trip Detection**: Intelligently segments location points into distinct journeys based on temporal gaps and stationary thresholds.
- **High-Precision Distance Calculation**: Haversine great-circle trigonometric formula computing exact transit distances in kilometers and meters.
- **Travel Timeline**: Interactive chronologically ordered travel logs grouped by date and trip segmentation.
- **Multi-Period Travel Analytics**: Real-time aggregated statistics covering daily, weekly, and monthly travel distance, trip counts, active duration, and average speeds.
- **Online Google Maps Integration**: Vector-rendered online basemaps with custom GPS markers and styled travel path polylines.
- **Offline MapLibre Engine**: Fully independent offline map rendering using MapLibre Native SDK for complete functionality in remote or airplane mode environments.
- **Offline Region Management**: Download, inspect, diagnose, and delete localized geographic map regions with actual filesystem byte-size accounting.
- **Network-Aware Map Switching**: Real-time network monitor detecting cellular/Wi-Fi transitions and switching or alerting the map provider accordingly.
- **Calendar-Based History**: Interactive calendar interface allowing users to navigate and inspect past routes by specific days, weeks, or months.
- **GPS Markers & Polyline Routes**: Color-coded waypoints highlighting departure points, arrival destinations, and directional vectors.
- **Full Data Export (CSV & JSON)**: Comprehensive export pipelines generating clean, standard CSV tables and JSON documents.
- **Preserved Geographic Coordinates**: Exports preserve raw, unrounded decimal latitude, longitude, speed, and accuracy values without lossy reverse-geocoding substitution.
- **Biometric Export Authentication**: AndroidX BiometricPrompt gate requiring fingerprint, face, or device credential before sensitive data can be exported.
- **Optional Biometric App Lock**: Full application locking mechanism that protects user records while allowing the background tracking service to record seamlessly.
- **Encrypted Backup & Restore**: Password-derived AES-256-GCM encrypted backup archives with PBKDF2 key derivation and 128-bit authentication tags.
- **Dual Restore Modes (Merge & Replace)**: Choose between non-destructive merging (skipping duplicate timestamps) or full database replacement.
- **Duplicate-Fix Protection**: Intelligent multi-field identity verification ensuring restored datasets never corrupt database integrity.
- **Automatic Service Restart**: System-level resilience restarting tracking if killed by aggressive OEM task managers.
- **Boot-Time Tracking Recovery**: `BroadcastReceiver` listening for `BOOT_COMPLETED` and `QUICKBOOT_POWERON` to automatically restore active tracking after device restarts.
- **Battery Optimization Guidance**: In-app guidance directing users to exempt TravelTrace from restrictive OS battery optimization (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

---

## 3. Technology Stack

The project relies exclusively on modern, stable, native Android components:

| Category | Technology / Library | Version / Detail |
|---|---|---|
| **Language** | Kotlin | v2.0+ (100% Kotlin codebase) |
| **Minimum SDK** | Android 8.0 (API Level 26) | Oreo |
| **Target / Compile SDK** | Android 14 (API Level 34) | Upside Down Cake |
| **UI Framework** | Jetpack Compose & Material 3 | Declarative UI, Dynamic Color, Material Icons Extended |
| **Architecture** | Modern Android Architecture (MVVM) | ViewModels, Kotlin Coroutines, StateFlow, Navigation Compose |
| **Local Database** | Android Jetpack Room | SQLite ORM with KSP code generation |
| **Preferences** | Jetpack DataStore Preferences | Asynchronous, transactional key-value storage |
| **Location Engine** | Google Play Services Location | FusedLocationProviderClient, High-Accuracy Priority |
| **Online Maps** | Google Maps SDK & Maps Compose | `play-services-maps:18.2.0`, `maps-compose:4.3.3` |
| **Offline Maps** | MapLibre Native SDK for Android | `org.maplibre.gl:android-sdk:11.5.1` |
| **Security & Biometrics** | AndroidX Biometric | `androidx.biometric:biometric:1.2.0-alpha05` |
| **Cryptography** | Java Cryptography Extension (JCE) | AES-256-GCM (`AES/GCM/NoPadding`), PBKDF2WithHmacSHA256 |
| **File Sharing** | AndroidX Core FileProvider | Secure URI sharing for CSV/JSON exports |
| **Build System** | Gradle (Kotlin DSL) | AGP 8.x, Java 17 toolchain |

---

## 4. System Architecture

TravelTrace follows the official Android Architecture Guidelines, separating concerns across unidirectional data flow layers:

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│          Jetpack Compose UI (Material 3)               │
│   HomeScreen  •  MapScreen  •  HistoryScreen  •  Settings │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / UI Events
┌───────────────────────────┴────────────────────────────┐
│                    VIEWMODEL LAYER                     │
│    HomeViewModel  •  MapViewModel  •  HistoryViewModel  │
│          ExportViewModel  •  OfflineMapsViewModel       │
└───────────────────────────▲────────────────────────────┘
                            │ Coroutine Dispatchers
┌───────────────────────────┴────────────────────────────┐
│                   APPLICATION LOGIC                    │
│   TripDetector  •  TravelAnalyticsCalculator  •  Crypto│
│     OfflineMapManager  •  BiometricAuthManager         │
└───────────────────────────▲────────────────────────────┘
                            │ Repository Pattern
┌───────────────────────────┴────────────────────────────┐
│                 DATA & PERSISTENCE LAYER               │
│  LocationRepository  •  Room Database (SQLite)        │
│  DataStore Preferences  •  MapLibre Offline Cache      │
└───────────────────────────▲────────────────────────────┘
                            │ System Broadcasts / GPS
┌───────────────────────────┴────────────────────────────┐
│                 DEVICE HARDWARE & OS                   │
│  FusedLocationProviderClient  •  LocationTrackingService│
│  BootReceiver  •  Biometric Sensor  •  NetworkMonitor  │
└────────────────────────────────────────────────────────┘
```

---

## 5. Location Tracking Engine

The tracking subsystem is designed for zero data loss and deterministic behavior across all supported Android versions:

- **Foreground Service**: Runs `LocationTrackingService` bound to a persistent ongoing notification displaying real-time recording intervals, last update timestamps, and a quick-stop action.
- **Configurable Intervals**: Supports 8 distinct frequency profiles:
  - High frequency: `15s`, `30s` (walking, urban navigation)
  - Medium frequency: `1m`, `2m`, `5m` (cycling, transit)
  - Battery saver: `15m`, `30m`, `1h` (long-distance road trips)
- **GPS Accuracy Filtering**: Waypoints are evaluated prior to persistence. Points with horizontal accuracy poorer than 50 meters are rejected to avoid GPS drift artifacts.
- **Stationary Duplicate Rejection**: If consecutive location updates reflect negligible movement (< 5 meters) while stationary, the duplicate coordinate is discarded to conserve storage and avoid cluttering timeline analytics.
- **Timeout & Failure Handling**: Uses structured coroutine timeouts to handle GPS warm-up delays without freezing background worker threads.
- **Service Recovery**: If the Android system terminates the service under memory pressure, `START_STICKY` ensures the service restarts automatically as soon as memory clears.

---

## 6. Dual-Map System (Online & Offline)

TravelTrace features a hybrid dual-map architecture that guarantees visual mapping whether tracking through major cities or remote wilderness trails.

```
                  ┌──────────────────────┐
                  │ Network State Check  │
                  └──────────┬───────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
       [Network Active]              [No Connection]
              │                             │
    Google Maps (Online)          MapLibre SDK (Offline)
  • Google Play Services        • Self-contained SQLite MBTiles
  • Vector Basemap              • Offline Raster/Vector Cache
  • Requires API Key            • Zero Internet Required
```

### Online Mapping (Google Maps)
- Utilizes the Google Maps SDK for Android with Jetpack Compose bindings.
- Renders rich vector tiles, dynamic camera controls, smooth route polylines, and customized location markers.
- **Note**: Requires a valid Google Maps Android API Key defined in `local.properties` (`MAPS_API_KEY=your_key_here`). If unconfigured or offline, the app provides a graceful fallback.

### Offline Mapping (MapLibre Native)
- Utilizes the MapLibre Native Android SDK (`org.maplibre.gl:android-sdk`).
- Allows users to pre-download geographic bounds (e.g., states, districts, parks) directly to local internal storage.
- Operates 100% offline using bundled raster/vector tile schemas and local cache databases without pinging external servers.
- Includes built-in storage inspection tools to verify downloaded region files, byte sizes, and database consistency on the filesystem.

---

## 7. Travel Analytics & Trip Detection

Raw coordinates are transformed into actionable mobility insights using mathematical algorithms:

- **Haversine Distance Formula**: Computes precise spherical distance between coordinate pairs:
  $$\Delta\sigma = 2 \arcsin \left( \sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)} \right)$$
  $$d = R \cdot \Delta\sigma \quad (\text{where } R = 6371.0\text{ km})$$
- **Automated Trip Segmentation**:
  - Consecutive waypoints are aggregated into trips based on velocity and dwell time.
  - Periods of stationary dwelling exceeding 10 minutes delineate trip endpoints.
  - Each trip computes start time, end time, total transit duration, distance traveled, and average velocity.
- **Period Summaries**:
  - **Daily**: Total distance, trip count, total active time, and chronological waypoint distribution.
  - **Weekly**: Daily breakdown bar graphs, peak travel days, and cumulative distance.
  - **Monthly**: High-level transit aggregates, comparison indicators, and monthly distance trends.

---

## 8. Security & Privacy

Privacy is the core founding principle of TravelTrace. The application operates without remote analytics, tracking pixels, advertising SDKs, or cloud synchronization.

- **Local-Only Database**: All location records remain stored in the sandboxed SQLite database managed by Room at `/data/data/com.travelhistory.app/databases/`.
- **Biometric App Lock**:
  - Protects app entry behind the device's biometric authentication (`BIOMETRIC_STRONG` or device credential PIN/pattern).
  - Background tracking continues operating completely uninterrupted while the application UI is locked.
- **Biometric Export Protection**:
  - Every data export operation requires explicit biometric verification before generating or sharing CSV/JSON files.
- **Hardware-Grade Backup Cryptography**:
  - Cipher: **AES-256 in Galois/Counter Mode** (`AES/GCM/NoPadding`) providing authenticated encryption.
  - Key Derivation: **PBKDF2WithHmacSHA256** with **65,536 iterations** and a 16-byte cryptographically secure random salt.
  - Authentication Tag: **128-bit integrity tag** preventing tampering or unauthorized modification.
  - Nonce/IV: Unique **12-byte initialization vector** generated per backup archive using `SecureRandom`.
  - Zero Credential Storage: Passwords and biometric credentials are never saved to disk; keys are derived on the fly in memory and wiped after use.

---

## 9. Data Export (CSV & JSON)

TravelTrace enables complete user data ownership. Location data can be exported into open formats for GIS software (QGIS, ArcGIS), spreadsheet analysis, or personal archiving.

### Export Formats

1. **CSV (Comma-Separated Values)**:
   - Includes standard headers: `id, latitude, longitude, accuracy, speed, timestamp, formatted_time`
   - Coordinates are exported as exact decimal degrees (e.g., `12.971598, 77.594562`) without obfuscation or truncation.
2. **JSON (JavaScript Object Notation)**:
   - Structured format containing metadata (export timestamp, record count, application version) and an array of coordinate objects.

All exported files are served through a secure Android `FileProvider` (`com.travelhistory.app.fileprovider`), ensuring no world-readable files are created in shared external storage.

---

## 10. Backup & Restore

TravelTrace includes a robust backup subsystem supporting both unencrypted and AES-256-GCM encrypted backup files.

```
┌─────────────────────────┐         ┌─────────────────────────┐
│ Plaintext JSON Backup   │   OR    │ Password-Protected      │
│ (Unencrypted JSON)      │         │ AES-256-GCM Archive     │
└────────────┬────────────┘         └────────────┬────────────┘
             │                                   │
             └─────────────────┬─────────────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Restore Engine Mode │
                    └──────────┬──────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
     [Merge Strategy]                      [Replace Strategy]
  • Preserves existing data             • Clears current tables
  • Skips timestamp duplicates          • Replaces with backup data
  • Appends new waypoints               • Complete snapshot restore
```

- **Merge Mode**: Preserves existing records and selectively inserts points from the backup file that do not collide with existing timestamps.
- **Replace Mode**: Atomically wipes the current database and populates it with the exact snapshot from the backup file.
- **Integrity Validation**: Detects corrupted, truncated, or tampered backup files prior to database transaction execution.

---

## 11. Battery Optimization & Reliability

Long-term GPS recording requires careful balancing of tracking accuracy and energy consumption:

- **Foreground Service with Wake Lock Safety**: Uses short-lived system wake locks only during active coordinate acquisition, preventing battery drain from permanent device wakefulness.
- **Stationary Detection**: When the device is motionless, tracking intervals automatically conserve radio activity.
- **Battery Optimization Exemption**: Restrictive battery savers (Doze mode, App Standby) can kill background tracking. TravelTrace includes built-in guidance to prompt for exemption via `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`.
- **Boot Recovery (`BootReceiver`)**: Automatically resumes tracking after the device boots up if tracking was active prior to device shutdown.

---

## 12. Installation

The pre-compiled production release APK is located at:

```
app/build/outputs/apk/release/app-release.apk
```

### Sideloading Instructions
1. Copy `app-release.apk` to your Android device via USB cable, Google Drive, or local file transfer.
2. Open your device's **Files** or **File Manager** app and locate `app-release.apk`.
3. Tap the APK file to initiate installation.
4. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **Allow from this source**.
5. Tap **Install** and launch **Travel History** from your app drawer.
6. Grant the required location permissions (**Allow all the time** is required for background tracking).

---

## 13. Build Instructions

### Prerequisites
- Java Development Kit (JDK) 17
- Android SDK Platform 34 & Build-Tools
- Windows PowerShell / Command Prompt / Terminal

### Commands (Windows PowerShell)

```powershell
# 1. Clean the project and build caches
.\gradlew.bat clean

# 2. Execute all unit tests
.\gradlew.bat testDebugUnitTest

# 3. Compile the Debug APK
.\gradlew.bat assembleDebug

# 4. Compile the signed Release APK
.\gradlew.bat assembleRelease
```

### Resulting Artifact Locations

| Build Type | Artifact Path | Typical Size |
|---|---|---|
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | ~62.5 MB |
| **Release APK** | `app/build/outputs/apk/release/app-release.apk` | ~55.9 MB |

---

## 14. Testing & Quality Assurance

The codebase includes an extensive suite of local unit tests verifying critical mathematical algorithms, database constraints, export formats, encryption integrity, and background tracking state transitions.

### Latest Validation Results

```
==================================================
TEST EXECUTION SUMMARY
==================================================
Total Tests:      74
Passed:           74
Failed:            0
Skipped:           0
Status:           BUILD SUCCESSFUL (100% Pass Rate)
==================================================
```

### Verified Test Categories

- **Database & Data Access**: Room DAO CRUD operations, date-range filtering, and relational integrity.
- **Distance & Mathematics**: Haversine distance accuracy across equator, prime meridian, and coordinate poles.
- **Trip Detection**: Threshold clustering, dwell time segmentation, and speed computation.
- **Location Filtering**: Horizontal accuracy bounds checking, duplicate rejection, and interval logic.
- **Serialization & Formats**: CSV escaping, RFC 4180 compliance, JSON schema validation, and coordinate preservation.
- **Cryptographic Security**: AES-256-GCM encryption/decryption, PBKDF2 key generation, wrong-password rejection, and tag tampering detection.
- **System Reliability**: Service restart logic, tracking state persistence, and battery optimization state evaluation.
- **Offline Maps**: Offline region bounds calculation, style parsing, and dual-provider switching.

---

## 15. Project Structure

```
Location recorder/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── assets/
│   │   │   │   └── offline_map_style.json
│   │   │   ├── java/com/travelhistory/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── TravelHistoryApp.kt
│   │   │   │   ├── TravelHistoryApplication.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── analytics/        # TravelAnalyticsCalculator & Models
│   │   │   │   │   ├── backup/           # BackupCrypto (AES-256-GCM) & BackupManager
│   │   │   │   │   ├── db/               # Room AppDatabase, DAOs & Entities
│   │   │   │   │   ├── export/           # LocationExporter (CSV & JSON)
│   │   │   │   │   ├── map/offline/      # OfflineMapManager & OfflineMapRegion
│   │   │   │   │   ├── network/          # NetworkMonitor (ConnectivityManager)
│   │   │   │   │   ├── repository/       # LocationRepository
│   │   │   │   │   └── trip/             # Haversine & TripDetector
│   │   │   │   ├── location/             # LocationTrackingService, Tracker & BootReceiver
│   │   │   │   ├── navigation/           # AppNavHost & Screen definitions
│   │   │   │   ├── security/             # AppLockManager & BiometricAuthManager
│   │   │   │   └── ui/                   # Jetpack Compose Screens & Theme
│   │   │   │       ├── home/             # Smart Dashboard & Live Status
│   │   │   │       ├── map/              # Dual Map View (Google & MapLibre)
│   │   │   │       ├── history/          # Calendar & Travel Timeline
│   │   │   │       ├── analytics/        # Travel Analytics & Charts
│   │   │   │       └── settings/         # Intervals, Biometrics, Export & Backup
│   │   │   └── res/
│   │   │       ├── drawable/             # App icons & master assets
│   │   │       ├── mipmap-*/             # Multi-density launcher icons
│   │   │       └── values/               # Strings, Colors & Styles
│   │   └── test/java/com/travelhistory/app/
│   │       ├── AppLockAndBackupTest.kt
│   │       ├── LocationDatabaseTest.kt
│   │       ├── LocationExportTest.kt
│   │       ├── LocationTrackingValidationTest.kt
│   │       ├── OfflineMapAndDualSystemTest.kt
│   │       ├── SmartDashboardTest.kt
│   │       ├── TrackingHealthAndReliabilityTest.kt
│   │       ├── TravelTimelineAndAnalyticsTest.kt
│   │       └── TripDetectionAndDistanceTest.kt
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 16. Screenshots

> *Screenshots can be added to the repository under the `docs/screenshots/` directory.*

| Smart Dashboard | Online Google Map | Offline MapLibre |
|:---:|:---:|:---:|
| `docs/screenshots/home.png` | `docs/screenshots/map-online.png` | `docs/screenshots/map-offline.png` |

| Travel History & Timeline | App Settings & Security |
|:---:|:---:|
| `docs/screenshots/history.png` | `docs/screenshots/settings.png` |

---

## 17. Future Improvements

- **GPX File Format Support**: Add standard GPS Exchange Format (`.gpx`) export alongside existing CSV and JSON options.
- **Elevation Profiling**: Store and visualize altitude changes across mountainous and elevation-heavy hiking routes.
- **Geofenced Tracking**: Automated tracking triggers that start or stop recording upon entering or exiting pre-configured zones.
- **Custom Tile Server Support**: Allow users to configure custom self-hosted vector/raster tile URLs for offline mapping.

---

## 18. License

This repository does not currently include an open-source license. All rights are reserved by the author. Inquiries regarding academic use, distribution, or commercial licensing should be directed to the repository owner.

---

## 19. Author & Contact

- **Developer**: Kaviraj R
- **GitHub**: [@KAVIRAJ-27](https://github.com/KAVIRAJ-27)
- **Repository**: [TravelTrace](https://github.com/KAVIRAJ-27/TravelTrace)
