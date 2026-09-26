package com.bjwag.mensaminus.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.bjwag.mensaminus.MainActivity
import com.bjwag.mensaminus.R
import com.bjwag.mensaminus.api.MensaRepository
import com.bjwag.mensaminus.model.Meal
import com.bjwag.mensaminus.model.MealItem
import com.bjwag.mensaminus.store.SettingsStore
import com.bjwag.mensaminus.utils.MealFilterEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class MealNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val settings = settingsStore.settingsFlow.first()

        if (!settings.morningMatchNotification) return Result.success()

        val repository = MensaRepository(applicationContext)
        val likedMeals = settings.likedMeals
        if (likedMeals.isEmpty()) return Result.success()

        val canteenIds = settings.activeCanteens
        if (canteenIds.isEmpty()) return Result.success()

        try {
            val today = LocalDate.now()
            val result = repository.getMealsForCanteens(canteenIds, today)
            
            val matches = result.meals.filter { item ->
                MealFilterEngine.passesUserFilters(item, settings) &&
                likedMeals.any { liked -> 
                    item.meal.mainName.equals(liked, ignoreCase = true) 
                }
            }

            if (matches.isNotEmpty()) {
                sendNotification(matches.size, matches.first().meal.mainName, matches.first().canteen.name)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(matchCount: Int, firstMealName: String, canteenName: String) {
        val channelId = "mensa_matches"
        createNotificationChannel(channelId)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = applicationContext.getString(R.string.notification_match_title)
        val content = if (matchCount == 1) {
            applicationContext.getString(R.string.notification_match_single, firstMealName, canteenName)
        } else {
            applicationContext.getString(R.string.notification_match_multiple, matchCount)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            try {
                notify(1, builder.build())
            } catch (e: SecurityException) {
                // Handle missing permission on Android 13+
            }
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.notification_channel_name)
            val descriptionText = applicationContext.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val WORK_NAME = "MorningMealCheck"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate delay until 8:30 AM
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 8)
                set(java.util.Calendar.MINUTE, 30)
                set(java.util.Calendar.SECOND, 0)
            }
            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<MealNotificationWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun triggerNow(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MealNotificationWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
