package nz.beeaudit.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import nz.beeaudit.app.ui.theme.BeeAuditTheme
import java.io.File
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private var volumeCaptureHandler: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BeeAuditTheme {
                HiveAuditApp(
                    setVolumeCaptureHandler = { handler ->
                        volumeCaptureHandler = handler
                    },
                )
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey) {
            volumeCaptureHandler?.invoke()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun HiveAuditApp(
    modifier: Modifier = Modifier,
    setVolumeCaptureHandler: ((() -> Unit)?) -> Unit = {},
) {
    val context = LocalContext.current
    val storage = remember { AuditStorage(context) }
    val prefs = remember { AuditPrefs(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    val externalMirror = remember { ExternalAuditMirror(context) }

    var pin by rememberSaveable { mutableStateOf(prefs.pin) }
    var group by rememberSaveable { mutableStateOf(prefs.group) }
    var visitDate by rememberSaveable { mutableStateOf(prefs.visitDate) }
    var sequenceNumber by rememberSaveable { mutableIntStateOf(prefs.sequenceNumber) }
    var forcedFlash by rememberSaveable { mutableStateOf(prefs.forcedFlash) }
    var photoCount by rememberSaveable { mutableIntStateOf(0) }
    var voiceNoteCount by rememberSaveable { mutableIntStateOf(0) }
    var hivePresent by rememberSaveable { mutableStateOf(true) }
    var conditionName by rememberSaveable { mutableStateOf(HiveCondition.Good.name) }
    var activityName by rememberSaveable { mutableStateOf(BeeActivity.High.name) }
    var damageVisible by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Ready") }
    var gpsFix by remember { mutableStateOf<GpsFix?>(null) }
    var gpsStatus by rememberSaveable { mutableStateOf("No GPS fix yet") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraEnabled by rememberSaveable { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    var audioPermissionGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO)) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
        )
    }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recordingVoiceNumber by remember { mutableIntStateOf(0) }
    var pendingPhotoCapture by rememberSaveable { mutableStateOf(false) }
    var pendingVoiceStart by rememberSaveable { mutableStateOf(false) }
    var previewPhoto by remember { mutableStateOf<File?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var playingVoiceFile by remember { mutableStateOf<File?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var saveFolderUriText by rememberSaveable { mutableStateOf(prefs.saveFolderUri) }
    var storagePath by remember(pin, visitDate) {
        mutableStateOf(
            if (pin.isBlank()) {
                storage.directories("UNKNOWN", visitDate).root.absolutePath
            } else {
                storage.directories(pin, visitDate).visitDir.absolutePath
            },
        )
    }
    var auditFiles by remember {
        mutableStateOf(storage.filesFor(pin.ifBlank { "UNKNOWN" }, group, visitDate, sequenceNumber))
    }

    val condition = HiveCondition.valueOf(conditionName)
    val activity = BeeActivity.valueOf(activityName)
    val gpsTracker = remember {
        GpsTracker(
            context = context,
            onFix = {
                gpsFix = it
                gpsStatus = "GPS ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)} (${it.accuracyMeters.toInt()} m)"
            },
            onStatus = { gpsStatus = it },
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) {
            cameraEnabled = true
            status = "Camera permission granted"
        } else {
            status = "Camera permission denied"
            pendingPhotoCapture = false
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        audioPermissionGranted = granted
        status = if (granted) "Microphone ready, starting recording" else "Microphone permission denied"
        if (!granted) pendingVoiceStart = false
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationPermissionGranted = result.values.any { it }
        if (locationPermissionGranted) {
            gpsTracker.refresh()
        } else {
            gpsStatus = "Location permission denied"
        }
    }
    val saveFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
            saveFolderUriText = uri.toString()
            prefs.saveFolderUri = uri.toString()
            status = "External save folder selected"
        }
    }

    fun persistSession() {
        prefs.saveSession(pin, group, visitDate, sequenceNumber, forcedFlash)
        if (pin.isNotBlank()) {
            storagePath = storage.ensureSession(pin, group, visitDate).visitDir.absolutePath
        }
    }

    fun refreshAuditFiles() {
        auditFiles = storage.filesFor(pin.ifBlank { "UNKNOWN" }, group, visitDate, sequenceNumber)
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        playingVoiceFile = null
    }

    fun mirrorToExternal(file: File) {
        val uriText = saveFolderUriText
        if (uriText.isBlank()) return
        val mirrored = runCatching {
            externalMirror.mirrorFile(
                rootUri = Uri.parse(uriText),
                source = file,
                pin = pin,
                visitDate = visitDate,
                group = group,
                sequenceNumber = sequenceNumber,
            )
        }.getOrDefault(false)
        if (!mirrored) status = "Saved locally, external mirror failed"
    }

    fun capturePhoto() {
        if (pin.isBlank()) {
            status = "Enter PIN / KPIN before taking photos"
            return
        }
        if (!cameraPermissionGranted) {
            pendingPhotoCapture = true
            cameraEnabled = true
            status = "Camera permission needed"
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        if (!cameraEnabled) {
            pendingPhotoCapture = true
            cameraEnabled = true
            status = "Opening camera"
            return
        }
        val capture = imageCapture
        if (capture == null) {
            pendingPhotoCapture = true
            status = "Camera is starting"
            return
        }

        persistSession()
        val numberedFile = storage.nextAvailablePhotoFile(pin, group, visitDate, sequenceNumber, photoCount + 1)
        pendingPhotoCapture = false
        status = "Taking photo ${numberedFile.file.name}"
        val outputOptions = ImageCapture.OutputFileOptions.Builder(numberedFile.file).build()
        capture.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    writeGpsExif(numberedFile.file, gpsFix)
                    photoCount = max(photoCount, numberedFile.number)
                    refreshAuditFiles()
                    mirrorToExternal(numberedFile.file)
                    status = "Saved photo ${numberedFile.file.name} in ${numberedFile.file.parentFile?.absolutePath.orEmpty()}"
                }

                override fun onError(exception: ImageCaptureException) {
                    pendingPhotoCapture = false
                    status = "Photo failed: ${exception.message.orEmpty()}"
                }
            },
        )
    }

    fun startRecording() {
        if (pin.isBlank()) {
            status = "Enter PIN / KPIN before recording"
            return
        }
        if (isRecording) {
            status = "Already recording. Tap End recording when finished."
            return
        }
        if (!audioPermissionGranted) {
            pendingVoiceStart = true
            status = "Microphone permission needed"
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        persistSession()
        cameraEnabled = false
        imageCapture = null
        val numberedFile = storage.nextAvailableVoiceFile(pin, group, visitDate, sequenceNumber, voiceNoteCount + 1)
        runCatching {
            voiceRecorder.start(numberedFile.file)
            recordingVoiceNumber = numberedFile.number
            recordingSeconds = 0
            isRecording = true
            pendingVoiceStart = false
            refreshAuditFiles()
            status = "Recording ${numberedFile.file.name}"
        }.onFailure {
            status = "Recording failed: ${it.message.orEmpty()}"
            isRecording = false
            pendingVoiceStart = false
        }
    }

    fun stopRecording() {
        if (!isRecording) {
            status = "No recording in progress"
            return
        }
        val file = voiceRecorder.stop()
        isRecording = false
        voiceNoteCount = max(voiceNoteCount, recordingVoiceNumber)
        status = if (file != null) {
            mirrorToExternal(file)
            "Saved voice note ${file.name} in ${file.parentFile?.absolutePath.orEmpty()}"
        } else {
            "Recording stopped"
        }
        recordingVoiceNumber = 0
        recordingSeconds = 0
        pendingVoiceStart = false
        refreshAuditFiles()
    }

    fun saveAndNextHive() {
        if (pin.isBlank()) {
            status = "Enter PIN / KPIN before saving"
            return
        }
        persistSession()
        val record = HiveRecordData(
            pin = pin,
            group = group,
            visitDate = visitDate,
            sequenceNumber = sequenceNumber,
            createdAt = AuditStorage.nowIso(),
            gpsFix = gpsFix,
            photoCount = photoCount,
            voiceNoteCount = voiceNoteCount,
            form = HiveFormData(
                hivePresent = hivePresent,
                condition = condition,
                activity = activity,
                damageVisible = damageVisible,
                notes = notes,
            ),
        )
        runCatching {
            storage.saveHiveRecord(record)
        }.onSuccess { formFile ->
            mirrorToExternal(formFile)
            val photoWarning = when {
                photoCount < 2 -> " Add more photos next hive."
                photoCount > 5 -> " Photo count is above target."
                else -> ""
            }
            sequenceNumber += 1
            photoCount = 0
            voiceNoteCount = 0
            hivePresent = true
            conditionName = HiveCondition.Good.name
            activityName = BeeActivity.High.name
            damageVisible = false
            notes = ""
            stopPlayback()
            previewPhoto = null
            prefs.saveSession(pin, group, visitDate, sequenceNumber, forcedFlash)
            status = "Saved ${formFile.name}.$photoWarning"
        }.onFailure {
            status = "Save failed: ${it.message.orEmpty()}"
        }
    }

    fun resetHiveInputs() {
        photoCount = 0
        voiceNoteCount = 0
        hivePresent = true
        conditionName = HiveCondition.Good.name
        activityName = BeeActivity.High.name
        damageVisible = false
        notes = ""
        recordingSeconds = 0
        recordingVoiceNumber = 0
        pendingPhotoCapture = false
        pendingVoiceStart = false
        previewPhoto = null
        deleteTarget = null
        stopPlayback()
        refreshAuditFiles()
    }

    fun newPinGroup() {
        if (isRecording) stopRecording()
        pin = ""
        group = "Hive"
        visitDate = AuditStorage.todayDate()
        sequenceNumber = 1
        resetHiveInputs()
        cameraEnabled = false
        imageCapture = null
        prefs.saveSession(pin, group, visitDate, sequenceNumber, forcedFlash)
        status = "Ready for new PIN / Group"
    }

    fun closeAndExit() {
        if (isRecording) voiceRecorder.stop()
        stopPlayback()
        cameraEnabled = false
        imageCapture = null
        pin = ""
        group = "Hive"
        visitDate = AuditStorage.todayDate()
        resetHiveInputs()
        (context as? ComponentActivity)?.finish()
    }

    fun openMap() {
        val fix = gpsFix
        if (fix == null) {
            status = "No GPS fix yet"
            return
        }
        val geoUri = Uri.parse("geo:${fix.latitude},${fix.longitude}?q=${fix.latitude},${fix.longitude}")
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
        }.onFailure {
            status = "No map app available"
        }
    }

    fun playVoice(file: File) {
        stopPlayback()
        runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    status = "Finished ${file.name}"
                }
                prepare()
                start()
                mediaPlayer = this
                playingVoiceFile = file
                status = "Playing ${file.name}"
            }
        }.onFailure {
            status = "Playback failed: ${it.message.orEmpty()}"
            stopPlayback()
        }
    }

    fun deleteFile(file: File) {
        val deleted = runCatching { file.delete() }.getOrDefault(false)
        if (deleted) {
            if (previewPhoto == file) previewPhoto = null
            if (playingVoiceFile == file) stopPlayback()
            refreshAuditFiles()
            status = "Deleted ${file.name}"
        } else {
            status = "Delete failed: ${file.name}"
        }
    }

    fun exportVisit() {
        if (pin.isBlank()) {
            status = "Enter PIN / KPIN before export"
            return
        }
        runCatching {
            val zipFile = storage.exportVisitZip(pin, visitDate)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bee Audit ${pin}_${visitDate}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Bee Audit visit"))
            status = "Exported ${zipFile.name}"
        }.onFailure {
            status = "Export failed: ${it.message.orEmpty()}"
        }
    }

    LaunchedEffect(pin, group, visitDate, sequenceNumber, forcedFlash) {
        prefs.saveSession(pin, group, visitDate, sequenceNumber, forcedFlash)
    }

    LaunchedEffect(pin, group, visitDate, sequenceNumber) {
        stopPlayback()
        previewPhoto = null
        refreshAuditFiles()
    }

    LaunchedEffect(cameraPermissionGranted, cameraEnabled, imageCapture, pendingPhotoCapture) {
        if (pendingPhotoCapture && cameraPermissionGranted && cameraEnabled && imageCapture != null) {
            capturePhoto()
        }
    }

    LaunchedEffect(audioPermissionGranted, pendingVoiceStart) {
        if (pendingVoiceStart && audioPermissionGranted && !isRecording) {
            startRecording()
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1_000L)
            recordingSeconds += 1
        }
    }

    DisposableEffect(Unit) {
        if (locationPermissionGranted) gpsTracker.refresh()
        onDispose {
            gpsTracker.stop()
            voiceRecorder.stop()
            stopPlayback()
        }
    }

    SideEffect {
        setVolumeCaptureHandler { capturePhoto() }
    }

    DisposableEffect(Unit) {
        onDispose { setVolumeCaptureHandler(null) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(
                sequenceNumber = sequenceNumber,
                onDecrease = {
                    if (sequenceNumber > 1) {
                        sequenceNumber -= 1
                        resetHiveInputs()
                    }
                },
                onIncrease = {
                    sequenceNumber += 1
                    resetHiveInputs()
                },
            )

            SessionFields(
                pin = pin,
                onPinChange = {
                    pin = it
                    storagePath = storage.directories(it.ifBlank { "UNKNOWN" }, visitDate).visitDir.absolutePath
                },
                group = group,
                onGroupChange = { group = it },
                visitDate = visitDate,
                onVisitDateChange = { visitDate = it },
            )

            GpsStatus(
                gpsStatus = gpsStatus,
                gpsFix = gpsFix,
                onRefresh = {
                    if (locationPermissionGranted) {
                        gpsTracker.refresh()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                onOpenMap = { openMap() },
            )

            FlashControl(
                forcedFlash = forcedFlash,
                onForcedFlashChange = { forcedFlash = it },
            )

            if (cameraEnabled && cameraPermissionGranted) {
                CameraPreview(
                    forcedFlash = forcedFlash,
                    onImageCaptureReady = { imageCapture = it },
                    onStatus = {
                        if (!it.startsWith("Camera ready")) status = it
                    },
                )
            }

            CapturePanel(
                photoCount = photoCount,
                voiceNoteCount = voiceNoteCount,
                cameraEnabled = cameraEnabled,
                onCameraEnabledChange = {
                    cameraEnabled = it
                    if (!it) imageCapture = null
                },
                isRecording = isRecording,
                recordingSeconds = recordingSeconds,
                onPhotoCaptured = { capturePhoto() },
                onVoiceStart = { startRecording() },
                onVoiceStop = { stopRecording() },
            )

            StorageStatus(
                path = storagePath,
                externalPath = saveFolderUriText,
                onChooseFolder = { saveFolderLauncher.launch(null) },
                onClearFolder = {
                    saveFolderUriText = ""
                    prefs.saveFolderUri = ""
                    status = "Using default save folder"
                },
                onExport = { exportVisit() },
                exportEnabled = pin.isNotBlank(),
            )

            CurrentFilesPanel(
                auditFiles = auditFiles,
                playingVoiceFile = playingVoiceFile,
                onViewPhoto = { previewPhoto = it },
                onPlayVoice = { playVoice(it) },
                onStopVoice = { stopPlayback() },
                onDeleteFile = { deleteTarget = it },
            )

            HiveForm(
                hivePresent = hivePresent,
                onHivePresentChange = { hivePresent = it },
                condition = condition,
                onConditionChange = { conditionName = it.name },
                activity = activity,
                onActivityChange = { activityName = it.name },
                damageVisible = damageVisible,
                onDamageVisibleChange = { damageVisible = it },
                notes = notes,
                onNotesChange = { notes = it },
            )

            StatusCard(status = status)

            PrimaryActions(
                onSaveAndNext = { saveAndNextHive() },
                onNewPinGroup = { newPinGroup() },
                onCloseAndExit = { closeAndExit() },
                saveEnabled = pin.isNotBlank(),
            )

            HelpButton(onClick = { showHelp = true })
        }
    }

    previewPhoto?.let { photo ->
        PhotoPreviewDialog(
            file = photo,
            photos = auditFiles.photos,
            onSelect = { previewPhoto = it },
            onDismiss = { previewPhoto = null },
        )
    }

    deleteTarget?.let { file ->
        ConfirmDeleteDialog(
            fileName = file.name,
            onConfirm = {
                deleteTarget = null
                deleteFile(file)
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun Header(
    sequenceNumber: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Bee Audit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Hive ${sequenceNumber.toString().padStart(4, '0')}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) {
                Icon(Icons.Filled.Remove, contentDescription = "Previous sequence")
            }
            Text(
                text = sequenceNumber.toString().padStart(4, '0'),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onIncrease) {
                Icon(Icons.Filled.Add, contentDescription = "Next sequence")
            }
        }
    }
}

@Composable
private fun SessionFields(
    pin: String,
    onPinChange: (String) -> Unit,
    group: String,
    onGroupChange: (String) -> Unit,
    visitDate: String,
    onVisitDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Visit Record",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                label = { Text("PIN / KPIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = visitDate,
                onValueChange = onVisitDateChange,
                label = { Text("Visit date") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = group,
                onValueChange = onGroupChange,
                label = { Text("Group") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StorageStatus(
    path: String,
    externalPath: String,
    onChooseFolder: () -> Unit,
    onClearFolder: () -> Unit,
    onExport: () -> Unit,
    exportEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Save folder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (externalPath.isNotBlank()) {
                    Text(
                        text = "External mirror: $externalPath",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseFolder) {
                    Text("Choose")
                }
                if (externalPath.isNotBlank()) {
                    OutlinedButton(onClick = onClearFolder) {
                        Text("Clear")
                    }
                }
                OutlinedButton(
                    onClick = onExport,
                    enabled = exportEnabled,
                ) {
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun GpsStatus(
    gpsStatus: String,
    gpsFix: GpsFix?,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (gpsFix == null) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (gpsFix == null) "No GPS fix yet" else "GPS fix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = gpsStatus,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) {
                    Text("GPS")
                }
                Button(
                    onClick = onOpenMap,
                    enabled = gpsFix != null,
                ) {
                    Text("Map")
                }
            }
        }
    }
}

@Composable
private fun FlashControl(
    forcedFlash: Boolean,
    onForcedFlashChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FlashOn, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Forced flash", style = MaterialTheme.typography.titleMedium)
            }
            Switch(checked = forcedFlash, onCheckedChange = onForcedFlashChange)
        }
    }
}

@Composable
private fun CameraPreview(
    forcedFlash: Boolean,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(1280, 960),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                ),
            )
            .build()
    }

    Card(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            update = {},
        )
    }

    DisposableEffect(previewView, lifecycleOwner, forcedFlash, resolutionSelector) {
        val currentPreviewView = previewView
        if (currentPreviewView == null) {
            onDispose { }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener(
                {
                    val cameraProvider = providerFuture.get()
                    val preview = Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                        .also { it.setSurfaceProvider(currentPreviewView.surfaceProvider) }
                    val imageCapture = ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(
                            if (forcedFlash) {
                                ImageCapture.FLASH_MODE_ON
                            } else {
                                ImageCapture.FLASH_MODE_OFF
                            },
                        )
                        .build()

                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                        onImageCaptureReady(imageCapture)
                        onStatus("Camera ready")
                    }.onFailure {
                        onImageCaptureReady(null)
                        onStatus("Camera failed: ${it.message.orEmpty()}")
                    }
                },
                ContextCompat.getMainExecutor(context),
            )

            onDispose {
                onImageCaptureReady(null)
                if (providerFuture.isDone) {
                    runCatching { providerFuture.get().unbindAll() }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CapturePanel(
    photoCount: Int,
    voiceNoteCount: Int,
    cameraEnabled: Boolean,
    onCameraEnabledChange: (Boolean) -> Unit,
    isRecording: Boolean,
    recordingSeconds: Int,
    onPhotoCaptured: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill("$photoCount photos")
                StatusPill("$voiceNoteCount voice notes")
                StatusPill(if (cameraEnabled) "Camera on" else "Camera off")
                if (isRecording) {
                    StatusPill("REC ${recordingSeconds}s")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { onCameraEnabledChange(!cameraEnabled) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Text(if (cameraEnabled) "Camera off" else "Camera on")
                }
                Button(
                    onClick = onPhotoCaptured,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Photo")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onVoiceStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    enabled = !isRecording,
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Record")
                }
                Button(
                    onClick = onVoiceStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    enabled = isRecording,
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("End")
                }
            }
        }
    }
}

@Composable
private fun CurrentFilesPanel(
    auditFiles: AuditFileSnapshot,
    playingVoiceFile: File?,
    onViewPhoto: (File) -> Unit,
    onPlayVoice: (File) -> Unit,
    onStopVoice: () -> Unit,
    onDeleteFile: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Current files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = "This check folder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = auditFiles.recordDir.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            FileGroup(
                title = "Photos",
                files = auditFiles.photos,
                emptyText = "No photos saved for this hive yet",
                primaryActionLabel = "View",
                onPrimaryAction = onViewPhoto,
                onDeleteFile = onDeleteFile,
            )

            VoiceFileGroup(
                title = "Voice notes",
                files = auditFiles.voiceNotes,
                playingVoiceFile = playingVoiceFile,
                emptyText = "No voice notes saved for this hive yet",
                onPlayVoice = onPlayVoice,
                onStopVoice = onStopVoice,
                onDeleteFile = onDeleteFile,
            )
        }
    }
}

