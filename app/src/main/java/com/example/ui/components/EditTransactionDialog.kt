package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Edits amount, date, note, category and account of an existing transaction. Balances are
 * re-applied by the repository (old effect reverted, new one applied), so editing is safe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var amountInput by remember { mutableStateOf(transaction.amount.toLong().toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var categoryId by remember { mutableStateOf(transaction.categoryId) }
    var accountId by remember { mutableStateOf(transaction.sourceAccountId) }
    var timestamp by remember { mutableLongStateOf(transaction.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val categoryType = when (transaction.type) {
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        else -> null
    }
    val pickableCategories = remember(categories, categoryType) {
        if (categoryType == null) emptyList()
        else categories.filter { it.type == categoryType }
            // Current choice first, then the user's starred categories.
            .sortedWith(compareByDescending<Category> { it.id == transaction.categoryId }.thenByDescending { it.isPriority })
    }

    if (showDatePicker) {
        // DatePicker works in UTC midnight; keep the original time of day when changing the date.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = toUtcMidnight(timestamp))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { timestamp = withDate(timestamp, it) }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa giao dịch", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Số tiền (VNĐ)") },
                    supportingText = { Text(formatVnd(amountInput.toDoubleOrNull() ?: 0.0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ngày: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp)))
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (pickableCategories.isNotEmpty()) {
                    Text("Danh mục", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pickableCategories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text("${cat.emoji ?: ""} ${cat.name}".trim(), fontSize = 12.sp) }
                            )
                        }
                    }
                }
                Text(
                    text = if (transaction.type == TransactionType.INCOME) "Vào tài khoản" else "Từ tài khoản",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accounts.filter { it.id != transaction.destinationAccountId }, key = { it.id }) { acc ->
                        FilterChip(
                            selected = accountId == acc.id,
                            onClick = { accountId = acc.id },
                            label = { Text(acc.name, fontSize = 12.sp) }
                        )
                    }
                }
                error?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFFF453A), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountInput.toDoubleOrNull() ?: 0.0
                if (amount <= 0) {
                    error = "Số tiền phải lớn hơn 0"
                    return@Button
                }
                onSave(
                    transaction.copy(
                        amount = amount,
                        note = note.trim(),
                        categoryId = if (categoryType == null) transaction.categoryId else categoryId,
                        sourceAccountId = accountId,
                        timestamp = timestamp
                    )
                )
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

private fun toUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withDate(originalLocalMillis: Long, pickedUtcMidnight: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcMidnight }
    return Calendar.getInstance().apply {
        timeInMillis = originalLocalMillis
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
