package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Extension function to make a composable clickable with ripple effect
 */
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    )
}
