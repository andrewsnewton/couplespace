package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * A current time indicator with an animated pulse effect for the split timeline.
 * This shows the current time on the central timeline axis.
 */
@Composable
fun CurrentTimeIndicator(
    currentTime: LocalTime = LocalTime.now(),
    isDisplayed: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Don't show anything if not displayed
    if (!isDisplayed) return
    
    // Animation for pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), 
        label = "pulse"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), 
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        // Current time display
        Text(
            text = currentTime.format(DateTimeFormatter.ofPattern("h:mm a")),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF48FB1),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = Color(0xFFF48FB1).copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Animated pulse circle 
        Box(
            modifier = Modifier
                .size((8 * pulseScale).dp)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    clip = false,
                    spotColor = Color(0xFFF48FB1)
                )
                .clip(CircleShape)
                .background(Color(0xFFF48FB1).copy(alpha = pulseAlpha))
        )
        
        // Vertical line extending down
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(40.dp)
                .background(Color(0xFFF48FB1))
        )
    }
}
