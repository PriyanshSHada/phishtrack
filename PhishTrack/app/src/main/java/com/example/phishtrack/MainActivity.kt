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

