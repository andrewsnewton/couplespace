package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.EventCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: EventCategory,
    onCategorySelected: (EventCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val categories = remember {
        EventCategory.values().map { category ->
            CategoryOption(
                category = category,
                color = getCategoryColor(category),
                label = category.name.lowercase().replaceFirstChar { it.uppercase() }
            )
        }
    }
    
    val selectedOption = categories.find { it.category == selectedCategory }
        ?: categories.first()
    
    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedOption.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                leadingIcon = {
                    Surface(
                        shape = CircleShape,
                        color = selectedOption.color,
                        modifier = Modifier.size(16.dp)
                    ) {}
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = option.color,
                                    modifier = Modifier.size(16.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = option.label)
                            }
                        },
                        onClick = {
                            onCategorySelected(option.category)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

private fun getCategoryColor(category: EventCategory): Color {
    return when (category) {
        EventCategory.PERSONAL -> Color(0xFF4285F4) // Blue
        EventCategory.COUPLE -> Color(0xFFEA4335)   // Red
        EventCategory.WORK -> Color(0xFF34A853)     // Green
        EventCategory.FAMILY -> Color(0xFFFBBC05)   // Yellow
        EventCategory.HEALTH -> Color(0xFF46BDC6)   // Teal
        EventCategory.SOCIAL -> Color(0xFFAB47BC)   // Purple
        EventCategory.FINANCIAL -> Color(0xFF0F9D58) // Dark Green
        EventCategory.EDUCATION -> Color(0xFF4285F4) // Blue
        EventCategory.HOBBY -> Color(0xFFDB4437)    // Red
        EventCategory.OTHER -> Color(0xFF757575)    // Gray
    }
}

data class CategoryOption(
    val category: EventCategory,
    val color: Color,
    val label: String
)
