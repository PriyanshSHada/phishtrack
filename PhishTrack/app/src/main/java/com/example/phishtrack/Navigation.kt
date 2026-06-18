package com.example.phishtrack

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.phishtrack.data.repository.AuthRepository
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.analysis.AnalysisLoadingScreen
import com.example.phishtrack.ui.auth.AuthViewModel
import com.example.phishtrack.ui.auth.LoginScreen
import com.example.phishtrack.ui.auth.OtpVerifyScreen
import com.example.phishtrack.ui.auth.SignUpScreen
import com.example.phishtrack.ui.auth.SecurityCheckScreen
import com.example.phishtrack.ui.main.MainScreen
import com.example.phishtrack.ui.newcase.NewCaseScreen
import com.example.phishtrack.ui.report.ReportScreen
import com.example.phishtrack.ui.splash.SplashScreen
import com.example.phishtrack.ui.update.UpdateRequiredScreen
import com.example.phishtrack.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(
    authRepository: AuthRepository,
    casesRepository: CasesRepository,
    tokenManager: TokenManager
) {
  // Start destination: Splash Screen
  val backStack = rememberNavBackStack(Splash)

  BackHandler(enabled = backStack.size > 1) {
      backStack.removeLastOrNull()
  }

  when (val destination = backStack.lastOrNull() ?: Splash) {
        Splash -> {
          SplashScreen(
              tokenManager = tokenManager,
              authRepository = authRepository,
              onNavigateNext = { isLoggedIn ->
                  if (isLoggedIn) {
                      backStack.replaceTop(SecurityCheck)
                  } else {
                      backStack.replaceTop(Login)
                  }
              },
              onUpdateRequired = { updateUrl ->
                  backStack.replaceTop(UpdateRequired(updateUrl))
              }
          )
        }

        is UpdateRequired -> {
            UpdateRequiredScreen(updateUrl = destination.updateUrl)
        }

        Login -> {
          val authViewModel: AuthViewModel = hiltViewModel()
          LoginScreen(
              viewModel = authViewModel,
              onNavigateToSignUp = { backStack.add(SignUp) },
              onLoginSuccess = { email, token, userId ->
                  backStack.add(OtpVerify(email = email))
              },
              onBiometricClick = {
                  // Quick bypass for biometric: log in directly to Main if session exists
                  if (tokenManager.getToken() != null) {
                      backStack.replaceTop(Main)
                  }
              }
          )
        }

        SignUp -> {
          val authViewModel: AuthViewModel = hiltViewModel()
          SignUpScreen(
              viewModel = authViewModel,
              onNavigateToLogin = { backStack.removeLastOrNull() },
              onRegisterSuccess = {
                  backStack.replaceTop(Login)
              }
          )
        }

        is OtpVerify -> {
          val authViewModel: AuthViewModel = hiltViewModel()
          OtpVerifyScreen(
              email = destination.email,
              viewModel = authViewModel,
              onVerificationSuccess = { token, userId ->
                  backStack.removeLastOrNull() // Remove OtpVerify
                  backStack.replaceTop(Main)
              },
              onBackToLogin = {
                  backStack.removeLastOrNull()
              }
          )
        }

        SecurityCheck -> {
          SecurityCheckScreen(
              authRepository = authRepository,
              onSuccess = {
                  backStack.replaceTop(Main)
              },
              onLogout = {
                  backStack.replaceTop(Login)
              }
          )
        }

         Main -> {
           MainScreen(
               authRepository = authRepository,
               casesRepository = casesRepository,
               onNewCaseClick = { backStack.add(NewCase) },
               onCaseClick = { caseId -> backStack.add(Report(caseId = caseId)) },
               onLogoutClick = {
                   authRepository.logout()
                   backStack.replaceTop(Login)
               }
           )
         }

        NewCase -> {
          val coroutineScope = rememberCoroutineScope()
          val context = LocalContext.current
          NewCaseScreen(
              onBackClick = { backStack.removeLastOrNull() },
              onSubmitCase = { title, url, desc, src, priority, tags ->
                  coroutineScope.launch {
                      casesRepository.createCase(title, url, desc, src, priority, tags).collect { result ->
                          result.fold(
                              onSuccess = { caseResponse ->
                                  backStack.replaceTop(AnalysisLoading(caseId = caseResponse.id))
                              },
                              onFailure = { err ->
                                  Toast.makeText(context, "Failed to create case: ${err.message}", Toast.LENGTH_LONG).show()
                              }
                          )
                      }
                  }
              }
          )
        }

        is AnalysisLoading -> {
          AnalysisLoadingScreen(
              caseId = destination.caseId,
              casesRepository = casesRepository,
              onAnalysisComplete = { caseId ->
                  backStack.replaceTop(Report(caseId = caseId))
              },
              onBackOnError = {
                  backStack.removeLastOrNull()
              }
          )
        }

        is Report -> {
          ReportScreen(
              caseId = destination.caseId,
              onBackClick = { backStack.removeLastOrNull() }
          )
        }
        is CaseDetail -> {
            backStack.replaceTop(Report(caseId = destination.caseId))
        }
  }
}

private fun MutableList<NavKey>.replaceTop(destination: NavKey) {
    if (isEmpty()) {
        add(destination)
    } else {
        this[lastIndex] = destination
    }
}
