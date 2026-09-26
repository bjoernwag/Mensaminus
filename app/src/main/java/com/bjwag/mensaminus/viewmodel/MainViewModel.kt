package com.bjwag.mensaminus.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bjwag.mensaminus.api.MensaRepository
import com.bjwag.mensaminus.model.Canteen
import com.bjwag.mensaminus.store.SettingsStore
import com.bjwag.mensaminus.store.UserSettings
import com.bjwag.mensaminus.utils.CardReader
import android.nfc.NfcAdapter
import android.nfc.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var fullScreenImageUrl by mutableStateOf<String?>(null)
        private set

    fun setFullScreenImage(url: String?) {
        fullScreenImageUrl = url
    }

    var showNfcBalanceDialog by mutableStateOf(false)
        private set

    fun dismissNfcBalanceDialog() {
        showNfcBalanceDialog = false
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val repository = MensaRepository(application)
    private val settingsStore = SettingsStore(application)

    val userSettings: StateFlow<UserSettings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _allCanteens = MutableStateFlow<List<Canteen>>(emptyList())
    val allCanteens: StateFlow<List<Canteen>> = _allCanteens

    private val nfcAdapter = NfcAdapter.getDefaultAdapter(application)
    
    val isNfcEnabled: Boolean
        get() = nfcAdapter?.isEnabled ?: false
        
    val isNfcSupported: Boolean
        get() = nfcAdapter != null

    init {
        viewModelScope.launch {
            _allCanteens.value = repository.getCanteens()
        }
    }

    fun changeDate(daysToAdd: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(daysToAdd)
    }

    fun resetDateToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsStore.setShowOnboarding(false) }
    }

    fun dismissCoachMark(screen: String) {
        viewModelScope.launch { settingsStore.dismissHint(screen) }
    }

    fun setNfcReaderEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNfcReaderEnabled(enabled) }
    }

    fun setMorningMatchNotification(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setMorningMatchNotification(enabled) }
    }

    fun processNfcTag(tag: Tag) {
        viewModelScope.launch {
            try {
                val balance = CardReader.readBalance(tag)
                if (balance != null) {
                    settingsStore.updateLastScannedBalance(balance)
                    showNfcBalanceDialog = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "NFC scan error", e)
            }
        }
    }

    fun clearCardBalance() {
        viewModelScope.launch { settingsStore.clearLastScannedBalance() }
    }
}
