package com.onmi.qing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.data.datastore.UserPreferences
import com.onmi.qing.ui.navigation.ChatNavHost
import com.onmi.qing.ui.navigation.Screen
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SESSION_ID = "sessionId"
        private const val EXTRA_SHOW_HISTORY = "showHistory"

        fun createIntent(context: Context, sessionId: String? = null): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                if (sessionId != null) {
                    putExtra(EXTRA_SESSION_ID, sessionId)
                }
            }
        }

        fun createHistoryIntent(context: Context): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(EXTRA_SHOW_HISTORY, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val showHistory = intent.getBooleanExtra(EXTRA_SHOW_HISTORY, false)
        val application = application as QingApplication

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val startDestination = when {
                    showHistory -> Screen.History.route
                    sessionId != null -> Screen.Chat.createRoute(sessionId)
                    else -> Screen.Chat.createRoute()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        chatRepository = application.chatRepository,
                        demoModeManager = application.demoModeManager,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
