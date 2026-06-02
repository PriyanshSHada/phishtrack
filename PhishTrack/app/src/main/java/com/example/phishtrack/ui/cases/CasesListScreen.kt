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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.dashboard.CaseItemCard
import com.example.phishtrack.ui.dashboard.EmptyCasesPlaceholder
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesListScreen(
    casesRepository: CasesRepository,
    onCaseClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Date") } // Date, Priority, Status

    val casesList by casesRepository.cachedCasesFlow.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        casesRepository.refreshCases()
    }

    val filteredCases = remember(casesList, searchQuery, selectedFilter, sortBy) {
        var list = casesList.filter { case ->
            case.case_number.contains(searchQuery, ignoreCase = true) ||
            case.url.contains(searchQuery, ignoreCase = true)
        }

        // Apply Status & Priority filters
        list = when (selectedFilter) {
            "Open" -> list.filter { it.status == "Open" || it.status == "Investigating" }
            "Closed" -> list.filter { it.status == "Closed" }
            "High" -> list.filter { it.priority == "High" }
            "Critical" -> list.filter { it.priority == "Critical" }
            else -> list
        }

        // Apply Sorting
        list = when (sortBy) {
            "Priority" -> {
                val weight = mapOf("Critical" to 4, "High" to 3, "Medium" to 2, "Low" to 1)
                list.sortedByDescending { weight[it.priority] ?: 0 }
            }
            "Status" -> list.sortedBy { it.status }
            else -> list.sortedByDescending { it.created_at } // Date desc
        }

        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A))
            .padding(16.dp)
    ) {
        // Search Bar
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val chips = listOf("All", "Open", "Closed", "High", "Critical")
            items(chips) { chip ->
                val isSelected = selectedFilter == chip
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilter = chip }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chip,
                        color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sort by: ", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            
            val sortOptions = listOf("Date", "Priority", "Status")
            sortOptions.forEach { opt ->
                val isSelected = sortBy == opt
                Text(
                    text = opt,
                    color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clickable { sortBy = opt }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List Column
        if (filteredCases.isEmpty()) {
            EmptyCasesPlaceholder()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCases) { case ->
                    CaseItemCard(case = case, onClick = { onCaseClick(case.id) })
                }
            }
        }
    }
}
