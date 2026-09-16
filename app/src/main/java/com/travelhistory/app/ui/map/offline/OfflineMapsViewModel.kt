package com.travelhistory.app.ui.map.offline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travelhistory.app.data.map.offline.OfflineMapManager
import com.travelhistory.app.data.map.offline.OfflineMapRegion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class OfflineMapsUiState(
    val downloadedRegions: List<OfflineMapRegion> = emptyList(),
    val availableRegions: List<OfflineMapRegion> = emptyList(),
    val totalStorageBytes: Long = 0L,
    val isDownloading: Boolean = false,
    val downloadingRegionId: Long? = null,
    val downloadProgress: Int = 0
) {
    val totalStorageFormatted: String
        get() {
            if (totalStorageBytes <= 0L) return "0 MB"
            val mb = totalStorageBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1000.0) {
                String.format(Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(Locale.US, "%.0f MB", mb)
            }
        }

    val hasDownloadedRegions: Boolean
        get() = downloadedRegions.isNotEmpty()
}

class OfflineMapsViewModel(application: Application) : AndroidViewModel(application) {

    private val offlineMapManager = OfflineMapManager(application, viewModelScope)

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            offlineMapManager.downloadedRegions.collect { downloaded ->
                _uiState.update { it.copy(downloadedRegions = downloaded) }
            }
        }

        viewModelScope.launch {
            offlineMapManager.availableRegions.collect { available ->
                _uiState.update { it.copy(availableRegions = available) }
            }
        }

        viewModelScope.launch {
            offlineMapManager.totalStorageBytes.collect { totalBytes ->
                _uiState.update { it.copy(totalStorageBytes = totalBytes) }
            }
        }
    }

    fun downloadRegion(region: OfflineMapRegion) {
        _uiState.update {
            it.copy(
                isDownloading = true,
                downloadingRegionId = region.id,
                downloadProgress = 0
            )
        }

        offlineMapManager.downloadRegion(
            regionId = region.id,
            onProgress = { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            },
            onComplete = { success ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadingRegionId = null,
                        downloadProgress = 0
                    )
                }
            }
        )
    }

    fun deleteRegion(region: OfflineMapRegion) {
        offlineMapManager.deleteRegion(region.id)
    }
}
