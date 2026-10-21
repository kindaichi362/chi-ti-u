package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.BudgetStatus
import com.example.data.local.InitialCategories
import com.example.data.model.CategoryType
import com.example.ui.components.AppCard
import com.example.ui.components.CategoryTile
import com.example.ui.components.EmptyState
import com.example.ui.components.HeroCard
import com.example.ui.components.HeroMoney
import com.example.ui.components.MonthStepper
import com.example.ui.components.MoneyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.ThresholdBar
import com.example.ui.components.formatVnd
import com.example.ui.components.thresholdColor
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

/** Budgets of the selected month: total, used, left, then per category with 80% / 100% warnings. */
@Composable
fun ScreenBudgets(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val context = LocalContext.current
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var editing by remember { mutableStateOf<BudgetStatus?>(null) }
    var creating by remember { mutableStateOf(false) }

    val statuses = snapshot.budgetStatuses
    val budgetedIds = statuses.map { it.budget.categoryId }.toSet()

    if (creating || editing != null) {
        val current = editing
        var catId by remember(current) { mutableStateOf(current?.budget?.categoryId) }
        var amount by remember(current) { mutableStateOf(current?.budget?.amountLimit?.toLong()?.toString() ?: "") }
        val choices = remember(categories, budgetedIds) {
            categories.filter { it.type == CategoryType.EXPENSE && (it.id !in budgetedIds || it.id == current?.budget?.categoryId) }
                .sortedWith(compareBy({ InitialCategories.groupOrder.indexOf(it.groupName).let { i -> if (i < 0) 99 else i } }, { it.displayOrder }))
        }
        AlertDialog(
            onDismissRequest = { creating = false; editing = null },
            containerColor = p.card,
            title = { Text(if (current == null) "Đặt ngân sách" else "Sửa ngân sách: ${current.categoryName}", fontWeight = FontWeight.Bold, color = p.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    if (current == null) {
                        Text("Danh mục", fontSize = TextSize.xs, color = p.textSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(choices, key = { it.id }) { cat ->
                                FilterChip(
                                    selected = catId == cat.id,
                                    onClick = { catId = cat.id },
                                    label = { Text(cat.name) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = p.accent, selectedLabelColor = p.onAccent)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit) },
                        label = { Text("Hạn mức mỗi tháng (₫)") },
                        supportingText = { Text(formatVnd(amount.toDoubleOrNull() ?: 0.0)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ngân sách tự lặp lại các tháng sau.", fontSize = TextSize.xs, color = p.textSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = amount.toDoubleOrNull() ?: 0.0
                        val id = catId
                        if (id == null || limit <= 0) {
                            Toast.makeText(context, "Chọn danh mục và nhập hạn mức", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addBudget(id, limit)
                            creating = false; editing = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent)
                ) { Text("Lưu") }
            },
            dismissButton = {
                Row {
                    if (current != null) {
                        TextButton(onClick = { viewModel.deleteBudget(current.budget.id); editing = null }) { Text("Xóa", color = p.danger) }
                    }
                    TextButton(onClick = { creating = false; editing = null }) { Text("Hủy", color = p.textSecondary) }
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        if (statuses.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        icon = Icons.Outlined.Savings,
                        title = "Chưa có ngân sách tháng này",
                        body = "Đặt hạn mức cho vài nhóm chi chính (ăn uống, đi lại, mua sắm) để biết mỗi ngày còn được tiêu bao nhiêu.",
                        actionLabel = "Thiết lập ngân sách",
                        onAction = { creating = true }
                    )
                }
            }
            return@LazyColumn
        }

        item {
            val ratio = snapshot.totalBudgetSpent / snapshot.totalBudget
            val left = snapshot.totalBudget - snapshot.totalBudgetSpent
            HeroCard {
                Text(if (left >= 0) "Còn lại" else "Đã vượt", fontSize = TextSize.sm, color = p.onHeroMuted)
                HeroMoney(formatVnd(kotlin.math.abs(left)))
                Spacer(Modifier.height(Space.md))
                ThresholdBar(
                    ratio,
                    trackColor = p.onHero.copy(alpha = 0.18f),
                    color = if (ratio > 1) p.expenseSoft else if (ratio >= 0.8) p.warning else p.onHero
                )
                Spacer(Modifier.height(Space.md))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Tổng ngân sách", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(formatVnd(snapshot.totalBudget), color = p.onHero)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Đã dùng (${(ratio * 100).roundToInt()}%)", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(formatVnd(snapshot.totalBudgetSpent), color = p.onHero)
                    }
                }
            }
        }

        item {
            SectionHeader("Theo danh mục", action = "Thêm", onAction = { creating = true }, modifier = Modifier.testTag("add_budget_button"))
        }

        items(statuses, key = { it.budget.id }) { s ->
            val cat = categories.firstOrNull { it.id == s.budget.categoryId }
            AppCard(onClick = { editing = s }, contentPadding = Space.lg) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
                    CategoryTile(cat)
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text(s.categoryName, fontSize = TextSize.base, fontWeight = FontWeight.Medium, color = p.textPrimary, maxLines = 1)
                        Text(
                            when {
                                s.remaining < 0 -> "Vượt ${formatVnd(-s.remaining)}"
                                s.ratio >= 0.8 -> "Sắp chạm hạn mức · còn ${formatVnd(s.remaining)}"
                                else -> "Còn ${formatVnd(s.remaining)}"
                            },
                            fontSize = TextSize.xs,
                            color = if (s.ratio >= 0.8) thresholdColor(s.ratio) else p.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${(s.ratio * 100).roundToInt()}%", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = thresholdColor(s.ratio))
                        MoneyText("${formatVnd(s.spent)} / ${formatVnd(s.budget.amountLimit)}", fontSize = TextSize.xs, color = p.textSecondary, fontWeight = FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(Space.md))
                ThresholdBar(s.ratio)
            }
        }
    }
}

/** Reports: Analysis / Budget, both for the month picked in the stepper (shared with Overview). */
@Composable
fun ScreenReports(
    viewModel: FinanceViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val offset by viewModel.selectedMonthOffset.collectAsState()
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.page, end = Space.sm, top = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Báo cáo", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = p.textPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenCategories) { Text("Danh mục", color = p.accent, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold) }
        }
        SegmentedTabs(
            options = listOf("Phân tích", "Ngân sách"),
            selected = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(horizontal = Space.page, vertical = Space.sm)
        )
        MonthStepper(selectedOffset = offset, onSelect = viewModel::selectMonth, modifier = Modifier.padding(horizontal = Space.sm))
        when (selectedTab) {
            0 -> ScreenAnalytics(viewModel = viewModel, onOpenBudgets = { onSelectTab(1) })
            else -> ScreenBudgets(viewModel = viewModel)
        }
    }
}

