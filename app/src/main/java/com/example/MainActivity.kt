package com.example

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.model.StampedPhoto
import com.example.data.settings.AppThemeMode
import com.example.data.update.UpdateChecker
import com.example.ui.MainViewModel
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.PhotoDetailDialog
import com.example.ui.components.StampEditorDialog
import com.example.ui.components.UpdateDialog
import androidx.activity.compose.BackHandler
import com.example.ui.screens.CameraPreviewScreen
import com.example.ui.screens.CollageScreen
import com.example.ui.screens.DocConverterScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.ReportsHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.FotoReporteTheme
import com.example.util.FileUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var tempCameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Clean any old dummy data so the app starts 100% clean
        viewModel.cleanSampleData()

        setContent {
            val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemIsDark = isSystemInDarkTheme()
            val isDarkTheme = when (currentThemeMode) {
                AppThemeMode.SYSTEM -> systemIsDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            FotoReporteTheme(darkTheme = isDarkTheme) {
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(targetState = showSplash, label = "SplashTransition") { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        MainAppScreen(
                            viewModel = viewModel,
                            onLaunchCamera = { launchCamera() },
                            onPickSingleImageForStamp = { pickSingleImageForStamp() },
                            onPickMultipleImagesForCollage = { pickImagesForCollage() }
                        )
                    }
                }
            }
        }
    }

    private var takePictureLauncher: androidx.activity.result.ActivityResultLauncher<Uri>? = null
    private var singlePickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var multiplePickerLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var permissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>? = null

    fun setLaunchers(
        takePicture: androidx.activity.result.ActivityResultLauncher<Uri>,
        singlePicker: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
        multiplePicker: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
        permissions: androidx.activity.result.ActivityResultLauncher<Array<String>>
    ) {
        takePictureLauncher = takePicture
        singlePickerLauncher = singlePicker
        multiplePickerLauncher = multiplePicker
        permissionsLauncher = permissions
    }

    private fun launchCamera() {
        permissionsLauncher?.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun startCameraCapture() {
        try {
            val file = FileUtils.createTempImageFile(this)
            val uri = FileUtils.getUriForFile(this, file)
            tempCameraImageUri = uri
            takePictureLauncher?.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al preparar cámara: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickSingleImageForStamp() {
        singlePickerLauncher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun pickImagesForCollage() {
        multiplePickerLauncher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun onCameraResult(success: Boolean) {
        if (success && tempCameraImageUri != null) {
            viewModel.preparePhotoForStamping(tempCameraImageUri!!, this, prefillLocation = true)
        }
    }

    fun onSingleImagePicked(uri: Uri?) {
        if (uri != null) {
            viewModel.preparePhotoForStamping(uri, this, prefillLocation = true)
        }
    }

    fun onMultipleImagesPickedForCollage(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            lifecycleScope.launch {
                for (uri in uris.take(9)) {
                    val bmp = FileUtils.loadBitmapFromUri(applicationContext, uri) ?: continue
                    val file = FileUtils.saveBitmapToFile(applicationContext, bmp, "GALLERY_IMPORT")
                    val photo = StampedPhoto(
                        filePath = file.absolutePath,
                        title = "Importada de Galería",
                        category = "General"
                    )
                    val db = com.example.data.AppDatabase.getDatabase(applicationContext)
                    val id = db.photoDao().insertPhoto(photo)
                    viewModel.toggleCollagePhoto(id)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onLaunchCamera: () -> Unit,
    onPickSingleImageForStamp: () -> Unit,
    onPickMultipleImagesForCollage: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var selectedDetailPhoto by remember { mutableStateOf<StampedPhoto?>(null) }
    var isLiveCameraOpen by remember { mutableStateOf(false) }

    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val draftBitmap by viewModel.draftBitmap.collectAsStateWithLifecycle()
    val draftConfig by viewModel.draftConfig.collectAsStateWithLifecycle()
    val isStampEditorVisible by viewModel.isStampEditorVisible.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val lastExportedFile by viewModel.lastExportedFile.collectAsStateWithLifecycle()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()
    val filteredPhotos by viewModel.filteredPhotos.collectAsStateWithLifecycle()
    val activeUpdateDialogInfo by viewModel.activeUpdateDialogInfo.collectAsStateWithLifecycle()

    // Activity Result Launchers
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        activity?.onCameraResult(success)
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        activity?.onSingleImagePicked(uri)
    }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        activity?.onMultipleImagesPickedForCollage(uris)
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isLiveCameraOpen = true
    }

    val openLiveCamera = {
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        activity?.setLaunchers(
            takePictureLauncher,
            singlePickerLauncher,
            multiplePickerLauncher,
            permissionsLauncher
        )
    }

    // Handle snackbar messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (isLiveCameraOpen) {
        BackHandler {
            isLiveCameraOpen = false
        }
        CameraPreviewScreen(
            viewModel = viewModel,
            onClose = { isLiveCameraOpen = false },
            onPickFromGallery = {
                isLiveCameraOpen = false
                onPickSingleImageForStamp()
            },
            onOpenSettings = {
                isLiveCameraOpen = false
                selectedTab = 4
            }
        )
    } else {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Fotos") },
                    label = { Text("Fotos", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_photos")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = "Collage") },
                    label = { Text("Collage", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_collage")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "A PDF") },
                    label = { Text("A PDF", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_doc_convert")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Reportes") },
                    label = { Text("Reportes", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_reports")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> PhotosScreen(
                    viewModel = viewModel,
                    onTakePhoto = openLiveCamera,
                    onPickFromGallery = onPickSingleImageForStamp,
                    onOpenPdfExport = { showPdfExportDialog = true },
                    onPhotoClick = { photo -> selectedDetailPhoto = photo },
                    onNavigateToSettings = { selectedTab = 4 }
                )
                1 -> CollageScreen(
                    viewModel = viewModel,
                    onPickGalleryImages = onPickMultipleImagesForCollage
                )
                2 -> DocConverterScreen(viewModel = viewModel)
                3 -> ReportsHistoryScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
    }

    // Stamp Editor Dialog
    if (isStampEditorVisible && draftBitmap != null) {
        StampEditorDialog(
            originalBitmap = draftBitmap!!,
            initialConfig = draftConfig,
            availableCategories = availableCategories,
            isBusy = isBusy,
            onDismiss = { viewModel.dismissStampEditor() },
            onSave = { config ->
                viewModel.updateDraftConfig { config }
                viewModel.saveStampedPhoto(context)
            }
        )
    }

    // PDF Export Dialog
    if (showPdfExportDialog) {
        val count = if (selectedPhotoIds.isNotEmpty()) selectedPhotoIds.size else filteredPhotos.size
        PdfExportDialog(
            photoCount = count,
            isBusy = isBusy,
            onDismiss = { showPdfExportDialog = false },
            onGenerate = { config ->
                showPdfExportDialog = false
                viewModel.generatePdfReport(context, config, useSelectedOnly = selectedPhotoIds.isNotEmpty())
            }
        )
    }

    // Photo Detail Dialog
    selectedDetailPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { selectedDetailPhoto = null },
            onUpdate = { updated -> viewModel.updateExistingPhoto(updated) },
            onDelete = { toDelete ->
                viewModel.deletePhoto(toDelete)
                selectedDetailPhoto = null
            }
        )
    }

    // Report Exported Success Dialog
    lastExportedFile?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLastExportedFile() },
            title = {
                Text(
                    text = "Reporte PDF Generado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "El archivo '${file.name}' está listo con diseño profesional.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        FileUtils.openFile(context, file, "application/pdf")
                        viewModel.clearLastExportedFile()
                    },
                    modifier = Modifier.testTag("dialog_open_pdf_button")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Text("Abrir PDF", modifier = Modifier.padding(start = 4.dp))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        FileUtils.shareFile(context, file, "application/pdf", "Compartir Reporte PDF")
                        viewModel.clearLastExportedFile()
                    },
                    modifier = Modifier.testTag("dialog_share_pdf_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text("Compartir", modifier = Modifier.padding(start = 4.dp))
                }
            }
        )
    }
}
