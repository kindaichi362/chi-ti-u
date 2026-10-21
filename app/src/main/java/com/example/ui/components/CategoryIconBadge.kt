package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary

@Composable
fun CategoryIconBadge(
    category: Category,
    size: Dp = 44.dp,
    isCircle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val catColor = parseColorSafe(category.colorHex)
    val shape = if (isCircle) CircleShape else RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(catColor.copy(alpha = 0.16f))
            .border(1.dp, catColor.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (!category.emoji.isNullOrBlank()) {
            Text(
                text = category.emoji,
                fontSize = (size.value * 0.45).sp
            )
        } else {
            Icon(
                imageVector = getIconForName(category.iconName),
                contentDescription = category.name,
                tint = catColor,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

@Composable
fun CategoryProgressCard(
    category: Category,
    spentAmount: Double,
    budgetAmount: Double?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val percentSpent = if (budgetAmount != null && budgetAmount > 0) {
        ((spentAmount / budgetAmount) * 100).toInt()
    } else {
        null
    }

    val progressValue = if (budgetAmount != null && budgetAmount > 0) {
        (spentAmount / budgetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val statusColor = when {
        percentSpent == null -> AppleOrange
        percentSpent > 100 -> AppleRed
        percentSpent > 80 -> AppleOrange
        else -> AppleGreen
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AppleSurface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppleBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIconBadge(
                    category = category,
                    size = 42.dp,
                    isCircle = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatVnd(spentAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                }

                if (percentSpent != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$percentSpent%",
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (budgetAmount != null && budgetAmount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = AppleSurface2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đã chi ${formatVnd(spentAmount)}",
                        fontSize = 11.sp,
                        color = AppleTextSecondary
                    )
                    Text(
                        text = "Ngân sách ${formatVnd(budgetAmount)}",
                        fontSize = 11.sp,
                        color = AppleTextSecondary
                    )
                }
            }
        }
    }
}
