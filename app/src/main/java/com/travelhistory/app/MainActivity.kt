package com.travelhistory.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.travelhistory.app.security.AppLockManager
import com.travelhistory.app.ui.theme.TravelHistoryTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLockManager.onActivityCreated(this)
        enableEdgeToEdge()
        setContent {
            TravelHistoryTheme {
                TravelHistoryApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppLockManager.onActivityStarted(this)
    }

    override fun onStop() {
        super.onStop()
        AppLockManager.onActivityStopped()
    }
}
