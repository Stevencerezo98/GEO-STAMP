package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ImageStamper
import com.example.util.StampBgStyle
import com.example.util.StampConfig
import com.example.util.StampPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StampEditorDialog(
    originalBitmap: Bitmap,
    initialConfig: StampConfig,
    availableCategories: List<String> = emptyList(),
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (StampConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    // Re-render preview with current stamp config
    LaunchedEffect(config, originalBitmap) {
        isPreviewLoading = true
        withContext(Dispatchers.Default) {
            val maxPreviewDim = 900
            val scale = minOf(1f, maxPreviewDim / maxOf(originalBitmap.width, originalBitmap.height).toFloat())
            val previewSource = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else originalBitmap

            val stamped = ImageStamper.applyStamp(previewSource, config)
            previewBitmap = stamped
        }
        isPreviewLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Personalizar Sello TimeStamp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Elige exactamente qué datos estampar sobre tu foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_stamp_editor_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Live Preview Card with Glassmorphic border
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            previewBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Vista previa del estampado",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            if (isPreviewLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. SELECTOR DE ELEMENTOS A ESTAMPAR (TOGGLES)
                    Text(
                        text = "1. ELEMENTOS A ESTAMPAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Toggle Fecha y Hora
                            StampToggleRow(
                                title = "Fecha y Hora",
                                subtitle = config.dateTime,
                                checked = config.showDateTime,
                                onCheckedChange = { config = config.copy(showDateTime = it) },
                                testTag = "toggle_date_time"
                            )

                            // Toggle Dirección
                            StampToggleRow(
                                title = "Dirección / Lugar",
                                subtitle = config.address.ifBlank { "Ubicación detectada" },
                                checked = config.showLocationAddress,
                                onCheckedChange = { config = config.copy(showLocationAddress = it) },
                                testTag = "toggle_address"
                            )

                            // Toggle Coordenadas GPS
                            StampToggleRow(
                                title = "Coordenadas GPS (Lat / Lon)",
                                subtitle = if (config.latitude != null && config.longitude != null)
                                    "${String.format(java.util.Locale.US, "%.5f", config.latitude)}, ${String.format(java.util.Locale.US, "%.5f", config.longitude)}"
                                else "GPS automático",
                                checked = config.showGpsCoordinates,
                                onCheckedChange = { config = config.copy(showGpsCoordinates = it) },
                                testTag = "toggle_gps"
                            )

                            // Toggle Altitud
                            StampToggleRow(
                                title = "Altitud",
                                subtitle = if (config.altitude != null) "${String.format(java.util.Locale.US, "%.1f", config.altitude)} m" else "Nivel de elevación",
                                checked = config.showAltitude,
                                onCheckedChange = { config = config.copy(showAltitude = it) },
                                testTag = "toggle_altitude"
                            )

                            // Toggle Texto Personalizado
                            StampToggleRow(
                                title = "Texto o Nota Personalizada",
                                subtitle = config.customText.ifBlank { "Añadir texto, firma o notas" },
                                checked = config.showCustomText,
                                onCheckedChange = { config = config.copy(showCustomText = it) },
                                testTag = "toggle_custom_text"
                            )
                        }
                    }

                    // Field for custom text if enabled
                    AnimatedVisibility(visible = config.showCustomText) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedTextField(
                                value = config.customText,
                                onValueChange = { config = config.copy(customText = it) },
                                label = { Text("Escribe tu texto o nota personalizada") },
                                placeholder = { Text("Ej. Foto familiar / Registro 2026 / Mi Firma") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_text"),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. POSICIÓN DEL SELLO
                    Text(
                        text = "2. POSICIÓN DEL SELLO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StampPosition.values().forEach { pos ->
                            val isSelected = config.position == pos
                            FilterChip(
                                selected = isSelected,
                                onClick = { config = config.copy(position = pos) },
                                label = { Text(pos.displayName, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("pos_chip_${pos.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. COLOR DEL TEXTO
                    Text(
                        text = "3. COLOR DEL TEXTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(
                            "Blanco" to AndroidColor.WHITE,
                            "Amarillo GPS" to AndroidColor.parseColor("#FFEB3B"),
                            "Cian Neón" to AndroidColor.parseColor("#00E5FF"),
                            "Verde Neón" to AndroidColor.parseColor("#00E676"),
                            "Naranja" to AndroidColor.parseColor("#FF9100")
                        )
                        colors.forEach { (name, colorVal) ->
                            val isSelected = config.textColor == colorVal
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clickable { config = config.copy(textColor = colorVal) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = name,
                                        tint = if (colorVal == AndroidColor.WHITE || colorVal == AndroidColor.parseColor("#FFEB3B")) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. ESTILO DE FONDO / GLASS
                    Text(
                        text = "4. ESTILO DE FONDO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StampBgStyle.values().forEach { bg ->
                            val isSelected = config.bgStyle == bg
                            FilterChip(
                                selected = isSelected,
                                onClick = { config = config.copy(bgStyle = bg) },
                                label = { Text(bg.displayName, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("bg_chip_${bg.name}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_stamp_button")
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = { onSave(config) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_stamp_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Guardar Estampada", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}