@Composable
private fun FileGroup(
    title: String,
    files: List<File>,
    emptyText: String,
    primaryActionLabel: String,
    onPrimaryAction: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (files.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        } else {
            files.forEach { file ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { onPrimaryAction(file) }) {
                        Text(primaryActionLabel)
                    }
                    OutlinedButton(onClick = { onDeleteFile(file) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceFileGroup(
    title: String,
    files: List<File>,
    playingVoiceFile: File?,
    emptyText: String,
    onPlayVoice: (File) -> Unit,
    onStopVoice: () -> Unit,
    onDeleteFile: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (files.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            )
        } else {
            files.forEach { file ->
                val isPlaying = playingVoiceFile == file
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { if (isPlaying) onStopVoice() else onPlayVoice(file) }) {
                        Text(if (isPlaying) "Stop" else "Play")
                    }
                    OutlinedButton(onClick = { onDeleteFile(file) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPreviewDialog(
    file: File,
    photos: List<File>,
    onSelect: (File) -> Unit,
    onDismiss: () -> Unit,
) {
    val bitmap = remember(file) {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }
    val index = photos.indexOf(file)
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (bitmap == null) {
                    Text("Could not load photo", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Photo ${file.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { onSelect(photos[index - 1]) },
                        enabled = index > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Previous")
                    }
                    OutlinedButton(
                        onClick = { onSelect(photos[index + 1]) },
                        enabled = index >= 0 && index < photos.lastIndex,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Next")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete file?") },
        text = { Text(fileName) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PrimaryActions(
    onSaveAndNext: () -> Unit,
    onNewPinGroup: () -> Unit,
    onCloseAndExit: () -> Unit,
    saveEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ElevatedButton(
            onClick = onSaveAndNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = saveEnabled,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Save and next hive")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onNewPinGroup,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text("New PIN / Group")
            }
            OutlinedButton(
                onClick = onCloseAndExit,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text("Close and exit")
            }
        }
    }
}

@Composable
private fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text("Help")
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bee Audit help") },
        text = {
            Text(
                "1. Enter PIN / KPIN, visit date, and group.\n" +
                    "2. Check GPS, then use Map if needed.\n" +
                    "3. Set forced flash before taking photos.\n" +
                    "4. Take 2-5 photos and record voice notes.\n" +
                    "5. Review, play, or delete files in Current files.\n" +
                    "6. Save and next hive, or start a new PIN / Group.",
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
    )
}

@Composable
private fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun HiveForm(
    hivePresent: Boolean,
    onHivePresentChange: (Boolean) -> Unit,
    condition: HiveCondition,
    onConditionChange: (HiveCondition) -> Unit,
    activity: BeeActivity,
    onActivityChange: (BeeActivity) -> Unit,
    damageVisible: Boolean,
    onDamageVisibleChange: (Boolean) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Hive check",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            BinaryRow(
                label = "Hive present",
                checked = hivePresent,
                onCheckedChange = onHivePresentChange,
            )
            ChoiceRow(
                label = "Condition",
                options = HiveCondition.entries,
                selected = condition,
                optionLabel = { it.label },
                onSelected = onConditionChange,
            )
            ChoiceRow(
                label = "Bee activity",
                options = BeeActivity.entries,
                selected = activity,
                optionLabel = { it.label },
                onSelected = onActivityChange,
            )
            BinaryRow(
                label = "Damage visible",
                checked = damageVisible,
                onCheckedChange = onDamageVisibleChange,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BinaryRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    label = { Text(optionLabel(option)) },
                )
            }
        }
    }
}

@Composable
private fun StatusCard(status: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun writeGpsExif(file: File, gpsFix: GpsFix?) {
    if (gpsFix == null) return
    runCatching {
        ExifInterface(file).apply {
            setLatLong(gpsFix.latitude, gpsFix.longitude)
            setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, gpsFix.capturedAtMillis.toString())
            saveAttributes()
        }
    }
}

@PreviewLightDark
@Composable
private fun HiveAuditAppPreview() {
    BeeAuditTheme {
        HiveAuditApp()
    }
}
