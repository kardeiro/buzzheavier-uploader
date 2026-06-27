package com.buzzheavier.uploader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzheavier.uploader.data.DirectoryInfo
import com.buzzheavier.uploader.data.UserPreferences
import com.buzzheavier.uploader.network.BuzzHeavierApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    val accountId: StateFlow<String> = prefs.accountId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _directoryInfo = MutableStateFlow<DirectoryInfo?>(null)
    val directoryInfo: StateFlow<DirectoryInfo?> = _directoryInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showCreateFolder = MutableStateFlow(false)
    val showCreateFolder: StateFlow<Boolean> = _showCreateFolder.asStateFlow()

    private val _newFolderName = MutableStateFlow("")
    val newFolderName: StateFlow<String> = _newFolderName.asStateFlow()

    private val _deleteConfirmItemId = MutableStateFlow<String?>(null)
    val deleteConfirmItemId: StateFlow<String?> = _deleteConfirmItemId.asStateFlow()

    private var api: BuzzHeavierApi = BuzzHeavierApi()

    fun loadDirectory(directoryId: String = "") {
        _isLoading.value = true
        _error.value = null
        api = BuzzHeavierApi(accountId.value)
        viewModelScope.launch {
            val result = if (directoryId.isEmpty()) api.getRootDirectory() else api.getDirectory(directoryId)
            result.onSuccess { _directoryInfo.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreateFolderDialog() {
        _showCreateFolder.value = true
        _newFolderName.value = ""
    }

    fun dismissCreateFolderDialog() {
        _showCreateFolder.value = false
    }

    fun setNewFolderName(name: String) {
        _newFolderName.value = name
    }

    fun createFolder(parentId: String) {
        val name = _newFolderName.value
        if (name.isBlank()) return
        viewModelScope.launch {
            api.createDirectory(parentId, name)
            _showCreateFolder.value = false
            _newFolderName.value = ""
            loadDirectory(parentId)
        }
    }

    fun showDeleteConfirm(itemId: String) {
        _deleteConfirmItemId.value = itemId
    }

    fun dismissDeleteConfirm() {
        _deleteConfirmItemId.value = null
    }

    fun deleteItem(itemId: String, directoryId: String) {
        viewModelScope.launch {
            api.deleteItem(itemId)
            _deleteConfirmItemId.value = null
            loadDirectory(directoryId)
        }
    }
}
