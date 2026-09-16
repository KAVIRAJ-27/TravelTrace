package com.travelhistory.app.ui.map.provider

/**
 * Supported map rendering providers.
 */
enum class MapProviderType(val displayName: String) {
    GOOGLE_MAPS("Google Maps"),
    MAPLIBRE_OFFLINE("Offline MapLibre")
}

/**
 * User/system selection mode for choosing the active map provider.
 */
enum class MapSelectionMode(val label: String) {
    AUTO("Auto (Online / Offline)"),
    FORCE_GOOGLE_MAPS("Google Maps (Online)"),
    FORCE_MAPLIBRE("MapLibre (Offline)")
}
