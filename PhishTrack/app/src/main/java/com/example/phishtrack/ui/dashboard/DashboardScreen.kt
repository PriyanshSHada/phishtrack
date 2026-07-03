package com.example.phishtrack.ui.dashboard

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.gson.JsonObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.api.StatsResponse
import com.example.phishtrack.data.api.ThreatLocation
import com.example.phishtrack.data.api.WeeklyGraphData
import com.example.phishtrack.utils.UiState
import com.example.phishtrack.ui.components.EmptyStateComponent
import com.example.phishtrack.ui.components.ErrorStateComponent
import com.example.phishtrack.ui.theme.shimmerEffect

@Composable
fun DashboardScreen(
    analystName: String,
    viewModel: DashboardViewModel,
    onNewCaseClick: () -> Unit,
    onCaseClick: (String) -> Unit,
    onBottomNavClick: (String) -> Unit,
    onDateFilterClick: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val statsState by viewModel.statsState
    val recentCasesState by viewModel.recentCasesState
    val threatMapState by viewModel.threatMapState
    val weeklyGraphState by viewModel.weeklyGraphState

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
        )),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCaseClick,
                containerColor = Color(0x00, 0xF5, 0xFF),
                contentColor = Color(0x0A, 0x0E, 0x1A),
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Case")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $analystName 👋",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "PhishTrack Security Operations Center",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics Cards Row
            when (statsState) {
                is UiState.Success -> {
                    val stats = (statsState as UiState.Success).data
                    MetricsGrid(stats)
                }
                is UiState.Loading -> {
                    MetricsGridPlaceholder()
                }
                is UiState.Error -> {
                    val errorMsg = (statsState as UiState.Error).message
                    ErrorStateComponent(message = errorMsg, onRetry = { viewModel.refresh() })
                }
                else -> {
                    MetricsGrid(StatsResponse(highRisk = 0, cases = 0, analyses = 0, reports = 0))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Satellite Threat Map
            Text(
                text = "SATELLITE THREAT MAP",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            when (threatMapState) {
                is UiState.Success -> {
                    val locations = (threatMapState as UiState.Success).data
                    ThreatRadarMapCard(locations)
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                    }
                }
                is UiState.Error -> {
                    val errorMsg = (threatMapState as UiState.Error).message
                    ErrorStateComponent(message = errorMsg, onRetry = { viewModel.refresh() })
                }
                else -> {
                    ThreatRadarMapCard(emptyList())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Graph Chart — title is rendered inside WeeklyHeatmapSection
            when (weeklyGraphState) {
                is UiState.Success -> {
                    val weeklyData = (weeklyGraphState as UiState.Success).data
                    WeeklyHeatmapSection(
                        weeklyData = weeklyData.currentWeek,
                        selectedMonth = viewModel.selectedMonth.value,
                        selectedYear = viewModel.selectedYear.value,
                        onMonthYearSelected = viewModel::onMonthYearSelected,
                        onDateSelected = onDateFilterClick
                    )
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                    }
                }
                is UiState.Error -> {
                    val errorMsg = (weeklyGraphState as UiState.Error).message
                    ErrorStateComponent(message = errorMsg, onRetry = { viewModel.refresh() })
                }
                else -> {
                    WeeklyHeatmapSection(
                        weeklyData = emptyList(),
                        selectedMonth = viewModel.selectedMonth.value,
                        selectedYear = viewModel.selectedYear.value,
                        onMonthYearSelected = viewModel::onMonthYearSelected,
                        onDateSelected = onDateFilterClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Cases
            Text(
                text = "RECENT INVESTIGATIVE CASES",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (recentCasesState) {
                is UiState.Success -> {
                    val cases = (recentCasesState as UiState.Success).data
                    if (cases.isEmpty()) {
                        EmptyStateComponent(message = "No analysis data yet")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 500.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(cases, key = { it.id }) { case ->
                                CaseItemCard(case = case, onClick = { onCaseClick(case.id) })
                            }
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                    }
                }
                is UiState.Error -> {
                    val errorMsg = (recentCasesState as UiState.Error).message
                    ErrorStateComponent(message = errorMsg, onRetry = { viewModel.refresh() })
                }
                else -> {
                    EmptyStateComponent(message = "No analysis data yet")
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // padding for floating button / nav
        }
    }
}

@Composable
fun MetricsGrid(stats: StatsResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(title = "High Risk", value = stats.highRisk.toString(), color = Color(0xFF, 0x3B, 0x3B), modifier = Modifier.weight(1f), trend = "↑")
        MetricCard(title = "Cases", value = stats.cases.toString(), color = Color(0x00, 0xF5, 0xFF), modifier = Modifier.weight(1f), trend = "↑")
        MetricCard(title = "Scans", value = stats.analyses.toString(), color = Color(0x00, 0xFF, 0x88), modifier = Modifier.weight(1f), trend = "↑")
        MetricCard(title = "Reports", value = stats.reports.toString(), color = Color(0xFF, 0xA5, 0x00), modifier = Modifier.weight(1f), trend = "↑")
    }
}

@Composable
fun MetricsGridPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x2A, 0x35, 0x58).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier, trend: String? = null) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF141829), Color(0xFF0D1020))
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color(0xFF8892B0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    color = color,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (trend != null) {
                    val trendIcon = if (trend.startsWith("↑")) "↑" else "↓"
                    val trendColor = if (trend.startsWith("↑")) Color(0x00, 0xFF, 0x88) else Color(0xFF, 0x3B, 0x3B)
                    Text(
                        text = trendIcon,
                        color = trendColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatRadarMapCard(locations: List<ThreatLocation>, modifier: Modifier = Modifier) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var selectedThreat by remember { mutableStateOf<ThreatLocation?>(null) }
    var mapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredLocations = remember(searchQuery, locations) {
        if (searchQuery.isBlank()) locations else {
            locations.filter { 
                it.ip?.contains(searchQuery, ignoreCase = true) == true || 
                it.url?.contains(searchQuery, ignoreCase = true) == true || 
                it.caseNumber?.contains(searchQuery, ignoreCase = true) == true 
            }
        }
    }
    val hasRealData = locations.isNotEmpty()

    Column {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x0D, 0x14, 0x26)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            Column {
                // ── Map always fixed 240dp ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapLibre.getInstance(ctx)
                        val options = org.maplibre.android.maps.MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)
                        MapView(ctx, options).also { mv ->
                            mv.onCreate(null)
                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_START   -> mv.onStart()
                                    Lifecycle.Event.ON_RESUME  -> mv.onResume()
                                    Lifecycle.Event.ON_PAUSE   -> mv.onPause()
                                    Lifecycle.Event.ON_STOP    -> mv.onStop()
                                    Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                                    else -> {}
                                }
                            }
                            lifecycle.addObserver(observer)

                            mv.getMapAsync { map ->
                                mapRef = map
                                // Remove MapLibre logo/attribution from the map
                                map.uiSettings.isAttributionEnabled = false
                                map.uiSettings.isLogoEnabled = false
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(20.0, 10.0))
                                    .zoom(1.0)
                                    .build()

                                // Satellite map using ESRI World Imagery (free, no API key)
                                // glyphs are required for SymbolLayer text labels
                                val satelliteMapStyle = """
                                {
                                  "version": 8,
                                  "name": "Satellite",
                                  "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
                                  "sources": {
                                    "esri-satellite": {
                                      "type": "raster",
                                      "tiles": [
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                                      ],
                                      "tileSize": 256,
                                      "attribution": "© Esri, Maxar, Earthstar Geographics, and the GIS User Community"
                                    }
                                  },
                                  "layers": [
                                    {
                                      "id": "satellite-layer",
                                      "type": "raster",
                                      "source": "esri-satellite"
                                    }
                                  ]
                                }
                                """.trimIndent()

                                map.setStyle(Style.Builder().fromJson(satelliteMapStyle)) { style ->
                                    val features = filteredLocations.mapIndexedNotNull { idx, loc ->
                                        if (loc.latitude != null && loc.longitude != null) {
                                            val props = JsonObject().apply {
                                                val priorityStr = (loc.priority ?: "Low").lowercase()
                                                addProperty("color", when (priorityStr) {
                                                    "critical", "high" -> "#FF3B3B"
                                                    "medium" -> "#FFA500"
                                                    else -> "#00CC66"
                                                })
                                                // Numeric severity for cluster color pick (highest wins)
                                                addProperty("severity", when (priorityStr) {
                                                    "critical" -> 4
                                                    "high" -> 3
                                                    "medium" -> 2
                                                    else -> 1
                                                })
                                                addProperty("score", loc.threatScore ?: 0)
                                                addProperty("index", idx)
                                            }
                                            Feature.fromGeometry(
                                                Point.fromLngLat(loc.longitude ?: 0.0, loc.latitude ?: 0.0),
                                                props
                                            )
                                        } else null
                                    }

                                    // Clustered GeoJson source — MapLibre merges nearby pins automatically
                                    style.addSource(
                                        GeoJsonSource(
                                            "threats",
                                            FeatureCollection.fromFeatures(features),
                                            GeoJsonOptions()
                                                .withCluster(true)
                                                .withClusterRadius(50)       // px radius to merge
                                                .withClusterMaxZoom(12)       // stop clustering at zoom 12+
                                        )
                                    )

                                    // ── Layer 1: Cluster circle (big glow ring) ─────────────────
                                    style.addLayer(
                                        CircleLayer("cluster-glow", "threats")
                                            .withFilter(has("point_count"))   // only clusters
                                            .withProperties(
                                                circleRadius(24f),
                                                circleColor("#FF3B3B"),
                                                circleOpacity(0.20f),
                                                circleBlur(0.8f)
                                            )
                                    )

                                    // ── Layer 2: Cluster circle (solid badge) ───────────────────
                                    style.addLayer(
                                        CircleLayer("cluster-circle", "threats")
                                            .withFilter(has("point_count"))
                                            .withProperties(
                                                circleRadius(16f),
                                                circleColor("#FF3B3B"),
                                                circleStrokeColor("#FFFFFF"),
                                                circleStrokeWidth(2f)
                                            )
                                    )

                                    // ── Layer 3: Cluster count label ("3", "7" etc.) ────────────
                                    style.addLayer(
                                        SymbolLayer("cluster-count", "threats")
                                            .withFilter(has("point_count"))
                                            .withProperties(
                                                textField(Expression.toString(get("point_count"))),
                                                textSize(13f),
                                                textColor("#FFFFFF"),
                                                textIgnorePlacement(true),
                                                textAllowOverlap(true)
                                            )
                                    )

                                    // ── Layer 4: Individual unclustered glow ────────────────────
                                    style.addLayer(
                                        CircleLayer("threats-glow", "threats")
                                            .withFilter(not(has("point_count")))
                                            .withProperties(
                                                circleRadius(14f),
                                                circleColor(get("color")),
                                                circleOpacity(0.25f),
                                                circleBlur(1f)
                                            )
                                    )

                                    // ── Layer 5: Individual unclustered dot ─────────────────────
                                    style.addLayer(
                                        CircleLayer("threats-dot", "threats")
                                            .withFilter(not(has("point_count")))
                                            .withProperties(
                                                circleRadius(7f),
                                                circleColor(get("color")),
                                                circleStrokeColor("#FFFFFF"),
                                                circleStrokeWidth(2f)
                                            )
                                    )

                                    // ── Click handler ───────────────────────────────────────────
                                    map.addOnMapClickListener { point ->
                                        val screenPoint = map.projection.toScreenLocation(point)

                                        // Tap on cluster → zoom into it
                                        val clusterFeatures = map.queryRenderedFeatures(screenPoint, "cluster-circle")
                                        if (clusterFeatures.isNotEmpty()) {
                                            map.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(point, map.cameraPosition.zoom + 2.5)
                                            )
                                            return@addOnMapClickListener true
                                        }

                                        // Tap on individual dot → show detail panel
                                        val dotFeatures = map.queryRenderedFeatures(screenPoint, "threats-dot")
                                        if (dotFeatures.isNotEmpty()) {
                                            val idx = dotFeatures[0].getNumberProperty("index")?.toInt() ?: 0
                                            if (idx < filteredLocations.size) {
                                                val threat = filteredLocations[idx]
                                                selectedThreat = threat
                                                map.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(threat.latitude ?: 0.0, threat.longitude ?: 0.0),
                                                        4.0
                                                    )
                                                )
                                            }
                                        } else {
                                            selectedThreat = null
                                        }
                                        true
                                    }
                                }
                            }
                        }
                    }
                )

                LaunchedEffect(filteredLocations, mapRef) {
                    mapRef?.getStyle { style ->
                        val source = style.getSourceAs<GeoJsonSource>("threats")
                        if (source != null) {
                            val features = filteredLocations.mapIndexedNotNull { idx, loc ->
                                if (loc.latitude != null && loc.longitude != null) {
                                    val props = JsonObject().apply {
                                        val priorityStr = (loc.priority ?: "Low").lowercase()
                                        addProperty("color", when (priorityStr) {
                                            "critical", "high" -> "#FF3B3B"
                                            "medium" -> "#FFA500"
                                            else -> "#00CC66"
                                        })
                                        addProperty("severity", when (priorityStr) {
                                            "critical" -> 4; "high" -> 3; "medium" -> 2; else -> 1
                                        })
                                        addProperty("score", loc.threatScore ?: 0)
                                        addProperty("index", idx)
                                    }
                                    Feature.fromGeometry(
                                        // No jitter — exact coordinates, clustering handles overlaps
                                        Point.fromLngLat(loc.longitude ?: 0.0, loc.latitude ?: 0.0),
                                        props
                                    )
                                } else null
                            }
                            source.setGeoJson(FeatureCollection.fromFeatures(features))
                        }
                    }
                }

                // Zoom buttons — top right (always on top of map, never behind overlay)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MapZoomButton("+") { mapRef?.animateCamera(CameraUpdateFactory.zoomIn()) }
                    MapZoomButton("−") { mapRef?.animateCamera(CameraUpdateFactory.zoomOut()) }
                    MapZoomButton("⊟") {
                        mapRef?.animateCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder().target(LatLng(20.0, 10.0)).zoom(1.0).build()))
                        selectedThreat = null
                    }
                }

                // Status badge & Search — top left
                Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    if (hasRealData) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x0D, 0x14, 0x26).copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(7.dp).background(Color(0x00, 0xFF, 0x88), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE: ${locations.size} SITES",
                                color = Color(0x00, 0xFF, 0x88),
                                fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0D, 0x14, 0x26).copy(alpha = 0.9f))
                            .border(1.dp, Color(0x00, 0xF5, 0xFF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0x00, 0xF5, 0xFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { query -> 
                                searchQuery = query
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (filteredLocations.isNotEmpty()) {
                                    val match = filteredLocations.first()
                                    selectedThreat = match
                                    mapRef?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(match.latitude ?: 0.0, match.longitude ?: 0.0),
                                            4.0
                                        )
                                    )
                                }
                            }),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search IP, URL, Case...", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
            // ── Threat Panel — below map, inside Card, slides in smoothly ──
            AnimatedVisibility(
                visible = selectedThreat != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                selectedThreat?.let { threat ->
                    ThreatOverlay(threat) { selectedThreat = null }
                }
            }
        }
    }
}
}

