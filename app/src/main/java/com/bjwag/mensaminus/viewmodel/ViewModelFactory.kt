package com.bjwag.mensaminus.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(
    private val application: Application,
    private val mainViewModel: MainViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MealsViewModel::class.java) -> {
                MealsViewModel(application, mainViewModel.selectedDate, mainViewModel.userSettings) as T
            }
            modelClass.isAssignableFrom(CanteensViewModel::class.java) -> {
                CanteensViewModel(application, mainViewModel.userSettings) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
