package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.PdfReport
import com.example.data.model.StampedPhoto
import com.example.data.settings.AppSettingsManager
import com.example.data.settings.AppThemeMode
import com.example.data.update.UpdateCheckResult
import com.example.data.update.UpdateChecker
import com.example.data.update.UpdateInfo
import com.example.util.CollageConfig
import com.example.util.CollageLayout
import com.example.util.CollageMaker
import com.example.util.FileUtils
import com.example.util.ImageStamper
import com.example.util.LocationHelper
import com.example.util.PdfReportConfig
import com.example.util.PdfReportGenerator
import com.example.util.StampConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilter(val displayName: String) {
    ALL("Todas"),
    TODAY("Hoy"),
    WEEK("7 días"),
    MONTH("Este mes")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val photoDao = db.photoDao()
    private val reportDao = db.reportDao()
    private val settingsManager = AppSettingsManager.getInstance(application)

    val themeMode: StateFlow<AppThemeMode> = settingsManager.themeMode
    val includeDate: StateFlow<Boolean> = settingsManager.includeDate
    val includeTime: StateFlow<Boolean> = settingsManager.includeTime
    val includeGps: StateFlow<Boolean> = settingsManager.includeGps
    val includeAddress: StateFlow<Boolean> = settingsManager.includeAddress
    val includeCustomNotes: StateFlow<Boolean> = settingsManager.includeCustomNotes
    val customNoteText: StateFlow<String> = settingsManager.customNoteText
    val stampPosition: StateFlow<com.example.util.StampPosition> = settingsManager.stampPosition
    val stampTextColor: StateFlow<Int> = settingsManager.stampTextColor
    val stampBgStyle: StateFlow<com.example.util.StampBgStyle> = settingsManager.stampBgStyle

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateCheckResult: StateFlow<UpdateCheckResult> = _updateCheckResult.asStateFlow()

    private val _activeUpdateDialogInfo = MutableStateFlow<UpdateInfo?>(null)
    val activeUpdateDialogInfo: StateFlow<UpdateInfo?> = _activeUpdateDialogInfo.asStateFlow()

    val rawPhotos: StateFlow<List<StampedPhoto>> = photoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedReports: StateFlow<List<PdfReport>> = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow(DateFilter.ALL)
    val selectedDateFilter = _selectedDateFilter.asStateFlow()

    // Multi-selection for export
    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds = _selectedPhotoIds.asStateFlow()

    // Filtered photos
    val filteredPhotos: StateFlow<List<StampedPhoto>> = combine(
        rawPhotos,
        searchQuery,
        selectedCategory,
        selectedDateFilter
    ) { photos, query, category, dateFilter ->
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sevenDaysAgo = startOfToday - (6 * 24 * 60 * 60 * 1000L)
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        photos.filter { photo ->
            // Category filter
            val matchesCategory = category == "Todas" || photo.category.equals(category, ignoreCase = true)

            // Date filter
            val matchesDate = when (dateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> photo.timestamp >= startOfToday
                DateFilter.WEEK -> photo.timestamp >= sevenDaysAgo
                DateFilter.MONTH -> photo.timestamp >= startOfMonth
            }

            // Search query filter
            val matchesQuery = query.isBlank() ||
                    photo.title.contains(query, ignoreCase = true) ||
                    photo.notes.contains(query, ignoreCase = true) ||
                    photo.address.contains(query, ignoreCase = true) ||
                    photo.project.contains(query, ignoreCase = true) ||
                    photo.author.contains(query, ignoreCase = true) ||
                    photo.category.contains(query, ignoreCase = true)

            matchesCategory && matchesDate && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All categories list (default + custom from photos)
    val availableCategories: StateFlow<List<String>> = rawPhotos.combine(MutableStateFlow(Unit)) { photos, _ ->
        val defaults = listOf("Trabajo", "Personal", "Documentos", "Evidencias", "General")
        val custom = photos.map { it.category }.filter { it.isNotBlank() }
        (listOf("Todas") + (defaults + custom).distinct()).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todas", "Trabajo", "Personal", "Documentos", "Evidencias", "General"))

    // Active stamp drafting state (when taking photo or picking from gallery)
    private val _draftBitmap = MutableStateFlow<Bitmap?>(null)
    val draftBitmap = _draftBitmap.asStateFlow()

    private val _draftConfig = MutableStateFlow(StampConfig())
    val draftConfig = _draftConfig.asStateFlow()

    private val _isStampEditorVisible = MutableStateFlow(false)
    val isStampEditorVisible = _isStampEditorVisible.asStateFlow()

    // Loading / status message
    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    // Last exported report file to open/share
    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile = _lastExportedFile.asStateFlow()

    // Collage builder state
    private val _collagePhotoIds = MutableStateFlow<List<Long>>(emptyList())
    val collagePhotoIds = _collagePhotoIds.asStateFlow()

    private val _collageConfig = MutableStateFlow(CollageConfig(title = "Collage de Evidencias", layoutType = CollageLayout.GRID_2X2))
    val collageConfig = _collageConfig.asStateFlow()

    private val _collagePreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val collagePreviewBitmap = _collagePreviewBitmap.asStateFlow()

    // Document to PDF converter state
    private val _converterImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val converterImageUris = _converterImageUris.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setDateFilter(filter: DateFilter) {
        _selectedDateFilter.value = filter
    }

    fun togglePhotoSelection(id: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedPhotoIds.value = current
    }

    fun selectAllFiltered() {
        val allIds = filteredPhotos.value.map { it.id }.toSet()
        _selectedPhotoIds.value = allIds
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastExportedFile() {
        _lastExportedFile.value = null
    }

    fun cleanSampleData() {
        viewModelScope.launch {
            try {
                photoDao.cleanOldSampleData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        settingsManager.setThemeMode(mode)
    }

    fun setIncludeDate(value: Boolean) {
        settingsManager.setIncludeDate(value)
    }

    fun setIncludeTime(value: Boolean) {
        settingsManager.setIncludeTime(value)
    }

    fun setIncludeGps(value: Boolean) {
        settingsManager.setIncludeGps(value)
    }

    fun setIncludeAddress(value: Boolean) {
        settingsManager.setIncludeAddress(value)
    }

    fun setIncludeCustomNotes(value: Boolean) {
        settingsManager.setIncludeCustomNotes(value)
    }

    fun setCustomNoteText(text: String) {
        settingsManager.setCustomNoteText(text)
    }

    fun setStampPosition(pos: com.example.util.StampPosition) {
        settingsManager.setStampPosition(pos)
    }

    fun setStampTextColor(color: Int) {
        settingsManager.setStampTextColor(color)
    }

    fun setStampBgStyle(style: com.example.util.StampBgStyle) {
        settingsManager.setStampBgStyle(style)
    }

    fun checkForUpdates(context: Context, simulate: Boolean = false) {
        viewModelScope.launch {
            _updateCheckResult.value = UpdateCheckResult.Checking
            try {
                val result = UpdateChecker.checkUpdate(context, simulateNewVersion = simulate)
                _updateCheckResult.value = result
                if (result is UpdateCheckResult.UpdateAvailable) {
                    _activeUpdateDialogInfo.value = result.info
                } else if (result is UpdateCheckResult.UpToDate) {
                    _statusMessage.value = "Tu aplicación está actualizada a la última versión."
                }
            } catch (e: Exception) {
                _updateCheckResult.value = UpdateCheckResult.Error(e.localizedMessage ?: "Error al comprobar actualización")
            }
        }
    }

    fun dismissUpdateDialog() {
        _activeUpdateDialogInfo.value = null
    }

    // Process photo taken by camera or chosen from gallery
    fun preparePhotoForStamping(uri: Uri, context: Context, prefillLocation: Boolean = true) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    FileUtils.loadBitmapFromUri(context, uri)
                }

                if (bitmap == null) {
                    _statusMessage.value = "No se pudo cargar la imagen."
                    _isBusy.value = false
                    return@launch
                }

                _draftBitmap.value = bitmap
                val now = Date()
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
                val nowStr = "$dateStr $timeStr"

                val incDate = includeDate.value
                val incTime = includeTime.value
                val incGps = includeGps.value
                val incAddress = includeAddress.value
                val incNotes = includeCustomNotes.value
                val noteText = customNoteText.value
                val pos = stampPosition.value
                val textColor = stampTextColor.value
                val bgStyle = stampBgStyle.value

                if (prefillLocation) {
                    val loc = LocationHelper.getCurrentLocation(context)
                    _draftConfig.value = StampConfig(
                        showDate = incDate,
                        showTime = incTime,
                        showDateTime = incDate || incTime,
                        dateString = dateStr,
                        timeString = timeStr,
                        dateTime = nowStr,
                        showGpsCoordinates = incGps,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        showLocationAddress = incAddress,
                        address = loc.address,
                        showCustomText = incNotes && noteText.isNotBlank(),
                        customText = noteText,
                        position = pos,
                        textColor = textColor,
                        bgStyle = bgStyle,
                        category = if (_selectedCategory.value != "Todas") _selectedCategory.value else "General"
                    )
                } else {
                    _draftConfig.value = StampConfig(
                        showDate = incDate,
                        showTime = incTime,
                        showDateTime = incDate || incTime,
                        dateString = dateStr,
                        timeString = timeStr,
                        dateTime = nowStr,
                        showGpsCoordinates = false,
                        showLocationAddress = false,
                        showCustomText = incNotes && noteText.isNotBlank(),
                        customText = noteText,
                        position = pos,
                        textColor = textColor,
                        bgStyle = bgStyle,
                        category = if (_selectedCategory.value != "Todas") _selectedCategory.value else "General"
                    )
                }

                _isStampEditorVisible.value = true
            } catch (e: Exception) {
                _statusMessage.value = "Error al procesar imagen: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateDraftConfig(updater: (StampConfig) -> StampConfig) {
        _draftConfig.value = updater(_draftConfig.value)
    }

    fun dismissStampEditor() {
        _isStampEditorVisible.value = false
        _draftBitmap.value = null
    }

    fun saveStampedPhoto(context: Context) {
        val originalBitmap = _draftBitmap.value ?: return
        val config = _draftConfig.value
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val stampedBitmap = withContext(Dispatchers.Default) {
                    ImageStamper.applyStamp(originalBitmap, config)
                }

                val savedFile = withContext(Dispatchers.IO) {
                    FileUtils.saveBitmapToFile(context, stampedBitmap, "STAMPED")
                }

                val photo = StampedPhoto(
                    filePath = savedFile.absolutePath,
                    title = config.customText.ifBlank { "Foto TimeStamp" },
                    notes = config.customText,
                    category = config.category.ifBlank { "General" },
                    timestamp = System.currentTimeMillis(),
                    formattedDateTime = config.dateTime,
                    latitude = config.latitude,
                    longitude = config.longitude,
                    altitude = config.altitude,
                    address = config.address,
                    author = "",
                    project = "",
                    stampStyle = config.bgStyle.name
                )

                photoDao.insertPhoto(photo)
                _statusMessage.value = "Foto guardada y estampada con éxito."
                dismissStampEditor()
            } catch (e: Exception) {
                _statusMessage.value = "Error al guardar foto: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateExistingPhoto(photo: StampedPhoto) {
        viewModelScope.launch {
            try {
                photoDao.updatePhoto(photo)
                _statusMessage.value = "Nota y detalles actualizados."
            } catch (e: Exception) {
                _statusMessage.value = "Error al actualizar: ${e.localizedMessage}"
            }
        }
    }

    fun deletePhoto(photo: StampedPhoto) {
        viewModelScope.launch {
            try {
                val file = File(photo.filePath)
                if (file.exists()) file.delete()
                photoDao.deletePhoto(photo)
                _statusMessage.value = "Foto eliminada."
            } catch (e: Exception) {
                _statusMessage.value = "Error al eliminar: ${e.localizedMessage}"
            }
        }
    }

    // Generate PDF Report
    fun generatePdfReport(context: Context, config: PdfReportConfig, useSelectedOnly: Boolean = false) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val targetPhotos = if (useSelectedOnly && _selectedPhotoIds.value.isNotEmpty()) {
                    rawPhotos.value.filter { _selectedPhotoIds.value.contains(it.id) }
                } else if (_selectedPhotoIds.value.isNotEmpty()) {
                    rawPhotos.value.filter { _selectedPhotoIds.value.contains(it.id) }
                } else {
                    filteredPhotos.value
                }

                if (targetPhotos.isEmpty()) {
                    _statusMessage.value = "No hay fotos seleccionadas para el reporte."
                    _isBusy.value = false
                    return@launch
                }

                val pdfFile = PdfReportGenerator.generatePhotoReport(context, targetPhotos, config)

                val report = PdfReport(
                    title = config.title,
                    filePath = pdfFile.absolutePath,
                    reportType = "PHOTO_REPORT",
                    pageCount = ((targetPhotos.size + config.layoutFormat.photosPerPage - 1) / config.layoutFormat.photosPerPage).coerceAtLeast(1),
                    photoCount = targetPhotos.size,
                    fileSizeBytes = pdfFile.length()
                )
                reportDao.insertReport(report)
                _lastExportedFile.value = pdfFile
                _statusMessage.value = "Reporte PDF generado exitosamente (${pdfFile.name})"
                clearSelection()
            } catch (e: Exception) {
                _statusMessage.value = "Error al generar PDF: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteReport(report: PdfReport) {
        viewModelScope.launch {
            try {
                val file = File(report.filePath)
                if (file.exists()) file.delete()
                reportDao.deleteReport(report)
                _statusMessage.value = "Reporte eliminado."
            } catch (e: Exception) {
                _statusMessage.value = "Error al eliminar reporte: ${e.localizedMessage}"
            }
        }
    }

    // Collage features
    fun toggleCollagePhoto(id: Long) {
        val current = _collagePhotoIds.value.toMutableList()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            if (current.size < 9) current.add(id)
            else _statusMessage.value = "Máximo 9 fotos para el collage."
        }
        _collagePhotoIds.value = current
        recalculateCollageLayout()
    }

    fun removeCollagePhoto(id: Long) {
        val current = _collagePhotoIds.value.toMutableList()
        current.remove(id)
        _collagePhotoIds.value = current
        recalculateCollageLayout()
    }

    private var lastGeneratedCollageKey: String? = null

    fun clearCollageSelection() {
        _collagePhotoIds.value = emptyList()
        _collagePreviewBitmap.value = null
        lastGeneratedCollageKey = null
    }

    private fun recalculateCollageLayout() {
        val count = _collagePhotoIds.value.size
        val newLayout = when {
            count <= 2 -> CollageLayout.HORIZONTAL_2
            count == 3 -> CollageLayout.ONE_TOP_TWO_BOTTOM
            count in 4..5 -> CollageLayout.GRID_2X2
            count in 6..8 -> CollageLayout.GRID_2X3
            else -> CollageLayout.GRID_3X3
        }
        _collageConfig.value = _collageConfig.value.copy(layoutType = newLayout)
    }

    fun updateCollageConfig(updater: (CollageConfig) -> CollageConfig) {
        _collageConfig.value = updater(_collageConfig.value)
    }

    fun generateCollagePreview(context: Context, force: Boolean = false) {
        val currentIds = _collagePhotoIds.value
        val config = _collageConfig.value
        val key = "${currentIds.joinToString(",")}_${config.layoutType.name}_${config.backgroundColor}_${config.title}_${config.subtitle}"

        if (!force && key == lastGeneratedCollageKey && _collagePreviewBitmap.value != null) {
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val selectedPhotos = rawPhotos.value.filter { currentIds.contains(it.id) }
                val bitmaps = withContext(Dispatchers.IO) {
                    selectedPhotos.mapNotNull { FileUtils.loadBitmapFromFile(it.filePath, 800) }
                }

                if (bitmaps.isEmpty()) {
                    _statusMessage.value = "Selecciona al menos una foto para el collage."
                    _isBusy.value = false
                    return@launch
                }

                val collage = withContext(Dispatchers.Default) {
                    CollageMaker.createCollage(bitmaps, config)
                }
                _collagePreviewBitmap.value = collage
                lastGeneratedCollageKey = key
            } catch (e: Exception) {
                _statusMessage.value = "Error al generar collage: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun exportCollagePdf(context: Context) {
        val collage = _collagePreviewBitmap.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val pdfFile = PdfReportGenerator.exportCollageToPdf(
                    context,
                    collage,
                    _collageConfig.value.title.ifBlank { "Collage de Fotos" }
                )
                val report = PdfReport(
                    title = _collageConfig.value.title.ifBlank { "Collage de Fotos" },
                    filePath = pdfFile.absolutePath,
                    reportType = "COLLAGE",
                    pageCount = 1,
                    photoCount = _collagePhotoIds.value.size,
                    fileSizeBytes = pdfFile.length()
                )
                reportDao.insertReport(report)
                _lastExportedFile.value = pdfFile
                _statusMessage.value = "Collage exportado a PDF correctamente."
            } catch (e: Exception) {
                _statusMessage.value = "Error al exportar collage a PDF: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun saveCollageToGallery(context: Context) {
        val collage = _collagePreviewBitmap.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val file = withContext(Dispatchers.IO) {
                    FileUtils.saveBitmapToFile(context, collage, "COLLAGE")
                }
                val photo = StampedPhoto(
                    filePath = file.absolutePath,
                    title = _collageConfig.value.title.ifBlank { "Collage" },
                    notes = "Collage de ${_collagePhotoIds.value.size} imágenes",
                    category = "Collage",
                    formattedDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                photoDao.insertPhoto(photo)
                _statusMessage.value = "Collage guardado en la app y almacenamiento."
            } catch (e: Exception) {
                _statusMessage.value = "Error al guardar collage: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    // Document Converter features
    fun addConverterImages(uris: List<Uri>) {
        val current = _converterImageUris.value.toMutableList()
        current.addAll(uris)
        _converterImageUris.value = current
    }

    fun removeConverterImage(index: Int) {
        val current = _converterImageUris.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _converterImageUris.value = current
        }
    }

    fun clearConverterImages() {
        _converterImageUris.value = emptyList()
    }

    fun convertDocumentsToPdf(context: Context, docTitle: String) {
        val uris = _converterImageUris.value
        if (uris.isEmpty()) {
            _statusMessage.value = "Agrega al menos una imagen para convertir a PDF."
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val tempFiles = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val bmp = FileUtils.loadBitmapFromUri(context, uri) ?: return@mapNotNull null
                        FileUtils.saveBitmapToFile(context, bmp, "CONV_TEMP")
                    }
                }

                val title = docTitle.ifBlank { "Documento_Convertido" }
                val pdfFile = PdfReportGenerator.convertImagesToPdf(context, tempFiles, title)

                val report = PdfReport(
                    title = title,
                    filePath = pdfFile.absolutePath,
                    reportType = "DOC_CONVERT",
                    pageCount = tempFiles.size,
                    photoCount = tempFiles.size,
                    fileSizeBytes = pdfFile.length()
                )
                reportDao.insertReport(report)
                _lastExportedFile.value = pdfFile
                _statusMessage.value = "PDF generado con éxito (${tempFiles.size} páginas)."
                clearConverterImages()
            } catch (e: Exception) {
                _statusMessage.value = "Error al convertir a PDF: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }
}
