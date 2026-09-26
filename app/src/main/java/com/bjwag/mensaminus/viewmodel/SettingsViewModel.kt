package com.bjwag.mensaminus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bjwag.mensaminus.store.*
import com.bjwag.mensaminus.worker.MealNotificationWorker
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)

    fun setPriceGroup(priceGroup: PriceGroup) {
        viewModelScope.launch { settingsStore.setPriceGroup(priceGroup) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsStore.setLanguage(language) }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDeveloperMode(enabled) }
    }

    fun setHideEveningMealsBefore16(hide: Boolean) {
        viewModelScope.launch { settingsStore.setHideEveningMealsBefore16(hide) }
    }

    fun setMorningMatchNotification(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setMorningMatchNotification(enabled) }
    }

    fun updateKeywordOverride(word: String, delta: Int) {
        viewModelScope.launch { settingsStore.updateKeywordOverride(word, delta) }
    }

    fun removeKeywordOverride(word: String, derivedScore: Int) {
        viewModelScope.launch { settingsStore.removeKeywordOverride(word, derivedScore) }
    }

    fun setDietaryPreference(preference: DietaryPreference) {
        viewModelScope.launch { settingsStore.setDietaryPreference(preference) }
    }

    fun toggleAllergy(allergy: String) {
        viewModelScope.launch { settingsStore.toggleAllergy(allergy) }
    }

    fun setHideSoldOut(hide: Boolean) {
        viewModelScope.launch { settingsStore.setHideSoldOut(hide) }
    }

    fun setShowScore(show: Boolean) {
        viewModelScope.launch { settingsStore.setShowScores(show) }
    }

    fun triggerTestNotification() {
        MealNotificationWorker.triggerNow(getApplication())
    }

    fun resetOnboarding() {
        viewModelScope.launch { settingsStore.setShowOnboarding(true) }
    }
}
