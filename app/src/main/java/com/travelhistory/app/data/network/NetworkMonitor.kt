package com.travelhistory.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Monitors network availability reactively using Android ConnectivityManager.
 * Used for automatic switching between online Google Maps and offline MapLibre.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Observable flow emitting true when active network has internet connectivity, false otherwise.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isCurrentlyOnline())
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyOnline())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial network status
        trySend(isCurrentlyOnline())

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()

    /**
     * Checks whether the device currently has an active internet connection.
     */
    fun isCurrentlyOnline(): Boolean {
        val manager = connectivityManager ?: return false
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
