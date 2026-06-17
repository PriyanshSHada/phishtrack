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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.dashboard.CaseItemCard
import com.example.phishtrack.ui.dashboard.EmptyCasesPlaceholder
import kotlinx.coroutines.launch
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesListScreen(
    casesRepository: CasesRepository,
    initialDateFilter: String? = null,
    onClearDateFilter: () -> Unit = {},
    onCaseClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("All") }
    var selectedPriority by remember { mutableStateOf("All") }
    var selectedDate by remember { mutableStateOf(initialDateFilter) }
    var sortBy by remember { mutableStateOf("Date") }
    var isRefreshing by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val casesList by casesRepository.cachedCasesFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    fun refresh() {
        coroutineScope.launch {
            isRefreshing = true
            errorMessage = null
            val statusParam = if (selectedStatus == "All") null else selectedStatus
            val priorityParam = if (selectedPriority == "All") null else selectedPriority
            val result = casesRepository.refreshCases(statusParam, priorityParam, selectedDate)
            if (result.isFailure) {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load cases"
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(selectedStatus, selectedPriority, selectedDate) {
        val statusParam = if (selectedStatus == "All") null else selectedStatus
        val priorityParam = if (selectedPriority == "All") null else selectedPriority
        casesRepository.refreshCases(statusParam, priorityParam, selectedDate)
    }

    val filteredCases = remember(casesList, searchQuery, sortBy) {
        var list = casesList.filter { case ->
            case.caseNumber.contains(searchQuery, ignoreCase = true) ||
            case.url.contains(searchQuery, ignoreCase = true)
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
            .background(Color(0x0A, 0x0E, 0x1A))
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
                placeholder = { Text("Search by URL or Case Number...") },
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

        if (selectedDate != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Date Filter: ", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF, 0x3B, 0x3B), RoundedCornerShape(20.dp))
                        .clickable {
                            selectedDate = null
                            onClearDateFilter()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$selectedDate ✕", color = Color(0xFF, 0x3B, 0x3B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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

        if (errorMessage != null && filteredCases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f)
                    .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Connection Error", color = Color(0xFF, 0x55, 0x55), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "Unknown error", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { refresh() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.2f))) {
                        Text("Try Again", color = Color(0xFF, 0x55, 0x55))
                    }
                }
            }
        } else if (filteredCases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp)
                    .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val message = if (selectedStatus != "All") "No $selectedStatus cases found." else "No cases found."
                Text(message, color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp, fontWeight = FontWeight.Normal)
            }
        } else {
            val pullRefreshState = rememberPullToRefreshState()

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refresh() },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredCases) { case ->
                        CaseItemCard(case = case, onClick = { onCaseClick(case.id) })
                    }
                }
            }
        }
    }
}