@Composable
private fun MapZoomButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x0D, 0x14, 0x26).copy(alpha = 0.85f))
            .border(1.dp, Color(0x00, 0xF5, 0xFF).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0x00, 0xF5, 0xFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThreatOverlay(threat: ThreatLocation, onDismiss: () -> Unit) {
    val severityColor = when {
        (threat.threatScore ?: 0) >= 70 -> Color(0xFF, 0x3B, 0x3B)
        (threat.threatScore ?: 0) >= 40 -> Color(0xFF, 0xA5, 0x00)
        else -> Color(0x00, 0xFF, 0x88)
    }
    val isRealData = threat.caseId != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0x0D, 0x14, 0x26).copy(alpha = 0.94f))
            .border(2.dp, severityColor.copy(alpha = 0.8f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Drag handle + header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRealData) "🔴 THREAT INTELLIGENCE" else "📍 DEMO LOCATION",
                    color = severityColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(severityColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${threat.threatScore ?: 0}/100",
                            color = severityColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.2f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Text("✕", color = Color(0xFF, 0x55, 0x55), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Case number + URL (real data) or location (demo)
            if (isRealData) {
                Text(
                    text = threat.caseNumber ?: "N/A",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = threat.url ?: threat.ip ?: "No target",
                    color = Color(0x00, 0xF5, 0xFF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "${threat.city ?: "Unknown"}, ${threat.country ?: "Unknown"}",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row: Location | Priority | ISP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip("🌍", "${threat.city ?: "?"}, ${threat.country ?: "?"}")
                if (isRealData) InfoChip("⚡", threat.priority ?: threat.severity ?: "?")
                if (threat.isp != null) InfoChip("📡", threat.isp ?: "?")
            }

            if (isRealData) {
                Spacer(modifier = Modifier.height(10.dp))

                // AI Summary
                val summary = threat.aiSummary
                if (!summary.isNullOrEmpty()) {
                    Text(
                        text = "AI ANALYSIS",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        color = Color(0xCC, 0xCC, 0xCC),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Indicators
                val indicators = threat.aiIndicators
                if (indicators.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "THREAT INDICATORS",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        indicators.take(8).forEach { ind ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .border(0.5.dp, Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ind,
                                    color = Color(0xFF, 0x6B, 0x6B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This is a demo marker. Build threat data by creating cases and running analyses.",
                    color = Color(0x66, 0x66, 0x66),
                    fontSize = 9.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun InfoChip(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 9.sp)
        Spacer(modifier = Modifier.width(3.dp))
        Text(text, color = Color(0xAA, 0xAA, 0xAA), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WeeklyGraphCard(
    weeklyData: com.example.phishtrack.data.api.WeeklyDashboardResponse?,
    onDateSelected: (String) -> Unit,
    onNewCaseClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (weeklyData == null || weeklyData.currentWeek.isEmpty() || weeklyData.totalThisWeek == 0) {
                // Empty state
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("📊", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No scan data this week yet.",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onNewCaseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x00, 0xF5, 0xFF))
                    ) {
                        Text("New Case", color = Color(0x00, 0xF5, 0xFF), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Summary Line
                val diff = weeklyData.totalThisWeek - weeklyData.totalLastWeek
                val arrow = if (diff > 0) "↑" else if (diff < 0) "↓" else "−"
                val trendColor = if (diff > 0) Color(0xFF, 0x3B, 0x3B) else if (diff < 0) Color(0x00, 0xFF, 0x88) else Color(0x88, 0x92, 0xB0)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(text = "${weeklyData.totalThisWeek} scans this week", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "$arrow ${Math.abs(diff)} from last week", color = trendColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Chart
                val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
                val list = weeklyData.currentWeek
                
                // Animation states for stagger
                val animationProgress = list.mapIndexed { index, _ ->
                    val anim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 50L) // stagger
                        anim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                    anim.value
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(list) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val sectionWidth = w / list.size
                                val index = (offset.x / sectionWidth).toInt().coerceIn(0, list.size - 1)
                                onDateSelected(list[index].date)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val maxVal = list.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                    val barWidth = (w / list.size) - 16f
                    
                    val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

                    // Base line
                    drawLine(color = Color(0x2A, 0x35, 0x58), start = Offset(0f, h - 24f), end = Offset(w, h - 24f), strokeWidth = 2f)

                    list.forEachIndexed { i, data ->
                        val animVal = animationProgress[i]
                        val finalBarHeight = ((h - 48f) * data.count) / maxVal
                        // minimum visible bar height for 0-count
                        val actualBarHeight = (finalBarHeight.coerceAtLeast(4f)) * animVal
                        val rx = i * (w / list.size) + 8f
                        val ry = h - 24f - actualBarHeight
                        
                        val isToday = data.date == today
                        val barAlpha = if (isToday) 1f else 0.5f

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x00, 0xF5, 0xFF).copy(alpha = barAlpha), Color(0x4A, 0x9E, 0xFF).copy(alpha = barAlpha))
                            ),
                            topLeft = Offset(rx, ry), size = Size(barWidth, actualBarHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Count Label on top
                        if (actualBarHeight > 4f) {
                            val countText = textMeasurer.measure(
                                text = data.count.toString(),
                                style = androidx.compose.ui.text.TextStyle(color = Color(0x00, 0xF5, 0xFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = countText,
                                topLeft = Offset(rx + (barWidth - countText.size.width) / 2f, ry - countText.size.height - 4f)
                            )
                        }

                        // X-axis Day Label
                        val dateObj = try { java.time.LocalDate.parse(data.date) } catch (e: Exception) { null }
                        val dayStr = dateObj?.dayOfWeek?.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) ?: ""
                        val dayText = textMeasurer.measure(
                            text = if (isToday) "Today" else dayStr,
                            style = androidx.compose.ui.text.TextStyle(color = if (isToday) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0), fontSize = 10.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        )
                        drawText(
                            textLayoutResult = dayText,
                            topLeft = Offset(rx + (barWidth - dayText.size.width) / 2f, h - 20f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CaseItemCard(case: CaseResponse, onClick: () -> Unit) {
    val severityColor = when (case.priority) {
        "Critical" -> Color(0xFF, 0x3B, 0x3B)
        "High" -> Color(0xFF, 0xA5, 0x00)
        "Medium" -> Color(0xFF, 0xD7, 0x00)
        else -> Color(0x4A, 0x9E, 0xFF)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.caseNumber,
                    color = Color(0x88, 0x92, 0xB0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = case.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Priority Badge
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, severityColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = case.priority,
                        color = severityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = case.displayTarget(),
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.status.replace("_", " "),
                    color = when (case.status) {
                        "Open" -> Color(0x00, 0xF5, 0xFF)
                        "Closed" -> Color(0x00, 0xFF, 0x88)
                        "False_Positive" -> Color(0xFF, 0x3B, 0x3B)
                        else -> Color(0xFF, 0xA5, 0x00)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = case.createdAt.split("T").firstOrNull() ?: "",
                    color = Color(0x88, 0x92, 0xB0).copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun EmptyCasesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No cases found in this database.",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}