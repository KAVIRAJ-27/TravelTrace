package com.travelhistory.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager handling Biometric App Lock state, lifecycle monitoring, and security preferences.
 *
 * CRITICAL PRIVACY & SECURITY RULES:
 * - Never stores fingerprint or face biometric templates.
 * - Relies strictly on official Android [BiometricPrompt] and [BiometricManager].
 * - UI lock state operates strictly on UI layer; does NOT stop or interfere with [LocationTrackingService].
 */
object AppLockManager {

    private const val PREFS_NAME = "travel_trace_security_prefs"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var suppressLockUntil: Long = 0L
    private var lastBackgroundTime: Long = 0L

    /**
     * Checks if App Lock is enabled in user settings.
     */
    fun isAppLockEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    /**
     * Updates App Lock setting.
     */
    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    /**
     * Unlocks the application for the current active session.
     */
    fun unlock() {
        _isAppLocked.value = false
    }

    /**
     * Forces the application into locked state if App Lock is enabled.
     */
    fun lock(context: Context) {
        if (isAppLockEnabled(context)) {
            _isAppLocked.value = true
        }
    }

    /**
     * Temporarily suppresses re-locking for system dialogs (e.g. SAF file pickers).
     */
    fun temporarilySuppressLock(durationMs: Long = 10000L) {
        suppressLockUntil = System.currentTimeMillis() + durationMs
    }

    /**
     * Called when the main activity is created / restarted.
     */
    fun onActivityCreated(context: Context) {
        if (isAppLockEnabled(context)) {
            _isAppLocked.value = true
        }
    }

    /**
     * Called when the main activity moves to foreground (onStart).
     */
    fun onActivityStarted(context: Context) {
        if (!isAppLockEnabled(context)) return

        val now = System.currentTimeMillis()
        val isSuppressed = now < suppressLockUntil

        if (!isSuppressed && lastBackgroundTime > 0L) {
            // App was in background and suppression was not active -> lock
            _isAppLocked.value = true
        }
    }

    /**
     * Called when the main activity moves to background (onStop).
     */
    fun onActivityStopped() {
        lastBackgroundTime = System.currentTimeMillis()
    }

    /**
     * Detailed check of device biometric authentication hardware and enrollment.
     */
    fun checkBiometricAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability.Unavailable("This device does not have biometric hardware (fingerprint or face sensor).")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability.Unavailable("Biometric hardware is currently unavailable. Please try again later.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability.Unavailable("No biometric credentials are enrolled. Please set up fingerprint or face authentication in your device's system settings.")
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricAvailability.Unavailable("A security update is required before biometric authentication can be used.")
            }
            else -> {
                BiometricAvailability.Unavailable("Biometric authentication is not supported or unavailable on this device.")
            }
        }
    }

    /**
     * Triggers the official Android BiometricPrompt to unlock the application.
     */
    fun promptBiometricUnlock(
        activity: FragmentActivity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Unlock TravelTrace",
            subtitle = "Verify your fingerprint or face to open TravelTrace",
            onSuccess = {
                unlock()
                onSuccess()
            },
            onError = onError,
            onCancel = onCancel
        )
    }
}
