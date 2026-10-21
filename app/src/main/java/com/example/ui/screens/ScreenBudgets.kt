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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.data.model.CategoryType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.formatVnd
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenBudgets(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val budgets by viewModel.budgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snapshot by viewModel.financialSnapshot.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var limitInput by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
    val unbudgetedCategories = expenseCategories.filter { cat -> budgets.none { it.categoryId == cat.id } }

    val totalBudgetLimit = budgets.sumOf { it.amountLimit }
    val totalBudgetSpent = budgets.sumOf { b ->
        val cat = categories.firstOrNull { it.id == b.categoryId }
        snapshot.categoryExpenses[cat?.name] ?: 0.0
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tạo ngân sách tháng", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        val currentSelected = expenseCategories.firstOrNull { it.id == selectedCatId }
                        OutlinedTextField(
                            value = currentSelected?.name ?: "Chọn danh mục",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Danh mục chi tiêu") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Hạn mức (VNĐ)") },
                        placeholder = { Text("Ví dụ: 5000000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val catId = selectedCatId ?: unbudgetedCategories.firstOrNull()?.id
                        val limit = limitInput.toDoubleOrNull() ?: 0.0
                        if (catId != null && limit > 0) {
                            viewModel.addBudget(catId, limit)
                            showAddDialog = false
                            limitInput = ""
                            Toast.makeText(context, "Đã lưu ngân sách!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Vui lòng nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ngân sách chi tiêu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(Date()).replaceFirstChar { it.titlecase() },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    selectedCatId = unbudgetedCategories.firstOrNull()?.id
                    showAddDialog = true
                },
                modifier = Modifier.testTag("add_budget_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Budget Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tổng ngân sách", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val overallPct = if (totalBudgetLimit > 0) (totalBudgetSpent / totalBudgetLimit) * 100 else 0.0
                    Text(
                        text = "Đã dùng: ${String.format("%.1f", overallPct)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overallPct > 100) ExpenseRed else if (overallPct > 70) WarningAmber else IncomeGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatVnd(totalBudgetSpent),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ ${formatVnd(totalBudgetLimit)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val progress = if (totalBudgetLimit > 0) (totalBudgetSpent / totalBudgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 1f) ExpenseRed else if (progress >= 0.7f) WarningAmber else IncomeGreen,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bạn chưa đặt ngân sách nào",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets) { budget ->
                    val cat = categories.firstOrNull { it.id == budget.categoryId }
                    val spent = snapshot.categoryExpenses[cat?.name] ?: 0.0
                    val remaining = budget.amountLimit - spent
                    val pct = if (budget.amountLimit > 0) (spent / budget.amountLimit) * 100 else 0.0
                    val progressVal = (spent / budget.amountLimit).toFloat().coerceIn(0f, 1f)

                    val statusColor = when {
                        pct >= 100 -> ExpenseRed
                        pct >= 70 -> WarningAmber
                        else -> IncomeGreen
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryAvatar(
                                    iconName = cat?.iconName ?: "shopping_cart",
                                    colorHex = cat?.colorHex ?: "#0A5C36",
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat?.name ?: "Danh mục",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Hạn mức: ${formatVnd(budget.amountLimit)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${String.format("%.0f", pct)}%",
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBudget(budget.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Xóa",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { progressVal },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = statusColor,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Đã tiêu: ${formatVnd(spent)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (remaining >= 0) "Còn lại: ${formatVnd(remaining)}" else "Vượt: ${formatVnd(-remaining)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (remaining < 0) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
