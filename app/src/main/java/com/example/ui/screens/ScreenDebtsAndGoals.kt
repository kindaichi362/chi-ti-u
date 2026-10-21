package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Goal
import com.example.data.model.RecurringBill
import com.example.ui.components.formatVnd
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenDebtsAndGoals(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val debts by viewModel.debts.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialogs
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var depositingGoal by remember { mutableStateOf<Goal?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var selectedAccountForAction by remember { mutableStateOf<Long?>(null) }

    if (selectedAccountForAction == null && accounts.isNotEmpty()) {
        selectedAccountForAction = accounts.first().id
    }

    // Add Debt Dialog
    if (showAddDebtDialog) {
        var debtType by remember { mutableStateOf(DebtType.OWED_TO_ME) }
        var personName by remember { mutableStateOf("") }
        var debtAmount by remember { mutableStateOf("") }
        var debtNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDebtDialog = false },
            title = { Text("Thêm khoản nợ mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (debtType == DebtType.OWED_TO_ME) IncomeGreen.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtType = DebtType.OWED_TO_ME }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Người khác nợ tôi",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (debtType == DebtType.OWED_TO_ME) FontWeight.Bold else FontWeight.Normal,
                                color = if (debtType == DebtType.OWED_TO_ME) IncomeGreen else Color.DarkGray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (debtType == DebtType.I_OWE) ExpenseRed.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtType = DebtType.I_OWE }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Tôi nợ người khác",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (debtType == DebtType.I_OWE) FontWeight.Bold else FontWeight.Normal,
                                color = if (debtType == DebtType.I_OWE) ExpenseRed else Color.DarkGray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Tên người liên quan") },
                        placeholder = { Text("Ví dụ: Anh Nam, Bạn Hùng...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtAmount,
                        onValueChange = { debtAmount = it },
                        label = { Text("Số tiền (VNĐ)") },
                        placeholder = { Text("Ví dụ: 5000000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = debtNote,
                        onValueChange = { debtNote = it },
                        label = { Text("Ghi chú") },
                        placeholder = { Text("Lý do vay/mượn") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = debtAmount.toDoubleOrNull() ?: 0.0
                        if (personName.isNotBlank() && amt > 0) {
                            viewModel.addDebt(
                                Debt(
                                    type = debtType,
                                    personName = personName,
                                    initialAmount = amt,
                                    remainingAmount = amt,
                                    note = debtNote
                                )
                            )
                            showAddDebtDialog = false
                            Toast.makeText(context, "Đã lưu khoản nợ!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDebtDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetAmt by remember { mutableStateOf("") }
        var initialAmt by remember { mutableStateOf("") }
        var monthlyTarget by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Tạo mục tiêu tài chính", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Tên mục tiêu") },
                        placeholder = { Text("Ví dụ: Mua ô tô, Quỹ dự phòng, Mua nhà...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmt,
                        onValueChange = { targetAmt = it },
                        label = { Text("Số tiền mục tiêu (VNĐ)") },
                        placeholder = { Text("Ví dụ: 500000000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialAmt,
                        onValueChange = { initialAmt = it },
                        label = { Text("Đã có sẵn (VNĐ)") },
                        placeholder = { Text("0") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = monthlyTarget,
                        onValueChange = { monthlyTarget = it },
                        label = { Text("Dự kiến tiết kiệm / tháng (VNĐ)") },
                        placeholder = { Text("Ví dụ: 20000000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = targetAmt.toDoubleOrNull() ?: 0.0
                        val initial = initialAmt.toDoubleOrNull() ?: 0.0
                        val monthly = monthlyTarget.toDoubleOrNull() ?: 0.0
                        if (goalName.isNotBlank() && target > 0) {
                            viewModel.addGoal(goalName, target, initial, monthly)
                            showAddGoalDialog = false
                            Toast.makeText(context, "Đã tạo mục tiêu!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Vui lòng nhập tên và số tiền mục tiêu", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Pay Debt Dialog
    if (payingDebt != null) {
        val debt = payingDebt!!
        AlertDialog(
            onDismissRequest = { payingDebt = null },
            title = { Text("Ghi nhận trả nợ: ${debt.personName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Còn nợ: ${formatVnd(debt.remainingAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Số tiền thanh toán lần này") },
                        placeholder = { Text("Ví dụ: 1000000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        val accId = selectedAccountForAction ?: accounts.firstOrNull()?.id
                        if (amt > 0 && accId != null) {
                            viewModel.recordDebtPayment(debt, amt, accId)
                            payingDebt = null
                            paymentAmountInput = ""
                            Toast.makeText(context, "Đã ghi nhận thanh toán!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { payingDebt = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Deposit to Goal Dialog
    if (depositingGoal != null) {
        val goal = depositingGoal!!
        AlertDialog(
            onDismissRequest = { depositingGoal = null },
            title = { Text("Nạp tiết kiệm: ${goal.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mục tiêu: ${formatVnd(goal.targetAmount)} (Đã có: ${formatVnd(goal.currentAmount)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Số tiền nạp thêm (VNĐ)") },
                        placeholder = { Text("Ví dụ: 5000000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = paymentAmountInput.toDoubleOrNull() ?: 0.0
                        val accId = selectedAccountForAction ?: accounts.firstOrNull()?.id
                        if (amt > 0 && accId != null) {
                            viewModel.depositToGoal(goal, amt, accId)
                            depositingGoal = null
                            paymentAmountInput = ""
                            Toast.makeText(context, "Đã nạp tiền vào mục tiêu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Nạp ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositingGoal = null }) {
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

        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kế hoạch & Sổ Nợ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    if (selectedTabIndex == 0) showAddDebtDialog = true
                    else if (selectedTabIndex == 1) showAddGoalDialog = true
                },
                modifier = Modifier.testTag("add_plan_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (selectedTabIndex) {
                        0 -> "+ Khoản nợ"
                        1 -> "+ Mục tiêu"
                        else -> "+ Hóa đơn"
                    },
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Sổ Nợ (Debts) | Mục tiêu (Goals) | Hóa đơn (Bills)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Sổ Nợ (${debts.count { !it.isSettled }})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Mục tiêu (${goals.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Hóa đơn (${bills.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content for each tab
        when (selectedTabIndex) {
            0 -> { // SỔ NỢ
                val totalOwedToMe = debts.filter { it.type == DebtType.OWED_TO_ME && !it.isSettled }.sumOf { it.remainingAmount }
                val totalIOwe = debts.filter { it.type == DebtType.I_OWE && !it.isSettled }.sumOf { it.remainingAmount }

                // Net debt banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Người khác nợ tôi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatVnd(totalOwedToMe), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Tôi nợ người khác", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatVnd(totalIOwe), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (debts.isEmpty()) {
                    Text(
                        text = "Chưa có khoản nợ nào được ghi nhận.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(debts) { debt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (debt.type == DebtType.OWED_TO_ME) IncomeGreen else ExpenseRed,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = debt.personName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = if (debt.type == DebtType.OWED_TO_ME) "Nợ tôi: ${formatVnd(debt.remainingAmount)}" else "Tôi nợ: ${formatVnd(debt.remainingAmount)}",
                                            fontSize = 12.sp,
                                            color = if (debt.type == DebtType.OWED_TO_ME) IncomeGreen else ExpenseRed,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (debt.note.isNotBlank()) {
                                            Text(text = debt.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (!debt.isSettled) {
                                        Button(
                                            onClick = { payingDebt = debt },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(if (debt.type == DebtType.OWED_TO_ME) "Thu tiền" else "Trả nợ", fontSize = 11.sp)
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = IncomeGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Đã tất toán",
                                                color = IncomeGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> { // MỤC TIÊU TÀI CHÍNH (GOALS)
                if (goals.isEmpty()) {
                    Text(
                        text = "Chưa có mục tiêu nào. Hãy đặt mục tiêu mua xe, mua nhà hoặc quỹ khẩn cấp!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(goals) { goal ->
                            val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100 else 0.0
                            val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                            val monthsLeft = if (goal.monthlyTarget > 0) kotlin.math.ceil(remaining / goal.monthlyTarget).toInt() else 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${String.format("%.0f", pct)}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { (pct / 100).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Đã có: ${formatVnd(goal.currentAmount)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Mục tiêu: ${formatVnd(goal.targetAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (monthsLeft > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "⏱️ Dự kiến hoàn thành sau ~$monthsLeft tháng (với mức tiết kiệm ${formatVnd(goal.monthlyTarget)}/tháng)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { depositingGoal = goal },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nạp tiền vào mục tiêu", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // HÓA ĐƠN ĐỊNH KỲ (BILLS)
                if (bills.isEmpty()) {
                    Text(
                        text = "Chưa có hóa đơn định kỳ nào. Bạn có thể thêm tiền điện, nước, internet...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bills) { bill ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = bill.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "Số tiền: ${formatVnd(bill.amount)} • Hạn: Ngày ${bill.dayOfMonth} hàng tháng",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.payBill(bill)
                                            Toast.makeText(context, "Đã ghi nhận thanh toán hóa đơn!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Đã trả", fontSize = 11.sp)
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
