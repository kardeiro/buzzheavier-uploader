package com.buzzheavier.uploader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzzheavier.uploader.data.AccountInfo
import com.buzzheavier.uploader.data.StorageLocation
import com.buzzheavier.uploader.data.UserPreferences
import com.buzzheavier.uploader.network.BuzzHeavierApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferences(application)
    private var api = BuzzHeavierApi()

    val accountId: StateFlow<String> = prefs.accountId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val parentDirectory: StateFlow<String> = prefs.parentDirectory
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val locationId: StateFlow<String> = prefs.locationId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val isAnonymous: StateFlow<Boolean> = prefs.isAnonymous
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()

    private val _locations = MutableStateFlow<List<StorageLocation>>(emptyList())
    val locations: StateFlow<List<StorageLocation>> = _locations.asStateFlow()

    private val _isLoadingAccount = MutableStateFlow(false)
    val isLoadingAccount: StateFlow<Boolean> = _isLoadingAccount.asStateFlow()

    private val _isLoadingLocations = MutableStateFlow(false)
    val isLoadingLocations: StateFlow<Boolean> = _isLoadingLocations.asStateFlow()

    fun loadAccountInfo() {
        val id = accountId.value
        if (id.isEmpty()) return
        _isLoadingAccount.value = true
        api = BuzzHeavierApi(id)
        viewModelScope.launch {
            api.getAccountInfo().onSuccess { _accountInfo.value = it }
            _isLoadingAccount.value = false
        }
    }

    fun loadLocations() {
        val id = accountId.value
        _isLoadingLocations.value = true
        api = BuzzHeavierApi(id)
        viewModelScope.launch {
            api.getStorageLocations().onSuccess { _locations.value = it }
            _isLoadingLocations.value = false
        }
    }

    fun saveAccountId(id: String) {
        viewModelScope.launch { prefs.saveAccountId(id) }
        if (id.isNotEmpty()) {
            loadAccountInfo()
            loadLocations()
        }
    }

    fun saveParentDirectory(dir: String) {
        viewModelScope.launch { prefs.saveParentDirectory(dir) }
    }

    fun saveLocationId(id: String) {
        viewModelScope.launch { prefs.saveLocationId(id) }
    }

    fun saveIsAnonymous(anonymous: Boolean) {
        viewModelScope.launch { prefs.saveIsAnonymous(anonymous) }
        if (!anonymous) {
            loadAccountInfo()
            loadLocations()
        }
    }
}
