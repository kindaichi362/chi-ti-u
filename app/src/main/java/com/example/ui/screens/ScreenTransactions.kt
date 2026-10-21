package com.example.ui.screens

import com.example.data.model.countsInReports
import com.example.ui.theme.Tone
import com.example.ui.theme.TextSize
import com.example.ui.theme.Space
import com.example.ui.theme.Radius
import com.example.ui.components.compactVnd
import com.example.ui.components.TransactionRow
import com.example.ui.components.RowDivider
import com.example.ui.components.Label
import com.example.ui.components.EmptyState
import com.example.ui.components.AppCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.NaturalLanguageSearchEngine
import com.example.ai.NaturalQueryResult
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.BankLogoBadge
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.components.formatVnd
import com.example.ui.components.getIconForName
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleCyan
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.OledBlack
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScreenTransactions(
    viewModel: FinanceViewModel,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<TransactionType?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showVoiceSheet by remember { mutableStateOf(false) }

    // Natural Language Query check
    val isNaturalQuery = remember(searchQuery) {
        val q = searchQuery.lowercase(Locale.ROOT)
        q.contains("tháng") || q.contains("bao nhiêu") || q.contains("khoản") ||
        q.contains("trên") || q.contains("dưới") || q.contains("tiêu gì") ||
        q.contains("ngày") || q.contains("hôm nay") || q.contains("hôm qua")
    }

    val naturalResult: NaturalQueryResult? = remember(searchQuery, transactions, accounts, categories) {
        if (isNaturalQuery && searchQuery.isNotBlank()) {
            NaturalLanguageSearchEngine.executeQuery(searchQuery, transactions, accounts, categories)
        } else {
            null
        }
    }

    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val accountNames = remember(accounts) { accounts.associate { it.id to it.name } }

    val filteredList = remember(searchQuery, selectedFilterType, transactions, naturalResult, categoryNames, accountNames) {
        if (naturalResult != null) {
            naturalResult.filteredTransactions
        } else {
            transactions.filter { tx ->
                val matchesType = selectedFilterType == null || tx.type == selectedFilterType
                val catName = categoryNames[tx.categoryId] ?: ""
                val accName = accountNames[tx.sourceAccountId] ?: ""
                val matchesSearch = searchQuery.isBlank() ||
                        tx.note.contains(searchQuery, ignoreCase = true) ||
                        catName.contains(searchQuery, ignoreCase = true) ||
                        accName.contains(searchQuery, ignoreCase = true)
                matchesType && matchesSearch
            }
        }
    }

    // Group by Date (computed once per list change, not on every recomposition)
    val grouped = remember(filteredList) {
        val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = dayFormat.format(Date())
        val yesterday = dayFormat.format(Date(System.currentTimeMillis() - 86_400_000L))
        filteredList.groupBy { tx ->
            when (val day = dayFormat.format(Date(tx.date))) {
                today -> "Hôm nay"
                yesterday -> "Hôm qua"
                else -> day
            }
        }
    }

    transactionToEdit?.let { editing ->
        EditTransactionDialog(
            transaction = editing,
            accounts = accounts,
            categories = categories,
            onDismiss = { transactionToEdit = null },
            onDelete = {
                transactionToDelete = editing
                transactionToEdit = null
            },
            onSave = {
                viewModel.updateTransaction(it)
                transactionToEdit = null
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = AppleSurface2,
            title = { Text("Xác nhận xóa giao dịch?", color = AppleTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Số dư tài khoản liên quan sẽ được tự động hoàn lại tương ứng.", color = AppleTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    }
                ) {
                    Text("Xóa", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Hủy", color = AppleTextSecondary)
                }
            }
        )
    }

    val accountByIdMap = remember(accounts) { accounts.associateBy { it.id } }
    val categoryByIdMap = remember(categories) { categories.associateBy { it.id } }
    val filterOptions = listOf<Pair<String, TransactionType?>>(
        "Tất cả" to null, "Chi" to TransactionType.EXPENSE, "Thu" to TransactionType.INCOME, "Chuyển" to TransactionType.TRANSFER
    )

    Box(modifier = modifier.fillMaxSize().background(OledBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Space.page, end = Space.page, top = Space.lg, bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            item {
                Text("Giao dịch", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = AppleTextPrimary)
            }

            // Search: plain text, or a question ("tháng này tiêu ăn uống bao nhiêu")
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm, hoặc hỏi \"tháng này ăn uống bao nhiêu\"", fontSize = TextSize.sm, color = AppleTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppleTextSecondary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm", tint = AppleTextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { showVoiceSheet = true }, modifier = Modifier.testTag("search_voice_btn")) {
                                Icon(Icons.Default.Mic, contentDescription = "Nói để ghi", tint = Tone.accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("transaction_search_field"),
                    shape = Radius.md,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppleSurface1,
                        unfocusedContainerColor = AppleSurface1,
                        focusedBorderColor = Tone.accent,
                        unfocusedBorderColor = Tone.hairline
                    ),
                    singleLine = true
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    filterOptions.forEach { (label, type) ->
                        FilterChip(
                            selected = selectedFilterType == type,
                            onClick = { selectedFilterType = type },
                            label = { Text(label, fontSize = TextSize.sm) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Tone.accent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            if (naturalResult != null) {
                item {
                    AppCard(modifier = Modifier.testTag("natural_search_result_card"), borderColor = Tone.accent.copy(alpha = 0.4f)) {
                        Label("Kết quả", color = Tone.accent)
                        Spacer(Modifier.height(Space.xs))
                        Text(naturalResult.summaryText, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    AppCard {
                        EmptyState(
                            title = if (transactions.isEmpty()) "Chưa có giao dịch" else "Không có giao dịch phù hợp",
                            body = if (transactions.isEmpty()) "Bấm ➕ ở thanh dưới để ghi khoản đầu tiên." else "Thử bỏ bộ lọc hoặc đổi từ khóa.",
                            actionLabel = if (transactions.isEmpty()) "Ghi giao dịch" else null,
                            onAction = if (transactions.isEmpty()) onNavigateToAddTransaction else null
                        )
                    }
                }
            }

            // One card per day, with that day's spending on the header line.
            for ((groupDate, txs) in grouped) {
                item(key = "day_$groupDate") {
                    val daySpend = txs.filter { it.type == TransactionType.EXPENSE && it.countsInReports }.sumOf { it.amount }
                    val dayIncome = txs.filter { it.type == TransactionType.INCOME && it.countsInReports }.sumOf { it.amount }
                    Row(Modifier.fillMaxWidth().padding(top = Space.xs, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        Text(groupDate, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppleTextSecondary, modifier = Modifier.weight(1f))
                        if (dayIncome > 0) Text("+${compactVnd(dayIncome)}  ", fontSize = TextSize.xs, color = Tone.income)
                        if (daySpend > 0) Text("-${compactVnd(daySpend)}", fontSize = TextSize.xs, color = AppleTextSecondary)
                    }
                    AppCard(contentPadding = Space.lg) {
                        txs.forEachIndexed { i, tx ->
                            if (i > 0) RowDivider()
                            Box(Modifier.testTag("tx_item_${tx.id}")) {
                                TransactionRow(
                                    tx = tx,
                                    category = categoryByIdMap[tx.categoryId],
                                    account = accountByIdMap[tx.sourceAccountId],
                                    destination = tx.destinationAccountId?.let { accountByIdMap[it] },
                                    // Adjustments are corrected by reconciling again; tapping one offers deletion.
                                    onClick = { if (tx.type == TransactionType.ADJUSTMENT) transactionToDelete = tx else transactionToEdit = tx }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showVoiceSheet) {
            VoiceTransactionBottomSheet(
                viewModel = viewModel,
                onDismiss = { showVoiceSheet = false },
                onTransactionSaved = { showVoiceSheet = false },
                autoStart = true
            )
        }
    }
}
