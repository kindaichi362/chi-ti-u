package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FinancialAdvisorEngine
import com.example.ai.FinancialSnapshot
import com.example.ai.Insight
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
import com.example.data.model.countsInReports
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FinanceRepository(AppDatabase.getDatabase(application))

    /** "yyyy-MM" of today; re-checked every minute so an app left open rolls over to the new month. */
    private val currentMonthKey: StateFlow<String> = flow {
        while (true) {
            emit(monthKeyOf(System.currentTimeMillis()))
            delay(60_000)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, monthKeyOf(System.currentTimeMillis()))

    init {
        viewModelScope.launch { repository.ensureSeeded() }
        // Budgets repeat every month until the user changes them.
        currentMonthKey.onEach { repository.carryOverBudgets(it) }.launchIn(viewModelScope)
    }

    /** Re-adds any default categories the user is missing; reports how many were added. */
    fun restoreDefaultCategories(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch { onDone(repository.restoreMissingDefaultCategories()) }
    }

    private fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val accounts: StateFlow<List<Account>> = repository.allAccounts.asState(emptyList())
    val categories: StateFlow<List<Category>> = repository.allCategories.asState(emptyList())
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions.asState(emptyList())
    val budgets: StateFlow<List<Budget>> = repository.allBudgets.asState(emptyList())
    val debts: StateFlow<List<Debt>> = repository.allDebts.asState(emptyList())
    val goals: StateFlow<List<Goal>> = repository.allGoals.asState(emptyList())
    val bills: StateFlow<List<RecurringBill>> = repository.activeBills.asState(emptyList())

    /** Only this month's budgets; older months stay in the DB for history but never show as current. */
    val currentMonthBudgets: StateFlow<List<Budget>> = combine(budgets, currentMonthKey) { all, month ->
        all.filter { it.monthYear == month }
    }.asState(emptyList())

    /** This month's transactions that actually moved money (pending / cancelled QR payments excluded). */
    val currentMonthTransactions: StateFlow<List<Transaction>> = combine(transactions, currentMonthKey) { txs, month ->
        txs.filter { it.countsInReports && monthKeyOf(it.timestamp) == month }
    }.asState(emptyList())

    // ---- The month the Overview and Reports tabs are looking at (0 = this month, -1 = last…) ----
    private val _selectedMonthOffset = MutableStateFlow(0)
    val selectedMonthOffset: StateFlow<Int> = _selectedMonthOffset.asStateFlow()
    fun selectMonth(offset: Int) { _selectedMonthOffset.value = offset.coerceAtMost(0) }

    /** "yyyy-MM" of the selected month; budgets set while viewing a month belong to that month. */
    fun selectedMonthKey(): String = Calendar.getInstance().run {
        set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, _selectedMonthOffset.value); monthKeyOf(timeInMillis)
    }

    /** Totals, budgets and categories for the selected month. Past months are computed as of their last day. */
    val financialSnapshot: StateFlow<FinancialSnapshot> = combine(
        combine(accounts, categories, transactions) { a, c, t -> Triple(a, c, t) },
        combine(budgets, goals, debts) { b, g, d -> Triple(b, g, d) },
        combine(_selectedMonthOffset, currentMonthKey) { off, _ -> off }
    ) { (accs, cats, all), (bdgs, gls, dbts), offset ->
        snapshotFor(offset, accs, cats, all, bdgs, gls, dbts)
    }.asState(FinancialAdvisorEngine.computeSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

    /** Always this month — the assistant talks about "now" whatever month a tab is showing. */
    val currentSnapshot: StateFlow<FinancialSnapshot> = combine(
        combine(accounts, categories, transactions) { a, c, t -> Triple(a, c, t) },
        combine(budgets, goals, debts) { b, g, d -> Triple(b, g, d) },
        currentMonthKey
    ) { (accs, cats, all), (bdgs, gls, dbts), _ ->
        snapshotFor(0, accs, cats, all, bdgs, gls, dbts)
    }.asState(FinancialAdvisorEngine.computeSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))

    private fun snapshotFor(
        offset: Int, accs: List<Account>, cats: List<Category>, all: List<Transaction>,
        bdgs: List<Budget>, gls: List<Goal>, dbts: List<Debt>
    ): FinancialSnapshot {
        val now = if (offset == 0) System.currentTimeMillis() else Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset + 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        val key = monthKeyOf(now)
        val monthTx = all.filter { it.countsInReports && monthKeyOf(it.timestamp) == key }
        return FinancialAdvisorEngine.computeSnapshot(accs, cats, monthTx, bdgs.filter { it.monthYear == key }, gls, dbts, allTransactions = all, now = now)
    }

    /** Automatic findings (credit card due, budgets, pace, trends) for the assistant. */
    val insights: StateFlow<List<Insight>> = currentSnapshot.map { FinancialAdvisorEngine.insights(it) }.asState(emptyList())

    // ---- Preferences shared by every screen ----
    private val prefs = application.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

    private val _balancesHidden = MutableStateFlow(prefs.getBoolean("balances_hidden", false))
    /** Hide amounts on Overview and Accounts (one switch for both). */
    val balancesHidden: StateFlow<Boolean> = _balancesHidden.asStateFlow()
    fun toggleBalancesHidden() {
        _balancesHidden.value = !_balancesHidden.value
        prefs.edit().putBoolean("balances_hidden", _balancesHidden.value).apply()
    }

    private val _themeMode = MutableStateFlow(ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    // ---- Transactions tab filters, kept here so they survive switching tabs ----
    data class TxFilter(val query: String = "", val type: TransactionType? = null, val monthOffset: Int? = 0, val accountId: Long? = null) {
        val isDefault: Boolean get() = query.isBlank() && type == null && monthOffset == 0 && accountId == null
    }
    private val _txFilter = MutableStateFlow(TxFilter())
    val txFilter: StateFlow<TxFilter> = _txFilter.asStateFlow()
    fun updateTxFilter(transform: (TxFilter) -> TxFilter) { _txFilter.value = transform(_txFilter.value) }

    // ---- Accounts ----
    val archivedAccounts: StateFlow<List<Account>> = repository.archivedAccounts.asState(emptyList())
    fun unarchiveAccount(id: Long) { viewModelScope.launch { repository.unarchiveAccount(id) } }
    fun transactionsForAccount(accountId: Long): Flow<List<Transaction>> = repository.transactionsForAccount(accountId)

    /** Pays [amount] of a credit card's debt from another account (a transfer, not spending). */
    fun payCreditCard(cardId: Long, fromAccountId: Long, amount: Double) {
        addTransaction(
            type = TransactionType.TRANSFER,
            amount = amount,
            sourceAccountId = fromAccountId,
            destinationAccountId = cardId,
            note = "Thanh toán thẻ tín dụng"
        )
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch { repository.updateAccount(account) }
    }

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

    /** Saves an edited transaction; account balances are recomputed from the old and new values. */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.updateTransaction(transaction) }
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
                    parentId = parentId,
                    isDefault = false
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

    /** [onResult] gets false when the account has history and was kept (archive it instead). */
    fun deleteAccountById(id: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onResult(repository.deleteAccountById(id))
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
        viewModelScope.launch {
            repository.upsertBudget(categoryId, amountLimit, selectedMonthKey())
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

    /** Creates a monthly bill due on [dayOfMonth] (clamped for short months). */
    fun addBill(name: String, amount: Double, accountId: Long, categoryId: Long, dayOfMonth: Int) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val due = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            val firstDue = if (due.before(now)) FinanceRepository.nextDueDateAfter(due.timeInMillis, dayOfMonth) else due.timeInMillis
            repository.insertBill(
                RecurringBill(name = name, amount = amount, accountId = accountId, categoryId = categoryId, dayOfMonth = dayOfMonth, nextDueDate = firstDue)
            )
        }
    }

    fun deleteBill(bill: RecurringBill) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    fun payBill(bill: RecurringBill) {
        viewModelScope.launch {
            repository.payBill(bill)
        }
    }

    fun askAiAssistant(query: String): String {
        return FinancialAdvisorEngine.answerQuery(query, currentSnapshot.value)
    }

    companion object {
        fun monthKeyOf(timestamp: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))
    }
}

enum class ThemeMode(val label: String) { SYSTEM("Theo hệ thống"), LIGHT("Sáng"), DARK("Tối") }
