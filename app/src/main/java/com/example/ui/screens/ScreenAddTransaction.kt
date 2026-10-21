package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ParsedTransaction
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.components.formatVnd
import com.example.ui.components.getIconForName
import com.example.ui.components.parseColorSafe
import com.example.ui.screens.paytrack.PayTrackBottomSheet
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenAddTransaction(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var rawAmountInput by remember { mutableStateOf("") }
    var currentAmount by remember { mutableDoubleStateOf(0.0) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedSourceAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedDestAccountId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var showPayTrackSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }

    // AI Natural Language input state
    var naturalLanguageInput by remember { mutableStateOf("") }
    var parsedPreview by remember { mutableStateOf<ParsedTransaction?>(null) }

    // Default accounts
    if (selectedSourceAccountId == null && accounts.isNotEmpty()) {
        selectedSourceAccountId = accounts.first().id
    }
    if (selectedDestAccountId == null && accounts.size > 1) {
        selectedDestAccountId = accounts[1].id
    }

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.INCOME) it.type == CategoryType.INCOME else it.type == CategoryType.EXPENSE
    }
    if (selectedCategoryId == null && filteredCategories.isNotEmpty() && selectedType != TransactionType.TRANSFER) {
        selectedCategoryId = filteredCategories.first().id
    }

    fun applyParsedTransaction(parsed: ParsedTransaction) {
        selectedType = parsed.type
        currentAmount = parsed.amount
        rawAmountInput = parsed.amount.toLong().toString()
        if (parsed.categoryId != null) selectedCategoryId = parsed.categoryId
        selectedSourceAccountId = parsed.sourceAccountId
        if (parsed.destinationAccountId != null) selectedDestAccountId = parsed.destinationAccountId
        note = parsed.note
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghi nhận giao dịch", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showVoiceSheet = true },
                        modifier = Modifier.testTag("btn_voice_add_screen")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Nhập giọng nói",
                            tint = AppleOrange
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                ) {
                    if (selectedType == TransactionType.EXPENSE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (currentAmount <= 0) {
                                        Toast.makeText(context, "Vui lòng nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val srcId = selectedSourceAccountId
                                    if (srcId == null) {
                                        Toast.makeText(context, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addTransaction(
                                        type = TransactionType.EXPENSE,
                                        amount = currentAmount,
                                        sourceAccountId = srcId,
                                        categoryId = selectedCategoryId,
                                        note = note,
                                        paymentStatus = PaymentStatus.PAID
                                    )
                                    Toast.makeText(context, "Đã lưu giao dịch: ${formatVnd(currentAmount)}!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("save_transaction_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentAmount > 0) "Lưu chi (${formatCompactVnd(currentAmount)})" else "Xác nhận lưu",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = {
                                    if (currentAmount <= 0) {
                                        Toast.makeText(context, "Vui lòng nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val srcId = selectedSourceAccountId
                                    if (srcId == null) {
                                        Toast.makeText(context, "Vui lòng chọn ví/tài khoản nguồn", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showPayTrackSheet = true
                                },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(50.dp)
                                    .testTag("btn_pay_track_qr"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Quét QR",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentAmount <= 0) {
                                    Toast.makeText(context, "Vui lòng nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val srcId = selectedSourceAccountId
                                if (srcId == null) {
                                    Toast.makeText(context, "Vui lòng chọn tài khoản", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedType == TransactionType.TRANSFER && selectedDestAccountId == null) {
                                    Toast.makeText(context, "Vui lòng chọn tài khoản đích", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addTransaction(
                                    type = selectedType,
                                    amount = currentAmount,
                                    sourceAccountId = srcId,
                                    destinationAccountId = if (selectedType == TransactionType.TRANSFER) selectedDestAccountId else null,
                                    categoryId = if (selectedType != TransactionType.TRANSFER) selectedCategoryId else null,
                                    note = note,
                                    paymentStatus = PaymentStatus.PAID
                                )
                                Toast.makeText(context, "Đã lưu giao dịch: ${formatVnd(currentAmount)}!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_transaction_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType == TransactionType.INCOME) IncomeGreen else TransferBlue
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Xác nhận lưu (${formatVnd(currentAmount)})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Transaction Type Tabs: Chi tiêu | Thu nhập | Chuyển tiền
            item {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                val selectedIndex = types.indexOf(selectedType).coerceAtLeast(0)

                TabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    types.forEachIndexed { index, type ->
                        val isSelected = selectedIndex == index
                        val activeColor = when (type) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Tab(
                            selected = isSelected,
                            onClick = {
                                selectedType = type
                                if (type == TransactionType.INCOME) {
                                    val incCat = categories.firstOrNull { it.type == CategoryType.INCOME }
                                    selectedCategoryId = incCat?.id
                                } else if (type == TransactionType.EXPENSE) {
                                    val expCat = categories.firstOrNull { it.type == CategoryType.EXPENSE }
                                    selectedCategoryId = expCat?.id
                                }
                            },
                            text = {
                                Text(
                                    text = type.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // 3. Big Amount Display & Quick Increment Chips
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SỐ TIỀN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val displayColor = when (selectedType) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        // Editable Amount Input Field (Allows typing any custom amount directly)
                        OutlinedTextField(
                            value = rawAmountInput,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                rawAmountInput = digits
                                currentAmount = digits.toDoubleOrNull() ?: 0.0
                            },
                            placeholder = {
                                Text(
                                    text = "0",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = displayColor,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = {
                                Text("₫", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = displayColor)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("amount_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (currentAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bằng chữ: ${formatVnd(currentAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = displayColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Increment chips: +20k, +50k, +100k, +200k, +500k, +1tr, Clear
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val increments = listOf(
                                "+20k" to 20000.0,
                                "+50k" to 50000.0,
                                "+100k" to 100000.0,
                                "+200k" to 200000.0,
                                "+500k" to 500000.0,
                                "+1tr" to 1000000.0
                            )

                            for ((label, inc) in increments) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .clickable {
                                            currentAmount += inc
                                            rawAmountInput = currentAmount.toLong().toString()
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ExpenseRed.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .clickable {
                                        currentAmount = 0.0
                                        rawAmountInput = ""
                                    }
                            ) {
                                Text(
                                    text = "Xóa",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Category Selector (Only for Expense & Income) - ⭐ Danh mục ưu tiên (Bao gồm cả các mục nhỏ con)
            if (selectedType != TransactionType.TRANSFER) {
                item {
                    val priorityCats = filteredCategories.filter { it.isPriority }
                    val displayPriorityCats = if (priorityCats.isNotEmpty()) {
                        priorityCats
                    } else {
                        // Gợi ý ưu tiên mặc định bao gồm các mục nhỏ thiết thực hàng ngày
                        val defaultKeywords = listOf("Ăn sáng", "Ăn trưa", "Ăn tối", "Cà phê", "Trà sữa", "Xăng xe", "Mua thực phẩm", "Đi chợ", "Lương", "Thưởng")
                        val matched = filteredCategories.filter { cat -> defaultKeywords.any { kw -> cat.name.contains(kw, ignoreCase = true) } }
                        if (matched.isNotEmpty()) matched else filteredCategories.take(8)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⭐ Danh mục ưu tiên",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppleOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${displayPriorityCats.size} mục",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (showAllCategories) "Thu gọn ▲" else "Tất cả (${filteredCategories.size}) ▼",
                            color = AppleOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showAllCategories = !showAllCategories }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Priority Categories in a responsive flow
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        displayPriorityCats.forEach { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = cat.id },
                                label = {
                                    Text(
                                        text = "${cat.emoji ?: ""} ${cat.name}".trim(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getIconForName(cat.iconName),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else parseColorSafe(cat.colorHex),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = parseColorSafe(cat.colorHex),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Expandable other categories organized by Group with ⭐ toggle on every category/subcategory
                    AnimatedVisibility(visible = showAllCategories) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val groups = filteredCategories.groupBy { it.groupName ?: "Khác" }
                            groups.forEach { (groupName, catList) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = groupName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            catList.forEach { cat ->
                                                val isSelected = selectedCategoryId == cat.id
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { selectedCategoryId = cat.id },
                                                    label = {
                                                        Text(
                                                            text = "${cat.emoji ?: ""} ${cat.name}".trim(),
                                                            fontSize = 11.sp
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = if (cat.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                                                            contentDescription = "Ưu tiên",
                                                            tint = if (cat.isPriority) AppleOrange else Color.Gray.copy(alpha = 0.4f),
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable {
                                                                    viewModel.toggleCategoryPriority(cat.id, !cat.isPriority)
                                                                }
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = parseColorSafe(cat.colorHex),
                                                        selectedLabelColor = Color.White
                                                    )
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

            // 5. Source Account Picker
            item {
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "Từ tài khoản (Nguồn)" else "Tài khoản / Ví",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts) { acc ->
                        val isSelected = selectedSourceAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSourceAccountId = acc.id },
                            label = { Text(acc.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getIconForName(acc.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // 6. Destination Account Picker (Only for Transfer)
            if (selectedType == TransactionType.TRANSFER) {
                item {
                    Text(
                        text = "Đến tài khoản (Đích)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TransferBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accounts.filter { it.id != selectedSourceAccountId }) { acc ->
                            val isSelected = selectedDestAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDestAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getIconForName(acc.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 7. Note TextField
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú / Người nhận") },
                    placeholder = { Text("Ví dụ: Ăn bún chả với đồng nghiệp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Pay & Track QR Bottom Sheet Flow
        if (showPayTrackSheet) {
            val selectedCat = categories.firstOrNull { it.id == selectedCategoryId }
            val selectedAcc = accounts.firstOrNull { it.id == selectedSourceAccountId }

            PayTrackBottomSheet(
                initialAmount = currentAmount,
                initialNote = note,
                selectedCategory = selectedCat,
                selectedAccount = selectedAcc,
                accounts = accounts,
                onDismiss = { showPayTrackSheet = false },
                onPaymentConfirmed = { amount, noteText, catId, accId, qr ->
                    viewModel.addTransaction(
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        sourceAccountId = accId,
                        categoryId = catId,
                        note = noteText,
                        paymentStatus = PaymentStatus.PAID,
                        qrBankBin = qr.bankBin,
                        qrAccountNumber = qr.accountNumber,
                        qrBeneficiaryName = qr.beneficiaryName,
                        qrContent = qr.transferContent
                    )
                    Toast.makeText(context, "Đã thanh toán & ghi nhận vào sổ chi tiêu!", Toast.LENGTH_LONG).show()
                    showPayTrackSheet = false
                    onNavigateBack()
                },
                onPaymentPendingSaved = { amount, noteText, catId, accId, qr ->
                    viewModel.addTransaction(
                        type = TransactionType.EXPENSE,
                        amount = amount,
                        sourceAccountId = accId,
                        categoryId = catId,
                        note = noteText,
                        paymentStatus = PaymentStatus.PENDING_PAYMENT,
                        qrBankBin = qr.bankBin,
                        qrAccountNumber = qr.accountNumber,
                        qrBeneficiaryName = qr.beneficiaryName,
                        qrContent = qr.transferContent
                    )
                    Toast.makeText(context, "Đã lưu tạm: Chờ thanh toán!", Toast.LENGTH_LONG).show()
                    showPayTrackSheet = false
                    onNavigateBack()
                }
            )
        }

        if (showVoiceSheet) {
            VoiceTransactionBottomSheet(
                viewModel = viewModel,
                onDismiss = { showVoiceSheet = false },
                onTransactionSaved = {
                    showVoiceSheet = false
                    onNavigateBack()
                },
                autoStart = true
            )
        }
    }
}
