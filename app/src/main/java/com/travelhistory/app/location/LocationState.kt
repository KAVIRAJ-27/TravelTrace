package com.travelhistory.app.location

sealed interface LocationState {
    data object Idle : LocationState
    data object RequestingPermission : LocationState
    data class Tracking(val location: LocationData, val totalRecords: Int = 1) : LocationState
    data class PermissionDenied(val isPermanentlyDenied: Boolean = false) : LocationState
    data object LocationDisabled : LocationState
    data class Error(val message: String) : LocationState
}
