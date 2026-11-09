package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.PdfReportConfig
import com.example.util.ReportLayout

@Composable
fun PdfExportDialog(
    photoCount: Int,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (PdfReportConfig) -> Unit
) {
    var title by remember { mutableStateOf("Reporte Fotográfico TimeStamp") }
    var subtitle by remember { mutableStateOf("Registro con Fecha, Hora y Coordenadas GPS") }
    var author by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var selectedLayout by remember { mutableStateOf(ReportLayout.ONE_PER_PAGE) }
    var includeGps by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exportar Reporte PDF",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generando con $photoCount foto(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pdf_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Layout Format Selector
                Text(
                    text = "Diseño de Página",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportLayout.values().forEach { layout ->
                        FilterChip(
                            selected = selectedLayout == layout,
                            onClick = { selectedLayout = layout },
                            label = { Text(layout.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                if (selectedLayout == layout) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("layout_chip_${layout.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Subtitle inputs
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del Reporte") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtítulo / Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_subtitle_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Inspector / Autor") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_author_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = organization,
                        onValueChange = { organization = it },
                        label = { Text("Organización / Referencia") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_org_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = includeGps,
                                onCheckedChange = { includeGps = it },
                                modifier = Modifier.testTag("checkbox_include_gps")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Incluir Coordenadas GPS y Ubicación", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = includeNotes,
                                onCheckedChange = { includeNotes = it },
                                modifier = Modifier.testTag("checkbox_include_notes")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Incluir Notas y Observaciones de Campo", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_export_button")
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val config = PdfReportConfig(
                                title = title.ifBlank { "Reporte Fotográfico" },
                                subtitle = subtitle,
                                author = author,
                                organization = organization,
                                layoutFormat = selectedLayout,
                                includeGps = includeGps,
                                includeNotes = includeNotes
                            )
                            onGenerate(config)
                        },
                        enabled = !isBusy && photoCount > 0,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_export_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Generar PDF")
                        }
                    }
                }
            }
        }
    }
}
