package com.buzzheavier.uploader.ui.screens.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buzzheavier.uploader.data.UploadProgress
import com.buzzheavier.uploader.data.UploadResult
import com.buzzheavier.uploader.data.UploadStatus
import com.buzzheavier.uploader.network.UploadManager
import com.buzzheavier.uploader.network.UploadService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onNavigateToFiles: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var note by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(true) }
    var accountId by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf("") }
    var uploadStatus by remember { mutableStateOf(UploadStatus.IDLE) }
    var uploadProgress by remember { mutableStateOf(UploadProgress()) }
    var uploadResult by remember { mutableStateOf<UploadResult?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val uploadManager = remember { UploadManager(context) }
    val uploadState by uploadManager.uploadState.collectAsState()

    LaunchedEffect(uploadState) {
        uploadStatus = uploadState.status
        uploadProgress = uploadState.progress
        uploadResult = uploadState.result
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileName = uploadManager.getFileName(it) ?: "unknown"
            selectedFileSize = uploadManager.getFileSize(it)
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "dragScale"
    )

    val dragColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        label = "dragColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BuzzHeavier",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToFiles) {
                        Icon(Icons.Filled.Folder, contentDescription = "My Files")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomAction(
                        icon = Icons.Outlined.InsertDriveFile,
                        label = "Arquivos",
                        onClick = onNavigateToFiles
                    )
                    BottomAction(
                        icon = Icons.Filled.Settings,
                        label = "Config",
                        onClick = onNavigateToSettings
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.IDLE || uploadStatus == UploadStatus.FAILED,
                enter = fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.6f)),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        "Envie seus arquivos",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Rápido, seguro e sem limites",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = selectedUri == null && uploadStatus == UploadStatus.IDLE,
                enter = fadeIn() + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = 0.5f)),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                DropZone(
                    isDragging = isDragging,
                    animatedScale = animatedScale,
                    dragColor = dragColor,
                    onDragEnter = { isDragging = true },
                    onDragExit = { isDragging = false },
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    }
                )
            }

            AnimatedVisibility(
                visible = selectedUri != null && uploadStatus == UploadStatus.IDLE,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SelectedFileCard(
                    fileName = selectedFileName,
                    fileSize = selectedFileSize,
                    onRemove = {
                        selectedUri = null
                        selectedFileName = ""
                        selectedFileSize = 0
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = selectedUri != null && (uploadStatus == UploadStatus.IDLE || uploadStatus == UploadStatus.FAILED),
                enter = fadeIn() + slideInVertically { height -> height / 4 },
                exit = fadeOut() + slideOutVertically { height -> height / 4 }
            ) {
                UploadOptionsCard(
                    isAnonymous = isAnonymous,
                    onIsAnonymousChange = { isAnonymous = it },
                    accountId = accountId,
                    onAccountIdChange = { accountId = it },
                    parentId = parentId,
                    onParentIdChange = { parentId = it },
                    locationId = locationId,
                    onLocationIdChange = { locationId = it },
                    note = note,
                    onNoteChange = { note = it }
                )
            }

            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.UPLOADING || uploadStatus == UploadStatus.PREPARING,
                enter = fadeIn() + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.5f)),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                UploadProgressCard(
                    progress = uploadProgress,
                    fileName = uploadState.currentFile,
                    status = uploadStatus,
                    onCancel = { uploadManager.cancelUpload() }
                )
            }

            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.COMPLETED && uploadResult != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                UploadCompleteCard(
                    result = uploadResult!!,
                    onCopyLink = { url ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Upload URL", url))
                        scope.launch {
                            snackbarHostState.showSnackbar("Link copiado!", duration = SnackbarDuration.Short)
                        }
                    },
                    onShareLink = { url ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar link"))
                    },
                    onNewUpload = {
                        uploadManager.resetState()
                        selectedUri = null
                        selectedFileName = ""
                        selectedFileSize = 0
                        uploadResult = null
                    }
                )
            }

            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.FAILED && uploadResult != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f)
            ) {
                UploadFailedCard(
                    error = uploadResult?.error ?: "Unknown error",
                    onRetry = {
                        selectedUri?.let { uri ->
                            scope.launch {
                                uploadManager.uploadFile(
                                    uri = uri,
                                    accountId = if (isAnonymous) "" else accountId,
                                    parentId = parentId,
                                    locationId = locationId,
                                    note = note
                                )
                            }
                        }
                    },
                    onCancel = {
                        uploadManager.resetState()
                        selectedUri = null
                        selectedFileName = ""
                        selectedFileSize = 0
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        if (selectedUri != null && (uploadStatus == UploadStatus.IDLE || uploadStatus == UploadStatus.FAILED)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(end = 24.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedUri?.let { uri ->
                            scope.launch {
                                uploadManager.uploadFile(
                                    uri = uri,
                                    accountId = if (isAnonymous) "" else accountId,
                                    parentId = parentId,
                                    locationId = locationId,
                                    note = note
                                )
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null) },
                    text = { Text("Enviar", fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DropZone(
    isDragging: Boolean,
    animatedScale: Float,
    dragColor: Color,
    onDragEnter: () -> Unit,
    onDragExit: () -> Unit,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .scale(animatedScale)
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(32.dp),
        color = dragColor,
        border = if (isDragging) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Arraste e solte",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "ou toque para selecionar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectedFileCard(
    fileName: String,
    fileSize: Long,
    onRemove: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatFileSize(fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Remover",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun UploadOptionsCard(
    isAnonymous: Boolean,
    onIsAnonymousChange: (Boolean) -> Unit,
    accountId: String,
    onAccountIdChange: (String) -> Unit,
    parentId: String,
    onParentIdChange: (String) -> Unit,
    locationId: String,
    onLocationIdChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Opções de Upload",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Modo Anônimo", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = isAnonymous,
                    onCheckedChange = onIsAnonymousChange
                )
            }

            AnimatedVisibility(visible = !isAnonymous) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = accountId,
                        onValueChange = onAccountIdChange,
                        label = { Text("Account ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parentId,
                        onValueChange = onParentIdChange,
                        label = { Text("Diretório Pai (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = locationId,
                onValueChange = onLocationIdChange,
                label = { Text("Location ID (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                minLines = 2,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun UploadProgressCard(
    progress: UploadProgress,
    fileName: String,
    status: UploadStatus,
    onCancel: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (status == UploadStatus.PREPARING) "Preparando..." else "Enviando",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = progress.percentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                strokeCap = StrokeCap.Round,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${progress.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (progress.speed > 0) {
                    Text(
                        "${formatSpeed(progress.speed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onCancel,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
private fun UploadCompleteCard(
    result: UploadResult,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onNewUpload: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "check")
    val checkScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checkScale"
    )

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "\u2713",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.scale(checkScale)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Upload Concluído!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                result.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatFileSize(result.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCopyLink(result.url) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        result.url,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onShareLink(result.url) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Compartilhar", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalButton(
                    onClick = onNewUpload,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Novo Upload", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun UploadFailedCard(
    error: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "\u2717",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Falha no Upload",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Tentar Novamente", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> "${"%.1f".format(bytesPerSecond / (1024.0 * 1024))} MB/s"
    }
}
