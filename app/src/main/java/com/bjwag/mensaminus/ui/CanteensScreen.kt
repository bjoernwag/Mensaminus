package com.bjwag.mensaminus.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.bjwag.mensaminus.model.*
import com.bjwag.mensaminus.store.DietaryPreference
import com.bjwag.mensaminus.ui.components.OpenStatusText
import com.bjwag.mensaminus.utils.OpeningHoursHelper
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.CanteensViewModel
import com.bjwag.mensaminus.viewmodel.MealsViewModel
import java.text.NumberFormat
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bjwag.mensaminus.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CanteensScreen(
    mainViewModel: MainViewModel,
    canteensViewModel: CanteensViewModel,
    mealsViewModel: MealsViewModel,
    onNavigateToDetail: (Int) -> Unit
) {
    val allCanteens by mainViewModel.allCanteens.collectAsState()
    val settings by mainViewModel.userSettings.collectAsState()
    val uiState by mealsViewModel.uiState.collectAsState()
    val selectedDate by mainViewModel.selectedDate.collectAsState()

    var selectedMealForSheet by remember { mutableStateOf<MealItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeIds = settings.activeCanteens
    val activeCanteens = activeIds.mapNotNull { id -> allCanteens.find { it.id == id } }
    val inactiveCanteens = allCanteens.filter { it.id !in activeIds }.sortedBy { it.name }

    val allMeals = if (uiState is UiState.Success) (uiState as UiState.Success).meals else emptyList()

    var draggedCanteenId by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(java.util.Locale.GERMANY) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item {
            com.bjwag.mensaminus.ui.components.FeatureHintCard(
                title = stringResource(R.string.tip_canteens_title),
                description = stringResource(R.string.tip_canteens_desc),
                onDismiss = { mainViewModel.dismissCoachMark("canteens") },
                isVisible = "canteens" !in settings.dismissedHints,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.active_canteens), 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.ExtraBold, 
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
        }

        itemsIndexed(activeCanteens, key = { _, c -> c.id }) { index, canteen ->
            val canteenMeals = allMeals.filter { item ->
                val meal = item.meal
                val passDietary = when (settings.dietaryPreference) {
                    DietaryPreference.ANY -> true
                    DietaryPreference.VEGETARIAN -> meal.isVeggie || meal.isVegan
                    DietaryPreference.VEGAN -> meal.isVegan
                }
                val passSoldOut = if (settings.hideSoldOut) !meal.isSoldOut else true
                item.canteen.id == canteen.id && passDietary && passSoldOut
            }

            val isDragged = draggedCanteenId == canteen.id
            val offsetDp by animateDpAsState(
                targetValue = if (isDragged) (dragOffsetY / LocalDensity.current.density).dp else 0.dp,
                label = "dragOffset"
            )

            var cardHeightPx by remember { mutableFloatStateOf(0f) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .zIndex(if (isDragged) 1f else 0f)
                    .offset(y = offsetDp)
                    .onGloballyPositioned { cardHeightPx = it.size.height.toFloat() }
                    .animateItem(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDragged) 8.dp else 2.dp
                ),
                border = if (isDragged) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(canteen.id) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            draggedCanteenId = canteen.id
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggedCanteenId = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedCanteenId = null
                                            dragOffsetY = 0f
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount
                                        
                                        val currentIndex = activeCanteens.indexOfFirst { it.id == draggedCanteenId }
                                        
                                        if (currentIndex != -1 && cardHeightPx > 0) {
                                            if (dragOffsetY > cardHeightPx * 0.6f && currentIndex < activeCanteens.size - 1) {
                                                canteensViewModel.moveCanteen(canteen.id, currentIndex + 1)
                                                dragOffsetY -= cardHeightPx
                                            } else if (dragOffsetY < -cardHeightPx * 0.6f && currentIndex > 0) {
                                                canteensViewModel.moveCanteen(canteen.id, currentIndex - 1)
                                                dragOffsetY += cardHeightPx
                                            }
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = stringResource(R.string.drag_indicator),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = canteen.name,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(role = Role.Button) { onNavigateToDetail(canteen.id) }
                        )
                        val status = OpeningHoursHelper.getStatus(LocalContext.current, canteen.id, selectedDate)
                        if (status != null) {
                            OpenStatusText(status, modifier = Modifier.padding(end = 8.dp))
                        }

                        IconButton(
                            onClick = { canteensViewModel.toggleCanteenActive(canteen.id) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.deactivate), modifier = Modifier.size(18.dp))
                        }
                    }

                    if (canteenMeals.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_meals), 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(canteenMeals) { mealItem ->
                                val meal = mealItem.meal
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(140.dp)
                                        .clickable(role = Role.Button) { selectedMealForSheet = mealItem },
                                    border = if (settings.likedMeals.contains(meal.mainName))
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        else null
                                ) {
                                    Column(modifier = Modifier.semantics(mergeDescendants = true) {
                                        val price = meal.getPriceForGroup(settings.priceGroup)
                                        contentDescription = "${meal.mainName}, ${meal.category}, " +
                                                (if (price != null) currencyFormatter.format(price) else "?€")
                                    }) {
                                        if (meal.imageUrl != null) {
                                            AsyncImage(
                                                model = meal.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                                    .clickable(role = Role.Image) { mainViewModel.setFullScreenImage(meal.imageUrl) }
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .clearAndSetSemantics {  },
                                                contentAlignment = Alignment.Center
                                            ) { Text("🍽️") }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .clearAndSetSemantics {  }
                                        ) {
                                            Text(meal.mainName, style = MaterialTheme.typography.bodySmall, maxLines = 2, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                                            
                                            Spacer(Modifier.weight(1f))
                                            val price = meal.getPriceForGroup(settings.priceGroup)
                                            Text(
                                                text = if (price != null) currencyFormatter.format(price) else stringResource(R.string.price_not_available),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.inactive_canteens), 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        item {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                inactiveCanteens.forEach { canteen ->
                    FilterChip(
                        selected = false,
                        onClick = { canteensViewModel.toggleCanteenActive(canteen.id) },
                        label = { Text(canteen.name) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    if (selectedMealForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMealForSheet = null },
            sheetState = sheetState
        ) {
            com.bjwag.mensaminus.ui.components.MealDetailSheet(
                mealItem = selectedMealForSheet!!,
                settings = settings,
                allMeals = allMeals,
                isLiked = settings.likedMeals.contains(selectedMealForSheet!!.meal.mainName),
                isDisliked = settings.dislikedMeals.contains(selectedMealForSheet!!.meal.mainName),
                score = selectedMealForSheet!!.score,
                onLikeToggle = { mealsViewModel.toggleLikeMeal(selectedMealForSheet!!.meal.mainName) },
                onDislikeToggle = { mealsViewModel.toggleDislikeMeal(selectedMealForSheet!!.meal.mainName) },
                onCanteenClick = {},
                onFullScreenImage = { mainViewModel.setFullScreenImage(it) }
            )
        }
    }
}
