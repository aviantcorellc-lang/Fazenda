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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    fun createInfoView(plant: PlantEntity, bitmap: Bitmap?): android.view.View {
        val density = context.resources.displayMetrics.density
        val iconSize = (48 * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        if (bitmap != null) {
            val photoView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = (4 * density).toInt()
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bitmap)
            }
            root.addView(photoView)
        }

        val nameText = TextView(context).apply {
            text = plant.name
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(nameText)

        val subText = buildString {
            plant.zoneId?.let { id -> zoneMap[id]?.name?.let { append("$it · ") } }
            plant.categoryId?.let { id -> categoryMap[id]?.name?.let { append(it) } }
        }
        if (subText.isNotBlank()) {
            val descText = TextView(context).apply {
                text = subText
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            root.addView(descText)
        }

        return root
    }

    fun addMarkers(mv: MapView, bitmaps: Map<Long, Bitmap?>) {
        // Shared info window – reuse the same window for all markers
        val density = context.resources.displayMetrics.density
        val infoRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
            setBackgroundColor(android.graphics.Color.WHITE)
            minimumWidth = (120 * density).toInt()
        }
        val infoWindow = object : org.osmdroid.views.overlay.infowindow.InfoWindow(infoRoot, mv) {
            override fun onOpen(item: Any?) {
                val m = item as? Marker ?: return
                val pIdx = plantsWithCoords.indexOfFirst { it.latitude == m.position.latitude && it.longitude == m.position.longitude }
                val plant = if (pIdx >= 0) plantsWithCoords[pIdx] else return
                val bmp = bitmaps[plant.id]

                infoRoot.removeAllViews()
                val bmpSize = (48 * density).toInt()
                if (bmp != null) {
                    infoRoot.addView(ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(bmpSize, bmpSize).apply {
                            gravity = Gravity.CENTER_HORIZONTAL
                            bottomMargin = (4 * density).toInt()
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(bmp)
                    })
                }
                infoRoot.addView(TextView(context).apply {
                    text = plant.name
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER_HORIZONTAL
                })
                val subText = buildString {
                    plant.zoneId?.let { id -> zoneMap[id]?.name?.let { append("$it · ") } }
                    plant.categoryId?.let { id -> categoryMap[id]?.name?.let { append(it) } }
                }
                if (subText.isNotBlank()) {
                    infoRoot.addView(TextView(context).apply {
                        text = subText
                        textSize = 12f
                        setTextColor(android.graphics.Color.GRAY)
                        gravity = Gravity.CENTER_HORIZONTAL
                    })
                }
                // Tap on info window → navigate to plant details
                infoRoot.setOnClickListener { onPlantClick(plant.id) }
            }
            override fun onClose() {}
        }

        plantsWithCoords.forEach { plant ->
            val marker = Marker(mv)
            marker.position = GeoPoint(plant.latitude!!, plant.longitude!!)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = plant.name
            marker.subDescription = buildString {
                plant.zoneId?.let { id -> zoneMap[id]?.name?.let { append("$it · ") } }
                plant.categoryId?.let { id -> categoryMap[id]?.name?.let { append(it) } }
            }

            val photoBitmap = bitmaps[plant.id]
            if (photoBitmap != null) {
                marker.icon = BitmapDrawable(context.resources, photoBitmap)
            }

            marker.infoWindow = infoWindow
            marker.setOnMarkerClickListener { m, _ ->
                m.showInfoWindow(); true
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
