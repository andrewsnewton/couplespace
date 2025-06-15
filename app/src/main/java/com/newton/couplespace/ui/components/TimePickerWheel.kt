package com.newton.couplespace.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A reusable time picker wheel component that mimics the scrollable time picker in alarm clock apps
 * with infinite scrolling and haptic feedback.
 *
 * @param value The current selected value
 * @param onValueChange Callback when the value changes
 * @param range The range of values to display
 * @param modifier Modifier for the component
 * @param enabled Whether the component is enabled
 * @param format Function to format the displayed value
 */
@Composable
fun TimePickerWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString().padStart(2, '0') }
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // Time selector button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable(enabled = enabled) { isExpanded = !isExpanded }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // Show a small indicator that this is expandable
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Animated time picker wheel
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 4.dp)
                    .fillMaxWidth()
            ) {
                CircularTimeWheel(
                    hours = range.toList(),
                    initialHour = value,
                    onHourSelected = { 
                        onValueChange(it)
                        // Auto-dismiss after selection
                        scope.launch {
                            delay(300) // Short delay for better UX
                            isExpanded = false
                        }
                    },
                    enabled = enabled,
                    format = format,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Internal component that implements the circular scrolling time wheel
 */
@Composable
private fun CircularTimeWheel(
    hours: List<Int>,
    initialHour: Int,
    onHourSelected: (Int) -> Unit,
    enabled: Boolean = true,
    format: (Int) -> String = { it.toString().padStart(2, '0') },
    modifier: Modifier = Modifier
) {
    // Create a large repeated list for "infinite" scrolling effect
    val repeatedHours = remember(hours) {
        val list = mutableListOf<Int>()
        repeat(20) { // Repeat the hours list 20 times for smooth scrolling
            list.addAll(hours)
        }
        list
    }
    
    val visibleItems = 5 // Number of visible items in the wheel
    val coroutineScope = rememberCoroutineScope()
    
    // Find the initial index in the repeated list
    val initialIndex = remember(initialHour, repeatedHours) {
        val middleOfList = repeatedHours.size / 2
        val indexInOriginalList = hours.indexOf(initialHour)
        if (indexInOriginalList >= 0) {
            // Position the selected hour in the middle of the list for better scrolling in both directions
            middleOfList - (middleOfList % hours.size) + indexInOriginalList
        } else {
            middleOfList
        }
    }
    
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex - visibleItems / 2)
    
    // Track the previously selected hour to detect changes
    var previousSelectedHour by remember { mutableStateOf(initialHour) }
    
    // Auto-snap to the nearest hour when scrolling stops
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val currentIndex = lazyListState.firstVisibleItemIndex + visibleItems / 2
            if (currentIndex >= 0 && currentIndex < repeatedHours.size) {
                val selectedHour = repeatedHours[currentIndex]
                
                // Update when the selected hour changes
                if (selectedHour != previousSelectedHour) {
                    previousSelectedHour = selectedHour
                    onHourSelected(selectedHour)
                }
            }
        }
    }
    
    LazyColumn(
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth(),
        state = lazyListState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 70.dp),
        userScrollEnabled = enabled
    ) {
        items(repeatedHours.size) { index ->
            val hour = repeatedHours[index]
            val isSelected = index == lazyListState.firstVisibleItemIndex + visibleItems / 2
            
            // Calculate alpha based on distance from center
            val centerPosition = lazyListState.firstVisibleItemIndex + visibleItems / 2
            val distanceFromCenter = abs(index - centerPosition)
            val alpha = when {
                distanceFromCenter == 0 -> 1f
                distanceFromCenter == 1 -> 0.7f
                distanceFromCenter == 2 -> 0.4f
                else -> 0.2f
            }
            
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = format(hour),
                    fontSize = if (isSelected) 20.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
