package com.example.phishtrack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
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

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(
              tokenManager = tokenManager,
              onNavigateNext = { isLoggedIn ->
                  backStack.removeLastOrNull() // Remove splash
                  if (isLoggedIn) {
                      backStack.add(SecurityCheck)
                  } else {
                      backStack.add(Login)
                  }
              }
          )
        }

        entry<Login> {
          val authViewModel: AuthViewModel = hiltViewModel()
          LoginScreen(
              viewModel = authViewModel,
              onNavigateToSignUp = { backStack.add(SignUp) },
              onLoginSuccess = { email, token, userId ->
                  if (token != null) {
                      // Test account direct login bypasses verification
                      backStack.removeLastOrNull()
                      backStack.add(Main)
                  } else {
                      backStack.add(OtpVerify(email = email))
                  }
              },
              onBiometricClick = {
                  // Quick bypass for biometric: log in directly to Main if session exists or test bypass
                  if (tokenManager.getToken() != null) {
                      backStack.removeLastOrNull()
                      backStack.add(Main)
                  } else {
                      // Login via test account as biometric mock fallback
                      authViewModel.login("test@example.com", "Test@1234")
                  }
              }
          )
        }

        entry<SignUp> {
          val authViewModel: AuthViewModel = hiltViewModel()
          SignUpScreen(
              viewModel = authViewModel,
              onNavigateToLogin = { backStack.removeLastOrNull() },
              onRegisterSuccess = {
                  backStack.removeLastOrNull()
                  backStack.add(Login)
              }
          )
        }

        entry<OtpVerify> { key ->
          val authViewModel: AuthViewModel = hiltViewModel()
          OtpVerifyScreen(
              email = key.email,
              viewModel = authViewModel,
              onVerificationSuccess = { token, userId ->
                  backStack.removeLastOrNull() // Remove OtpVerify
                  backStack.removeLastOrNull() // Remove Login
                  backStack.add(Main)
              },
              onBackToLogin = {
                  backStack.removeLastOrNull()
              }
          )
        }

        entry<SecurityCheck> {
          SecurityCheckScreen(
              authRepository = authRepository,
              onSuccess = {
                  backStack.removeLastOrNull()
                  backStack.add(Main)
              },
              onLogout = {
                  backStack.removeLastOrNull()
                  backStack.add(Login)
              }
          )
        }

        entry<Main> {
          MainScreen(
              authRepository = authRepository,
              casesRepository = casesRepository,
              onNewCaseClick = { backStack.add(NewCase) },
              onCaseClick = { caseId -> backStack.add(Report(caseId = caseId)) },
              onLogoutClick = {
                  backStack.removeLastOrNull() // Remove Main
                  backStack.add(Login)
              }
          )
        }

        entry<NewCase> {
          val coroutineScope = rememberCoroutineScope()
          NewCaseScreen(
              onBackClick = { backStack.removeLastOrNull() },
              onSubmitCase = { url, desc, src, priority, tags ->
                  coroutineScope.launch {
                      casesRepository.createCase(url, desc, src, priority, tags).collect { result ->
                          result.fold(
                              onSuccess = { caseResponse ->
                                  backStack.removeLastOrNull() // Remove NewCase form
                                  backStack.add(AnalysisLoading(caseId = caseResponse.id))
                              },
                              onFailure = {}
                          )
                      }
                  }
              }
          )
        }

        entry<AnalysisLoading> { key ->
          AnalysisLoadingScreen(
              caseId = key.caseId,
              casesRepository = casesRepository,
              onAnalysisComplete = { caseId ->
                  backStack.removeLastOrNull() // Remove loading screen
                  backStack.add(Report(caseId = caseId))
              },
              onBackOnError = {
                  backStack.removeLastOrNull()
              }
          )
        }

        entry<Report> { key ->
          ReportScreen(
              caseId = key.caseId,
              casesRepository = casesRepository,
              onBackClick = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
