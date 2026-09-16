package com.travelhistory.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Result of checking device biometric authentication hardware and enrollment.
 */
sealed class BiometricAvailability {
    data object Available : BiometricAvailability()
    data class Unavailable(val reason: String) : BiometricAvailability()
}

/**
 * Manager for handling Android BiometricPrompt authentication.
 *
 * IMPORTANT SECURITY RULES:
 * - Never stores biometric data or credentials.
 * - Leaves all biometric verification to the Android operating system.
 * - Does not bypass authentication under any circumstance.
 */
object BiometricAuthManager {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    /**
     * Checks if biometric authentication is available and ready on the device.
     */
    fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability.Unavailable("This device does not have biometric hardware.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability.Unavailable("Biometric hardware is currently unavailable.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability.Unavailable("No biometric credentials are enrolled on this device. Please set up fingerprint or face authentication in system settings.")
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricAvailability.Unavailable("A security update is required before biometric authentication can be used.")
            }
            else -> {
                BiometricAvailability.Unavailable("Biometric authentication is required to export location data.")
            }
        }
    }

    /**
     * Shows the official Android BiometricPrompt.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate to export your location history.",
        subtitle: String = "Verify your identity to proceed with data export",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            onCancel()
                        }
                        else -> {
                            onError(
                                errString.toString().ifBlank {
                                    "Biometric authentication is required to export location data."
                                }
                            )
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric rejected by sensor (e.g. unrecognized fingerprint).
                    // The system dialog remains visible allowing retries.
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
