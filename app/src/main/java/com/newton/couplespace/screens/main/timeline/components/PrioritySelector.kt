package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.Priority

@Composable
fun PrioritySelector(
    selectedPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorities = remember {
        listOf(
            PriorityOption(Priority.LOW, "Low", Color(0xFF4CAF50)),
            PriorityOption(Priority.MEDIUM, "Medium", Color(0xFFFFC107)),
            PriorityOption(Priority.HIGH, "High", Color(0xFFFF9800)),
            PriorityOption(Priority.URGENT, "Urgent", Color(0xFFF44336))
        )
    }
    
    Column(modifier = modifier) {
        priorities.forEach { option ->
            PriorityOption(
                option = option,
                isSelected = selectedPriority == option.priority,
                onClick = { onPrioritySelected(option.priority) }
            )
        }
    }
}

@Composable
private fun PriorityOption(
    option: PriorityOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = option.color
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Priority indicator
        Surface(
            shape = MaterialTheme.shapes.small,
            color = option.color.copy(alpha = if (isSelected) 0.2f else 0.1f),
            modifier = Modifier
                .padding(end = 8.dp)
                .height(24.dp)
                .width(64.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = option.color
                )
            }
        }
    }
}

data class PriorityOption(
    val priority: Priority,
    val label: String,
    val color: Color
)
