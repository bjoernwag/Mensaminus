package com.bjwag.mensaminus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bjwag.mensaminus.model.*
import com.bjwag.mensaminus.ui.components.TagChip
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.CanteensViewModel
import com.bjwag.mensaminus.viewmodel.MealsViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bjwag.mensaminus.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import com.bjwag.mensaminus.ui.theme.MatchFire
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanteenDetailScreen(
    canteen: Canteen,
    mainViewModel: MainViewModel,
    canteensViewModel: CanteensViewModel,
    mealsViewModel: MealsViewModel,
    allMeals: List<MealItem>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val loadingText = stringResource(R.string.loading_hours)
    val noHoursText = stringResource(R.string.no_hours_found)
    val noWebsiteText = stringResource(R.string.no_website_found)
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(java.util.Locale.GERMANY) }

    var openingHours by remember { mutableStateOf<String?>(loadingText) }
    var selectedMeal by remember { mutableStateOf<MealItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val settings by mainViewModel.userSettings.collectAsState()
    val selectedDate by mainViewModel.selectedDate.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val (passingMeals, failingMeals) = remember(allMeals, settings, selectedDate) {
        allMeals.partition { item ->
            val meal = item.meal
            val passDietary = when (settings.dietaryPreference) {
                com.bjwag.mensaminus.store.DietaryPreference.ANY -> true
                com.bjwag.mensaminus.store.DietaryPreference.VEGETARIAN -> meal.isVeggie || meal.isVegan
                com.bjwag.mensaminus.store.DietaryPreference.VEGAN -> meal.isVegan
            }
            
            val passAllergies = if (settings.excludedAllergies.isNotEmpty()) {
                val notes = meal.notes ?: emptyList()
                settings.excludedAllergies.none { allergyName ->
                    com.bjwag.mensaminus.model.MealAllergy.fromName(allergyName)?.isMatch(notes) == true
                }
            } else true

            val passSoldOut = if (settings.hideSoldOut) !meal.isSoldOut else true

            val isToday = selectedDate == java.time.LocalDate.now()
            val isAfterEveningThreshold = java.time.LocalTime.now().isAfter(java.time.LocalTime.of(16, 0))
            val passEveningFilter = if (settings.hideEveningMealsBefore16 && isToday && !isAfterEveningThreshold) {
                !meal.isEveningMeal
            } else true

            passDietary && passAllergies && passSoldOut && passEveningFilter
        }
    }

    val eveningMeals = passingMeals.filter { it.meal.category.contains("Abend", ignoreCase = true) }
    val lunchMeals = passingMeals.filter { !it.meal.category.contains("Abend", ignoreCase = true) }

    val failingMealsSorted = failingMeals.sortedByDescending { it.meal.category.contains("Abend", ignoreCase = true) }

    LaunchedEffect(canteen.url) {
        if (!canteen.url.isNullOrEmpty()) {
            val hours = canteensViewModel.getOpeningHours(canteen.url)
            openingHours = hours ?: noHoursText
        } else {
            openingHours = noWebsiteText
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(canteen.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (!canteen.url.isNullOrEmpty()) {
                        IconButton(onClick = {
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, Uri.parse(canteen.url))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Website"
                            )
                        }
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
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.opening_hours_label), 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val rawHours = openingHours ?: ""
                        if (rawHours == loadingText || rawHours == noHoursText || rawHours == noWebsiteText) {
                            Text(text = rawHours, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            rawHours.split("\n").filter { it.isNotBlank() }.forEach { line ->
                                val parts = line.split("\t\t")
                                if (parts.size >= 2) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                        Text(
                                            text = parts[0].trim(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.alpha(0.8f)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = parts[1].trim().replace(" | ", "\n"),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = line.trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (lunchMeals.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lunch_offer), 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold, 
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(lunchMeals) { mealItem ->
                    DetailMealRow(
                        mealItem = mealItem, 
                        settings = settings, 
                        isFilteredOut = false,
                        onClick = { selectedMeal = mealItem },
                        currencyFormatter = currencyFormatter,
                        onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
                    )
                }
            }

            if (eveningMeals.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.evening_offer), 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TagChip(
                            text = stringResource(R.string.late_chip), 
                            color = MaterialTheme.colorScheme.tertiaryContainer, 
                            textColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                items(eveningMeals) { mealItem ->
                    DetailMealRow(
                        mealItem = mealItem, 
                        settings = settings, 
                        isFilteredOut = false,
                        onClick = { selectedMeal = mealItem },
                        currencyFormatter = currencyFormatter,
                        onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
                    )
                }
            }

            if (failingMealsSorted.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.filtered_out_meals),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                items(failingMealsSorted) { mealItem ->
                    DetailMealRow(
                        mealItem = mealItem, 
                        settings = settings, 
                        isFilteredOut = true,
                        onClick = { selectedMeal = mealItem },
                        currencyFormatter = currencyFormatter,
                        onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
                    )
                }
            }

            if (allMeals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_meals_day), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (selectedMeal != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedMeal = null },
                sheetState = sheetState
            ) {
            com.bjwag.mensaminus.ui.components.MealDetailSheet(
                mealItem = selectedMeal!!,
                settings = settings,
                allMeals = allMeals,
                isLiked = settings.likedMeals.contains(selectedMeal!!.meal.mainName),
                isDisliked = settings.dislikedMeals.contains(selectedMeal!!.meal.mainName),
                score = selectedMeal!!.score,
                onLikeToggle = { mealsViewModel.toggleLikeMeal(selectedMeal!!.meal.mainName) },
                onDislikeToggle = { mealsViewModel.toggleDislikeMeal(selectedMeal!!.meal.mainName) },
                onCanteenClick = { selectedMeal = null },
                onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
            )
            }
        }
    }
}

@Composable
fun DetailMealRow(
    mealItem: MealItem, 
    settings: com.bjwag.mensaminus.store.UserSettings, 
    isFilteredOut: Boolean,
    onClick: () -> Unit,
    currencyFormatter: NumberFormat,
    onFullScreenImage: (String) -> Unit
) {
    val meal = mealItem.meal
    val price = meal.getPriceForGroup(settings.priceGroup)
    val score = mealItem.score

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFilteredOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = if (isFilteredOut) CardDefaults.cardElevation(defaultElevation = 0.dp) 
                    else CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .alpha(if (isFilteredOut) 0.5f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (meal.imageUrl != null) {
                Box {
                    AsyncImage(
                        model = meal.imageUrl,
                        contentDescription = meal.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onFullScreenImage(meal.imageUrl!!) }
                    )
                    if (score > 800) {
                        Surface(
                            color = MatchFire,
                            modifier = Modifier
                                .padding(4.dp)
                                .align(Alignment.TopStart),
                            shape = RoundedCornerShape(6.dp),
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                text = "🔥",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.mainName, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (isFilteredOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                if (meal.sideDishes.isNotEmpty()) {
                    Text(
                        text = meal.sideDishes, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (meal.isSoldOut) {
                        Text(stringResource(R.string.sold_out), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    } else {
                        Text(
                            text = if (price != null) currencyFormatter.format(price) else stringResource(R.string.price_not_available),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFilteredOut) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                        )
                    }

                    com.bjwag.mensaminus.ui.components.DietaryBadges(
                        meal = meal,
                        isCompact = true
                    )
                }
            }
        }
    }
}
