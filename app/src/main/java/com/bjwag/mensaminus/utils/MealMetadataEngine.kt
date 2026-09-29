package com.bjwag.mensaminus.utils

import com.bjwag.mensaminus.model.Meal
import com.bjwag.mensaminus.model.MealAllergy
import java.util.regex.Pattern

object MealMetadataEngine {
    private val SPLIT_REGEX = Regex("\\b(?:mit|aus|auf|an|dazu)\\b", RegexOption.IGNORE_CASE)
    private val PARENTHESIS_REGEX = Regex("\\s*\\([^)]*\\)")
    private val CO2_REGEX = Regex("([\\d.,]+)\\s*g\\s*CO₂-Äquivalente", RegexOption.IGNORE_CASE)

    private fun cleanName(name: String): String = name.replace(PARENTHESIS_REGEX, "").trim()

    fun getMainName(name: String): String {
        val cleaned = cleanName(name)
        if (cleaned.isBlank()) return name

        val parts = cleaned.split(SPLIT_REGEX)
        return parts.first().trim()
    }

    fun getSideDishes(name: String): String {
        val cleaned = cleanName(name)
        val main = getMainName(name)

        if (cleaned.startsWith(main, ignoreCase = true)) {
            val remainder = cleaned.substring(main.length).trim()
            if (remainder.isNotBlank()) {
                return remainder
            }
        }
        return ""
    }

    fun isVegan(notes: List<String>?): Boolean = notes?.any { it.contains("vegan", true) } == true
    fun isVeggie(notes: List<String>?): Boolean = notes?.any { it.contains("vegetarisch", true) || it.contains("veggie", true) } == true
    fun isPork(notes: List<String>?): Boolean = notes?.any { it.contains("schweinefleisch", true) } == true
    fun isBeef(notes: List<String>?): Boolean = notes?.any { it.contains("rindfleisch", true) } == true
    fun isFish(notes: List<String>?): Boolean = notes?.any { it.contains("fisch", true) } == true

    fun isPoultry(name: String): Boolean {
        return name.contains("geflügel", true) ||
                name.contains("ente", true) ||
                name.contains("pute", true) ||
                name.contains("hähnchen", true)
    }

    fun isAllergyMatch(allergy: MealAllergy, notes: List<String>): Boolean {
        return notes.any { note ->
            val hasCode = allergy.codes.any { code ->
                val codeRegex = Regex("(?<=[^a-zA-Z0-9]|^)$code(?=[^a-zA-Z0-9]|$)")
                note.contains(codeRegex) || note.contains("($code)", ignoreCase = true)
            }
            
            val hasKeyword = allergy.keywords.any { keyword ->
                val regex = Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                note.contains(regex)
            }
            hasCode || hasKeyword
        }
    }

    fun extractCo2(notes: List<String>?): String? {
        notes?.forEach { note ->
            val match = CO2_REGEX.find(note)
            if (match != null) return match.groupValues[1] + " g CO₂"
        }
        return null
    }

    fun isKlimaTeller(notes: List<String>?): Boolean {
        return notes?.any { it.contains("KlimaTeller", true) } == true
    }

    fun calculateRelevanceScore(
        mealName: String,
        canteenRank: Int,
        likedMeals: Set<String>,
        dislikedMeals: Set<String>,
        keywordScores: Map<String, Int>
    ): Double {
        var score = 0.0
        val lowerMealName = mealName.lowercase()

        val bestLikeSimilarity = likedMeals.maxOfOrNull { lowerMealName.similarityTo(it.lowercase()) } ?: 0.0
        if (bestLikeSimilarity > 0.8) {
            score += 1000.0 * bestLikeSimilarity
        }

        val bestDislikeSimilarity = dislikedMeals.maxOfOrNull { lowerMealName.similarityTo(it.lowercase()) } ?: 0.0
        if (bestDislikeSimilarity > 0.8) {
            score -= 1000.0 * bestDislikeSimilarity
        }

        val mealKeywords = MealFilterEngine.extractKeywords(mealName)
        val totalKeywordWeight = mealKeywords.sumOf { keywordScores[it] ?: 0 }

        if (totalKeywordWeight > 0) {
            score += (totalKeywordWeight * 50.0)
        } else if (totalKeywordWeight < 0) {
            score += (totalKeywordWeight * 30.0) 
        }

        score -= (canteenRank * 0.1)
        return score
    }
}
