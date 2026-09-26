package com.bjwag.mensaminus.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bjwag.mensaminus.store.PriceGroup
import com.bjwag.mensaminus.store.DietaryPreference
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Coffee
import com.bjwag.mensaminus.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, mainViewModel: MainViewModel) {
    val settings by mainViewModel.userSettings.collectAsState()
    val context = LocalContext.current
    var showDatabaseDialog by remember { mutableStateOf(false) }
    var devModeClickCount by remember { mutableIntStateOf(0) }

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showPriceSheet by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var allergiesExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            com.bjwag.mensaminus.ui.components.FeatureHintCard(
                title = stringResource(R.string.tip_settings_title),
                description = stringResource(R.string.tip_settings_desc),
                onDismiss = { mainViewModel.dismissCoachMark("settings") },
                isVisible = "settings" !in settings.dismissedHints,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
            )
        }

        item {
            SettingsGroup(title = stringResource(R.string.dietary_preference)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val options = listOf(
                        DietaryPreference.ANY to stringResource(R.string.dietary_all),
                        DietaryPreference.VEGETARIAN to stringResource(R.string.dietary_vegetarian),
                        DietaryPreference.VEGAN to stringResource(R.string.dietary_vegan)
                    )
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (pref, label) ->
                            val isSelected = settings.dietaryPreference == pref
                            val selectedColor = when (pref) {
                                DietaryPreference.ANY -> Color(0xFFFFDAD6)
                                DietaryPreference.VEGETARIAN -> Color(0xFFFFDDB3)
                                DietaryPreference.VEGAN -> Color(0xFFC2EFAD)
                            }
                            val onSelectedColor = when (pref) {
                                DietaryPreference.ANY -> Color(0xFF410002)
                                DietaryPreference.VEGETARIAN -> Color(0xFF291800)
                                DietaryPreference.VEGAN -> Color(0xFF002200)
                            }

                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { settingsViewModel.setDietaryPreference(pref) },
                                selected = isSelected,
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = selectedColor,
                                    activeContentColor = onSelectedColor
                                )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    DietaryNoticeBox(settings.dietaryPreference)
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingsSwitchItem(
                    icon = Icons.Default.Block,
                    title = stringResource(R.string.hide_sold_out),
                    description = stringResource(R.string.hide_sold_out_desc),
                    checked = settings.hideSoldOut,
                    onCheckedChange = { settingsViewModel.setHideSoldOut(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsSwitchItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.hide_evening_before_16),
                    description = stringResource(R.string.hide_evening_before_16_desc),
                    checked = settings.hideEveningMealsBefore16,
                    onCheckedChange = { settingsViewModel.setHideEveningMealsBefore16(it) }
                )
                if (settings.isDeveloperMode) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchItem(
                        icon = Icons.Default.Score,
                        title = stringResource(R.string.show_scores),
                        description = stringResource(R.string.show_scores_desc),
                        checked = settings.showScores,
                        onCheckedChange = { settingsViewModel.setShowScore(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsClickableItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.dev_reset_onboarding),
                        value = "",
                        onClick = { settingsViewModel.resetOnboarding() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsClickableItem(
                        icon = Icons.Default.NotificationsActive,
                        title = stringResource(R.string.dev_trigger_notification),
                        value = "",
                        onClick = { settingsViewModel.triggerTestNotification() }
                    )
                }
            }
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.allergies_title),
                titleClickable = { allergiesExpanded = !allergiesExpanded }
            ) {
                Column(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(horizontal = 16.dp, vertical = if (allergiesExpanded) 16.dp else 8.dp)
                ) {
                    if (!allergiesExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { allergiesExpanded = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (settings.excludedAllergies.isEmpty()) 
                                    stringResource(R.string.allergies_show_all)
                                    else stringResource(R.string.allergies_active, settings.excludedAllergies.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                        }
                    } else {
                        DisclaimerBox(modifier = Modifier.padding(bottom = 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.allergies_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { allergiesExpanded = false }) {
                                Icon(Icons.Default.ExpandLess, contentDescription = null)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        val allergies = com.bjwag.mensaminus.model.MealAllergy.entries
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allergies.forEach { allergy ->
                                val isExcluded = settings.excludedAllergies.contains(allergy.name)
                                FilterChip(
                                    selected = isExcluded,
                                    onClick = { settingsViewModel.toggleAllergy(allergy.name) },
                                    label = { Text(stringResource(allergy.displayNameResId)) },
                                    leadingIcon = if (isExcluded) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.notifications_title)) {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.daily_match_notification),
                    description = stringResource(R.string.daily_match_notification_desc),
                    checked = settings.morningMatchNotification,
                    onCheckedChange = { settingsViewModel.setMorningMatchNotification(it) }
                )
            }
        }

        item {
            SettingsGroup(
                title = stringResource(R.string.smart_recommendations),
                titleClickable = {
                    devModeClickCount++
                    if (devModeClickCount >= 5) {
                        settingsViewModel.setDeveloperMode(!settings.isDeveloperMode)
                        devModeClickCount = 0
                    }
                }
            ) {
                SettingsClickableItem(
                    icon = Icons.Default.SettingsSuggest,
                    title = stringResource(R.string.view_scoring_db),
                    value = "",
                    onClick = { showDatabaseDialog = true }
                )
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.language) + " & " + stringResource(R.string.price_profile)) {
                val langText = when (settings.language) {
                    com.bjwag.mensaminus.store.AppLanguage.GERMAN -> stringResource(R.string.language_german)
                    com.bjwag.mensaminus.store.AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                }
                SettingsClickableItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    value = langText,
                    onClick = { showLanguageSheet = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val priceText = when (settings.priceGroup) {
                    PriceGroup.STUDENT -> stringResource(R.string.price_group_studi)
                    PriceGroup.EMPLOYEE -> stringResource(R.string.price_group_employee)
                }
                SettingsClickableItem(
                    icon = Icons.Default.Payments,
                    title = stringResource(R.string.price_profile),
                    value = priceText,
                    onClick = { showPriceSheet = true }
                )
            }
        }

        item {
            SettingsGroup(title = stringResource(R.string.nfc_campus_card)) {
                val isNfcSupported = mainViewModel.isNfcSupported
                SettingsSwitchItem(
                    icon = Icons.Default.Nfc,
                    title = stringResource(R.string.nfc_enable_reader),
                    description = if (isNfcSupported) stringResource(R.string.nfc_enable_reader_desc) else stringResource(R.string.nfc_not_supported),
                    checked = settings.nfcReaderEnabled && isNfcSupported,
                    onCheckedChange = { mainViewModel.setNfcReaderEnabled(it) },
                    enabled = isNfcSupported
                )
                
                if (settings.lastScannedBalance != null && isNfcSupported) {
                    SettingsClickableItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.nfc_clear_balance),
                        value = "",
                        onClick = { mainViewModel.clearCardBalance() }
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.padding(top = 16.dp)) {
                SettingsGroup(title = stringResource(R.string.about_and_licenses)) {
                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about_and_licenses),
                        value = "",
                        onClick = { showLicensesDialog = true }
                    )
                }
            }
        }

        item {
            val versionName = try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_version, versionName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showLanguageSheet) {
        ModernSelectionSheet(
            title = stringResource(R.string.language),
            onDismiss = { showLanguageSheet = false },
            options = listOf(
                com.bjwag.mensaminus.store.AppLanguage.GERMAN to stringResource(R.string.language_german),
                com.bjwag.mensaminus.store.AppLanguage.ENGLISH to stringResource(R.string.language_english)
            ),
            selectedOption = settings.language,
            onOptionSelected = {
                settingsViewModel.setLanguage(it)
                showLanguageSheet = false
            }
        )
    }

    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            title = { Text(stringResource(R.string.licenses_dialog_title)) },
            text = {
                Text(
                    text = stringResource(R.string.licenses_dialog_text),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showPriceSheet) {
        ModernSelectionSheet(
            title = stringResource(R.string.price_profile),
            onDismiss = { showPriceSheet = false },
            options = listOf(
                PriceGroup.STUDENT to stringResource(R.string.price_group_studi),
                PriceGroup.EMPLOYEE to stringResource(R.string.price_group_employee)
            ),
            selectedOption = settings.priceGroup,
            onOptionSelected = {
                settingsViewModel.setPriceGroup(it)
                showPriceSheet = false
            }
        )
    }

    if (showDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { showDatabaseDialog = false },
            title = { Text(stringResource(R.string.scoring_db_title)) },
            text = {
                val db = com.bjwag.mensaminus.utils.MealFilterEngine.getKeywordDatabase(
                    settings.likedMeals, 
                    settings.dislikedMeals,
                    settings.keywordOverrides
                )

                if (db.isEmpty()) {
                    Text(stringResource(R.string.scoring_db_empty))
                } else {
                    val positive = db.filter { it.second > 0 }
                    val negative = db.filter { it.second < 0 }.sortedBy { it.second }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (positive.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.scoring_db_positive),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(positive) { (word, score) ->
                                val derived = (settings.likedMeals.flatMap { com.bjwag.mensaminus.utils.MealFilterEngine.extractKeywords(it) }.count { it == word }) -
                                              (settings.dislikedMeals.flatMap { com.bjwag.mensaminus.utils.MealFilterEngine.extractKeywords(it) }.count { it == word })
                                KeywordItem(
                                    word = word, 
                                    score = score, 
                                    color = Color(0xFF4CAF50),
                                    onIncrease = { settingsViewModel.updateKeywordOverride(word, 1) },
                                    onDecrease = { settingsViewModel.updateKeywordOverride(word, -1) },
                                    onDelete = { settingsViewModel.removeKeywordOverride(word, derived) }
                                )
                            }
                        }

                        if (negative.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.scoring_db_negative),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(negative) { (word, score) ->
                                val derived = (settings.likedMeals.flatMap { com.bjwag.mensaminus.utils.MealFilterEngine.extractKeywords(it) }.count { it == word }) -
                                              (settings.dislikedMeals.flatMap { com.bjwag.mensaminus.utils.MealFilterEngine.extractKeywords(it) }.count { it == word })
                                KeywordItem(
                                    word = word, 
                                    score = score, 
                                    color = MaterialTheme.colorScheme.error,
                                    onIncrease = { settingsViewModel.updateKeywordOverride(word, 1) },
                                    onDecrease = { settingsViewModel.updateKeywordOverride(word, -1) },
                                    onDelete = { settingsViewModel.removeKeywordOverride(word, derived) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatabaseDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ModernSelectionSheet(
    title: String,
    onDismiss: () -> Unit,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { (value, label) ->
                    val isSelected = value == selectedOption
                    Surface(
                        onClick = { onOptionSelected(value) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
fun SettingsGroup(
    title: String, 
    titleClickable: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 8.dp)
                .then(if (titleClickable != null) Modifier.clickable { titleClickable() } else Modifier)
        )
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun KeywordItem(
    word: String, 
    score: Int, 
    color: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (score > 0) "+$score" else "$score",
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun DietaryNoticeBox(preference: DietaryPreference) {
    val config = when (preference) {
        DietaryPreference.ANY -> DietaryNoticeConfig(
            backgroundColor = Color(0xFFFFDAD6),
            contentColor = Color(0xFF410002),
            title = stringResource(R.string.dietary_banner_any_title),
            description = stringResource(R.string.dietary_banner_any_desc),
            icon = "🧐"
        )
        DietaryPreference.VEGETARIAN -> DietaryNoticeConfig(
            backgroundColor = Color(0xFFFFDDB3),
            contentColor = Color(0xFF291800),
            title = stringResource(R.string.dietary_banner_vegetarian_title),
            description = stringResource(R.string.dietary_banner_vegetarian_desc),
            icon = "🧀"
        )
        DietaryPreference.VEGAN -> DietaryNoticeConfig(
            backgroundColor = Color(0xFFC2EFAD),
            contentColor = Color(0xFF002200),
            title = stringResource(R.string.dietary_banner_vegan_title),
            description = stringResource(R.string.dietary_banner_vegan_desc),
            icon = "🌱"
        )
    }

    Surface(
        color = config.backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = config.icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = config.contentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = config.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = config.contentColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private data class DietaryNoticeConfig(
    val backgroundColor: Color,
    val contentColor: Color,
    val title: String,
    val description: String,
    val icon: String
)

@Composable
fun DisclaimerBox(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.disclaimer_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = if (value.isNotEmpty()) {
            { Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
        } else null,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ) 
        },
        supportingContent = { 
            Text(
                text = description, 
                style = MaterialTheme.typography.bodyMedium, 
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ) 
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        },
        modifier = if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier
    )
}
