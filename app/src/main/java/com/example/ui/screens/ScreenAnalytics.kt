package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.TransactionAmountText
import com.example.ui.components.formatVnd
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import androidx.compose.foundation.BorderStroke
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Curated high-contrast harmonious color palette for financial charts & reports
val REPORT_CHART_COLORS = listOf(
    Color(0xFF2563EB), // Xanh dương hoàng gia
    Color(0xFF10B981), // Xanh lục ngọc bảo
    Color(0xFFF59E0B), // Vàng hổ phách
    Color(0xFFEC4899), // Hồng tươi
    Color(0xFF8B5CF6), // Tím violet
    Color(0xFF06B6D4), // Xanh lam ngọc
    Color(0xFFF97316), // Cam đào
    Color(0xFF14B8A6), // Xanh ngọc mint
    Color(0xFF6366F1), // Chàm indigo
    Color(0xFFEF4444), // Đỏ san hô
    Color(0xFF84CC16), // Xanh cốm lime
    Color(0xFFD946EF), // Tím hồng fuchsia
    Color(0xFF0EA5E9), // Xanh da trời
    Color(0xFFE11D48), // Hồng mận
    Color(0xFFEAB308), // Vàng hướng dương
    Color(0xFFA855F7), // Tím thạch anh
    Color(0xFF38BDF8), // Xanh pastel
    Color(0xFF4ADE80), // Xanh lá pastel
    Color(0xFFFB923C), // Cam sáng
    Color(0xFFA78BFA)  // Tím hoa cà
)

fun formatCompactVnd(amount: Double): String {
    val absAmt = kotlin.math.abs(amount)
    return when {
        absAmt >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.1ftỷ", absAmt / 1_000_000_000).replace(".0", "")
        absAmt >= 1_000_000 -> String.format(Locale.getDefault(), "%.1ftr", absAmt / 1_000_000).replace(".0", "")
        absAmt >= 1_000 -> String.format(Locale.getDefault(), "%.0fk", absAmt / 1_000)
        else -> "${absAmt.toInt()}"
    }
}

