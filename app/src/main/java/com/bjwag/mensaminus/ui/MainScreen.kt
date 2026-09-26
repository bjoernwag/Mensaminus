package com.bjwag.mensaminus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bjwag.mensaminus.R
import com.bjwag.mensaminus.ui.components.CampusCardBalanceCard
import com.bjwag.mensaminus.ui.components.FullScreenImageDialog
import com.bjwag.mensaminus.ui.components.SplashContinuation
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.MealsViewModel
import com.bjwag.mensaminus.viewmodel.CanteensViewModel
import com.bjwag.mensaminus.viewmodel.SettingsViewModel
import java.time.LocalDate
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import android.view.animation.OvershootInterpolator
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel,
    mealsViewModel: MealsViewModel,
    canteensViewModel: CanteensViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val selectedDate by mainViewModel.selectedDate.collectAsState()
    val settings by mainViewModel.userSettings.collectAsState()
    val isSearchActive by mealsViewModel.isSearchActive.collectAsState()
    val searchQuery by mealsViewModel.searchQuery.collectAsState()
    
    var showNfcInfoDialog by remember { mutableStateOf(false) }
    
    val showNfcBalanceDialog = mainViewModel.showNfcBalanceDialog

    LaunchedEffect(settings.showOnboarding) {
        if (!settings.showOnboarding) {
            mealsViewModel.refreshCurrentDate()
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (settings.showOnboarding) {
        OnboardingScreen(
            mainViewModel = mainViewModel,
            canteensViewModel = canteensViewModel,
            settingsViewModel = settingsViewModel,
            onFinished = { mainViewModel.completeOnboarding() }
        )
    } else {
        val isDetailScreen = currentRoute?.startsWith("canteen/") == true
        val isMealsScreen = currentRoute == Screen.Meals.route

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                if (!isDetailScreen) {
                    if (isMealsScreen && isSearchActive) {
                        TopAppBar(
                            title = {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { mealsViewModel.setSearchQuery(it) },
                                    placeholder = { Text(stringResource(R.string.search)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { mealsViewModel.setSearchActive(false) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            actions = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { mealsViewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            }
                        )
                    } else {
                        LargeTopAppBar(
                            title = {
                                val dateText = mealsViewModel.getFormattedDate(selectedDate, settings.language) ?: when {
                                    selectedDate == LocalDate.now() -> stringResource(R.string.date_today)
                                    selectedDate == LocalDate.now().plusDays(1) -> stringResource(R.string.date_tomorrow)
                                    else -> ""
                                }
                                Column {
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            },
                            navigationIcon = {
                                FilledIconButton(
                                    onClick = { mainViewModel.changeDate(-1) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.prev_day), modifier = Modifier.size(18.dp))
                                }
                            },
                            actions = {
                                if (isMealsScreen) {
                                    IconButton(onClick = { showNfcInfoDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard, 
                                            contentDescription = stringResource(R.string.nfc_campus_card),
                                            tint = if (settings.nfcReaderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { mealsViewModel.setSearchActive(true) }) {
                                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                                    }
                                }
                                FilledIconButton(
                                    onClick = { mainViewModel.changeDate(1) },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = stringResource(R.string.next_day), modifier = Modifier.size(18.dp))
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                }
            },
            bottomBar = {
                if (!isDetailScreen) {
                    val items = listOf(Screen.Meals, Screen.Canteens, Screen.Settings)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { screen ->
                            val title = when (screen) {
                                Screen.Meals -> stringResource(R.string.nav_meals)
                                Screen.Canteens -> stringResource(R.string.nav_canteens)
                                Screen.Settings -> stringResource(R.string.nav_settings)
                                else -> ""
                            }
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = title) },
                                label = { Text(title, style = MaterialTheme.typography.labelMedium) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) {
                                        if (screen == Screen.Meals || screen == Screen.Canteens) {
                                            mainViewModel.resetDateToToday()
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Meals.route,
                modifier = Modifier.padding(if (isDetailScreen) PaddingValues(0.dp) else innerPadding)
            ) {
                composable(Screen.Meals.route) {
                    MealsScreen(
                        mainViewModel = mainViewModel,
                        mealsViewModel = mealsViewModel, 
                        onNavigateToDetail = { id ->
                            navController.navigate(Screen.CanteenDetail.createRoute(id))
                        }
                    )
                }
                composable(Screen.Canteens.route) {
                    CanteensScreen(
                        mainViewModel = mainViewModel,
                        canteensViewModel = canteensViewModel,
                        mealsViewModel = mealsViewModel,
                        onNavigateToDetail = { id ->
                            navController.navigate(Screen.CanteenDetail.createRoute(id))
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(settingsViewModel = settingsViewModel, mainViewModel = mainViewModel)
                }
                composable(
                    route = Screen.CanteenDetail.route,
                    arguments = listOf(navArgument("canteenId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val canteenId = backStackEntry.arguments?.getInt("canteenId") ?: 0
                    val allCanteens by mainViewModel.allCanteens.collectAsState()
                    val canteen = allCanteens.find { it.id == canteenId }
                    val allMeals by mealsViewModel.rawMeals.collectAsState()
                    val meals = allMeals.filter { it.canteen.id == canteenId }

                    if (canteen != null) {
                        CanteenDetailScreen(
                            canteen = canteen,
                            mainViewModel = mainViewModel,
                            canteensViewModel = canteensViewModel,
                            mealsViewModel = mealsViewModel,
                            allMeals = meals,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (showNfcInfoDialog) {
        AlertDialog(
            onDismissRequest = { showNfcInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.nfc_campus_card))
                }
            },
            text = {
                Column {
                    val nfcSupported = mainViewModel.isNfcSupported
                    val nfcEnabled = mainViewModel.isNfcEnabled
                    
                    Text(
                        text = when {
                            !nfcSupported -> stringResource(R.string.nfc_not_supported)
                            !nfcEnabled -> stringResource(R.string.nfc_disabled_hint)
                            !settings.nfcReaderEnabled -> stringResource(R.string.nfc_enable_reader_desc)
                            else -> stringResource(R.string.nfc_tap_hint)
                        }
                    )
                    
                    if (nfcSupported && nfcEnabled && !settings.nfcReaderEnabled) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                mainViewModel.setNfcReaderEnabled(true)
                                showNfcInfoDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.nfc_enable_reader))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNfcInfoDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showNfcBalanceDialog) {
        Dialog(
            onDismissRequest = { mainViewModel.dismissNfcBalanceDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var animateIn by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { animateIn = true }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { mainViewModel.dismissNfcBalanceDialog() }
                        .background(Color.Black.copy(alpha = 0.6f))
                )

                AnimatedVisibility(
                    visible = animateIn,
                    enter = fadeIn(tween(400)) + scaleIn(tween(500, easing = OvershootInterpolator(1.2f).toEasing()), initialScale = 0.8f) + slideInVertically(tween(500)) { it / 2 },
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CampusCardBalanceCard(
                            balance = settings.lastScannedBalance,
                            timestamp = settings.lastScanTimestamp,
                            onClear = null
                        )
                        Spacer(Modifier.height(32.dp))
                        
                        IconButton(
                            onClick = { mainViewModel.dismissNfcBalanceDialog() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
