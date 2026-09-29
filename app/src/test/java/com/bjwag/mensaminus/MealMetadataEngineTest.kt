package com.bjwag.mensaminus

import com.bjwag.mensaminus.utils.MealFilterEngine
import com.bjwag.mensaminus.utils.MealMetadataEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealMetadataEngineTest {

    @Test
    fun testGenericMealNameIncludesSauce() {
        val name = "Spaghetti mit Tomaten-Bolognese (A1, G)"
        val main = MealMetadataEngine.getMainName(name)
        val sides = MealMetadataEngine.getSideDishes(name)

        assertEquals("Spaghetti", main)
        assertEquals("mit Tomaten-Bolognese", sides)
    }

    @Test
    fun testGenericMealNameWithDazuSplitsSideDish() {
        val name = "Pasta mit Bärlauchpesto dazu Beilagensalat (A1)"
        val main = MealMetadataEngine.getMainName(name)
        val sides = MealMetadataEngine.getSideDishes(name)

        assertEquals("Pasta", main)
        assertEquals("mit Bärlauchpesto dazu Beilagensalat", sides)
    }

    @Test
    fun testPasta() {
        val name = "Pastasoße mit Tomaten, Oliven und Kapern (A1)"
        val main = MealMetadataEngine.getMainName(name)
        val sides = MealMetadataEngine.getSideDishes(name)

        assertEquals("Pastasoße", main)
        assertEquals("mit Tomaten, Oliven und Kapern", sides)
    }

    @Test
    fun testDistinctMealNameSplitsSides() {
        val name = "Rindergeschnetzeltes mit Spätzle dazu Rotkohl (G, I)"
        val main = MealMetadataEngine.getMainName(name)
        val sides = MealMetadataEngine.getSideDishes(name)

        assertEquals("Rindergeschnetzeltes", main)
        assertEquals("mit Spätzle dazu Rotkohl", sides)
    }

    @Test
    fun testKeywordExtractionIncludesAllIngredients() {
        val keywords = MealFilterEngine.extractKeywords("Spaghetti mit Tomaten-Bolognese dazu Beilagensalat")
        assertTrue(keywords.contains("spaghetti"))
        assertTrue(keywords.contains("tomat"))
        assertTrue(keywords.contains("bolognese"))
        assertTrue(keywords.contains("beilagensalat"))
    }
}
