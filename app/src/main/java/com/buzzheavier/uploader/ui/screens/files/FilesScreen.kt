package com.buzzheavier.uploader.ui.screens.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buzzheavier.uploader.data.AccountInfo
import com.buzzheavier.uploader.data.DirectoryInfo
import com.buzzheavier.uploader.data.FileInfo
import com.buzzheavier.uploader.data.StorageLocation
import com.buzzheavier.uploader.network.BuzzHeavierApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    directoryId: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToDirectory: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var directoryInfo by remember { mutableStateOf<DirectoryInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf("") }
    var menuExpandedFor by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(directoryId) {
        val api = BuzzHeavierApi(accountId)
        val result = if (directoryId.isEmpty()) api.getRootDirectory() else api.getDirectory(directoryId)
        result.onSuccess { directoryInfo = it }
            .onFailure { error = it.message }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        directoryInfo?.name ?: "Meus Arquivos",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            if (accountId.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateFolder = true },
                    icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                    text = { Text("Nova Pasta") },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "Erro desconhecido", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                val api = BuzzHeavierApi(accountId)
                                val result = if (directoryId.isEmpty()) api.getRootDirectory() else api.getDirectory(directoryId)
                                result.onSuccess { directoryInfo = it }
                                    .onFailure { error = it.message }
                                isLoading = false
                            }
                        }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                directoryInfo?.children?.isEmpty() == true -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum arquivo encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        directoryInfo?.children?.let { files ->
                            items(files, key = { it.id }) { file ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.6f))
                                ) {
                                    FileItem(
                                        file = file,
                                        onNavigateToDirectory = onNavigateToDirectory,
                                        menuExpanded = menuExpandedFor == file.id,
                                        onMenuToggle = { menuExpandedFor = if (menuExpandedFor == file.id) null else file.id },
                                        onDelete = { showDeleteConfirm = file.id },
                                        onDismissMenu = { menuExpandedFor = null }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("Criar Pasta") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nome da Pasta") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val api = BuzzHeavierApi(accountId)
                            val parentId = directoryId.ifEmpty { directoryInfo?.id ?: "" }
                            api.createDirectory(parentId, newFolderName)
                            showCreateFolder = false
                            newFolderName = ""
                            isLoading = true
                            val result = if (directoryId.isEmpty()) api.getRootDirectory() else api.getDirectory(directoryId)
                            result.onSuccess { directoryInfo = it }
                            isLoading = false
                        }
                    },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolder = false }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    showDeleteConfirm?.let { itemId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirmar Exclusão") },
            text = { Text("Deseja realmente excluir este item?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val api = BuzzHeavierApi(accountId)
                        api.deleteItem(itemId)
                        showDeleteConfirm = null
                        isLoading = true
                        val result = if (directoryId.isEmpty()) api.getRootDirectory() else api.getDirectory(directoryId)
                        result.onSuccess { directoryInfo = it }
                        isLoading = false
                    }
                }) {
                    Text("Deletar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun FileItem(
    file: FileInfo,
    onNavigateToDirectory: (String) -> Unit,
    menuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onDelete: () -> Unit,
    onDismissMenu: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = file.isDirectory) {
                if (file.isDirectory) onNavigateToDirectory(file.id)
            },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (file.isDirectory) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (file.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                        contentDescription = null,
                        tint = if (file.isDirectory) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (file.isDirectory && file.childrenCount > 0) {
                    Text(
                        "${file.childrenCount} itens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                FilledTonalIconButton(
                    onClick = onMenuToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opções", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text("Deletar") },
                        onClick = {
                            onDismissMenu()
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                    )
                }
            }
        }
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