@Composable
fun ScreenAnalytics(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Biểu đồ tròn, 1: Lịch thu chi

    // Selected month navigation
    var calendarMonthOffset by remember { mutableIntStateOf(0) } // 0: current month, -1: prev, etc.

    val displayCalendar = remember(calendarMonthOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, calendarMonthOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal
    }

    val monthStartTimestamp = displayCalendar.timeInMillis
    val monthEndTimestamp = remember(calendarMonthOffset) {
        val cal = displayCalendar.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        cal.timeInMillis
    }

    val monthTransactions = remember(transactions, calendarMonthOffset) {
        transactions.filter { it.timestamp in monthStartTimestamp..monthEndTimestamp }
    }

    val monthExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthIncome = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val monthNetSavings = monthIncome - monthExpense

    val categoryExpenseMap = remember(monthTransactions, categories) {
        val map = mutableMapOf<String, Double>()
        for (tx in monthTransactions) {
            if (tx.type == TransactionType.EXPENSE) {
                val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Khác"
                map[catName] = (map[catName] ?: 0.0) + tx.amount
            }
        }
        map
    }

    val sortedCategories = remember(categoryExpenseMap) {
        categoryExpenseMap.toList().sortedByDescending { it.second }
    }

    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(displayCalendar.time)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Báo cáo tài chính",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = monthTitle.replaceFirstChar { it.titlecase(Locale("vi", "VN")) },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { calendarMonthOffset -= 1 },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Tháng trước")
                }

                if (calendarMonthOffset != 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { calendarMonthOffset = 0 }
                    ) {
                        Text(
                            text = "Nay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { calendarMonthOffset += 1 },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Tháng sau")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 High-Contrast Clean Cashflow Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tổng thu card (Emerald/Green tint)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tổng thu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+${formatCompactVnd(monthIncome)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tổng chi card (Coral Red/Rose tint)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tổng chi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB91C1C)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "-${formatCompactVnd(monthExpense)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFDC2626),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Dư ròng card (Indigo/Blue or Amber if deficit)
            val isSurplus = monthNetSavings >= 0
            val savingsColor = if (isSurplus) Color(0xFF2563EB) else Color(0xFFD97706)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = savingsColor.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(savingsColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSurplus) "✓" else "!",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSurplus) "Dư ròng" else "Thâm hụt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = savingsColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${if (monthNetSavings > 0) "+" else ""}${formatCompactVnd(monthNetSavings)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = savingsColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab selection: Biểu đồ tròn vs Lịch thu chi
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Biểu đồ tròn", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lịch thu chi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // BIỂU ĐỒ TRÒN (DONUT / PIE CHART)
                DonutChartSection(
                    totalExpense = monthExpense,
                    sortedCategories = sortedCategories,
                    categories = categories,
                    monthTransactions = monthTransactions
                )
            }
            1 -> {
                // LỊCH THU CHI (FINANCIAL CALENDAR)
                FinancialCalendarSection(
                    displayCalendar = displayCalendar,
                    transactions = monthTransactions,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    monthNetSavings = monthNetSavings,
                    categories = categories,
                    accounts = accounts
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. DONUT / PIE CHART COMPONENT & LIST
// -------------------------------------------------------------

@Composable
fun DonutChartSection(
    totalExpense: Double,
    sortedCategories: List<Pair<String, Double>>,
    categories: List<Category>,
    monthTransactions: List<Transaction>
) {
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (totalExpense <= 0 || sortedCategories.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có chi tiêu nào trong tháng này",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Donut Canvas Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CƠ CẤU CHI TIÊU THÁNG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Donut Chart Canvas with Center Hole Text
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val categoryColors = sortedCategories.mapIndexed { idx, _ ->
                                REPORT_CHART_COLORS[idx % REPORT_CHART_COLORS.size]
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 36.dp.toPx()
                                val diameter = size.minDimension - strokeWidth
                                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                val arcSize = Size(diameter, diameter)

                                var currentStartAngle = -90f

                                for (i in sortedCategories.indices) {
                                    val (name, amount) = sortedCategories[i]
                                    val sweep = ((amount / totalExpense) * 360.0).toFloat()
                                    val isSelected = selectedCategoryName == name
                                    val arcStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                                    drawArc(
                                        color = categoryColors[i],
                                        startAngle = currentStartAngle,
                                        sweepAngle = (sweep - 2.5f).coerceAtLeast(1.5f), // Clean separator gap
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = arcStroke, cap = StrokeCap.Round)
                                    )
                                    currentStartAngle += sweep
                                }
                            }

                            // Center Info
                            val focusedIndex = sortedCategories.indexOfFirst { it.first == selectedCategoryName }
                            val focusedCategory = if (focusedIndex >= 0) sortedCategories[focusedIndex] else null
                            val focusedColor = if (focusedIndex >= 0) REPORT_CHART_COLORS[focusedIndex % REPORT_CHART_COLORS.size] else MaterialTheme.colorScheme.primary

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                if (focusedCategory != null) {
                                    val pct = (focusedCategory.second / totalExpense) * 100
                                    Text(
                                        text = focusedCategory.first,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = focusedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatVnd(focusedCategory.second),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = focusedColor
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = focusedColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", pct)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = focusedColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Tổng chi tiêu",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatVnd(totalExpense),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(
                                        text = "${sortedCategories.size} danh mục",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (selectedCategoryName != null) {
                            Text(
                                text = "Chạm để xem lại tổng thể ↺",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { selectedCategoryName = null }
                                    .padding(4.dp)
                            )
                        } else {
                            Text(
                                text = "Chạm vào danh mục để xem chi tiết",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories Breakdown List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chi tiết theo danh mục",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${sortedCategories.size} nhóm chi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(sortedCategories.indices.toList()) { idx ->
                val (catName, amount) = sortedCategories[idx]
                val cat = categories.firstOrNull { it.name == catName }
                val pct = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
                val color = REPORT_CHART_COLORS[idx % REPORT_CHART_COLORS.size]
                val isSelected = selectedCategoryName == catName

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategoryName = if (isSelected) null else catName
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, color) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!cat?.emoji.isNullOrBlank()) {
                                    Text(text = cat!!.emoji!!, fontSize = 20.sp)
                                } else {
                                    CategoryAvatar(
                                        iconName = cat?.iconName ?: "shopping_cart",
                                        colorHex = cat?.colorHex ?: "#0A5C36",
                                        size = 36.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = catName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (cat?.groupName != null) {
                                    Text(
                                        text = cat.groupName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatVnd(amount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", pct)}%",
                                        color = color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (pct / 100).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.12f)
                        )

                        // If expanded, show transactions under this category
                        val categoryTxs = remember(monthTransactions, cat?.id) {
                            monthTransactions.filter { it.type == TransactionType.EXPENSE && (it.categoryId == cat?.id || (cat == null && it.categoryId == null)) }
                        }
                        AnimatedVisibility(visible = isSelected && categoryTxs.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${categoryTxs.size} giao dịch trong tháng:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = color
                                )
                                for (tx in categoryTxs.take(5)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (tx.note.isNotBlank()) tx.note else "Chi tiêu",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "-${formatVnd(tx.amount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. FINANCIAL CALENDAR SECTION (LỊCH THU CHI THEO NGÀY)
// -------------------------------------------------------------

@Composable
fun FinancialCalendarSection(
    displayCalendar: Calendar,
    transactions: List<Transaction>,
    monthIncome: Double,
    monthExpense: Double,
    monthNetSavings: Double,
    categories: List<Category>,
    accounts: List<com.example.data.model.Account>
) {
    var selectedDayNumber by remember { mutableStateOf<Int?>(null) }

    // Group transactions by day of month (1..31)
    val dayMap = remember(transactions, displayCalendar) {
        val cal = Calendar.getInstance()
        val map = mutableMapOf<Int, MutableList<Transaction>>()
        for (tx in transactions) {
            cal.timeInMillis = tx.timestamp
            // Match month & year
            if (cal.get(Calendar.MONTH) == displayCalendar.get(Calendar.MONTH) &&
                cal.get(Calendar.YEAR) == displayCalendar.get(Calendar.YEAR)
            ) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(day) { mutableListOf() }.add(tx)
            }
        }
        map
    }

    // Calendar grid calculations
    val calInstance = displayCalendar.clone() as Calendar
    calInstance.set(Calendar.DAY_OF_MONTH, 1)
    // In Java Calendar: SUNDAY=1, MONDAY=2, ... SATURDAY=7
    // Vietnamese week starts Monday: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
    val javaDayOfWeek = calInstance.get(Calendar.DAY_OF_WEEK)
    val startDayOffset = if (javaDayOfWeek == Calendar.SUNDAY) 6 else javaDayOfWeek - 2
    val maxDaysInMonth = calInstance.getActualMaximum(Calendar.DAY_OF_MONTH)

    val todayCalendar = Calendar.getInstance()
    val isCurrentMonth = todayCalendar.get(Calendar.MONTH) == displayCalendar.get(Calendar.MONTH) &&
            todayCalendar.get(Calendar.YEAR) == displayCalendar.get(Calendar.YEAR)
    val todayDayNumber = if (isCurrentMonth) todayCalendar.get(Calendar.DAY_OF_MONTH) else null

    // Default select today if in current month and nothing selected
    if (selectedDayNumber == null && todayDayNumber != null) {
        selectedDayNumber = todayDayNumber
    }

    val selectedDayTransactions = selectedDayNumber?.let { dayMap[it] } ?: emptyList()
    val dayExpense = selectedDayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val dayIncome = selectedDayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Cashflow Summary Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Tổng thu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "+${formatVnd(monthIncome)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Tổng chi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "-${formatVnd(monthExpense)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Còn lại", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${if (monthNetSavings > 0) "+" else ""}${formatVnd(monthNetSavings)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthNetSavings >= 0) Color(0xFF2563EB) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // Calendar Grid Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday headers: T2, T3, T4, T5, T6, T7, CN
                    val weekDays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (wd in weekDays) {
                            Text(
                                text = wd,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (wd == "CN") ExpenseRed.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid cells (Rows of 7 days)
                    val totalCells = startDayOffset + maxDaysInMonth
                    val rowCount = (totalCells + 6) / 7

                    for (row in 0 until rowCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - startDayOffset + 1

                                if (dayNum in 1..maxDaysInMonth) {
                                    val isToday = dayNum == todayDayNumber
                                    val isSelected = dayNum == selectedDayNumber
                                    val txList = dayMap[dayNum] ?: emptyList()
                                    val dayExp = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                                    val dayInc = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                                    CalendarDayCell(
                                        dayNumber = dayNum,
                                        isToday = isToday,
                                        isSelected = isSelected,
                                        hasTransactions = txList.isNotEmpty(),
                                        expenseAmount = dayExp,
                                        incomeAmount = dayInc,
                                        onClick = { selectedDayNumber = dayNum },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    // Empty placeholder cell
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Card
        if (selectedDayNumber != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ngày $selectedDayNumber/${displayCalendar.get(Calendar.MONTH) + 1}/${displayCalendar.get(Calendar.YEAR)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayIncome > 0) {
                                    Text(
                                        text = "+${formatVnd(dayIncome)}",
                                        color = IncomeGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                if (dayExpense > 0) {
                                    Text(
                                        text = "-${formatVnd(dayExpense)}",
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (selectedDayTransactions.isEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Không có giao dịch nào trong ngày này.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (tx in selectedDayTransactions) {
                                    val cat = categories.firstOrNull { it.id == tx.categoryId }
                                    val acc = accounts.firstOrNull { it.id == tx.sourceAccountId }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CategoryAvatar(
                                                iconName = cat?.iconName ?: if (tx.type == TransactionType.TRANSFER) "swap_horiz" else "wallet",
                                                colorHex = cat?.colorHex ?: "#0A5C36",
                                                size = 32.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (tx.note.isNotBlank()) tx.note else cat?.name ?: tx.type.displayName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${acc?.name ?: "Ví"} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            TransactionAmountText(
                                                type = tx.type,
                                                amount = tx.amount,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasTransactions: Boolean,
    expenseAmount: Double,
    incomeAmount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellBg = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasTransactions -> MaterialTheme.colorScheme.surface
        else -> Color.Transparent
    }

    val borderModifier = when {
        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        isSelected -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .padding(1.5.dp)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(8.dp))
            .background(cellBg)
            .then(borderModifier)
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day Number
            Text(
                text = dayNumber.toString(),
                fontSize = 11.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            // Financial Indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (incomeAmount > 0) {
                    Text(
                        text = "+${formatCompactVnd(incomeAmount)}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (expenseAmount > 0) {
                    Text(
                        text = "-${formatCompactVnd(expenseAmount)}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
