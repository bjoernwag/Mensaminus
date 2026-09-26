package com.bjwag.mensaminus.db

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "canteens")
data class CanteenEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val city: String,
    val address: String,
    val url: String?
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val dbId: String,
    val mealId: Int,
    val canteenId: Int,
    val date: String,
    val name: String,
    val category: String,
    val pricesJson: String,
    val notesJson: String,
    val imageUrl: String?,
    val isSoldOut: Boolean
)

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val id: String,
    val canteenId: Int,
    val date: String,
    val lastUpdated: Long
)

@Dao
interface MensaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanteens(canteens: List<CanteenEntity>)

    @Query("SELECT * FROM canteens")
    suspend fun getAllCanteens(): List<CanteenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Query("SELECT * FROM meals WHERE canteenId = :canteenId AND date = :date")
    suspend fun getMeals(canteenId: Int, date: String): List<MealEntity>

    @Query("DELETE FROM meals WHERE canteenId = :canteenId AND date = :date")
    suspend fun deleteMeals(canteenId: Int, date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CacheMetadataEntity)

    @Query("SELECT * FROM cache_metadata WHERE canteenId = :canteenId AND date = :date")
    suspend fun getMetadata(canteenId: Int, date: String): CacheMetadataEntity?
}

@Database(entities = [CanteenEntity::class, MealEntity::class, CacheMetadataEntity::class], version = 1)
abstract class MensaDatabase : RoomDatabase() {
    abstract fun dao(): MensaDao

    companion object {
        @Volatile
        private var INSTANCE: MensaDatabase? = null

        fun getDatabase(context: Context): MensaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MensaDatabase::class.java,
                    "mensa_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
