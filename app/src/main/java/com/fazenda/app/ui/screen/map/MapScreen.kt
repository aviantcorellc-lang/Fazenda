package com.fazenda.app.ui.screen.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.ui.viewmodel.CategoryViewModel
import com.fazenda.app.ui.viewmodel.ZoneViewModel
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    plants: List<PlantEntity>,
    zoneViewModel: ZoneViewModel = viewModel(),
    categoryViewModel: CategoryViewModel = viewModel(),
    onPlantClick: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val zones by zoneViewModel.zones.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val zoneMap = remember(zones) { zones.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val context = LocalContext.current

    val plantsWithCoords = remember(plants) {
        plants.filter { it.latitude != null && it.longitude != null }
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            mapView?.let { mv ->
                val ov = MyLocationNewOverlay(GpsMyLocationProvider(context), mv)
                ov.enableMyLocation()
                ov.enableFollowLocation()
                mv.overlays.add(ov)
                ov.myLocation?.let { mv.controller.animateTo(it) }
            }
        }
    }

    fun addMarkers(mv: MapView) {
        plantsWithCoords.forEach { plant ->
            val marker = Marker(mv)
            marker.position = GeoPoint(plant.latitude!!, plant.longitude!!)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = plant.name
            marker.subDescription = buildString {
                plant.zoneId?.let { id -> zoneMap[id]?.name?.let { append("$it · ") } }
                plant.categoryId?.let { id -> categoryMap[id]?.name?.let { append(it) } }
            }
            marker.setOnMarkerClickListener { m, _ ->
                m.showInfoWindow(); true
            }
            marker.infoWindow = object : org.osmdroid.views.overlay.infowindow.InfoWindow(
                android.R.layout.simple_list_item_2, mv
            ) {
                override fun onOpen(item: Any?) {
                    val m = item as? Marker ?: return
                    val tv1 = mView?.findViewById<android.widget.TextView>(android.R.id.text1)
                    val tv2 = mView?.findViewById<android.widget.TextView>(android.R.id.text2)
                    tv1?.text = m.title
                    tv2?.text = m.subDescription
                    tv2?.visibility = if (m.subDescription.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
                    mView?.setOnClickListener {
                        plantsWithCoords.firstOrNull { p ->
                            p.latitude == m.position.latitude && p.longitude == m.position.longitude
                        }?.let { onPlantClick(it.id) }
                    }
                }
                override fun onClose() {}
            }
            mv.overlays.add(marker)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мапа рослин") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            mapView?.let { mv ->
                                val ov = MyLocationNewOverlay(GpsMyLocationProvider(context), mv)
                                ov.enableMyLocation()
                                ov.enableFollowLocation()
                                mv.overlays.add(ov)
                                ov.myLocation?.let { mv.controller.animateTo(it) }
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Моє місцезнаходження")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (plantsWithCoords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Немає рослин з GPS координатами",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            setBuiltInZoomControls(false)
                            controller.setZoom(6.0)

                            val avgLat = plantsWithCoords.mapNotNull { it.latitude }.average()
                            val avgLon = plantsWithCoords.mapNotNull { it.longitude }.average()
                            controller.setCenter(GeoPoint(avgLat, avgLon))

                            val scaleBar = ScaleBarOverlay(this)
                            scaleBar.setAlignBottom(true)
                            scaleBar.setAlignRight(true)
                            overlays.add(scaleBar)

                            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                                override fun longPressHelper(p: GeoPoint?) = false
                            }))

                            addMarkers(this)
                            mapView = this
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}
