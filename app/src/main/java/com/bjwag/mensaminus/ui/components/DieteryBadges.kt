package com.bjwag.mensaminus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.bjwag.mensaminus.R
import com.bjwag.mensaminus.model.Meal

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun DietaryBadges(meal: Meal, isCompact: Boolean = false, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (meal.isVegan) {
            DietBadge(emoji = "\uD83C\uDF31", label = stringResource(R.string.badge_vegan), isCompact = isCompact)
        } else if (meal.isVeggie) {
            DietBadge(emoji = "\uD83E\uDD5B", label = stringResource(R.string.badge_vegetarian), isCompact = isCompact)
        }

        if (meal.isBeef) DietBadge(emoji = "\uD83D\uDC04", label = stringResource(R.string.badge_beef), isCompact = isCompact)
        if (meal.isPork) DietBadge(emoji = "\uD83D\uDC16", label = stringResource(R.string.badge_pork), isCompact = isCompact)
        if (meal.isPoultry) DietBadge(emoji = "\uD83D\uDC14", label = stringResource(R.string.badge_poultry), isCompact = isCompact)
        if (meal.isFish) DietBadge(emoji = "\uD83D\uDC1F", label = stringResource(R.string.badge_fish), isCompact = isCompact)
    }
}

@Composable
private fun DietBadge(
    emoji: String, 
    label: String, 
    isCompact: Boolean, 
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
) {
    Surface(
        shape = RoundedCornerShape(if (isCompact) 8.dp else 16.dp),
        color = color,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = label
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 6.dp else 10.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.labelMedium)

            if (!isCompact) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
