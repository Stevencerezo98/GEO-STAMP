package com.example.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.util.CollageLayout
import com.example.util.FileUtils
import java.io.File

@Composable
fun CollageScreen(
    viewModel: MainViewModel,
    onPickGalleryImages: () -> Unit
) {
    val context = LocalContext.current
    val rawPhotos by viewModel.rawPhotos.collectAsStateWithLifecycle()
    val collagePhotoIds by viewModel.collagePhotoIds.collectAsStateWithLifecycle()
    val collageConfig by viewModel.collageConfig.collectAsStateWithLifecycle()
    val collagePreview by viewModel.collagePreviewBitmap.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf(collageConfig.title) }
    var subtitle by rememberSaveable { mutableStateOf(collageConfig.subtitle) }

    LaunchedEffect(collagePhotoIds, collageConfig.layoutType, collageConfig.backgroundColor) {
        if (collagePhotoIds.isNotEmpty()) {
            viewModel.generateCollagePreview(context, force = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 64.dp)
    ) {
        // Screen Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DashboardCustomize,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Creador de Collages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Combina hasta 9 fotos y expórtalas a PDF o imagen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Photos selector tray
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1. Selecciona las fotos (${collagePhotoIds.size}/9)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (collagePhotoIds.isNotEmpty()) {
                Text(
                    text = "Limpiar",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clickable { viewModel.clearCollageSelection() }
                        .testTag("clear_collage_selection_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rawPhotos.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aún no tienes fotos tomadas. Toma fotos o importa desde tu galería.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onPickGalleryImages,
                        modifier = Modifier.testTag("collage_pick_gallery_button")
                    ) {
                        Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Elegir de Galería")
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rawPhotos, key = { it.id }) { photo ->
                    val isSelected = collagePhotoIds.contains(photo.id)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.toggleCollagePhoto(photo.id) }
                    ) {
                        AsyncImage(
                            model = File(photo.filePath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Seleccionada",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Layout Type Selector
        Text(
            text = "2. Plantilla de Cuadrícula",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CollageLayout.values().forEach { layout ->
                val isSelected = collageConfig.layoutType == layout
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.updateCollageConfig { it.copy(layoutType = layout) }
                    },
                    label = { Text(layout.displayName, fontSize = 11.sp) },
                    modifier = Modifier.testTag("layout_chip_${layout.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Background Color
        Text(
            text = "3. Fondo del Collage",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val colors = listOf(
                "Blanco" to AndroidColor.WHITE,
                "Gris Claro" to AndroidColor.parseColor("#F1F5F9"),
                "Pizarra" to AndroidColor.parseColor("#0F172A"),
                "Azul Marino" to AndroidColor.parseColor("#1E3A8A")
            )
            colors.forEach { (name, colorVal) ->
                FilterChip(
                    selected = collageConfig.backgroundColor == colorVal,
                    onClick = { viewModel.updateCollageConfig { it.copy(backgroundColor = colorVal) } },
                    label = { Text(name, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title and Subtitle for Header
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                viewModel.updateCollageConfig { cfg -> cfg.copy(title = it) }
            },
            label = { Text("Título superior del Collage (Opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("collage_title_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = subtitle,
            onValueChange = {
                subtitle = it
                viewModel.updateCollageConfig { cfg -> cfg.copy(subtitle = it) }
            },
            label = { Text("Subtítulo / Fecha del Collage (Opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("collage_subtitle_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Collage Preview Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (collagePreview != null) {
                    Image(
                        bitmap = collagePreview!!.asImageBitmap(),
                        contentDescription = "Vista previa del collage",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (collagePhotoIds.isEmpty()) "Selecciona al menos 2 fotos" else "Generando vista previa...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isBusy) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons: Export to PDF & Save Image
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.exportCollagePdf(context) },
                enabled = collagePreview != null && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_collage_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar a PDF")
            }

            Button(
                onClick = { viewModel.saveCollageToGallery(context) },
                enabled = collagePreview != null && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_collage_image_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Imagen")
            }
        }
    }
}
