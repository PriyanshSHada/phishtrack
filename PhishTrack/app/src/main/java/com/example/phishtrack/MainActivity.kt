package com.example.phishtrack

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.phishtrack.data.repository.AuthRepository
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.theme.PhishTrackTheme
import com.example.phishtrack.utils.NetworkMonitor
import com.example.phishtrack.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.*
import com.example.phishtrack.ui.auth.UiState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.example.phishtrack.BuildConfig
import com.example.phishtrack.data.api.VersionResponse

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

  @Inject lateinit var authRepository: AuthRepository
  @Inject lateinit var casesRepository: CasesRepository
  @Inject lateinit var tokenManager: TokenManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val networkMonitor = NetworkMonitor(applicationContext)
    lifecycleScope.launch {
      networkMonitor.isOnline.collect { isOnline ->
        if (isOnline && authRepository.isLoggedIn()) {
          casesRepository.refreshCases()
        }
      }
    }

    enableEdgeToEdge()
    setContent {
      PhishTrackTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            
          var needsUpdate by remember { mutableStateOf(false) }

          LaunchedEffect(Unit) {
            authRepository.checkVersion().collect { result ->
                result.onSuccess { versionData ->
                    val minVersion = versionData.minVersion.split(".").map { it.toIntOrNull() ?: 0 }
                    val currentVersion = BuildConfig.VERSION_NAME.split(".").map { it.toIntOrNull() ?: 0 }
                    
                    // Simple version comparison assuming semver "major.minor.patch"
                    for (i in 0 until maxOf(minVersion.size, currentVersion.size)) {
                        val min = minVersion.getOrElse(i) { 0 }
                        val curr = currentVersion.getOrElse(i) { 0 }
                        if (curr < min) {
                            needsUpdate = true
                            break
                        } else if (curr > min) {
                            break
                        }
                    }
                }
            }
          }

          if (needsUpdate) {
              AlertDialog(
                  onDismissRequest = { /* Block dismissal */ },
                  title = { Text("Update Required") },
                  text = {
                      Column {
                          Text("Your version of PhishTrack is outdated and no longer supported. Please update to the latest version to continue.")
                          Spacer(modifier = Modifier.height(8.dp))
                          Text("Current Version: ${BuildConfig.VERSION_NAME}", color = Color.Gray)
                      }
                  },
                  confirmButton = {
                      Button(onClick = {
                          // In a real app, open Play Store link here
                          finish()
                      }) {
                          Text("Close App")
                      }
                  }
              )
          } else {
              MainNavigation(
                  authRepository = authRepository,
                  casesRepository = casesRepository,
                  tokenManager = tokenManager
              )
          }
        }
      }
    }
  }
}

