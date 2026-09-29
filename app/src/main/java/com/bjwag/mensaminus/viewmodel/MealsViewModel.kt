package com.bjwag.mensaminus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bjwag.mensaminus.api.MensaRepository
import com.bjwag.mensaminus.model.MealItem
import com.bjwag.mensaminus.model.UiState
import com.bjwag.mensaminus.store.SettingsStore
import com.bjwag.mensaminus.store.UserSettings
import com.bjwag.mensaminus.utils.MealFilterEngine
import com.bjwag.mensaminus.utils.MealMetadataEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MealsViewModel(
    application: Application,
    private val selectedDate: StateFlow<LocalDate>,
    private val userSettings: StateFlow<UserSettings>
) : AndroidViewModel(application) {

    private val _rawMeals = MutableStateFlow<List<MealItem>>(emptyList())
    val rawMeals: StateFlow<List<MealItem>> = _rawMeals

    private val _keywordScores = userSettings
        .map { settings ->
            val likedKeywordsCount = settings.likedMeals.flatMap { MealFilterEngine.extractKeywords(it) }.groupingBy { it }.eachCount()
            val dislikedKeywordsCount = settings.dislikedMeals.flatMap { MealFilterEngine.extractKeywords(it) }.groupingBy { it }.eachCount()
            val allKeywords = (likedKeywordsCount.keys + dislikedKeywordsCount.keys + settings.keywordOverrides.keys).toSet()
            allKeywords.associateWith { word ->
                ((likedKeywordsCount[word] ?: 0) - (dislikedKeywordsCount[word] ?: 0)) + (settings.keywordOverrides[word] ?: 0)
            }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _scoredMeals = combine(
        _rawMeals,
        userSettings.map { it.activeCanteens to (it.likedMeals to it.dislikedMeals) }.distinctUntilChanged(),
        _keywordScores
    ) { meals, settingsInfo, keywordScores ->
        val (activeIds, preferences) = settingsInfo
        val (likedMeals, dislikedMeals) = preferences
        meals.map { item ->
            item.copy(
                score = MealMetadataEngine.calculateRelevanceScore(
                    mealName = item.meal.name,
                    canteenRank = activeIds.indexOf(item.canteen.id),
                    likedMeals = likedMeals,
                    dislikedMeals = dislikedMeals,
                    keywordScores = keywordScores
                )
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    val isRefreshing = MutableStateFlow(false)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val repository = MensaRepository(application)
    private val settingsStore = SettingsStore(application)

    private val _internalUiState = MutableStateFlow<UiState>(UiState.Loading)
    private val _searchState = combine(_searchQuery, _isSearchActive) { query, active -> query to active }

    val uiState: StateFlow<UiState> = combine(
        _internalUiState,
        _scoredMeals,
        userSettings.map { 
            // Filter out unrelated settings like NFC balance to prevent unnecessary UI updates
            it.copy(lastScannedBalance = null, lastScanTimestamp = 0L)
        }.distinctUntilChanged(),
        selectedDate,
        _searchState
    ) { state, meals, settings, date, searchState ->
        val (query, isSearchActive) = searchState
        if (state is UiState.Success) {
            val isToday = date == LocalDate.now()
            
            if (isSearchActive) {
                val matching = if (query.isBlank()) {
                    meals
                } else {
                    meals.filter {
                        it.meal.name.contains(query, ignoreCase = true) ||
                                it.canteen.name.contains(query, ignoreCase = true)
                    }
                }
                
                // Partition based on whether they pass the user's active filters
                val (passed, failed) = matching.partition {
                    MealFilterEngine.passesUserFilters(it, settings)
                }
                
                // Sort both lists for consistency
                val sortedPassed = sortPartition(passed, settings, isToday)
                val sortedFailed = sortPartition(failed, settings, isToday)
                
                UiState.Success(
                    meals = sortedPassed,
                    matchingMeals = sortedPassed,
                    otherMatchingMeals = sortedFailed
                )
            } else {
                val filtered = MealFilterEngine.filterAndSortMeals(
                    meals = meals,
                    settings = settings,
                    isToday = isToday
                )
                UiState.Success(meals = filtered)
            }
        } else {
            state
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private fun sortPartition(
        meals: List<MealItem>,
        settings: UserSettings,
        isToday: Boolean
    ): List<MealItem> {
        val isAfterEveningThreshold = java.time.LocalTime.now().isAfter(java.time.LocalTime.of(16, 0))

        return meals.sortedWith(compareByDescending<MealItem> { item ->
            if (isToday && isAfterEveningThreshold && item.meal.isEveningMeal) 1 else 0
        }.thenBy { item ->
            if (isToday && settings.hideEveningMealsBefore16 && !isAfterEveningThreshold && item.meal.isEveningMeal) 1 else 0
        }.thenByDescending { it.score })
    }

    init {
        viewModelScope.launch {
            combine(
                selectedDate,
                userSettings.map { it.activeCanteens.toSet() }.distinctUntilChanged()
            ) { date, activeIds ->
                date to activeIds
            }.distinctUntilChanged().collect { (date, activeIds) ->
                loadMeals(activeIds.toList(), date)
            }
        }
    }

    private suspend fun loadMeals(canteenIds: List<Int>, date: LocalDate, isSilent: Boolean = false) {
        if (canteenIds.isEmpty()) {
            _rawMeals.value = emptyList()
            _internalUiState.value = UiState.Success(emptyList())
            _lastUpdated.value = null
            return
        }

        if (!isSilent) {
            _internalUiState.value = UiState.Loading
        }
        
        try {
            val result = repository.getMealsForCanteens(canteenIds, date)
            _rawMeals.value = result.meals
            _lastUpdated.value = result.lastUpdated
            _isOffline.value = result.isOffline
            _internalUiState.value = UiState.Success(result.meals)
        } catch (e: IOException) {
            if (!isSilent || _rawMeals.value.isEmpty()) {
                _internalUiState.value = UiState.Error(com.bjwag.mensaminus.R.string.error_no_internet)
            }
        } catch (e: Exception) {
            if (!isSilent || _rawMeals.value.isEmpty()) {
                _internalUiState.value = UiState.Error(com.bjwag.mensaminus.R.string.error_server)
            }
        }
    }

    fun retry() {
        viewModelScope.launch {
            loadMeals(userSettings.value.activeCanteens, selectedDate.value)
        }
    }

    fun refreshCurrentDate(isSilent: Boolean = false) {
        viewModelScope.launch {
            // don't refresh if we are already doing stuff
            if (isSilent) {
                loadMeals(userSettings.value.activeCanteens, selectedDate.value, isSilent = true)
            } else {
                isRefreshing.value = true
                loadMeals(userSettings.value.activeCanteens, selectedDate.value)
                isRefreshing.value = false
            }
        }
    }

    fun toggleLikeMeal(mealName: String) {
        viewModelScope.launch { settingsStore.toggleLikedMeal(mealName) }
    }

    fun toggleDislikeMeal(mealName: String) {
        viewModelScope.launch { settingsStore.toggleDislikedMeal(mealName) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun getFormattedDate(date: LocalDate, language: com.bjwag.mensaminus.store.AppLanguage): String? {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        if (date == today || date == tomorrow) return null

        val locale = when (language) {
            com.bjwag.mensaminus.store.AppLanguage.GERMAN -> Locale.forLanguageTag("de")
            com.bjwag.mensaminus.store.AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
        }

        return date.format(DateTimeFormatter.ofPattern("EE, dd.MM.", locale))
    }
}
