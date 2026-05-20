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
import com.onmi.qing.ui.navigation.TestNavHost
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : ComponentActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, TestActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as QingApplication

        setContent {
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = UserPreferences()
            )
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) systemDark else userPrefs.isDarkTheme

            QingTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestNavHost(
                        navController = navController,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
