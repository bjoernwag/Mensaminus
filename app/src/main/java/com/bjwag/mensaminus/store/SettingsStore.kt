package com.bjwag.mensaminus.store

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringSetPreferencesKey
val Context.dataStore by preferencesDataStore(name = "mensa_settings")

enum class PriceGroup {
    STUDENT, EMPLOYEE
}

enum class AppLanguage {
    GERMAN, ENGLISH
}

enum class DietaryPreference {
    ANY, VEGETARIAN, VEGAN
    // more to add?
}

data class UserSettings(
    val priceGroup: PriceGroup = PriceGroup.STUDENT,
    val language: AppLanguage = if (java.util.Locale.getDefault().language == "de") AppLanguage.GERMAN else AppLanguage.ENGLISH,
    val dietaryPreference: DietaryPreference = DietaryPreference.ANY,
    val excludedAllergies: Set<String> = emptySet(),
    val showScores: Boolean = false,
    val hideSoldOut: Boolean = false,
    val activeCanteens: List<Int> = SettingsStore.DEFAULT_CANTEEN_IDS,
    val likedMeals: Set<String> = emptySet(),
    val dislikedMeals: Set<String> = emptySet(),
    val isDeveloperMode: Boolean = false,
    val hideEveningMealsBefore16: Boolean = false,
    val morningMatchNotification: Boolean = false,
    val showOnboarding: Boolean = true,
    val dismissedHints: Set<String> = emptySet(),
    val keywordOverrides: Map<String, Int> = emptyMap(),
    val nfcReaderEnabled: Boolean = false,
    val lastScannedBalance: Double? = null,
    val lastScanTimestamp: Long = 0L
)

