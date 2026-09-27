package com.bjwag.mensaminus.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bjwag.mensaminus.R
import com.bjwag.mensaminus.model.Canteen
import com.bjwag.mensaminus.store.AppLanguage
import com.bjwag.mensaminus.store.DietaryPreference
import com.bjwag.mensaminus.store.PriceGroup
import com.bjwag.mensaminus.store.UserSettings
import com.bjwag.mensaminus.ui.components.MensaminusLogo
import com.bjwag.mensaminus.viewmodel.MainViewModel
import com.bjwag.mensaminus.viewmodel.CanteensViewModel
import com.bjwag.mensaminus.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private enum class OnboardingStep {
    Welcome, Language, Canteens, Preferences, Allergens, Favorites
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    mainViewModel: MainViewModel,
    canteensViewModel: CanteensViewModel,
    settingsViewModel: SettingsViewModel,
    onFinished: () -> Unit
) {
    val allCanteens by mainViewModel.allCanteens.collectAsState()
    val settings by mainViewModel.userSettings.collectAsState()
    
    val steps = OnboardingStep.entries
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackground(pagerState = pagerState)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                OnboardingBottomBar(
                    pagerState = pagerState,
                    stepsCount = steps.size,
                    onBack = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onNext = {
                        if (pagerState.currentPage < (steps.size - 1)) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
                    pageSpacing = 16.dp,
                    userScrollEnabled = true
                ) { index ->
                    OnboardingCard {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (steps[index]) {
                                OnboardingStep.Welcome -> WelcomePage()
                                OnboardingStep.Language -> LanguagePage(
                                    currentLanguage = settings.language,
                                    onSetLanguage = { settingsViewModel.setLanguage(it) }
                                )
                                OnboardingStep.Canteens -> CanteenSelectionPage(
                                    allCanteens = allCanteens,
                                    activeIds = settings.activeCanteens,
                                    onToggle = { canteensViewModel.toggleCanteenActive(it) },
                                    onDeselectAll = { canteensViewModel.deselectAllCanteens() }
                                )
                                OnboardingStep.Preferences -> PreferencePage(
                                    settings = settings,
                                    onSetDietary = { settingsViewModel.setDietaryPreference(it) },
                                    onSetPrice = { settingsViewModel.setPriceGroup(it) }
                                )
                                OnboardingStep.Allergens -> AllergensPage(
                                    excludedAllergies = settings.excludedAllergies,
                                    onToggleAllergy = { settingsViewModel.toggleAllergy(it) }
                                )
                                OnboardingStep.Favorites -> FavoritePage(
                                    enabled = settings.morningMatchNotification,
                                    onToggle = { settingsViewModel.setMorningMatchNotification(it) },
                                    nfcEnabled = settings.nfcReaderEnabled,
                                    onToggleNfc = { mainViewModel.setNfcReaderEnabled(it) },
                                    isNfcSupported = mainViewModel.isNfcSupported
                                )
                            }

                            TextButton(
                                onClick = onFinished,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(stringResource(R.string.onboarding_skip), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingBackground(pagerState: PagerState) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    val tertiaryColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)

    val offset = pagerState.currentPage + pagerState.currentPageOffsetFraction
    
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            withTransform({
                rotate(rotation, pivot = Offset(width * 0.5f, height * 0.5f))
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor, Color.Transparent),
                        center = Offset(
                            x = width * 0.2f - (offset * 80f),
                            y = height * 0.2f
                        ),
                        radius = width * 0.9f
                    ),
                    radius = width * 0.9f,
                    center = Offset(
                        x = width * 0.2f - (offset * 80f),
                        y = height * 0.2f
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryColor, Color.Transparent),
                        center = Offset(
                            x = width * 0.8f + (offset * 40f),
                            y = height * 0.7f
                        ),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(
                        x = width * 0.8f + (offset * 40f),
                        y = height * 0.7f
                    )
                )
                
                if (offset > 1f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(tertiaryColor, Color.Transparent),
                            center = Offset(
                                x = width * 0.5f,
                                y = height * 0.4f + ((offset - 1f) * 60f)
                            ),
                            radius = width * 0.6f
                        ),
                        radius = width * 0.6f,
                        center = Offset(
                            x = width * 0.5f,
                            y = height * 0.4f + ((offset - 1f) * 60f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    pagerState: PagerState,
    stepsCount: Int,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }

            if (pagerState.currentPage == 0) {
                Spacer(Modifier.width(56.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(stepsCount) { index ->
                    val isSelected = pagerState.currentPage == index
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .height(56.dp)
                    .widthIn(min = 56.dp),
                shape = if (pagerState.currentPage == stepsCount - 1) RoundedCornerShape(20.dp) else CircleShape,
                contentPadding = if (pagerState.currentPage == stepsCount - 1) 
                    PaddingValues(horizontal = 24.dp) else PaddingValues(0.dp)
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage == stepsCount - 1,
                    label = "button_content"
                ) { isLastPage ->
                    if (isLastPage) {
                        Text(
                            stringResource(R.string.onboarding_start),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    var startAnim by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(300)
        startAnim = true
    }

    val lineProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1500, easing = EaseInOutQuart),
        label = "line"
    )
    
    val fillAlpha by animateFloatAsState(
        targetValue = if (startAnim) 0.15f else 0f,
        animationSpec = tween(1000, delayMillis = 800),
        label = "fill"
    )
    
    val cutleryAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(800, delayMillis = 1400),
        label = "cutlery_alpha"
    )
    
    val cutleryOffset by animateFloatAsState(
        targetValue = if (startAnim) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cutlery_offset"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 2000),
        label = "text_alpha"
    )
    
    val textOffsetY by animateDpAsState(
        targetValue = if (startAnim) 0.dp else 30.dp,
        animationSpec = tween(1000, delayMillis = 2000, easing = EaseOutCubic),
        label = "text_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = breathingScale
                    scaleY = breathingScale
                },
            contentAlignment = Alignment.Center
        ) {
            MensaminusLogo(
                size = 220.dp,
                color = MaterialTheme.colorScheme.primary,
                lineProgress = lineProgress,
                fillAlpha = fillAlpha,
                cutleryAlpha = cutleryAlpha,
                forkOffsetX = -cutleryOffset,
                spoonOffsetX = cutleryOffset,
                strokeWidth = 10f
            )
        }
        
        Spacer(Modifier.height(48.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    alpha = textAlpha
                    translationY = textOffsetY.toPx()
                }
        ) {
            val welcomeText = stringResource(R.string.onboarding_welcome_title)
            val brandName = stringResource(R.string.app_name)
            
            Text(
                text = buildAnnotatedString {
                    val parts = welcomeText.split(brandName)
                    if (parts.size > 1) {
                        append(parts[0])
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        ) {
                            append(brandName)
                        }
                        append(parts[1])
                    } else {
                        append(welcomeText)
                    }
                },
                style = MaterialTheme.typography.displaySmall.copy(
                    lineHeight = 42.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.onboarding_welcome_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 28.sp
            )
        }
    }
}

@Composable
private fun LanguagePage(
    currentLanguage: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PageHeader(
            title = stringResource(R.string.onboarding_lang_title),
            subtitle = stringResource(R.string.onboarding_lang_desc),
            icon = Icons.Default.Language
        )
        
        Spacer(Modifier.height(48.dp))
        
        val languages = listOf(
            AppLanguage.GERMAN to ("Deutsch" to "🇩🇪"),
            AppLanguage.ENGLISH to ("English" to "🇬🇧")
        )
        
        languages.forEach { (lang, data) ->
            val (label, flag) = data
            val isSelected = currentLanguage == lang
            
            SelectionCard(
                selected = isSelected,
                onClick = { onSetLanguage(lang) },
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(flag, fontSize = 36.sp, modifier = Modifier.padding(end = 20.dp))
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanteenSelectionPage(
    allCanteens: List<Canteen>,
    activeIds: List<Int>,
    onToggle: (Int) -> Unit,
    onDeselectAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.onboarding_canteen_title),
            subtitle = stringResource(R.string.onboarding_canteen_desc),
            icon = Icons.Default.Restaurant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDeselectAll) {
                Text(stringResource(R.string.onboarding_canteen_deselect_all))
            }
        }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            items(allCanteens.sortedBy { it.name }) { canteen ->
                val isActive = activeIds.contains(canteen.id)
                SelectionCard(
                    selected = isActive,
                    onClick = { onToggle(canteen.id) },
                    activeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (isActive) 4.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    canteen.name.take(1).uppercase(),
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        
                        Spacer(Modifier.width(20.dp))
                        
                        Text(
                            text = canteen.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                        
                        Icon(
                            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencePage(
    settings: UserSettings,
    onSetDietary: (DietaryPreference) -> Unit,
    onSetPrice: (PriceGroup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.onboarding_prefs_title),
            subtitle = stringResource(R.string.onboarding_prefs_desc),
            icon = Icons.Default.Settings
        )
        
        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(R.string.price_profile),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        
        val priceOptions = listOf(
            PriceGroup.STUDENT to (stringResource(R.string.price_group_studi) to Icons.Default.School),
            PriceGroup.EMPLOYEE to (stringResource(R.string.price_group_employee) to Icons.Default.Work)
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            priceOptions.forEach { (group, data) ->
                val (label, icon) = data
                val isSelected = settings.priceGroup == group
                SelectionCard(
                    selected = isSelected,
                    onClick = { onSetPrice(group) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            icon, 
                            contentDescription = null, 
                            modifier = Modifier.size(32.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))

        Text(
            stringResource(R.string.dietary_preference),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        
        val dietOptions = listOf(
            DietaryPreference.ANY to (stringResource(R.string.dietary_all) to "🍖"),
            DietaryPreference.VEGETARIAN to (stringResource(R.string.dietary_vegetarian) to "🧀"),
            DietaryPreference.VEGAN to (stringResource(R.string.dietary_vegan) to "🌱")
        )
        
        dietOptions.forEach { (pref, data) ->
            val (label, emoji) = data
            val isSelected = settings.dietaryPreference == pref
            SelectionCard(
                selected = isSelected,
                onClick = { onSetDietary(pref) },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 32.sp, modifier = Modifier.padding(end = 20.dp))
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        modifier = Modifier.size(28.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllergensPage(
    excludedAllergies: Set<String>,
    onToggleAllergy: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        PageHeader(
            title = stringResource(R.string.onboarding_allergies_title),
            subtitle = stringResource(R.string.onboarding_allergies_desc),
            icon = Icons.Default.Warning,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        val allAllergies = com.bjwag.mensaminus.model.MealAllergy.entries
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(allAllergies) { allergy ->
                val isExcluded = excludedAllergies.contains(allergy.name)
                SelectionCard(
                    selected = isExcluded,
                    onClick = { onToggleAllergy(allergy.name) },
                    activeColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    borderColor = if (isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(allergy.displayNameResId),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isExcluded) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2
                        )
                        Checkbox(
                            checked = isExcluded,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritePage(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    nfcEnabled: Boolean,
    onToggleNfc: (Boolean) -> Unit,
    isNfcSupported: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PageHeader(
            title = stringResource(R.string.onboarding_features_title),
            subtitle = stringResource(R.string.onboarding_features_desc),
            icon = Icons.Default.Extension
        )
        
        Spacer(Modifier.height(32.dp))

        ModernFeatureSwitch(
            title = stringResource(R.string.onboarding_fav_title),
            desc = stringResource(R.string.daily_match_notification_desc),
            icon = Icons.Default.NotificationsActive,
            checked = enabled,
            onCheckedChange = onToggle,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(20.dp))

        ModernFeatureSwitch(
            title = stringResource(R.string.nfc_onboarding_title),
            desc = if (isNfcSupported) stringResource(R.string.nfc_onboarding_desc) 
                   else stringResource(R.string.nfc_not_supported),
            icon = Icons.Default.Nfc,
            checked = nfcEnabled,
            onCheckedChange = onToggleNfc,
            color = MaterialTheme.colorScheme.tertiary,
            enabled = isNfcSupported
        )
    }
}

@Composable
private fun ModernFeatureSwitch(
    title: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color,
    enabled: Boolean = true
) {
    SelectionCard(
        selected = checked,
        onClick = { if (enabled) onCheckedChange(!checked) },
        activeColor = color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (checked) color else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = if (checked) 4.dp else 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (checked) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = color
                )
            )
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(20.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SelectionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    borderColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) activeColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        tonalElevation = if (selected) 4.dp else 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        content()
    }
}
