package com.bjwag.mensaminus.api

import com.bjwag.mensaminus.model.Canteen
import com.bjwag.mensaminus.model.Meal
import com.bjwag.mensaminus.model.MealItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import com.bjwag.mensaminus.db.MensaDatabase
import com.bjwag.mensaminus.db.CanteenEntity
import com.bjwag.mensaminus.db.MealEntity
import com.bjwag.mensaminus.db.CacheMetadataEntity
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MensaRepository(context: Context) {

    private val api = RetrofitClient.api
    private val db = MensaDatabase.getDatabase(context)
    private val dao = db.dao()
    private val gson = Gson()


    suspend fun getCanteens(): List<Canteen> = withContext(Dispatchers.IO) {
        val dbCanteens = dao.getAllCanteens()
        if (dbCanteens.isNotEmpty()) {
            return@withContext dbCanteens.map { it.toDomain() }
        }
        
        api.getCanteens().also { canteens ->
            dao.insertCanteens(canteens.map { it.toEntity() })
        }
    }

    data class MealsResult(val meals: List<MealItem>, val lastUpdated: Long?, val isOffline: Boolean = false)

    suspend fun getMealsForCanteens(canteenIds: List<Int>, date: LocalDate): MealsResult = coroutineScope {
        val allCanteens = getCanteens()
        val canteensToFetch = allCanteens.filter { canteenIds.contains(it.id) }
        val dateString = date.toString()
        var hasError = false
        
        //check cache first
        val metadata = canteenIds.map { id -> 
            async { dao.getMetadata(id, dateString) }
        }.awaitAll().filterNotNull()
        
        val now = System.currentTimeMillis()
        
        val deferredResults = canteensToFetch.map { canteen ->
            async(Dispatchers.IO) {
                try {
                    val remoteMeals = api.getMeals(canteen.id, dateString)
                    // update cache
                    // using a composite key to keep things unique across canteens and days
                    dao.deleteMeals(canteen.id, dateString)
                    dao.insertMeals(remoteMeals.map { it.toEntity(canteen.id, dateString) })
                    dao.insertMetadata(CacheMetadataEntity("${canteen.id}_$dateString", canteen.id, dateString, now))
                    
                    remoteMeals.map { MealItem(it, canteen) }
                } catch (e: Exception) {
                    hasError = true
                    //fetch from cache on error
                    //TODO implement some kind of debugging/ notification
                    val localMeals = dao.getMeals(canteen.id, dateString)
                    localMeals.map { it.toDomain(canteen) }
                }
            }
        }

        val allMeals = deferredResults.awaitAll().flatten()
        val finalLastUpdated = if (metadata.size == canteenIds.size) metadata.minOf { it.lastUpdated } else null
        
        MealsResult(allMeals, finalLastUpdated, isOffline = hasError)
    }

    private fun Canteen.toEntity() = CanteenEntity(id, name, city, address, url)
    private fun CanteenEntity.toDomain() = Canteen(id, name, city, address, url)
    
    private fun Meal.toEntity(canteenId: Int, date: String) = MealEntity(
        dbId = "${canteenId}_${date}_${id}",
        mealId = id,
        canteenId = canteenId,
        date = date,
        name = name,
        category = category,
        pricesJson = gson.toJson(prices),
        notesJson = gson.toJson(notes),
        imageUrl = imageUrl,
        isSoldOut = isSoldOut
    )
    
    private fun MealEntity.toDomain(canteen: Canteen): MealItem {
        val prices: Map<String, Double>? = gson.fromJson(pricesJson, object : TypeToken<Map<String, Double>?>() {}.type)
        val notes: List<String>? = gson.fromJson(notesJson, object : TypeToken<List<String>?>() {}.type)
        return MealItem(
            Meal(mealId, name, category, prices, notes, imageUrl, isSoldOut),
            canteen
        )
    }

    suspend fun fetchOpeningHours(url: String): String? {
        return HtmlScraper.fetchOpeningHours(url) //this meeting could've been an e-mail ahh fun
    }
}
