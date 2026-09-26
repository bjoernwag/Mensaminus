package com.bjwag.mensaminus.utils

import com.bjwag.mensaminus.model.*
import com.bjwag.mensaminus.store.UserSettings
import java.time.LocalTime

object MealFilterEngine {

    // after 4pm we start thinking about dinner, maybe make configurable
    private val EVENING_MEAL_THRESHOLD: LocalTime = LocalTime.of(16, 0)

    fun passesUserFilters(
        item: MealItem,
        settings: UserSettings,
    ): Boolean {
        val passDietary = when (settings.dietaryPreference) {
            com.bjwag.mensaminus.store.DietaryPreference.ANY -> true
            com.bjwag.mensaminus.store.DietaryPreference.VEGETARIAN -> item.meal.isVeggie || item.meal.isVegan
            com.bjwag.mensaminus.store.DietaryPreference.VEGAN -> item.meal.isVegan
        }
        
        val passAllergies = if (settings.excludedAllergies.isNotEmpty()) {
            val notes = item.meal.notes ?: emptyList()
            settings.excludedAllergies.none { allergyName ->
                MealAllergy.fromName(allergyName)?.isMatch(notes) == true
            }
        } else true

        val passSoldOut = !settings.hideSoldOut || !item.meal.isSoldOut
        val isCanteenActive = settings.activeCanteens.contains(item.canteen.id)

        return passDietary && passAllergies && passSoldOut && isCanteenActive
    }

    fun filterAndSortMeals(
        meals: List<MealItem>,
        settings: UserSettings,
        isToday: Boolean,
        now: LocalTime = LocalTime.now()
    ): List<MealItem> {
        val activeIds = settings.activeCanteens
        val likedKeywordsCount = settings.likedMeals.flatMap { extractKeywords(it) }.groupingBy { it }.eachCount()
        val dislikedKeywordsCount = settings.dislikedMeals.flatMap { extractKeywords(it) }.groupingBy { it }.eachCount()

        val allKeywords = (likedKeywordsCount.keys + dislikedKeywordsCount.keys + settings.keywordOverrides.keys).toSet()
        val keywordScores = allKeywords.associateWith { word ->
            ((likedKeywordsCount[word] ?: 0) - (dislikedKeywordsCount[word] ?: 0)) + (settings.keywordOverrides[word] ?: 0)
        }

        val isAfterEveningThreshold = now.isAfter(EVENING_MEAL_THRESHOLD)

        return meals.filter { item ->
            passesUserFilters(item, settings)
        }.map { item ->
            item.copy(
                score = MealMetadataEngine.calculateRelevanceScore(
                    mealName = item.meal.mainName,
                    canteenRank = activeIds.indexOf(item.canteen.id),
                    likedMeals = settings.likedMeals,
                    dislikedMeals = settings.dislikedMeals,
                    keywordScores = keywordScores
                )
            )
        }.sortedWith(compareByDescending<MealItem> { item ->
            // Evening meals always on top after 16:00 for today
            if (isToday && isAfterEveningThreshold && item.meal.isEveningMeal) 1 else 0
        }.thenBy { item ->
            // If setting active and before 16:00, move evening meals to bottom
            if (isToday && settings.hideEveningMealsBefore16 && !isAfterEveningThreshold && item.meal.isEveningMeal) 1 else 0
        }.thenByDescending { it.score })
    }

    fun extractKeywords(mealName: String): Set<String> {
        //TODO maybe centralise?
        val stopwords = setOf("mit", "und", "oder", "in", "an", "auf", "aus", "dazu", "von", "im", "vom", "der", "die", "das", "ein", "eine")

        return mealName.lowercase()
            .replace(Regex("[^a-zäöüß]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in stopwords }
            .map { word ->
                // probably there is a better way to do this
                when {
                    word.endsWith("en") && word.length > 5 -> word.dropLast(2)
                    word.endsWith("n") && word.length > 4 -> word.dropLast(1)
                    word.endsWith("s") && word.length > 4 -> word.dropLast(1)
                    word.endsWith("er") && word.length > 4 -> word.dropLast(2)
                    word.endsWith("ne") && word.length > 4 -> word.dropLast(1)
                    else -> word
                }
            }
            .toSet()
    }

    fun getKeywordDatabase(
        likedMeals: Set<String>,
        dislikedMeals: Set<String>,
        overrides: Map<String, Int> = emptyMap()
    ): List<Pair<String, Int>> {
        val likedKeywordsCount = likedMeals.flatMap { extractKeywords(it) }.groupingBy { it }.eachCount()
        val dislikedKeywordsCount = dislikedMeals.flatMap { extractKeywords(it) }.groupingBy { it }.eachCount()
        val allKeywords = (likedKeywordsCount.keys + dislikedKeywordsCount.keys + overrides.keys).toSet()

        return allKeywords.map { word ->
            val derived = (likedKeywordsCount[word] ?: 0) - (dislikedKeywordsCount[word] ?: 0)
            val override = overrides[word] ?: 0
            word to (derived + override)
        }.filter { it.second != 0 }
            .sortedByDescending { it.second }
    }
}
