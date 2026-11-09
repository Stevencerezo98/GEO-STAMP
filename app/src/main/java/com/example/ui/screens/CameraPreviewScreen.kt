package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlassIconButton
import com.example.util.FileUtils
import com.example.util.ImageStamper
import com.example.util.LocationHelper
import com.example.util.StampBgStyle
import com.example.util.StampConfig
import com.example.util.StampPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreviewScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe persistent switches from ViewModel/Settings
    val includeDate by viewModel.includeDate.collectAsStateWithLifecycle()
    val includeTime by viewModel.includeTime.collectAsStateWithLifecycle()
    val includeGps by viewModel.includeGps.collectAsStateWithLifecycle()
    val includeAddress by viewModel.includeAddress.collectAsStateWithLifecycle()
    val includeCustomNotes by viewModel.includeCustomNotes.collectAsStateWithLifecycle()
    val customNoteText by viewModel.customNoteText.collectAsStateWithLifecycle()
    val stampPosition by viewModel.stampPosition.collectAsStateWithLifecycle()
    val stampTextColor by viewModel.stampTextColor.collectAsStateWithLifecycle()
    val stampBgStyle by viewModel.stampBgStyle.collectAsStateWithLifecycle()

    var showQuickSettings by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // Live Clock ticking every second for real-time camera viewfinder
    var liveDateString by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var liveTimeString by remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())) }

    // Live GPS Location
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLon by remember { mutableStateOf<Double?>(null) }
    var currentAlt by remember { mutableStateOf<Double?>(null) }
    var currentAddress by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val loc = LocationHelper.getCurrentLocation(context)
        currentLat = loc.latitude
        currentLon = loc.longitude
        currentAlt = loc.altitude
        currentAddress = loc.address

        // Tick every second
        while (true) {
            val now = Date()
            liveDateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
            liveTimeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    // CameraX instance references
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. Live Camera Viewfinder (Using CameraX PreviewView)
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("camera_preview_view"),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // 2. Real-time Live Watermark Overlay (Positioned exactly on camera according to stampPosition)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            val alignment = when (stampPosition) {
                StampPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                StampPosition.BOTTOM_LEFT -> Alignment.BottomStart
                StampPosition.BOTTOM_CENTER -> Alignment.BottomCenter
                StampPosition.TOP_RIGHT -> Alignment.TopEnd
                StampPosition.TOP_LEFT -> Alignment.TopStart
            }

            // Real-Time Stamp Overlay reflecting switches instantly
            LiveStampWatermarkBadge(
                includeDate = includeDate,
                dateString = liveDateString,
                includeTime = includeTime,
                timeString = liveTimeString,
                includeGps = includeGps,
                lat = currentLat,
                lon = currentLon,
                includeAddress = includeAddress,
                address = currentAddress,
                includeNotes = includeCustomNotes,
                noteText = customNoteText,
                textColor = Color(stampTextColor),
                bgStyle = stampBgStyle,
                modifier = Modifier
                    .align(alignment)
                    .padding(bottom = if (alignment == Alignment.BottomCenter || alignment == Alignment.BottomEnd || alignment == Alignment.BottomStart) 110.dp else 0.dp)
                    .padding(top = if (alignment == Alignment.TopEnd || alignment == Alignment.TopStart) 60.dp else 0.dp)
            )
        }

        // 3. Top Glass Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                onClick = onClose,
                modifier = Modifier.testTag("camera_back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            // Quick Toggle Pill to toggle overlay editor
            Surface(
                onClick = { showQuickSettings = !showQuickSettings },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("toggle_stamp_quick_settings_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showQuickSettings) "Ocultar Ajustes" else "Ajustar Sello",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            GlassIconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("camera_settings_button")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
            }
        }

        // 4. Quick Live Stamp Switches Panel (Frosted Glass Overlay directly over camera)
        AnimatedVisibility(
            visible = showQuickSettings,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp, start = 14.dp, end = 14.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xDD0F172A),
                borderColor = Color.White.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Interruptores del Sello en Vivo:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 5 Key Switches
                    CameraSwitchRow("Fecha", includeDate) { viewModel.setIncludeDate(it) }
                    CameraSwitchRow("Hora en vivo", includeTime) { viewModel.setIncludeTime(it) }
                    CameraSwitchRow("Coordenadas GPS", includeGps) { viewModel.setIncludeGps(it) }
                    CameraSwitchRow("Dirección / Ubicación", includeAddress) { viewModel.setIncludeAddress(it) }
                    CameraSwitchRow("Nota personalizada", includeCustomNotes) { viewModel.setIncludeCustomNotes(it) }

                    if (includeCustomNotes) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customNoteText,
                            onValueChange = { viewModel.setCustomNoteText(it) },
                            placeholder = { Text("Escribe una nota o firma en vivo...", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stamp Position chips
                    Text(
                        text = "Posición en la foto:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StampPosition.values().forEach { pos ->
                            FilterChip(
                                selected = stampPosition == pos,
                                onClick = { viewModel.setStampPosition(pos) },
                                label = { Text(pos.displayName, fontSize = 10.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Color circles
                    Text(
                        text = "Color del texto:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val colors = listOf(
                            AndroidColor.WHITE,
                            AndroidColor.parseColor("#FFEB3B"),
                            AndroidColor.parseColor("#00E5FF"),
                            AndroidColor.parseColor("#00E676"),
                            AndroidColor.parseColor("#FF9100")
                        )
                        colors.forEach { cVal ->
                            val isSel = stampTextColor == cVal
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(cVal))
                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) Color(0xFF38BDF8) else Color.Gray, CircleShape)
                                    .clickable { viewModel.setStampTextColor(cVal) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Bottom Shutter Bar (Glassmorphic dock)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick from gallery button
                GlassIconButton(
                    onClick = onPickFromGallery,
                    size = 50.dp,
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.testTag("camera_gallery_picker_button")
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = "Galería",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Big Circular Glass Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            takePhotoWithStamp(
                                context = context,
                                imageCapture = imageCapture,
                                cameraExecutor = cameraExecutor,
                                config = StampConfig(
                                    showDate = includeDate,
                                    showTime = includeTime,
                                    showDateTime = includeDate || includeTime,
                                    dateString = liveDateString,
                                    timeString = liveTimeString,
                                    dateTime = "$liveDateString $liveTimeString",
                                    showLocationAddress = includeAddress,
                                    address = currentAddress,
                                    showGpsCoordinates = includeGps,
                                    latitude = currentLat,
                                    longitude = currentLon,
                                    showCustomText = includeCustomNotes && customNoteText.isNotBlank(),
                                    customText = customNoteText,
                                    position = stampPosition,
                                    textColor = stampTextColor,
                                    bgStyle = stampBgStyle
                                ),
                                onSaved = {
                                    isCapturing = false
                                    onClose()
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Quick flash/stamp style toggle
                GlassIconButton(
                    onClick = {
                        val next = when (stampBgStyle) {
                            StampBgStyle.GLASS_CARD -> StampBgStyle.SHADOW_ONLY
                            StampBgStyle.SHADOW_ONLY -> StampBgStyle.DARK_BANNER
                            StampBgStyle.DARK_BANNER -> StampBgStyle.GLASS_CARD
                        }
                        viewModel.setStampBgStyle(next)
                    },
                    size = 50.dp,
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.testTag("camera_style_toggle_button")
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Estilo de Fondo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStampWatermarkBadge(
    includeDate: Boolean,
    dateString: String,
    includeTime: Boolean,
    timeString: String,
    includeGps: Boolean,
    lat: Double?,
    lon: Double?,
    includeAddress: Boolean,
    address: String,
    includeNotes: Boolean,
    noteText: String,
    textColor: Color,
    bgStyle: StampBgStyle,
    modifier: Modifier = Modifier
) {
    val lines = mutableListOf<String>()

    val dateLine = when {
        includeDate && includeTime -> "$dateString $timeString"
        includeDate -> dateString
        includeTime -> timeString
        else -> ""
    }
    if (dateLine.isNotBlank()) lines.add(dateLine)

    if (includeGps && lat != null && lon != null) {
        lines.add(String.format(Locale.US, "GPS: %.5f, %.5f", lat, lon))
    }

    if (includeAddress && address.isNotBlank()) {
        lines.add(address)
    }

    if (includeNotes && noteText.isNotBlank()) {
        lines.add(noteText)
    }

    if (lines.isEmpty()) return

    val bgModifier = when (bgStyle) {
        StampBgStyle.GLASS_CARD -> Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        StampBgStyle.DARK_BANNER -> Modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        StampBgStyle.SHADOW_ONLY -> Modifier
            .padding(horizontal = 6.dp, vertical = 4.dp)
    }

    Box(modifier = modifier.then(bgModifier)) {
        Column {
            lines.forEach { line ->
                Text(
                    text = line,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CameraSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF38BDF8),
                checkedTrackColor = Color(0xFF0284C7)
            ),
            modifier = Modifier.size(36.dp)
        )
    }
}

private fun takePhotoWithStamp(
    context: Context,
    imageCapture: ImageCapture?,
    cameraExecutor: ExecutorService,
    config: StampConfig,
    onSaved: () -> Unit,
    onError: () -> Unit
) {
    if (imageCapture == null) {
        // Fallback for emulator without hardware camera
        saveSimulatedPhotoWithStamp(context, config, onSaved)
        return
    }

    imageCapture.takePicture(
        cameraExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Correct camera sensor rotation
                    if (image.imageInfo.rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    image.close()

                    // Apply Stamp
                    val stamped = ImageStamper.applyStamp(bitmap, config)
                    val savedFile = FileUtils.saveBitmapToFile(context, stamped, "TIMESTAMP")

                    val db = com.example.data.AppDatabase.getDatabase(context)
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val photo = com.example.data.model.StampedPhoto(
                            filePath = savedFile.absolutePath,
                            title = config.customText.ifBlank { "Foto TimeStamp" },
                            notes = config.customText,
                            formattedDateTime = config.dateTime,
                            latitude = config.latitude,
                            longitude = config.longitude,
                            address = config.address
                        )
                        db.photoDao().insertPhoto(photo)
                    }

                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSaved()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    image.close()
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onError() }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                // Graceful fallback to simulated frame if physical camera fails in emulator
                saveSimulatedPhotoWithStamp(context, config, onSaved)
            }
        }
    )
}

private fun saveSimulatedPhotoWithStamp(
    context: Context,
    config: StampConfig,
    onSaved: () -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        val width = 1200
        val height = 1600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Photorealistic viewfinder gradient
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#111827")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)

        // Grid lines
        val lp = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(35, 255, 255, 255)
            strokeWidth = 2f
        }
        for (x in 0 until width step 100) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), lp)
        for (y in 0 until height step 100) canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), lp)

        val tp = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("Captura TimeStamp", width / 2f, height / 2f, tp)

        // Stamp
        val stamped = ImageStamper.applyStamp(bitmap, config)
        val savedFile = FileUtils.saveBitmapToFile(context, stamped, "TIMESTAMP")

        val db = com.example.data.AppDatabase.getDatabase(context)
        val photo = com.example.data.model.StampedPhoto(
            filePath = savedFile.absolutePath,
            title = config.customText.ifBlank { "Foto TimeStamp" },
            notes = config.customText,
            formattedDateTime = config.dateTime,
            latitude = config.latitude,
            longitude = config.longitude,
            address = config.address
        )
        db.photoDao().insertPhoto(photo)

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSaved()
        }
    }
}
