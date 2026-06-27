package com.buzzheavier.uploader.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzheavier.uploader.data.UserPreferences
import com.buzzheavier.uploader.network.UploadManager
import com.buzzheavier.uploader.utils.isNetworkAvailable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val uploadManager = UploadManager(application)
    private val prefs = UserPreferences(application)

    val uploadState: StateFlow<UploadManager.UploadState> = uploadManager.uploadState

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _selectedFileName = MutableStateFlow("")
    val selectedFileName: StateFlow<String> = _selectedFileName.asStateFlow()

    private val _selectedFileSize = MutableStateFlow(0L)
    val selectedFileSize: StateFlow<Long> = _selectedFileSize.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    val isAnonymous: StateFlow<Boolean> = prefs.isAnonymous
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val accountId: StateFlow<String> = prefs.accountId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val parentId: StateFlow<String> = prefs.parentDirectory
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val locationId: StateFlow<String> = prefs.locationId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun selectFile(uri: Uri) {
        _selectedUri.value = uri
        _selectedFileName.value = uploadManager.getFileName(uri) ?: "unknown"
        _selectedFileSize.value = uploadManager.getFileSize(uri)
    }

    fun removeFile() {
        _selectedUri.value = null
        _selectedFileName.value = ""
        _selectedFileSize.value = 0
    }

    fun upload(note: String = "") {
        val uri = _selectedUri.value ?: return
        if (!isNetworkAvailable(getApplication())) {
            _networkError.value = "Sem conexão com a internet. Verifique sua rede."
            return
        }
        val anon = isAnonymous.value
        val accId = accountId.value
        val parId = parentId.value
        val locId = locationId.value
        viewModelScope.launch {
            uploadManager.uploadFile(
                uri = uri,
                accountId = if (anon) "" else accId,
                parentId = parId,
                locationId = locId,
                note = note
            )
        }
    }

    fun clearNetworkError() {
        _networkError.value = null
    }

    fun cancelUpload() {
        uploadManager.cancelUpload()
    }

    fun resetUpload() {
        uploadManager.resetState()
        _selectedUri.value = null
        _selectedFileName.value = ""
        _selectedFileSize.value = 0
    }

}
