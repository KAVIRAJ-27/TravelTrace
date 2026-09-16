package com.travelhistory.app.ui.map.provider

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.travelhistory.app.data.db.LocationRecord
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun OfflineMapLibreRenderer(
    records: List<LocationRecord>,
    hasLocationPermission: Boolean,
    onPointSelected: (LocationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapLibreInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // Lifecycle coordination
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Update map overlays when records or map instance changes
    LaunchedEffect(records, mapLibreInstance) {
        val map = mapLibreInstance ?: return@LaunchedEffect

        map.clear()

        // 1. Draw Polyline
        if (records.size >= 2) {
            val polylinePoints = records.map { LatLng(it.latitude, it.longitude) }
            map.addPolyline(
                PolylineOptions()
                    .addAll(polylinePoints)
                    .color(Color.parseColor("#0284C7")) // Cyan/Blue
                    .width(4f)
            )
        }

        // 2. Add Markers
        records.forEachIndexed { index, record ->
            val title = when (index) {
                0 -> "Start Point"
                records.size - 1 -> "End Point"
                else -> "Point #${index + 1}"
            }
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(record.latitude, record.longitude))
                    .title(title)
                    .snippet(record.formattedTime)
            )
        }

        // 3. Setup marker click listener
        map.setOnMarkerClickListener { marker ->
            val clicked = records.find {
                kotlin.math.abs(it.latitude - marker.position.latitude) < 0.0001 &&
                        kotlin.math.abs(it.longitude - marker.position.longitude) < 0.0001
            }
            if (clicked != null) {
                onPointSelected(clicked)
            }
            true
        }

        // 4. Fit Camera Bounds
        if (records.size == 1) {
            val single = LatLng(records[0].latitude, records[0].longitude)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(single, 15.0))
        } else if (records.size > 1) {
            val boundsBuilder = LatLngBounds.Builder()
            records.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 130))
            } catch (_: Exception) {
                val first = LatLng(records[0].latitude, records[0].longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 13.0))
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapLibreInstance = map
                    // Load the embedded offline map style
                    map.setStyle(Style.Builder().fromUri("asset://offline_map_style.json")) {
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isAttributionEnabled = false
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