class SettingsStore(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        private const val DEFAULT_CANTEEN_STRING = "1,4,29,9,6"
        val DEFAULT_CANTEEN_IDS = listOf(1, 4, 29, 9, 6)

        val PRICE_GROUP = stringPreferencesKey("price_group")
        val LANGUAGE = stringPreferencesKey("language")
        val DIETARY_PREFERENCE = stringPreferencesKey("dietary_preference")
        val EXCLUDED_ALLERGIES = stringSetPreferencesKey("excluded_allergies")
        val SHOW_SCORES = booleanPreferencesKey("show_scores")
        val HIDE_SOLD_OUT = booleanPreferencesKey("hide_sold_out")
        val ACTIVE_CANTEENS = stringPreferencesKey("active_canteens")
        val LIKED_MEALS = stringSetPreferencesKey("liked_meals")
        val DISLIKED_MEALS = stringSetPreferencesKey("disliked_meals")
        val IS_DEVELOPER_MODE = booleanPreferencesKey("is_developer_mode")
        val HIDE_EVENING_MEALS_BEFORE_16 = booleanPreferencesKey("hide_evening_meals_before_16")
        val MORNING_MATCH_NOTIFICATION = booleanPreferencesKey("morning_match_notification")
        val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
        val DISMISSED_HINTS = stringSetPreferencesKey("dismissed_hints")
        val KEYWORD_OVERRIDES = stringPreferencesKey("keyword_overrides")
        val NFC_READER_ENABLED = booleanPreferencesKey("nfc_reader_enabled")
        val LAST_SCANNED_BALANCE = doublePreferencesKey("last_scanned_balance")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
    }

    private val gson = com.google.gson.Gson()

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        val activeString = preferences[ACTIVE_CANTEENS] ?: DEFAULT_CANTEEN_STRING
        val activeList = if (activeString.isEmpty()) emptyList() else activeString.split(",").mapNotNull { it.toIntOrNull() }
        val priceGroup = try {
            PriceGroup.valueOf(preferences[PRICE_GROUP] ?: PriceGroup.STUDENT.name)
        } catch (e: Exception) {
            PriceGroup.STUDENT
        }

        val language = try {
            val saved = preferences[LANGUAGE]
            if (saved != null) {
                AppLanguage.valueOf(saved)
            } else {
                if (java.util.Locale.getDefault().language == "de") AppLanguage.GERMAN else AppLanguage.ENGLISH
            }
        } catch (e: Exception) {
            //don't expect the getDefault to fail though could just return eng in case
            if (java.util.Locale.getDefault().language == "de") AppLanguage.GERMAN else AppLanguage.ENGLISH
        }

        val dietaryPreference = try {
            DietaryPreference.valueOf(preferences[DIETARY_PREFERENCE] ?: DietaryPreference.ANY.name)
        } catch (e: Exception) {
            // TODO pre-release migration can probably removed
            val wasVegan = preferences[booleanPreferencesKey("vegan_only")] ?: false
            val wasVeggie = preferences[booleanPreferencesKey("vegetarian_only")] ?: false
            when {
                wasVegan -> DietaryPreference.VEGAN
                wasVeggie -> DietaryPreference.VEGETARIAN
                else -> DietaryPreference.ANY
            }
        }
        
        val overridesJson = preferences[KEYWORD_OVERRIDES] ?: "{}"
        val overrides: Map<String, Int> = try {
            gson.fromJson(overridesJson, object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }

        UserSettings(
            priceGroup = priceGroup,
            language = language,
            dietaryPreference = dietaryPreference,
            excludedAllergies = preferences[EXCLUDED_ALLERGIES] ?: emptySet(),
            showScores = preferences[SHOW_SCORES] ?: false,
            hideSoldOut = preferences[HIDE_SOLD_OUT] ?: false,
            activeCanteens = activeList,
            likedMeals = preferences[LIKED_MEALS] ?: emptySet(),
            dislikedMeals = preferences[DISLIKED_MEALS] ?: emptySet(),
            isDeveloperMode = preferences[IS_DEVELOPER_MODE] ?: false,
            hideEveningMealsBefore16 = preferences[HIDE_EVENING_MEALS_BEFORE_16] ?: false,
            morningMatchNotification = preferences[MORNING_MATCH_NOTIFICATION] ?: false,
            showOnboarding = preferences[SHOW_ONBOARDING] ?: true,
            dismissedHints = preferences[DISMISSED_HINTS] ?: emptySet(),
            keywordOverrides = overrides,
            nfcReaderEnabled = preferences[NFC_READER_ENABLED] ?: false,
            lastScannedBalance = preferences[LAST_SCANNED_BALANCE],
            lastScanTimestamp = preferences[LAST_SCAN_TIMESTAMP] ?: 0L
        )
    }


    suspend fun setPriceGroup(priceGroup: PriceGroup) {
        dataStore.edit { it[PRICE_GROUP] = priceGroup.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE] = language.name }
    }

    suspend fun setDietaryPreference(preference: DietaryPreference) {
        dataStore.edit { it[DIETARY_PREFERENCE] = preference.name }
    }

    suspend fun toggleAllergy(allergy: String) {
        dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_ALLERGIES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(allergy)) current.remove(allergy) else current.add(allergy)
            preferences[EXCLUDED_ALLERGIES] = current
        }
    }

    suspend fun setHideSoldOut(hide: Boolean) {
        dataStore.edit { it[HIDE_SOLD_OUT] = hide }
    }

    suspend fun setShowScores(show: Boolean) {
        dataStore.edit { it[SHOW_SCORES] = show}
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { it[IS_DEVELOPER_MODE] = enabled }
    }

    suspend fun setHideEveningMealsBefore16(hide: Boolean) {
        dataStore.edit { it[HIDE_EVENING_MEALS_BEFORE_16] = hide }
    }

    suspend fun setMorningMatchNotification(enabled: Boolean) {
        dataStore.edit { it[MORNING_MATCH_NOTIFICATION] = enabled }
    }

    suspend fun setShowOnboarding(show: Boolean) {
        dataStore.edit { it[SHOW_ONBOARDING] = show }
        if (show) {
            // Reset hints too when onboarding is reset
            dataStore.edit {
                it[DISMISSED_HINTS] = emptySet()
            }
        }
    }

    suspend fun dismissHint(hintId: String) {
        dataStore.edit { preferences ->
            val current = preferences[DISMISSED_HINTS]?.toMutableSet() ?: mutableSetOf()
            current.add(hintId)
            preferences[DISMISSED_HINTS] = current
        }
    }

    suspend fun updateKeywordOverride(word: String, delta: Int) {
        dataStore.edit { preferences ->
            val overridesJson = preferences[KEYWORD_OVERRIDES] ?: "{}"
            val overrides: MutableMap<String, Int> = try {
                gson.fromJson(overridesJson, object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type)
            } catch (e: Exception) {
                mutableMapOf()
            }
            val current = overrides[word] ?: 0
            overrides[word] = current + delta
            preferences[KEYWORD_OVERRIDES] = gson.toJson(overrides)
        }
    }

    suspend fun removeKeywordOverride(word: String, derivedScore: Int) {
        dataStore.edit { preferences ->
            val overridesJson = preferences[KEYWORD_OVERRIDES] ?: "{}"
            val overrides: MutableMap<String, Int> = try {
                gson.fromJson(overridesJson, object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type)
            } catch (e: Exception) {
                mutableMapOf()
            }
            // revert derived scores by flipping symbol, safe??
            overrides[word] = -derivedScore
            preferences[KEYWORD_OVERRIDES] = gson.toJson(overrides)
        }
    }

    suspend fun setActiveCanteens(canteenIds: List<Int>) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_CANTEENS] = canteenIds.joinToString(",")
        }
    }

    suspend fun toggleLikedMeal(mealName: String) {
        dataStore.edit { preferences ->
            val current = preferences[LIKED_MEALS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(mealName)) current.remove(mealName) else current.add(mealName)
            preferences[LIKED_MEALS] = current

            val disliked = preferences[DISLIKED_MEALS]?.toMutableSet() ?: mutableSetOf()
            if (disliked.contains(mealName)) {
                disliked.remove(mealName)
                preferences[DISLIKED_MEALS] = disliked
            }
        }
    }

    suspend fun toggleDislikedMeal(mealName: String) {
        dataStore.edit { preferences ->
            val current = preferences[DISLIKED_MEALS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(mealName)) current.remove(mealName) else current.add(mealName)
            preferences[DISLIKED_MEALS] = current

            val liked = preferences[LIKED_MEALS]?.toMutableSet() ?: mutableSetOf()
            if (liked.contains(mealName)) {
                liked.remove(mealName)
                preferences[LIKED_MEALS] = liked
            }
        }
    }

    suspend fun setNfcReaderEnabled(enabled: Boolean) {
        dataStore.edit { it[NFC_READER_ENABLED] = enabled }
    }

    suspend fun updateLastScannedBalance(balance: Double) {
        dataStore.edit { preferences ->
            preferences[LAST_SCANNED_BALANCE] = balance
            preferences[LAST_SCAN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearLastScannedBalance() {
        dataStore.edit { preferences ->
            preferences.remove(LAST_SCANNED_BALANCE)
            preferences[LAST_SCAN_TIMESTAMP] = 0L
        }
    }
}
