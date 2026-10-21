package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.payment.vietqr.VietnameseBankRegistry
import com.example.ui.components.AccountTypeBadge
import com.example.ui.components.BankLogoBadge
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OledBlack
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenAccounts(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var reconcilingAccount by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var newBalanceInput by remember { mutableStateOf("") }

    val totalBalance = accounts.sumOf { it.balance }

    // Delete Account Confirmation Dialog
    if (accountToDelete != null) {
        val target = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = AppleSurface2,
            title = {
                Text("Xóa tài khoản", color = AppleTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bạn muốn xóa tài khoản \"${target.name}\" (Số dư: ${formatVnd(target.balance)})?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppleTextPrimary
                    )
                    Text(
                        text = "• Xóa vĩnh viễn: Gỡ bỏ tài khoản hoàn toàn khỏi hệ thống.\n• Ngừng sử dụng: Ẩn tài khoản khỏi danh sách nhưng lưu giữ lịch sử.",
                        fontSize = 12.sp,
                        color = AppleTextSecondary
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.archiveAccount(target.id)
                            Toast.makeText(context, "Đã ẩn tài khoản: ${target.name}", Toast.LENGTH_SHORT).show()
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleSurface3)
                    ) {
                        Text("Ngừng dùng", color = AppleOrange)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteAccountById(target.id)
                            Toast.makeText(context, "Đã xóa tài khoản: ${target.name}", Toast.LENGTH_SHORT).show()
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                    ) {
                        Text("Xóa hẳn", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Hủy", color = AppleTextSecondary)
                }
            }
        )
    }

    // Reconcile Balance Dialog
    if (reconcilingAccount != null) {
        val acc = reconcilingAccount!!
        AlertDialog(
            onDismissRequest = { reconcilingAccount = null },
            containerColor = AppleSurface2,
            title = {
                Text("Điều chỉnh số dư: ${acc.name}", color = AppleTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Số dư hiện tại trên app: ${formatVnd(acc.balance)}",
                        fontSize = 13.sp,
                        color = AppleTextSecondary
                    )
                    OutlinedTextField(
                        value = newBalanceInput,
                        onValueChange = { newBalanceInput = it },
                        label = { Text("Số dư thực tế mới (VNĐ)") },
                        placeholder = { Text("Ví dụ: 1200000") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reconcile_balance_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleOrange,
                            unfocusedBorderColor = AppleBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBal = newBalanceInput.toDoubleOrNull()
                        if (newBal == null) {
                            Toast.makeText(context, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.reconcileAccountBalance(acc.id, newBal)
                        Toast.makeText(context, "Đã cập nhật số dư đối soát!", Toast.LENGTH_SHORT).show()
                        reconcilingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
                ) {
                    Text("Xác nhận", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reconcilingAccount = null }) {
                    Text("Hủy", color = AppleTextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tài khoản & Ngân hàng", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppleTextPrimary)
                        Text("${accounts.size} nguồn tiền • Chuẩn kiến trúc", fontSize = 12.sp, color = AppleTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = AppleTextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.testTag("btn_add_account")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm tài khoản", tint = AppleOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        },
        containerColor = OledBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Net Worth / Total Balance Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TỔNG TIỀN CÁC TÀI KHOẢN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AppleTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatVnd(totalBalance),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bao gồm tiền mặt, ngân hàng, ví điện tử & đầu tư",
                            fontSize = 12.sp,
                            color = AppleTextSecondary
                        )
                    }
                }
            }

            // Account Groups by Type
            val grouped = accounts.groupBy { it.type }

            for ((type, accList) in grouped) {
                item {
                    Text(
                        text = "${type.iconEmoji} ${type.displayName} (${accList.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(accList, key = { it.id }) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                        border = BorderStroke(1.dp, AppleBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BankLogoBadge(
                                bankId = acc.bankId,
                                bankCode = acc.bankCode,
                                size = 44.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AppleTextPrimary
                                )

                                if (!acc.accountNumberMasked.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = acc.accountNumberMasked,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AppleTextSecondary
                                    )
                                }

                                if (acc.type == AccountType.CREDIT_CARD && acc.creditLimit > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Hạn mức: ${formatVnd(acc.creditLimit)}",
                                        fontSize = 11.sp,
                                        color = AppleOrange
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatVnd(acc.balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (acc.balance >= 0) AppleGreen else AppleRed
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = {
                                            reconcilingAccount = acc
                                            newBalanceInput = acc.balance.toLong().toString()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppleSurface3
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = AppleOrange,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Đối soát",
                                                fontSize = 11.sp,
                                                color = AppleOrange,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { accountToDelete = acc },
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppleRed.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f)),
                                        modifier = Modifier.testTag("btn_delete_account_${acc.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Xóa tài khoản",
                                                tint = AppleRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Xóa",
                                                fontSize = 11.sp,
                                                color = AppleRed,
                                                fontWeight = FontWeight.SemiBold
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

    // Add Account Dialog
    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAddAccount = { newAcc ->
                viewModel.addAccount(newAcc)
                Toast.makeText(context, "Đã thêm tài khoản: ${newAcc.name}", Toast.LENGTH_SHORT).show()
                showAddAccountDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAddAccount: (Account) -> Unit
) {
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var selectedBankId by remember { mutableStateOf("vcb") }
    var accountName by remember { mutableStateOf("Vietcombank") }
    var initialBalance by remember { mutableStateOf("") }
    var maskedNumber by remember { mutableStateOf("•••• 8899") }
    var creditLimitInput by remember { mutableStateOf("") }

    val popularBanks = VietnameseBankRegistry.BANKS

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface2,
        title = {
            Text("Thêm tài khoản mới", color = AppleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Account Type chips
                Text("Loại tài khoản:", fontSize = 12.sp, color = AppleTextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccountType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                if (type == AccountType.CASH) {
                                    accountName = "Tiền mặt"
                                    selectedBankId = "cash"
                                }
                            },
                            label = { Text("${type.iconEmoji} ${type.displayName}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppleOrange,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                if (selectedType == AccountType.BANK || selectedType == AccountType.CREDIT_CARD || selectedType == AccountType.E_WALLET) {
                    Text("Chọn ngân hàng / ví:", fontSize = 12.sp, color = AppleTextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularBanks.take(8).forEach { bank ->
                            val isSelected = selectedBankId == bank.id
                            Surface(
                                onClick = {
                                    selectedBankId = bank.id
                                    accountName = bank.shortName
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) AppleOrange.copy(alpha = 0.2f) else AppleSurface3,
                                border = BorderStroke(1.dp, if (isSelected) AppleOrange else AppleBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BankLogoBadge(bankId = bank.id, size = 22.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bank.shortName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) AppleOrange else AppleTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Tên tài khoản", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleOrange,
                        unfocusedBorderColor = AppleBorder
                    )
                )

                OutlinedTextField(
                    value = maskedNumber,
                    onValueChange = { maskedNumber = it },
                    label = { Text("Số tài khoản hiển thị (ví dụ: •••• 8899)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleOrange,
                        unfocusedBorderColor = AppleBorder
                    )
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Số dư ban đầu (VNĐ)", fontSize = 12.sp) },
                    placeholder = { Text("Ví dụ: 10000000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleOrange,
                        unfocusedBorderColor = AppleBorder
                    )
                )

                if (selectedType == AccountType.CREDIT_CARD) {
                    OutlinedTextField(
                        value = creditLimitInput,
                        onValueChange = { creditLimitInput = it },
                        label = { Text("Hạn mức thẻ (VNĐ)", fontSize = 12.sp) },
                        placeholder = { Text("Ví dụ: 50000000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleOrange,
                            unfocusedBorderColor = AppleBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = initialBalance.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitInput.toDoubleOrNull() ?: 0.0
                    val bankObj = VietnameseBankRegistry.findById(selectedBankId)

                    val newAccount = Account(
                        name = accountName.ifBlank { "Tài khoản mới" },
                        type = selectedType,
                        balance = bal,
                        bankId = selectedBankId,
                        bankCode = bankObj?.shortName?.take(3)?.uppercase(),
                        accountNumberMasked = maskedNumber.ifBlank { "•••• 0000" },
                        creditLimit = limit,
                        colorHex = bankObj?.brandColorHex ?: "#005C2B"
                    )
                    onAddAccount(newAccount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
            ) {
                Text("Lưu tài khoản", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = AppleTextSecondary)
            }
        }
    )
}
