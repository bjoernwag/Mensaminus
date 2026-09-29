package com.bjwag.mensaminus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.bjwag.mensaminus.model.*
import com.bjwag.mensaminus.ui.components.EmptyState
import com.bjwag.mensaminus.ui.components.OpenStatusText
import com.bjwag.mensaminus.utils.OpeningHoursHelper
import java.time.LocalDate

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bjwag.mensaminus.R
import androidx.compose.ui.draw.alpha
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.MealsViewModel
import java.text.NumberFormat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    mainViewModel: MainViewModel,
    mealsViewModel: MealsViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val uiState by mealsViewModel.uiState.collectAsState()
    val settings by mainViewModel.userSettings.collectAsState()
    val selectedDate by mainViewModel.selectedDate.collectAsState()

    var selectedMeal by remember { mutableStateOf<MealItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isRefreshing by mealsViewModel.isRefreshing.collectAsState()
    val lastUpdated by mealsViewModel.lastUpdated.collectAsState()
    val isOffline by mealsViewModel.isOffline.collectAsState()

    val isSearchActive by mealsViewModel.isSearchActive.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            mealsViewModel.refreshCurrentDate()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Cache/Offline Hint
            AnimatedVisibility(
                visible = isOffline || (lastUpdated != null && (System.currentTimeMillis() - lastUpdated!!) / 60000 > 10),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val now = System.currentTimeMillis()
                val minutesAgo = if (lastUpdated != null) (now - lastUpdated!!) / 60000 else 0

                val containerColor: Color
                val textColor: Color
                val text: String
                val icon: ImageVector

                if (isOffline) {
                    val dateFormatted = if (lastUpdated != null) {
                        java.time.Instant.ofEpochMilli(lastUpdated!!)
                            .atZone(java.time.ZoneId.systemDefault())
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } else "?"
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    textColor = MaterialTheme.colorScheme.onErrorContainer
                    text = stringResource(R.string.cache_offline_hint, dateFormatted)
                    icon = Icons.Default.CloudOff
                } else {
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer
                    text = stringResource(R.string.cache_old_hint, minutesAgo)
                    icon = Icons.Default.History
                }

                Surface(
                    color = containerColor,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when (val state = uiState) {
                    is UiState.Loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    is UiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(state.messageResId),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { mealsViewModel.retry() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }

                    is UiState.Success -> {
                        if (state.meals.isEmpty() && state.matchingMeals.isEmpty() && state.otherMatchingMeals.isEmpty()) {
                            EmptyState(stringResource(R.string.empty_meals))
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val showHint = "meals" !in settings.dismissedHints && !isSearchActive
                                if (showHint) {
                                    item(key = "hint") {
                                        com.bjwag.mensaminus.ui.components.FeatureHintCard(
                                            title = stringResource(R.string.tip_meals_title),
                                            description = stringResource(R.string.tip_meals_desc),
                                            onDismiss = { mainViewModel.dismissCoachMark("meals") },
                                            isVisible = true
                                        )
                                    }
                                }

                                val isAfterThreshold = java.time.LocalTime.now().isAfter(java.time.LocalTime.of(16, 0))
                                val isToday = selectedDate == LocalDate.now()

                                if (isSearchActive) {
                                    if (state.matchingMeals.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.search_fits_filters),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        items(state.matchingMeals, key = { "search-fit-${it.canteen.id}-${it.meal.id}" }) { mealItem ->
                                            CompactMealCard(
                                                mealItem = mealItem,
                                                settings = settings,
                                                isLiked = settings.likedMeals.contains(mealItem.meal.mainName),
                                                selectedDate = selectedDate,
                                                onClick = { selectedMeal = mealItem },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }

                                    if (state.otherMatchingMeals.isNotEmpty()) {
                                        item {
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.search_other_results),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        items(state.otherMatchingMeals, key = { "search-other-${it.canteen.id}-${it.meal.id}" }) { mealItem ->
                                            CompactMealCard(
                                                mealItem = mealItem,
                                                settings = settings,
                                                isLiked = settings.likedMeals.contains(mealItem.meal.mainName),
                                                selectedDate = selectedDate,
                                                onClick = { selectedMeal = mealItem },
                                                modifier = Modifier.animateItem(),
                                                alpha = 0.5f
                                            )
                                        }
                                    }
                                } else {
                                    val (normalMeals, laterMeals) = if (isToday && settings.hideEveningMealsBefore16 && !isAfterThreshold) {
                                        state.meals.partition { !it.meal.isEveningMeal }
                                    } else {
                                        state.meals to emptyList()
                                    }

                                    items(normalMeals, key = { "${it.canteen.id}-${it.meal.id}" }) { mealItem ->
                                        CompactMealCard(
                                            mealItem = mealItem,
                                            settings = settings,
                                            isLiked = settings.likedMeals.contains(mealItem.meal.mainName),
                                            selectedDate = selectedDate,
                                            onClick = { selectedMeal = mealItem },
                                            modifier = Modifier.animateItem()
                                        )
                                    }

                                    if (laterMeals.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = stringResource(R.string.evening_meals_later),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                        items(laterMeals, key = { "${it.canteen.id}-${it.meal.id}" }) { mealItem ->
                                            CompactMealCard(
                                                mealItem = mealItem,
                                                settings = settings,
                                                isLiked = settings.likedMeals.contains(mealItem.meal.mainName),
                                                selectedDate = selectedDate,
                                                onClick = { selectedMeal = mealItem },
                                                modifier = Modifier.animateItem(),
                                                alpha = 0.6f
                                            )
                                        }
                                    }
                                }

                                item { Spacer(Modifier.height(32.dp)) }
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
                                    allMeals = state.meals,
                                    isLiked = settings.likedMeals.contains(selectedMeal!!.meal.mainName),
                                    isDisliked = settings.dislikedMeals.contains(selectedMeal!!.meal.mainName),
                                    score = selectedMeal!!.score,
                                    onLikeToggle = { mealsViewModel.toggleLikeMeal(selectedMeal!!.meal.mainName) },
                                    onDislikeToggle = { mealsViewModel.toggleDislikeMeal(selectedMeal!!.meal.mainName) },
                                    onCanteenClick = {
                                        val canteenId = selectedMeal!!.canteen.id
                                        selectedMeal = null
                                        onNavigateToDetail(canteenId)
                                    },
                                    onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactMealCard(
    mealItem: MealItem,
    settings: com.bjwag.mensaminus.store.UserSettings,
    isLiked: Boolean,
    selectedDate: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f
) {
    val meal = mealItem.meal
    val price = meal.getPriceForGroup(settings.priceGroup)
    val context = androidx.compose.ui.platform.LocalContext.current
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)

    val status = OpeningHoursHelper.getStatus(context, mealItem.canteen.id, selectedDate, meal.category)
    val priceText = if (price != null) currencyFormatter.format(price) else stringResource(R.string.price_not_available)

    val score = mealItem.score

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(role = Role.Button) { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = "${mealItem.canteen.name}, ${meal.category}: ${meal.mainName} ${meal.sideDishes}. " +
                        (if (meal.isSoldOut) "Ausverkauft. " else "$priceText. ") +
                        (status?.text ?: "")
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (score > 500) BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null
    ) {
        Row(modifier = Modifier.height(110.dp)) {
            Box(modifier = Modifier.width(110.dp)) {
                if (meal.imageUrl != null) {
                    AsyncImage(
                        model = meal.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍽️", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Row(modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                    if (score > 800) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
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
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val canteenAndCategory = if (meal.category.isNotBlank()) "${mealItem.canteen.name} · ${meal.category}" else mealItem.canteen.name
                        Text(
                            text = canteenAndCategory,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (status != null) {
                            OpenStatusText(status, modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Text(
                        text = meal.mainName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        if (meal.isSoldOut) {
                            Text(
                                text = stringResource(R.string.sold_out),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = priceText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
