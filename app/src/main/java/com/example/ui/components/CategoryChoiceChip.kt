package com.example.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.ui.theme.AppPalette
import com.example.ui.theme.TextSize

/** The same category mark is used in management, forms, and reports. */
@Composable
fun CategoryChoiceChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val p = AppPalette
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = { CategoryTile(category, size = 24.dp) },
        label = {
            Text(
                category.name,
                fontSize = TextSize.sm,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = p.card,
            labelColor = p.textPrimary,
            selectedContainerColor = p.accentSoft,
            selectedLabelColor = p.textPrimary
        ),
        modifier = Modifier.testTag("category_choice_${category.id}")
    )
}
