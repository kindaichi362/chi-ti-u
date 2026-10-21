package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FinancialAdvisorEngine
import com.example.ai.FinancialSnapshot
import com.example.ai.ParsedTransaction
import com.example.ai.VietnameseNlpParser
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = FinanceRepository(
        accountDao = database.accountDao(),
        categoryDao = database.categoryDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        debtDao = database.debtDao(),
        goalDao = database.goalDao(),
        recurringBillDao = database.recurringBillDao()
    )

    init {
        viewModelScope.launch {
            // Automatically ensure all 100+ default categories exist in database
            val existingNames = repository.getAllCategoryNames().toSet()
            val fullList = com.example.data.local.InitialCategories.getDefaultCategories()
            val missing = fullList.filter { it.name !in existingNames }
            if (missing.isNotEmpty()) {
                repository.insertCategories(missing)
            }
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            val fullList = com.example.data.local.InitialCategories.getDefaultCategories()
            repository.insertCategories(fullList)
        }
    }

    val accounts: StateFlow<List<Account>> = repository.allAccounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val budgets: StateFlow<List<Budget>> = repository.allBudgets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val debts: StateFlow<List<Debt>> = repository.allDebts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val bills: StateFlow<List<RecurringBill>> = repository.activeBills.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Current month timestamp boundaries
    private fun getMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    val currentMonthTransactions: StateFlow<List<Transaction>> = transactions.combine(
        MutableStateFlow(Unit)
    ) { txs, _ ->
        val (start, end) = getMonthRange()
        txs.filter { it.timestamp in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSnapshot: StateFlow<FinancialSnapshot> = combine(
        combine(accounts, categories, currentMonthTransactions) { a, c, t -> Triple(a, c, t) },
        combine(budgets, goals, debts) { b, g, d -> Triple(b, g, d) }
    ) { (accs, cats, txs), (bdgs, gls, dbts) ->
        FinancialAdvisorEngine.computeSnapshot(accs, cats, txs, bdgs, gls, dbts)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialAdvisorEngine.computeSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )

    fun parseNaturalLanguage(input: String): ParsedTransaction? {
        return VietnameseNlpParser.parse(input, accounts.value, categories.value)
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        sourceAccountId: Long,
        destinationAccountId: Long? = null,
        categoryId: Long? = null,
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        qrBankBin: String? = null,
        qrAccountNumber: String? = null,
        qrBeneficiaryName: String? = null,
        qrContent: String? = null
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destinationAccountId,
                    categoryId = categoryId,
                    note = note,
                    timestamp = timestamp,
                    paymentStatus = paymentStatus,
                    qrBankBin = qrBankBin,
                    qrAccountNumber = qrAccountNumber,
                    qrBeneficiaryName = qrBeneficiaryName,
                    qrContent = qrContent
                )
            )
        }
    }

    private val _activePendingPaymentId = MutableStateFlow<Long?>(null)
    val activePendingPaymentId: StateFlow<Long?> = _activePendingPaymentId.asStateFlow()

    fun createPendingQrPayment(
        amount: Double,
        sourceAccountId: Long,
        categoryId: Long?,
        note: String,
        bankBin: String?,
        accountNumber: String?,
        beneficiaryName: String?,
        qrContent: String?,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                type = TransactionType.EXPENSE,
                amount = amount,
                sourceAccountId = sourceAccountId,
                categoryId = categoryId,
                note = note,
                payeeOrPayer = beneficiaryName ?: "",
                paymentStatus = PaymentStatus.PENDING_PAYMENT,
                qrBankBin = bankBin,
                qrAccountNumber = accountNumber,
                qrBeneficiaryName = beneficiaryName,
                qrContent = qrContent,
                timestamp = System.currentTimeMillis()
            )
            val newId = repository.addTransaction(tx)
            _activePendingPaymentId.value = newId
            onCreated(newId)
        }
    }

    fun confirmQrPayment(transactionId: Long) {
        viewModelScope.launch {
            repository.confirmTransactionPayment(transactionId)
            if (_activePendingPaymentId.value == transactionId) {
                _activePendingPaymentId.value = null
            }
        }
    }

    fun cancelQrPayment(transactionId: Long) {
        viewModelScope.launch {
            repository.cancelTransactionPayment(transactionId)
            if (_activePendingPaymentId.value == transactionId) {
                _activePendingPaymentId.value = null
            }
        }
    }

    fun dismissPendingPaymentPrompt() {
        _activePendingPaymentId.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addCategory(
        name: String,
        type: CategoryType,
        iconName: String = "folder",
        colorHex: String = "#EF4444",
        emoji: String? = null,
        parentId: Long? = null
    ) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(
                    name = name,
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    emoji = emoji,
                    parentId = parentId
                )
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun reconcileAccountBalance(accountId: Long, targetBalance: Double, reason: String = "Điều chỉnh số dư") {
        viewModelScope.launch {
            repository.reconcileAccountBalance(accountId, targetBalance, reason)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccountById(account.id)
        }
    }

    fun deleteAccountById(id: Long) {
        viewModelScope.launch {
            repository.deleteAccountById(id)
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            repository.archiveAccount(id)
        }
    }

    fun toggleCategoryPriority(categoryId: Long, isPriority: Boolean) {
        viewModelScope.launch {
            repository.setCategoryPriority(categoryId, isPriority)
        }
    }

    fun addBudget(categoryId: Long, amountLimit: Double) {
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        viewModelScope.launch {
            repository.insertBudget(
                Budget(
                    categoryId = categoryId,
                    amountLimit = amountLimit,
                    monthYear = currentMonth
                )
            )
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }

    fun addDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt)
        }
    }

    fun recordDebtPayment(debt: Debt, amount: Double, accountId: Long) {
        viewModelScope.launch {
            repository.recordDebtPayment(debt, amount, accountId)
        }
    }

    fun addGoal(name: String, targetAmount: Double, initialAmount: Double = 0.0, monthlyTarget: Double = 0.0) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = initialAmount,
                    monthlyTarget = monthlyTarget
                )
            )
        }
    }

    fun depositToGoal(goal: Goal, amount: Double, sourceAccountId: Long) {
        viewModelScope.launch {
            repository.depositToGoal(goal, amount, sourceAccountId)
        }
    }

    fun payBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.payBill(bill)
        }
    }

    fun askAiAssistant(query: String): String {
        return FinancialAdvisorEngine.answerQuery(query, financialSnapshot.value)
    }
}
