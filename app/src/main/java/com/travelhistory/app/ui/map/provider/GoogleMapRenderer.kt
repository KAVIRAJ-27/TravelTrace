package com.travelhistory.app.ui.map.provider

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.travelhistory.app.data.db.LocationRecord

@Composable
fun GoogleMapRenderer(
    records: List<LocationRecord>,
    hasLocationPermission: Boolean,
    onPointSelected: (LocationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultLatLng = remember(records) {
        if (records.isNotEmpty()) {
            LatLng(records.first().latitude, records.first().longitude)
        } else {
            LatLng(13.0827, 80.2707)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 14f)
    }

    LaunchedEffect(records) {
        if (records.isNotEmpty()) {
            if (records.size == 1) {
                val singlePoint = LatLng(records.first().latitude, records.first().longitude)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(singlePoint, 16f))
            } else {
                val boundsBuilder = LatLngBounds.builder()
                records.forEach { record ->
                    boundsBuilder.include(LatLng(record.latitude, record.longitude))
                }
                try {
                    val bounds = boundsBuilder.build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 130))
                } catch (_: Exception) {
                    val first = LatLng(records.first().latitude, records.first().longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(first, 14f)
                }
            }
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    ) {
        // Polyline connecting points chronologically
        if (records.size >= 2) {
            val pathLatLngs = remember(records) {
                records.map { LatLng(it.latitude, it.longitude) }
            }
            Polyline(
                points = pathLatLngs,
                color = MaterialTheme.colorScheme.primary,
                width = 10f,
                geodesic = true
            )
        }

        // Start Point Marker (Green)
        records.firstOrNull()?.let { start ->
            val startPos = LatLng(start.latitude, start.longitude)
            Marker(
                state = rememberMarkerState(key = "gmap_start_${start.id}", position = startPos),
                title = "Start Point",
                snippet = start.formattedTime,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                onClick = {
                    onPointSelected(start)
                    true
                }
            )
        }

        // Intermediate Point Markers (Azure)
        if (records.size > 2) {
            val intermediatePoints = records.subList(1, records.size - 1)
            intermediatePoints.forEach { point ->
                val pos = LatLng(point.latitude, point.longitude)
                Marker(
                    state = rememberMarkerState(key = "gmap_point_${point.id}", position = pos),
                    title = "Point",
                    snippet = point.formattedTime,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        onPointSelected(point)
                        true
                    }
                )
            }
        }

        // End Point Marker (Red)
        if (records.size > 1) {
            val end = records.last()
            val endPos = LatLng(end.latitude, end.longitude)
            Marker(
                state = rememberMarkerState(key = "gmap_end_${end.id}", position = endPos),
                title = "End Point",
                snippet = end.formattedTime,
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                onClick = {
                    onPointSelected(end)
                    true
                }
            )
        }
    }
}
