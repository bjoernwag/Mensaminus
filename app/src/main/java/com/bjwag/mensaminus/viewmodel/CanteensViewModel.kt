package com.bjwag.mensaminus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bjwag.mensaminus.api.MensaRepository
import com.bjwag.mensaminus.store.SettingsStore
import com.bjwag.mensaminus.store.UserSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CanteensViewModel(
    application: Application,
    private val userSettings: StateFlow<UserSettings>
) : AndroidViewModel(application) {

    private val repository = MensaRepository(application)
    private val settingsStore = SettingsStore(application)

    fun toggleCanteenActive(canteenId: Int) {
        viewModelScope.launch {
            val current = userSettings.value.activeCanteens.toMutableList()
            if (current.contains(canteenId)) {
                current.remove(canteenId)
            } else {
                current.add(canteenId)
            }
            settingsStore.setActiveCanteens(current)
        }
    }

    fun deselectAllCanteens() {
        viewModelScope.launch {
            settingsStore.setActiveCanteens(emptyList())
        }
    }

    fun moveCanteen(canteenId: Int, toIndex: Int) {
        val current = userSettings.value.activeCanteens.toMutableList()
        val fromIndex = current.indexOf(canteenId)
        if (fromIndex == -1) return
        current.removeAt(fromIndex)
        current.add(toIndex.coerceIn(0, current.size), canteenId)
        viewModelScope.launch { settingsStore.setActiveCanteens(current) }
    }

    suspend fun getOpeningHours(url: String): String? {
        return repository.fetchOpeningHours(url)
    }
}
