package com.bjwag.mensaminus.ui.components

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bjwag.mensaminus.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashContinuation(onFinished: () -> Unit) {
    // Animation States
    val lineProgress = remember { Animatable(0f) }
    val fillAlpha = remember { Animatable(0f) }
    val cutleryAlpha = remember { Animatable(0f) }
    val forkOffsetX = remember { Animatable(-120f) }
    val spoonOffsetX = remember { Animatable(120f) }
    
    val textAlpha = remember { Animatable(0f) }
    val textSlideY = remember { Animatable(30f) }
    
    val screenAlpha = remember { Animatable(1f) }

    val backgroundColor = colorResource(id = R.color.ic_launcher_background)
    val darkerGreen = Color(0xFF1B5E20)
    val overshootEasing = remember { OvershootInterpolator(1.2f).toEasing() }
    
    // Remember the background brush
    val backgroundBrush = remember(backgroundColor, darkerGreen) {
        Brush.radialGradient(
            colors = listOf(backgroundColor, darkerGreen),
            radius = 1500f
        )
    }

    LaunchedEffect(Unit) {
        // Start the Line Draw (duration ~800ms)
        launch {
            lineProgress.animateTo(1f, tween(800, easing = LinearEasing))
        }

        // Once the outline is nearly drawn (at 80% progress), start fading in the white fill
        launch {
            delay(640)
            fillAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        }

        // Fly-in cutlery from sides
        launch {
            delay(300)
            launch { cutleryAlpha.animateTo(1f, tween(600)) }
            launch { forkOffsetX.animateTo(0f, tween(600, easing = overshootEasing)) }
            launch { spoonOffsetX.animateTo(0f, tween(600, easing = overshootEasing)) }
        }

        // App Name and Author animations
        launch {
            delay(200)
            launch { textAlpha.animateTo(1f, tween(600)) }
            launch { textSlideY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        }

        // Total splash duration
        delay(1800)
        
        // Exit Transition
        screenAlpha.animateTo(0f, tween(600))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The Logo Logo
            MensaminusLogo(
                lineProgress = lineProgress.value,
                fillAlpha = fillAlpha.value,
                cutleryAlpha = cutleryAlpha.value,
                forkOffsetX = forkOffsetX.value,
                spoonOffsetX = spoonOffsetX.value,
                size = 200.dp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // The App Name
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha.value
                        translationY = textSlideY.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Mensa")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraLight)) {
                            append("Minus")
                        }
                    },
                    color = Color.White,
                    fontSize = 42.sp,
                    letterSpacing = (-1).sp
                )
            }
        }
    }
}

private fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }
