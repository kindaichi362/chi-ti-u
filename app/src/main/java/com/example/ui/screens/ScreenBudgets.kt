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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.BudgetStatus
import com.example.data.local.InitialCategories
import com.example.data.model.CategoryType
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyState
import com.example.ui.components.IconTile
import com.example.ui.components.Label
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.ThresholdBar
import com.example.ui.components.compactVnd
import com.example.ui.components.formatVnd
import com.example.ui.components.parseColorSafe
import com.example.ui.components.thresholdColor
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import com.example.ui.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

/** Monthly budgets: how much is left overall and per category, coloured at 70 / 90 / 100%. */
@Composable
fun ScreenBudgets(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // null = closed; a status = editing that budget; Unit-like sentinel via -1 id = new budget
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
            title = { Text(if (current == null) "Đặt ngân sách" else "Sửa ngân sách: ${current.categoryName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    if (current == null) {
                        Label("Danh mục")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(choices, key = { it.id }) { cat ->
                                FilterChip(selected = catId == cat.id, onClick = { catId = cat.id }, label = { Text("${cat.emoji ?: ""} ${cat.name}".trim()) })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit) },
                        label = { Text("Hạn mức mỗi tháng (VNĐ)") },
                        supportingText = { Text(formatVnd(amount.toDoubleOrNull() ?: 0.0)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ngân sách tự lặp lại các tháng sau.", fontSize = TextSize.xs, color = AppleTextSecondary)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Tone.accent, contentColor = Color.Black)
                ) { Text("Lưu") }
            },
            dismissButton = {
                Row {
                    if (current != null) {
                        TextButton(onClick = { viewModel.deleteBudget(current.budget.id); editing = null }) { Text("Xóa", color = Tone.danger) }
                    }
                    TextButton(onClick = { creating = false; editing = null }) { Text("Hủy", color = AppleTextSecondary) }
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            AppCard {
                val ratio = if (snapshot.totalBudget > 0) snapshot.totalBudgetSpent / snapshot.totalBudget else 0.0
                Label("Còn lại tháng này")
                Text(formatVnd(snapshot.totalBudget - snapshot.totalBudgetSpent), fontSize = TextSize.figure, fontWeight = FontWeight.Bold,
                    color = if (ratio > 1) Tone.danger else AppleTextPrimary)
                Spacer(Modifier.height(Space.md))
                ThresholdBar(ratio)
                Spacer(Modifier.height(Space.sm))
                Row {
                    Text("Đã chi ${compactVnd(snapshot.totalBudgetSpent)} / ${compactVnd(snapshot.totalBudget)}", fontSize = TextSize.xs, color = AppleTextSecondary, modifier = Modifier.weight(1f))
                    Text("≈ ${compactVnd(snapshot.dailyAllowance)}/ngày · ${snapshot.daysLeft} ngày", fontSize = TextSize.xs, color = AppleTextSecondary)
                }
            }
        }

        item {
            SectionHeader("Theo danh mục", action = "+ Thêm", onAction = { creating = true }, modifier = Modifier.testTag("add_budget_button"))
        }

        if (statuses.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        title = "Chưa có ngân sách",
                        body = "Đặt hạn mức cho vài nhóm chi chính (ăn uống, đi lại, mua sắm) để biết mỗi ngày còn được tiêu bao nhiêu.",
                        actionLabel = "Đặt ngân sách",
                        onAction = { creating = true }
                    )
                }
            }
        }

        items(statuses, key = { it.budget.id }) { s ->
            val cat = categories.firstOrNull { it.id == s.budget.categoryId }
            AppCard(onClick = { editing = s }, contentPadding = Space.lg) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 40.dp)) {
                    IconTile(parseColorSafe(cat?.colorHex ?: "#8E8E93"), emoji = s.emoji ?: "🏷️")
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text(s.categoryName, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = AppleTextPrimary)
                        Text(
                            if (s.remaining >= 0) "Còn ${compactVnd(s.remaining)}" else "Vượt ${compactVnd(-s.remaining)}",
                            fontSize = TextSize.xs,
                            color = if (s.remaining >= 0) AppleTextSecondary else Tone.danger
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${(s.ratio * 100).roundToInt()}%", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = thresholdColor(s.ratio))
                        Text("${compactVnd(s.spent)} / ${compactVnd(s.budget.amountLimit)}", fontSize = TextSize.xs, color = AppleTextSecondary)
                    }
                }
                Spacer(Modifier.height(Space.md))
                ThresholdBar(s.ratio)
            }
        }
    }
}


/** Reports = analysis by month + budgets, one tab each. */
@Composable
fun ScreenReports(
    viewModel: FinanceViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.page, end = Space.page, top = Space.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Báo cáo", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = AppleTextPrimary, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenCategories) { Text("Danh mục", color = Tone.accent, fontSize = TextSize.sm) }
        }
        SegmentedTabs(
            options = listOf("Phân tích", "Ngân sách"),
            selected = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier.padding(horizontal = Space.page, vertical = Space.md)
        )
        when (selectedTab) {
            0 -> ScreenAnalytics(viewModel = viewModel)
            else -> ScreenBudgets(viewModel = viewModel)
        }
    }
}

