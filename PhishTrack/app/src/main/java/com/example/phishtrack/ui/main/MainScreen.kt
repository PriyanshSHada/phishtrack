package com.example.phishtrack.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishtrack.data.repository.AuthRepository
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.cases.CasesListScreen
import com.example.phishtrack.ui.dashboard.DashboardScreen
import com.example.phishtrack.ui.dashboard.DashboardViewModel
import com.example.phishtrack.ui.profile.ProfileScreen
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authRepository: AuthRepository,
    casesRepository: CasesRepository,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    onNewCaseClick: () -> Unit,
    onCaseClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var analystName by remember { mutableStateOf("SOC Analyst") }
    var selectedDateFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            withTimeout(10_000L) {
                authRepository.getProfile().collect { result ->
                    result.fold(
                        onSuccess = { analystName = it.name ?: "Analyst" },
                        onFailure = {} // Keep default name on error
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            // Backend didn't respond in time — keep the default "SOC Analyst" name
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0x14, 0x18, 0x29),
                contentColor = Color(0x88, 0x92, 0xB0)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { 
                        Icon(
                            Icons.Default.Dashboard, 
                            contentDescription = "Dashboard",
                            modifier = if (selectedTab == 0) Modifier else Modifier
                        )
                    },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0x00, 0xF5, 0xFF),
                        selectedTextColor = Color(0x00, 0xF5, 0xFF),
                        unselectedIconColor = Color(0x88, 0x92, 0xB0),
                        unselectedTextColor = Color(0x88, 0x92, 0xB0),
                        indicatorColor = Color(0x1E, 0x24, 0x40)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { 
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment, 
                            contentDescription = "Cases",
                            modifier = if (selectedTab == 1) Modifier else Modifier
                        )
                    },
                    label = { Text("Cases") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0x00, 0xF5, 0xFF),
                        selectedTextColor = Color(0x00, 0xF5, 0xFF),
                        unselectedIconColor = Color(0x88, 0x92, 0xB0),
                        unselectedTextColor = Color(0x88, 0x92, 0xB0),
                        indicatorColor = Color(0x1E, 0x24, 0x40)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "Profile",
                            modifier = if (selectedTab == 2) Modifier else Modifier
                        )
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0x00, 0xF5, 0xFF),
                        selectedTextColor = Color(0x00, 0xF5, 0xFF),
                        unselectedIconColor = Color(0x88, 0x92, 0xB0),
                        unselectedTextColor = Color(0x88, 0x92, 0xB0),
                        indicatorColor = Color(0x1E, 0x24, 0x40)
                    )
                )
            }
        },
        containerColor = Color(0x0A, 0x0E, 0x1A)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Dashboard Tab with fade animation
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DashboardScreen(
                    analystName = analystName,
                    viewModel = dashboardViewModel,
                    onNewCaseClick = onNewCaseClick,
                    onCaseClick = onCaseClick,
                    onBottomNavClick = { tab ->
                        when(tab) {
                            "Cases" -> selectedTab = 1
                            "Profile" -> selectedTab = 2
                        }
                    },
                    onDateFilterClick = { date ->
                        selectedDateFilter = date
                        selectedTab = 1
                    }
                )
            }

            // Cases Tab with fade animation
            AnimatedVisibility(
                visible = selectedTab == 1,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CasesListScreen(
                    initialDateFilter = selectedDateFilter,
                    onClearDateFilter = { selectedDateFilter = null },
                    onCaseClick = onCaseClick
                )
            }

            // Profile Tab with fade animation
            AnimatedVisibility(
                visible = selectedTab == 2,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ProfileScreen(
                    authRepository = authRepository,
                    casesRepository = casesRepository,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}
