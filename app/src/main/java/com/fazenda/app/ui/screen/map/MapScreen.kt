package com.fazenda.app.ui.screen.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Grass
import com.fazenda.app.ui.util.PhotoPathResolver
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fazenda.app.data.entity.PlantEntity
import com.fazenda.app.ui.viewmodel.CategoryViewModel
import com.fazenda.app.ui.viewmodel.ZoneViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.FileInputStream

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
        if (isGranted) addMyLocationOverlay(mapView, context)
    }

    var selectedPlantForSheet by remember { mutableStateOf<PlantEntity?>(null) }

    fun addMarkers(mv: MapView, bitmaps: Map<Long, Bitmap?>) {
        plantsWithCoords.forEach { plant ->
            val marker = Marker(mv)
            marker.position = GeoPoint(plant.latitude!!, plant.longitude!!)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = plant.name

            val photoBitmap = bitmaps[plant.id]
            if (photoBitmap != null) {
                marker.icon = BitmapDrawable(context.resources, photoBitmap)
            }

            marker.infoWindow = null
            marker.setOnMarkerClickListener { _, _ ->
                selectedPlantForSheet = plant
                true
            }
            mv.overlays.add(marker)
        }
    }

    LaunchedEffect(mapView, plantsWithCoords) {
        val mv = mapView ?: return@LaunchedEffect
        if (plantsWithCoords.isEmpty()) return@LaunchedEffect

        mv.post {
            if (plantsWithCoords.size == 1) {
                val p = plantsWithCoords.first()
                mv.controller.setZoom(15.0)
                mv.controller.setCenter(GeoPoint(p.latitude!!, p.longitude!!))
            } else {
                val north = plantsWithCoords.maxOf { it.latitude!! }
                val south = plantsWithCoords.minOf { it.latitude!! }
                val east = plantsWithCoords.maxOf { it.longitude!! }
                val west = plantsWithCoords.minOf { it.longitude!! }
                val latPad = ((north - south) * 0.1).coerceAtLeast(0.001)
                val lonPad = ((east - west) * 0.1).coerceAtLeast(0.001)
                val bb = BoundingBox(
                    north + latPad, east + lonPad,
                    south - latPad, west - lonPad
                )
                mv.zoomToBoundingBox(bb, true, 50)
            }
        }

        // Load photos on IO thread
        val bitmaps = withContext(Dispatchers.IO) {
            plantsWithCoords.associate { plant ->
                plant.id to loadThumbnail(context, plant.photoPath)
            }
        }

        // Clear old markers and re-add with photos
        mv.overlays.removeAll { it is Marker }
        addMarkers(mv, bitmaps)
        mv.invalidate()
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
                            addMyLocationOverlay(mapView, context)
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
                            controller.setZoom(3.0)

                            val scaleBar = ScaleBarOverlay(this)
                            scaleBar.setAlignBottom(true)
                            scaleBar.setAlignRight(true)
                            overlays.add(scaleBar)

                            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?) = false
                                override fun longPressHelper(p: GeoPoint?) = false
                            }))

                            // Add markers initially without photos (will be updated by LaunchedEffect)
                            addMarkers(this, emptyMap())
                            mapView = this
                        }
                    }
                )
            }
        }
    }

    if (selectedPlantForSheet != null) {
        val sheetState = rememberModalBottomSheetState()
        val plant = selectedPlantForSheet!!
        val zoneName = plant.zoneId?.let { zoneMap[it]?.name }
        val categoryName = plant.categoryId?.let { categoryMap[it]?.name }

        ModalBottomSheet(
            onDismissRequest = { selectedPlantForSheet = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photoModel = PhotoPathResolver.toAsyncImageModel(context, plant.photoPath)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (photoModel != null) {
                            coil.compose.AsyncImage(
                                model = photoModel,
                                contentDescription = plant.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Grass,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plant.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (categoryName != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = categoryName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            if (zoneName != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = zoneName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (plant.row != null || plant.position != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val posParts = mutableListOf<String>()
                    plant.row?.let { posParts.add("Ряд ${it.toInt()}") }
                    plant.position?.let { posParts.add("Номер ${it.toInt()}") }
                    Text(
                        text = "Розташування: ${posParts.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!plant.comment.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = plant.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        selectedPlantForSheet = null
                        onPlantClick(plant.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Переглянути деталі")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}

private fun addMyLocationOverlay(mapView: MapView?, context: android.content.Context) {
    mapView?.let { mv ->
        // Remove existing my location overlays
        mv.overlays.removeAll { it is MyLocationNewOverlay }
        val ov = MyLocationNewOverlay(GpsMyLocationProvider(context), mv)
        ov.enableMyLocation()
        ov.enableFollowLocation()
        mv.overlays.add(0, ov)
        ov.myLocation?.let { mv.controller.animateTo(it) }
    }
}

private const val THUMB_SIZE = 120

private fun loadThumbnail(context: android.content.Context, photoPath: String?): Bitmap? {
    if (photoPath.isNullOrBlank()) return null
    val path = photoPath.trim()

    val bitmap = try {
        when {
            path.startsWith("assets/") -> {
                context.assets.open(path.removePrefix("assets/")).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            path.startsWith("photos/") -> {
                val file = File(context.filesDir, path)
                BitmapFactory.decodeFile(file.absolutePath)
            }
            path.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            path.startsWith("file://") -> {
                BitmapFactory.decodeFile(Uri.parse(path).path)
            }
            else -> {
                if (File(path).isAbsolute) BitmapFactory.decodeFile(path)
                else null
            }
        }
    } catch (e: Exception) {
        null
    }

    if (bitmap == null) return null
    return createCircularThumbnail(bitmap, THUMB_SIZE)
}

private fun createCircularThumbnail(source: Bitmap, size: Int): Bitmap {
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = size / 2f

    // White background for border
    paint.color = android.graphics.Color.WHITE
    paint.style = Style.FILL
    canvas.drawCircle(radius, radius, radius, paint)

    // Crop the source to a square center
    val srcSize = minOf(source.width, source.height)
    val srcX = (source.width - srcSize) / 2f
    val srcY = (source.height - srcSize) / 2f

    // Draw the circular clipped bitmap
    val crop = Bitmap.createBitmap(source, srcX.toInt(), srcY.toInt(), srcSize, srcSize)
    val shader = BitmapShader(crop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val matrix = Matrix()
    val scale = (size - 4) / srcSize.toFloat()
    matrix.postScale(scale, scale)
    shader.setLocalMatrix(matrix)
    paint.shader = shader
    canvas.drawCircle(radius, radius, radius, paint)

    // Border stroke
    paint.shader = null
    paint.style = Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawCircle(radius, radius, radius - 1f, paint)

    return output
}
