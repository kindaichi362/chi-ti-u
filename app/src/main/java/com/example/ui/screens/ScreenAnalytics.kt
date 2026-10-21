package com.example.ui.screens

import kotlin.math.roundToInt
import com.example.ui.components.categoryIcon
import com.example.ui.components.compactVnd
import com.example.ui.components.thresholdColor
import com.example.ui.components.ThresholdBar
import com.example.ui.components.IconTile
import com.example.ui.components.RowDivider
import com.example.ui.components.EmptyState
import com.example.ui.components.MonthStepper
import com.example.ui.components.MoneyText
import com.example.ui.theme.MoneyStyle
import com.example.ui.theme.AppPalette
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.Radius
import com.example.ui.theme.Tone
import com.example.ui.theme.TextSize
import com.example.ui.theme.Space
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.Label
import com.example.ui.components.AppCard
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
import com.example.data.model.countsInReports
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

/** One slice of the month spending: a category, the colour of its group and its share. */
private data class CategoryShare(val category: Category?, val name: String, val group: String, val color: Color, val amount: Double)

/**
 * Analysis of the month chosen in the shared month stepper: income / expense / difference,
 * then either spending by category (donut by group + ranked list) or the calendar.
 * Only real data is drawn; an empty month says so and offers the previous month.
 */
@Composable
fun ScreenAnalytics(
    viewModel: FinanceViewModel,
    onOpenBudgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val offset by viewModel.selectedMonthOffset.collectAsState()
    var mode by remember { mutableIntStateOf(0) }

    val displayCalendar = remember(offset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, offset)
        }
    }
    val monthKey = FinanceViewModel.monthKeyOf(displayCalendar.timeInMillis)
    // Pending / cancelled QR payments never moved money, so they stay out of the report.
    val monthTransactions = remember(transactions, monthKey) {
        transactions.filter { it.countsInReports && FinanceViewModel.monthKeyOf(it.timestamp) == monthKey }
    }
    val monthExpense = monthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val monthIncome = monthTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val net = monthIncome - monthExpense

    val shares = remember(monthTransactions, categories) {
        val byId = categories.associateBy { it.id }
        monthTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, txs) ->
                val c = id?.let { byId[it] }
                CategoryShare(c, c?.name ?: "Chưa phân loại", c?.groupName ?: "Khác", parseColorSafe(c?.colorHex ?: "#78716C"), txs.sumOf { it.amount })
            }
            .sortedByDescending { it.amount }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {

        item {
            AppCard(contentPadding = Space.lg) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryFigure("Thu", formatVnd(monthIncome), p.income, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(40.dp).background(p.border))
                    SummaryFigure("Chi", formatVnd(monthExpense), p.expense, Modifier.weight(1f).padding(start = Space.md))
                    Box(Modifier.width(1.dp).height(40.dp).background(p.border))
                    SummaryFigure("Chênh lệch", (if (net > 0) "+" else "") + formatVnd(net), p.textPrimary, Modifier.weight(1f).padding(start = Space.md))
                }
            }
        }

        item {
            SegmentedTabs(options = listOf("Theo danh mục", "Lịch thu chi"), selected = mode, onSelect = { mode = it })
        }

        if (mode == 0) {
            item {
                AppCard(contentPadding = Space.lg) {
                    Text("Chi tiêu theo danh mục", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    if (shares.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.PieChart,
                            title = "Chưa có giao dịch tháng này",
                            body = "Giao dịch mới sẽ tự động cập nhật tại đây.",
                            actionLabel = "Xem tháng trước",
                            onAction = { viewModel.selectMonth(offset - 1) }
                        )
                    } else {
                        Spacer(Modifier.height(Space.lg))
                        GroupDonut(shares, monthExpense)
                        Spacer(Modifier.height(Space.lg))
                        shares.forEachIndexed { i, s ->
                            if (i > 0) RowDivider()
                            ShareRow(s, monthExpense)
                        }
                    }
                }
            }
        } else {
            item {
                FinancialCalendarSection(
                    displayCalendar = displayCalendar,
                    transactions = monthTransactions,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    monthNetSavings = net,
                    categories = categories,
                    accounts = accounts
                )
            }
        }

        // Budget summary for the same month, with a way into the Budget tab.
        item {
            AppCard(onClick = onOpenBudgets, contentPadding = Space.lg) {
                Text("Ngân sách tháng", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                Spacer(Modifier.height(Space.xs))
                if (snapshot.totalBudget > 0) {
                    val ratio = snapshot.totalBudgetSpent / snapshot.totalBudget
                    Row(verticalAlignment = Alignment.Bottom) {
                        MoneyText(formatVnd(snapshot.totalBudget), fontSize = TextSize.figure, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Đã dùng ${(ratio * 100).roundToInt()}%", fontSize = TextSize.sm, color = thresholdColor(ratio))
                    }
                    Spacer(Modifier.height(Space.md))
                    ThresholdBar(ratio)
                    Spacer(Modifier.height(Space.sm))
                    MoneyText("${formatVnd(snapshot.totalBudgetSpent)} / ${formatVnd(snapshot.totalBudget)}", fontSize = TextSize.xs, color = p.textSecondary, fontWeight = FontWeight.Normal)
                } else {
                    Text("Chưa đặt ngân sách cho tháng này.", fontSize = TextSize.sm, color = p.textSecondary)
                }
                Spacer(Modifier.height(Space.sm))
                Text("Quản lý ngân sách →", fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.accent)
            }
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = TextSize.xs, color = AppPalette.textSecondary)
        MoneyText(value, fontSize = TextSize.base, color = color)
    }
}

/** Donut of spending by category group, each slice in the colour of its group. */
@Composable
private fun GroupDonut(shares: List<CategoryShare>, total: Double) {
    val p = AppPalette
    val groups = shares.groupBy { it.group }.map { (g, l) -> Triple(g, l.first().color, l.sumOf { it.amount }) }.sortedByDescending { it.third }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(132.dp)) {
                val stroke = 20.dp.toPx()
                var start = -90f
                val gap = if (groups.size > 1) 2f else 0f
                groups.forEach { (_, color, amount) ->
                    val sweep = (amount / total * 360f).toFloat()
                    drawArc(
                        color = color,
                        startAngle = start + gap / 2,
                        sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(width = stroke),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tổng chi", fontSize = TextSize.xs, color = p.textSecondary)
                MoneyText(compactVnd(total), fontSize = TextSize.base, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.take(5).forEach { (g, color, amount) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(Space.sm))
                    Text(g, fontSize = TextSize.xs, color = p.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${(amount / total * 100).roundToInt()}%", fontSize = TextSize.xs, color = p.textSecondary, style = MoneyStyle, fontWeight = FontWeight.Medium)
                }
            }
            if (groups.size > 5) Text("+${groups.size - 5} nhóm khác", fontSize = TextSize.xs, color = p.textSecondary)
        }
    }
}

@Composable
private fun ShareRow(s: CategoryShare, total: Double) {
    val p = AppPalette
    val ratio = s.amount / total
    Row(Modifier.fillMaxWidth().padding(vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
        IconTile(categoryIcon(s.category), tint = s.color, background = s.color.copy(alpha = 0.14f), size = 36.dp)
        Spacer(Modifier.width(Space.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                MoneyText(formatVnd(s.amount), fontSize = TextSize.sm)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThresholdBar(ratio, color = s.color, height = 5.dp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(Space.sm))
                Text("${(ratio * 100).roundToInt()}%", fontSize = TextSize.xs, color = p.textSecondary, style = MoneyStyle, fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
        }
    }
}


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
