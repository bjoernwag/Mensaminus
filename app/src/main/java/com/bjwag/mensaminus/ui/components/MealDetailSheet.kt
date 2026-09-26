package com.bjwag.mensaminus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bjwag.mensaminus.ui.theme.MatchFire
import com.bjwag.mensaminus.ui.theme.SuccessGreen
import com.bjwag.mensaminus.model.*
import java.util.Locale
import java.text.NumberFormat

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bjwag.mensaminus.R

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun MealDetailSheet(
    mealItem: MealItem,
    settings: com.bjwag.mensaminus.store.UserSettings,
    allMeals: List<MealItem>,
    isLiked: Boolean,
    score: Double = 0.0,
    isDisliked: Boolean,
    onLikeToggle: () -> Unit,
    onDislikeToggle: () -> Unit,
    onCanteenClick: () -> Unit,
    onFullScreenImage: (String) -> Unit
) {
    val meal = mealItem.meal
    val price = meal.getPriceForGroup(settings.priceGroup)
    val otherMeals = allMeals.filter { it.canteen.id == mealItem.canteen.id && it.meal.id != meal.id }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        if (meal.imageUrl != null) {
            AsyncImage(
                model = meal.imageUrl, contentDescription = meal.name,
                contentScale = ContentScale.Crop, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable(
                        role = Role.Image,
                        onClickLabel = stringResource(R.string.nav_meals)
                    ) { onFullScreenImage(meal.imageUrl!!) }
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), 
                shape = RoundedCornerShape(12.dp), 
                modifier = Modifier.clickable(role = Role.Button) { onCanteenClick() }
            ) {
                Text(
                    text = stringResource(R.string.canteen_location, mealItem.canteen.name), 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                if (score != 0.0) {
                    val (matchText, matchIcon, matchColor) = when {
                        score >= 800.0 -> Triple(stringResource(R.string.top_match), "🔥", MatchFire)
                        score >= 50.0 -> Triple(
                            stringResource(R.string.good_match),
                            "✨",
                            SuccessGreen
                        )
                        score < -500.0 -> Triple(
                            stringResource(R.string.bad_match),
                            "🚫",
                            MaterialTheme.colorScheme.error
                        )

                        score < -5.5 -> Triple(
                            stringResource(R.string.low_match),
                            "🤔",
                            MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        else -> Triple(stringResource(R.string.neutral_match), "🍽️", MaterialTheme.colorScheme.secondary)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = matchColor.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, matchColor.copy(alpha = 0.2f)),
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = if (settings.showScores) "$matchText: ${"%.2f".format(score)}" else matchText
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(matchIcon, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clearAndSetSemantics {  })
                            Text(
                                text = if (settings.showScores) "$matchText (${"%.2f".format(score)})" else matchText,
                                style = MaterialTheme.typography.labelLarge,
                                color = matchColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Row {
                    FilledTonalIconButton(
                        onClick = onLikeToggle,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isLiked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, contentDescription = stringResource(R.string.like_meal))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick = onDislikeToggle,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isDisliked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown, contentDescription = stringResource(R.string.dislike_meal))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = meal.mainName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            if (meal.sideDishes.isNotEmpty()) {
                Text(
                    text = meal.sideDishes, 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.category_label, meal.category), 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = if (meal.isSoldOut) stringResource(R.string.sold_out) else if (price != null) currencyFormatter.format(price) else "?",
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = if (meal.isSoldOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            DietaryBadges(meal = meal, isCompact = false)

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.extra_info), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (meal.co2Content != null || meal.isKlimaTeller) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌍", fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (meal.isKlimaTeller) "KlimaTeller" else stringResource(R.string.co2_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            if (meal.co2Content != null) {
                                Text(
                                    text = meal.co2Content!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                meal.notes?.filter { !it.contains("KlimaTeller", ignoreCase = true) }?.forEach { note ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = note,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (otherMeals.isNotEmpty()) {
                Text(stringResource(R.string.other_meals_in, mealItem.canteen.name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(otherMeals) { other ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(140.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(other.meal.mainName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val otherPrice = other.meal.getPriceForGroup(settings.priceGroup)
                                Text(if (otherPrice != null) currencyFormatter.format(otherPrice) else "?", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
