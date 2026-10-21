package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.NaturalLanguageSearchEngine
import com.example.ai.NaturalQueryResult
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
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

    val filteredList = remember(searchQuery, selectedFilterType, transactions, naturalResult) {
        if (naturalResult != null) {
            naturalResult.filteredTransactions
        } else {
            transactions.filter { tx ->
                val matchesType = selectedFilterType == null || tx.type == selectedFilterType
                val catName = categories.firstOrNull { it.id == tx.categoryId }?.name ?: ""
                val accName = accounts.firstOrNull { it.id == tx.sourceAccountId }?.name ?: ""
                val matchesSearch = searchQuery.isBlank() ||
                        tx.note.contains(searchQuery, ignoreCase = true) ||
                        catName.contains(searchQuery, ignoreCase = true) ||
                        accName.contains(searchQuery, ignoreCase = true)
                matchesType && matchesSearch
            }
        }
    }

    // Group by Date
    val grouped = filteredList.groupBy { tx ->
        val cal = Calendar.getInstance()
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        cal.timeInMillis = tx.date
        val txDay = cal.get(Calendar.DAY_OF_YEAR)
        val txYear = cal.get(Calendar.YEAR)

        when {
            todayYear == txYear && todayDay == txDay -> "Hôm nay"
            todayYear == txYear && todayDay - txDay == 1 -> "Hôm qua"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(tx.date))
        }
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

    Box(modifier = modifier.fillMaxSize().background(OledBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lịch sử giao dịch",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                    Text(
                        text = "Tìm kiếm tự nhiên • Sổ dòng tiền",
                        fontSize = 12.sp,
                        color = AppleTextSecondary
                    )
                }

                Surface(
                    onClick = onNavigateToAddTransaction,
                    shape = CircleShape,
                    color = AppleSurface2,
                    border = BorderStroke(1.dp, AppleBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm giao dịch",
                            tint = AppleOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Natural Language Search Bar with Microphone button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Hỏi AI: \"Tháng này tiêu ăn uống bao nhiêu?\"",
                        fontSize = 12.sp,
                        color = AppleTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Tìm kiếm",
                        tint = AppleTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Xóa tìm kiếm",
                                    tint = AppleTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showVoiceSheet = true },
                            modifier = Modifier.testTag("search_voice_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Nói để tìm",
                                tint = AppleOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppleSurface1,
                    unfocusedContainerColor = AppleSurface1,
                    focusedBorderColor = AppleOrange,
                    unfocusedBorderColor = AppleBorder
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Natural Language Query Quick Suggestions Chips (Requirement 12)
            val sampleQueries = listOf(
                "Tháng này tôi tiêu ăn uống bao nhiêu?",
                "Những khoản trên 1 triệu tháng này?",
                "Tiền xăng tháng trước?",
                "Hôm nay tôi tiêu gì?"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (sq in sampleQueries) {
                    Surface(
                        onClick = { searchQuery = sq },
                        shape = RoundedCornerShape(12.dp),
                        color = AppleSurface2,
                        border = BorderStroke(1.dp, AppleBorder),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AppleOrange,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sq,
                                fontSize = 11.sp,
                                color = AppleTextPrimary
                            )
                        }
                    }
                }
            }

            // Natural Query AI Answer Card
            if (naturalResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("natural_search_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface2),
                    border = BorderStroke(1.dp, AppleOrange.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AppleOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kết quả phân tích dữ liệu thực tế",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = naturalResult.summaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Standard Filter Chips: Tất cả | Chi | Thu | Chuyển
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterType == null,
                    onClick = { selectedFilterType = null },
                    label = { Text("Tất cả (${transactions.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleOrange,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.EXPENSE,
                    onClick = { selectedFilterType = TransactionType.EXPENSE },
                    label = { Text("Chi tiêu", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleRed,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.INCOME,
                    onClick = { selectedFilterType = TransactionType.INCOME },
                    label = { Text("Thu nhập", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleGreen,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilterType == TransactionType.TRANSFER,
                    onClick = { selectedFilterType = TransactionType.TRANSFER },
                    label = { Text("Chuyển tiền", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleCyan,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timeline Transaction List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Không có giao dịch nào phù hợp", color = AppleTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    for ((groupDate, txs) in grouped) {
                        item {
                            Text(
                                text = groupDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppleTextSecondary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(txs, key = { it.id }) { tx ->
                            val category = categories.firstOrNull { it.id == tx.categoryId }
                            val account = accounts.firstOrNull { it.id == tx.sourceAccountId }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tx_item_${tx.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                                border = BorderStroke(1.dp, AppleBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category Avatar / Emoji
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(parseColorSafe(category?.colorHex ?: "#EF4444").copy(alpha = 0.16f))
                                            .border(1.dp, parseColorSafe(category?.colorHex ?: "#EF4444").copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!category?.emoji.isNullOrBlank()) {
                                            Text(text = category!!.emoji!!, fontSize = 20.sp)
                                        } else {
                                            Icon(
                                                imageVector = getIconForName(category?.iconName ?: "folder"),
                                                contentDescription = null,
                                                tint = parseColorSafe(category?.colorHex ?: "#EF4444"),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (tx.note.isNotBlank()) tx.note else (category?.name ?: "Giao dịch"),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = AppleTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.date))
                                            Text(text = timeStr, fontSize = 12.sp, color = AppleTextSecondary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "•", fontSize = 10.sp, color = AppleTextSecondary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (account != null) {
                                                BankLogoBadge(bankId = account.bankId, bankCode = account.bankCode, size = 18.dp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = account.name,
                                                    fontSize = 12.sp,
                                                    color = AppleTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val prefix = when (tx.type) {
                                            TransactionType.EXPENSE -> "-"
                                            TransactionType.INCOME -> "+"
                                            TransactionType.TRANSFER -> "⇄ "
                                            else -> ""
                                        }
                                        val color = when (tx.type) {
                                            TransactionType.EXPENSE -> AppleRed
                                            TransactionType.INCOME -> AppleGreen
                                            TransactionType.TRANSFER -> AppleCyan
                                            else -> AppleTextPrimary
                                        }

                                        Text(
                                            text = "$prefix${formatVnd(tx.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = color
                                        )

                                        if (tx.paymentStatus == PaymentStatus.PENDING_PAYMENT) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Chờ QR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleOrange
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { transactionToDelete = tx },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa",
                                            tint = AppleTextSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
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
