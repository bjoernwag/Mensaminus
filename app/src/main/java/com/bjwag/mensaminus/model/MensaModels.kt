package com.bjwag.mensaminus.model

import android.util.Log
import com.bjwag.mensaminus.utils.similarityTo
import com.google.gson.annotations.SerializedName

data class Canteen(
    val id: Int,
    val name: String,
    val city: String,
    val address: String,
    val url: String? = null
)

data class CanteenDay(
    val date: String,
    val closed: Boolean
)

data class Meal(
    val id: Int,
    val name: String,
    val category: String,
    val prices: Map<String, Double>?,
    val notes: List<String>?,
    @SerializedName("image") private val rawImageUrl: String? = null,
    @SerializedName("soldout") val isSoldOut: Boolean = false
) {

    //TODO probabyl should centralise this somewhere
    val priceStudent: Double? get() = prices?.get("Studierende")
    val priceEmployee: Double? get() = prices?.get("Bedienstete")

    fun getPriceForGroup(priceGroup: com.bjwag.mensaminus.store.PriceGroup): Double? {
        return when (priceGroup) {
            com.bjwag.mensaminus.store.PriceGroup.STUDENT -> priceStudent
            com.bjwag.mensaminus.store.PriceGroup.EMPLOYEE -> priceEmployee
        }
    }

    val isEveningMeal: Boolean get() = category.contains("Abend", ignoreCase = true)

    val imageUrl: String?
        get() {
            if (rawImageUrl.isNullOrBlank()) return null
            if (rawImageUrl.contains("studentenwerk-dresden-lieber-mensen-gehen.jpg")) return null
            return if (rawImageUrl.startsWith("//")) "https:$rawImageUrl" else rawImageUrl
        }

    val mainName: String get() = com.bjwag.mensaminus.utils.MealMetadataEngine.getMainName(name)
    val sideDishes: String get() = com.bjwag.mensaminus.utils.MealMetadataEngine.getSideDishes(name)

    val isVegan: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isVegan(notes)
    val isVeggie: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isVeggie(notes)
    val isPork: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isPork(notes)
    val isBeef: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isBeef(notes)
    val isFish: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isFish(notes)
    val isPoultry: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isPoultry(name)
    val co2Content: String? get() = com.bjwag.mensaminus.utils.MealMetadataEngine.extractCo2(notes)
    val isKlimaTeller: Boolean get() = com.bjwag.mensaminus.utils.MealMetadataEngine.isKlimaTeller(notes)
}

data class MealItem(
    val meal: Meal,
    val canteen: Canteen,
    val score: Double = 0.0
)

sealed class UiState {
    object Loading : UiState()
    data class Success(
        val meals: List<MealItem>,
        val matchingMeals: List<MealItem> = emptyList(),
        val otherMatchingMeals: List<MealItem> = emptyList()
    ) : UiState()
    data class Error(val messageResId: Int) : UiState()
}

enum class MealAllergy(val keywords: List<String>, val codes: List<String> = emptyList()) {
    GLUTEN(listOf("Glutenhaltiges Getreide", "Weizen", "Roggen", "Gerste", "Hafer", "Dinkel", "Grünkern", "Kamut"), listOf("A", "A1", "A2", "A3", "A4", "A5", "A6", "A7")),
    LACTOSE(listOf("Milch/Milchzucker", "Laktose", "Sahne", "Milch"), listOf("G")),
    EGGS(listOf("Eier", "Ei"), listOf("C")),
    PEANUTS(listOf("Erdnüsse", "Erdnuss"), listOf("E")),
    NUTS(listOf("Schalenfrüchte", "Nüsse", "Mandel", "Haselnuss", "Walnuss", "Cashewnuss", "Pekannuss", "Paranuss", "Pistazie", "Macadamia", "Queenslandnuss"), listOf("H", "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8")),
    SOY(listOf("Soja"), listOf("F")),
    FISH(listOf("Fisch"), listOf("D")),
    CRUSTACEANS(listOf("Krebstiere"), listOf("B")),
    CELERY(listOf("Sellerie"), listOf("I")),
    MUSTARD(listOf("Senf"), listOf("J")),
    SESAME(listOf("Sesam"), listOf("K")),
    SULPHITES(listOf("Sulfit", "Schwefeldioxid", "geschwefelt"), listOf("L", "5")),
    LUPIN(listOf("Lupine"), listOf("M")),
    MOLLUSCS(listOf("Weichtiere"), listOf("N")),
    ALCOHOL(listOf("enthält Alkohol", "Alkohol"), emptyList()),
    PORK(listOf("enthält Schweinefleisch", "Schweinefleisch", "Schinken aus Schinkenteilen zusammengesetzt"), emptyList()),
    BEEF(listOf("enthält Rindfleisch", "Rindfleisch"), emptyList()),
    GARLIC(listOf("enthält Knoblauch", "Knoblauch"), emptyList()),
    LAB(listOf("mit tierischem Lab"), emptyList()),
    GELATINE(listOf("mit Gelatine"), emptyList()),
    ADDITIVES(listOf("Farbstoff", "Konservierungsstoff", "Antioxydationsmittel", "Geschmacksverstärker", "geschwärzt", "gewachst", "Phosphat", "Süßungsmittel", "Phenylalaninquelle", "Aminosäuren", "koffeinhaltig", "chininhaltig"), listOf("1", "2", "3", "4", "6", "7", "8", "9", "10", "100", "101"));

    fun isMatch(notes: List<String>): Boolean {
        return com.bjwag.mensaminus.utils.MealMetadataEngine.isAllergyMatch(this, notes)
    }

    val displayNameResId: Int get() = when(this) {
        GLUTEN -> com.bjwag.mensaminus.R.string.allergy_gluten
        LACTOSE -> com.bjwag.mensaminus.R.string.allergy_lactose
        EGGS -> com.bjwag.mensaminus.R.string.allergy_eggs
        PEANUTS -> com.bjwag.mensaminus.R.string.allergy_peanuts
        NUTS -> com.bjwag.mensaminus.R.string.allergy_nuts
        SOY -> com.bjwag.mensaminus.R.string.allergy_soy
        FISH -> com.bjwag.mensaminus.R.string.allergy_fish
        CRUSTACEANS -> com.bjwag.mensaminus.R.string.allergy_crustaceans
        CELERY -> com.bjwag.mensaminus.R.string.allergy_celery
        MUSTARD -> com.bjwag.mensaminus.R.string.allergy_mustard
        SESAME -> com.bjwag.mensaminus.R.string.allergy_sesame
        SULPHITES -> com.bjwag.mensaminus.R.string.allergy_sulphites
        LUPIN -> com.bjwag.mensaminus.R.string.allergy_lupin
        MOLLUSCS -> com.bjwag.mensaminus.R.string.allergy_molluscs
        ALCOHOL -> com.bjwag.mensaminus.R.string.allergy_alcohol
        PORK -> com.bjwag.mensaminus.R.string.allergy_pork
        BEEF -> com.bjwag.mensaminus.R.string.allergy_beef
        GARLIC -> com.bjwag.mensaminus.R.string.allergy_garlic
        LAB -> com.bjwag.mensaminus.R.string.allergy_lab
        GELATINE -> com.bjwag.mensaminus.R.string.allergy_gelatine
        ADDITIVES -> com.bjwag.mensaminus.R.string.allergy_additives
    }

    companion object {
        fun fromName(name: String): MealAllergy? {
            return try {
                valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    }
}
