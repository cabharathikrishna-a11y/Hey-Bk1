package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.example.system.DailyReminderWorker
import com.example.system.MediaStorageHelper
import com.example.ui.screens.HeyBkApp
import com.example.ui.state.HeyBkViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: HeyBkViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchIntent(intent)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            HeyBkApp(
                viewModel = viewModel,
                windowSizeClass = windowSizeClass
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        val entryId = intent.getStringExtra(MediaStorageHelper.EXTRA_ENTRY_ID)
        if (!entryId.isNullOrBlank()) {
            viewModel.openEntry(entryId)
            return
        }
        if (intent.getBooleanExtra(DailyReminderWorker.EXTRA_OPEN_NEW_ENTRY, false)) {
            viewModel.startNewEntry()
        }
    }
}
