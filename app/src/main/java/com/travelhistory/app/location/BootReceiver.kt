package com.travelhistory.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.travelhistory.app.data.IntervalPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling device boot completion.
 *
 * IMPORTANT:
 * Per Requirement 10, tracking NEVER resumes automatically after reboot
 * unless the user explicitly enabled "Resume after device restart" in Settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = IntervalPreferences(context)
                    val shouldResume = prefs.resumeAfterRebootFlow.first()
                    val wasTrackingActive = prefs.isTrackingActiveFlow.first()
                    val tracker = LocationTracker(context)
                    val hasPermission = tracker.hasLocationPermission()

                    if (shouldResume && wasTrackingActive && hasPermission) {
                        LocationTrackingService.start(context)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
