package com.newton.couplespace.screens.health.components.nutrition.meal.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState // Explicitly ensure this is here
import androidx.compose.material3.SwipeToDismissBoxValue // Explicitly ensure this is here
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/**
 * Collection of reusable animations for meal tracking components
 */
object MealAnimations {

    /**
     * Staggered reveal animation for lists of items
     */
    @Composable
    fun StaggeredReveal(
        visible: Boolean,
        index: Int,
        modifier: Modifier = Modifier,
        initialOffsetY: Int = 100,
        delayMillis: Int = 50,
        content: @Composable AnimatedVisibilityScope.() -> Unit
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = delayMillis * index
                )
            ) + slideIn(
                initialOffset = { IntOffset(0, initialOffsetY) },
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = delayMillis * index,
                    easing = EaseOutQuart
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = delayMillis * index
                )
            ) + slideOut(
                targetOffset = { IntOffset(0, initialOffsetY) },
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = delayMillis * index,
                    easing = EaseInQuart
                )
            ),
            modifier = modifier,
            content = content
        )
    }

    /**
     * Pulse animation for highlighting items
     */
    @Composable
    fun PulseEffect(
        pulsing: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
        val scale by if (pulsing) {
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )
        } else {
            remember { mutableStateOf(1f) }
        }

        Box(
            modifier = modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            content()
        }
    }

    /**
     * Swipe animation for dismissing items using Material 3's SwipeToDismissBox.
     * This provides a complete swipe-to-dismiss experience with states and backgrounds.
     *
     * @param onSwipeLeft Callback when the item is swiped to the left (DismissDirection.EndToStart).
     * @param onSwipeRight Callback when the item is swiped to the right (DismissDirection.StartToEnd).
     * @param backgroundContent Composable to display behind the swiped item. Receives the current
     * [SwipeToDismissBoxState] for custom background animations.
     * @param content The main content of the item that can be swiped.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SwipeToAction(
        modifier: Modifier = Modifier,
        onSwipeLeft: () -> Unit,
        onSwipeRight: () -> Unit,
        backgroundContent: @Composable (dismissState: androidx.compose.material3.SwipeToDismissBoxState) -> Unit,
        content: @Composable () -> Unit
    ) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onSwipeRight()
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onSwipeLeft()
                        true
                    }
                    SwipeToDismissBoxValue.Settled -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {
                backgroundContent(dismissState)
            },
            content = {
                content()
            }
        )
    }
    /**
     * Bounce animation for adding new items
     */
    @Composable
    fun BounceInEffect(
        visible: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable AnimatedVisibilityScope.() -> Unit
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(300)
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(
                animationSpec = tween(200)
            ) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            ),
            modifier = modifier,
            content = content
        )
    }
}

// Custom easing functions
val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
val EaseInQuart = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)
val EaseInOutQuad = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

/**
 * Extension function to apply staggered reveal animation as a modifier
 */
fun Modifier.staggeredReveal(index: Int = 0, initialOffsetY: Int = 100): Modifier = composed {
    var visible by remember { mutableStateOf(true) } // Start visible immediately

    // No LaunchedEffect needed since we start visible

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        // Minimal duration and delay for near-instant appearance
        animationSpec = tween(durationMillis = 50, delayMillis = 5 * index),
        label = "staggeredRevealAlpha"
    )
    val animatedOffsetY by animateIntOffsetAsState(
        targetValue = if (visible) IntOffset(0, 0) else IntOffset(0, initialOffsetY),
        // Minimal duration and delay for near-instant appearance
        animationSpec = tween(durationMillis = 50, delayMillis = 5 * index, easing = EaseOutQuart),
        label = "staggeredRevealOffsetY"
    )

    this.graphicsLayer {
        alpha = animatedAlpha
        translationY = animatedOffsetY.y.toFloat()
    }
}

/**
 * Extension function to apply bounce in animation as a modifier
 */
fun Modifier.bounceInEffect(): Modifier = composed {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "bounceInAlpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceInScale"
    )

    this.graphicsLayer {
        alpha = animatedAlpha
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun SwipeToActionPreview() {
    val items = remember { mutableStateListOf("Item 1", "Item 2", "Item 3") }

    Column {
        items.forEachIndexed { index, item ->
            MealAnimations.SwipeToAction(
                onSwipeLeft = {
                    println("Swiped left: $item")
                    items.remove(item)
                },
                onSwipeRight = {
                    println("Swiped right: $item")
                    items.remove(item)
                },
                backgroundContent = { dismissState: androidx.compose.material3.SwipeToDismissBoxState ->
                    // Derive icon and alignment directly from SwipeToDismissBoxValue
                    val isDismissedToEnd = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                    val isDismissedToStart = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.Settled -> Color.LightGray
                            SwipeToDismissBoxValue.StartToEnd -> Color.Green
                            SwipeToDismissBoxValue.EndToStart -> Color.Red
                        }, label = "dismissColor"
                    )

                    val icon = when {
                        isDismissedToEnd -> Icons.Default.Done
                        isDismissedToStart -> Icons.Default.Delete
                        else -> null
                    }
                    val alignment = when {
                        isDismissedToEnd -> Alignment.CenterStart
                        isDismissedToStart -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            ) {
                // Your actual item content
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}