package com.bjwag.mensaminus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.bjwag.mensaminus.ui.MainAppScreen
import com.bjwag.mensaminus.ui.components.FullScreenImageDialog
import com.bjwag.mensaminus.ui.components.SplashContinuation
import com.bjwag.mensaminus.ui.theme.MensaminusTheme
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.MealsViewModel
import com.bjwag.mensaminus.viewmodel.CanteensViewModel
import com.bjwag.mensaminus.viewmodel.SettingsViewModel
import com.bjwag.mensaminus.viewmodel.ViewModelFactory
import android.Manifest
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import com.bjwag.mensaminus.worker.MealNotificationWorker
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import android.provider.Settings
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val factory by lazy { ViewModelFactory(application, mainViewModel) }
    
    private val mealsViewModel: MealsViewModel by viewModels { factory }
    private val canteensViewModel: CanteensViewModel by viewModels { factory }
    private val settingsViewModel: SettingsViewModel by viewModels { factory }

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    private fun openAppNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            MealNotificationWorker.schedule(this)
        } else {
            mainViewModel.setMorningMatchNotification(false)
            Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    openAppNotificationSettings()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )

        setContent {
            val settings by mainViewModel.userSettings.collectAsState()
            var showSplashContinuation by remember { mutableStateOf(true) }

            LaunchedEffect(settings.language) {
                val localeTag = when (settings.language) {
                    com.bjwag.mensaminus.store.AppLanguage.GERMAN -> "de"
                    com.bjwag.mensaminus.store.AppLanguage.ENGLISH -> "en"
                }
                
                val appLocales = if (localeTag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(localeTag)
                }
                
                if (AppCompatDelegate.getApplicationLocales() != appLocales) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            // Handle worker scheduling and permission
            LaunchedEffect(settings.morningMatchNotification) {
                if (settings.morningMatchNotification) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                            if (!notificationsEnabled) {
                                mainViewModel.setMorningMatchNotification(false)
                                Toast.makeText(this@MainActivity, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
                                openAppNotificationSettings()
                            } else {
                                MealNotificationWorker.schedule(this@MainActivity)
                            }
                        }
                    } else {
                        val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(this@MainActivity).areNotificationsEnabled()
                        if (!notificationsEnabled) {
                            mainViewModel.setMorningMatchNotification(false)
                            Toast.makeText(this@MainActivity, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
                            openAppNotificationSettings()
                        } else {
                            MealNotificationWorker.schedule(this@MainActivity)
                        }
                    }
                } else {
                    MealNotificationWorker.cancel(this@MainActivity)
                }
            }

            MensaminusTheme {
                // Monitor NFC setting to re-enable/disable dispatch
                LaunchedEffect(settings.nfcReaderEnabled) {
                    if (settings.nfcReaderEnabled) {
                        enableNfcForegroundDispatch()
                    } else {
                        disableNfcForegroundDispatch()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!showSplashContinuation) {
                        MainAppScreen(
                            mainViewModel = mainViewModel,
                            mealsViewModel = mealsViewModel,
                            canteensViewModel = canteensViewModel,
                            settingsViewModel = settingsViewModel
                        )

                        val fullScreenImageUrl = mainViewModel.fullScreenImageUrl
                        FullScreenImageDialog(
                            imageUrl = fullScreenImageUrl,
                            onDismiss = { mainViewModel.setFullScreenImage(null) }
                        )
                    }

                    if (showSplashContinuation) {
                        // show a separate continuation to bridge the gap after the system splash
                        SplashContinuation(onFinished = { showSplashContinuation = false })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        val settings = mainViewModel.userSettings.value
        if (settings.morningMatchNotification) {
            val notificationsEnabled = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()
            val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (!notificationsEnabled || !permissionGranted) {
                mainViewModel.setMorningMatchNotification(false)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        try {
            val settings = mainViewModel.userSettings.value
            android.util.Log.d("MainActivity", "Enabling NFC Dispatch. Settings: ${settings.nfcReaderEnabled}, Adapter: ${nfcAdapter != null}")
            if (settings.nfcReaderEnabled) {
                nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to enable NFC dispatch", e)
        }
    }

    private fun disableNfcForegroundDispatch() {
        try {
            android.util.Log.d("MainActivity", "Disabling NFC Dispatch")
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "onNewIntent: ${intent.action}")
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            android.util.Log.d("MainActivity", "Tag extra: ${tag != null}")
            tag?.let { mainViewModel.processNfcTag(it) }
        }
    }
}
