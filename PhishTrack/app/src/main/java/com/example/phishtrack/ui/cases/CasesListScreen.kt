package com.example.phishtrack.ui.cases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.phishtrack.data.api.CaseResponse
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishtrack.ui.components.EmptyStateComponent
import com.example.phishtrack.ui.components.ErrorStateComponent
import com.example.phishtrack.utils.UiState
import com.example.phishtrack.ui.dashboard.CaseItemCard
import com.example.phishtrack.ui.dashboard.EmptyCasesPlaceholder
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.phishtrack.ui.theme.shimmerEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesListScreen(
    viewModel: CasesListViewModel = hiltViewModel(),
    initialDateFilter: String? = null,
    onClearDateFilter: () -> Unit = {},
    onCaseClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("All") }
    var selectedPriority by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf(initialDateFilter) }
    var sortBy by remember { mutableStateOf("Date") }

    var showDatePicker by remember { mutableStateOf(false) }
    val sixMonthsAgo = remember {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MONTH, -6)
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(
        initialDisplayedMonthMillis = sixMonthsAgo,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        selectedDate = sdf.format(java.util.Date(millis))
                    }
                }) {
                    Text("OK", color = Color(0x00, 0xF5, 0xFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Color(0x88, 0x92, 0xB0))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0x14, 0x18, 0x29))
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = Color(0x00, 0xF5, 0xFF),
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color(0x88, 0x92, 0xB0),
                    subheadContentColor = Color.White,
                    yearContentColor = Color.White,
                    currentYearContentColor = Color(0x00, 0xF5, 0xFF),
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = Color(0x00, 0xF5, 0xFF),
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = Color(0x00, 0xF5, 0xFF),
                    todayContentColor = Color(0x00, 0xF5, 0xFF),
                    todayDateBorderColor = Color(0x00, 0xF5, 0xFF)
                )
            )
        }
    }

    val casesList by viewModel.casesList.collectAsState()
    val refreshState by viewModel.refreshState

    val isRefreshing = refreshState is UiState.Loading

    fun refresh() {
        val statusParam = if (selectedStatus == "All") null else selectedStatus
        val priorityParam = if (selectedPriority == "All") null else selectedPriority
        viewModel.refreshCases(statusParam, priorityParam, selectedDate)
    }

    LaunchedEffect(selectedStatus, selectedPriority, selectedDate) {
        refresh()
    }

    val filteredCases = remember(casesList, searchQuery, sortBy) {
        var list = casesList.filter { case ->
            case.caseNumber.contains(searchQuery, ignoreCase = true) ||
            case.displayTarget().contains(searchQuery, ignoreCase = true) ||
            (case.title ?: "").contains(searchQuery, ignoreCase = true)
        }
        list = when (sortBy) {
            "Priority" -> {
                val weight = mapOf("Critical" to 4, "High" to 3, "Medium" to 2, "Low" to 1)
                list.sortedByDescending { weight[it.priority] ?: 0 }
            }
            "Status" -> list.sortedBy { it.status }
            else -> list.sortedByDescending { it.createdAt }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
            ))
            .padding(16.dp)
    ) {
        // Search + Refresh row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by URL, IP, or Case Number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0x88, 0x92, 0xB0)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedBorderColor = Color(0x2A, 0x35, 0x58),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedLabelColor = Color(0x88, 0x92, 0xB0)
                ),
                modifier = Modifier.weight(1f).testTag("searchField")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { refresh() },
                enabled = !isRefreshing,
                modifier = Modifier.size(48.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = Color(0x00, 0xF5, 0xFF),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh cases",
                        tint = Color(0x00, 0xF5, 0xFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Date Filter: ", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(if (selectedDate != null) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29), RoundedCornerShape(20.dp))
                    .border(1.dp, if (selectedDate != null) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58), RoundedCornerShape(20.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(selectedDate ?: "All Time", color = if (selectedDate != null) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (selectedDate != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Clear Date",
                    tint = Color(0xFF, 0x3B, 0x3B),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { 
                            selectedDate = null
                            onClearDateFilter()
                        }
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val statusChips = listOf("All", "Open", "Investigating", "Closed", "False_Positive")
            items(statusChips) { chip ->
                val isSelected = selectedStatus == chip
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58), RoundedCornerShape(20.dp))
                        .clickable { selectedStatus = chip }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chip, color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val priorityChips = listOf("All", "Low", "Medium", "High", "Critical")
            items(priorityChips) { chip ->
                val isSelected = selectedPriority == chip
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58), RoundedCornerShape(20.dp))
                        .clickable { selectedPriority = chip }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chip, color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort by: ", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            listOf("Date", "Priority", "Status").forEach { opt ->
                val isSelected = sortBy == opt
                Text(
                    text = opt,
                    color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.clickable { sortBy = opt }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (refreshState is UiState.Error && filteredCases.isEmpty()) {
            val errorMsg = (refreshState as UiState.Error).message
            ErrorStateComponent(message = errorMsg, onRetry = { refresh() }, modifier = Modifier.fillMaxHeight(0.5f))
        } else if (filteredCases.isEmpty() && isRefreshing) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(10.dp))
                            .shimmerEffect()
                    )
                }
            }
        } else if (filteredCases.isEmpty()) {
            val message = if (selectedStatus != "All") "No $selectedStatus cases found." else "No cases yet"
            EmptyStateComponent(message = message, modifier = Modifier.fillMaxHeight(0.5f))
        } else {
            val pullRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refresh() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    if (refreshState is UiState.Error) {
                        item {
                            val errorMsg = (refreshState as UiState.Error).message
                            ErrorStateComponent(message = errorMsg, onRetry = { refresh() })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    items(filteredCases, key = { it.id }) { case ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }),
                        ) {
                            CaseItemCard(case = case, onClick = { onCaseClick(case.id) })
                        }
                    }
                }
            }
        }
    }
}