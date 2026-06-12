package com.fazenda.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerDialog(
    images: List<Any?>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var imageIntrinsicWidth by remember { mutableFloatStateOf(0f) }
    var imageIntrinsicHeight by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableIntStateOf(0) }
    var containerHeight by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.size - 1),
        pageCount = { images.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        imageIntrinsicWidth = 0f
        imageIntrinsicHeight = 0f
    }

    val maxOffsetX by remember(scale, imageIntrinsicWidth, containerWidth, containerHeight) {
        derivedStateOf {
            calculateMaxOffset(imageIntrinsicWidth, imageIntrinsicHeight, containerWidth, containerHeight, scale, true)
        }
    }
    val maxOffsetY by remember(scale, imageIntrinsicHeight, containerWidth, containerHeight) {
        derivedStateOf {
            calculateMaxOffset(imageIntrinsicHeight, imageIntrinsicWidth, containerWidth, containerHeight, scale, false)
        }
    }

    fun clampOffsets() {
        if (maxOffsetX < 0.001f) offsetX = 0f else offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        if (maxOffsetY < 0.001f) offsetY = 0f else offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { size ->
                    containerWidth = size.width
                    containerHeight = size.height
                }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = (scale <= 1f && maxOffsetX <= 0f),
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val model = images[page]
                if (model != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 2.5f
                                            val centerX = containerWidth / 2f
                                            val centerY = containerHeight / 2f
                                            offsetX = (centerX - tapOffset.x) * (scale - 1f) / scale
                                            offsetY = (centerY - tapOffset.y) * (scale - 1f) / scale
                                            clampOffsets()
                                        }
                                    },
                                    onTap = { onDismiss() }
                                )
                            }
                            .pointerInput(scale) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    if (newScale <= 1f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        val oldScale = scale
                                        scale = newScale
                                        val scaleChange = newScale / oldScale
                                        offsetX = (offsetX - centroid.x) * scaleChange + centroid.x + pan.x
                                        offsetY = (offsetY - centroid.y) * scaleChange + centroid.y + pan.y
                                        clampOffsets()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(model)
                                .crossfade(true)
                                .listener(object : ImageRequest.Listener {
                                    override fun onSuccess(request: ImageRequest, result: coil.request.SuccessResult) {
                                        imageIntrinsicWidth = result.drawable.intrinsicWidth.toFloat()
                                        imageIntrinsicHeight = result.drawable.intrinsicHeight.toFloat()
                                    }
                                    override fun onError(request: ImageRequest, result: coil.request.ErrorResult) {}
                                })
                                .build(),
                            contentDescription = "Зображення ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрити")
            }

            if (images.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun calculateMaxOffset(
    imageDimension: Float,
    imageDimensionOther: Float,
    containerWidth: Int,
    containerHeight: Int,
    scale: Float,
    isHorizontal: Boolean
): Float {
    if (imageDimension <= 0f || scale <= 1f) return 0f

    val containerDimension = if (isHorizontal) containerWidth.toFloat() else containerHeight.toFloat()
    val containerDimensionOther = if (isHorizontal) containerHeight.toFloat() else containerWidth.toFloat()

    val scaledDimension = imageDimension * scale
    val scaledDimensionOther = imageDimensionOther * scale

    if (scaledDimension <= containerDimension || scaledDimensionOther <= containerDimensionOther) return 0f

    return (scaledDimension - containerDimension) / 2f
}
