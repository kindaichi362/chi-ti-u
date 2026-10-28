package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AccountDao
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetDao
import com.example.data.local.CardInstallmentDao
import com.example.data.local.CategoryDao
import com.example.data.local.DebtDao
import com.example.data.local.GoalDao
import com.example.data.local.InitialCategories
import com.example.data.local.RecurringBillDao
import com.example.data.local.TransactionDao
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Budget
import com.example.data.model.CardInstallment
import com.example.data.model.CategoryType
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Goal
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.nextAmount
import com.example.data.model.nextInterest
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(
    private val database: AppDatabase,
    private val accountDao: AccountDao = database.accountDao(),
    private val categoryDao: CategoryDao = database.categoryDao(),
    private val transactionDao: TransactionDao = database.transactionDao(),
    private val budgetDao: BudgetDao = database.budgetDao(),
    private val debtDao: DebtDao = database.debtDao(),
    private val goalDao: GoalDao = database.goalDao(),
    private val recurringBillDao: RecurringBillDao = database.recurringBillDao(),
    private val cardInstallmentDao: CardInstallmentDao = database.cardInstallmentDao()
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val activeBills: Flow<List<RecurringBill>> = recurringBillDao.getActiveBills()
    val archivedAccounts: Flow<List<Account>> = accountDao.getArchivedAccounts()

    fun installmentsForCard(cardId: Long): Flow<List<CardInstallment>> = cardInstallmentDao.forCard(cardId)

    suspend fun addInstallment(item: CardInstallment): Long {
        require(item.name.isNotBlank() && item.totalAmount.isFinite() && item.totalAmount in 1.0..1_000_000_000_000_000.0 && item.monthCount in 2..60)
        require(item.monthlyAmount == null ||
            (item.monthlyAmount.isFinite() && item.monthlyAmount >= kotlin.math.ceil(item.totalAmount / item.monthCount) && item.monthlyAmount <= 1_000_000_000_000_000.0))
        require(accountDao.getAccountById(item.cardId)?.type == AccountType.CREDIT_CARD)
        return cardInstallmentDao.insert(item)
    }

    suspend fun deleteInstallment(item: CardInstallment) = cardInstallmentDao.delete(item)

    suspend fun updateInstallmentMonthlyAmount(id: Long, monthlyAmount: Double): Boolean = database.withTransaction {
        val item = cardInstallmentDao.byId(id) ?: return@withTransaction false
        if (item.paidMonths >= item.monthCount || !monthlyAmount.isFinite() || monthlyAmount < kotlin.math.ceil(item.totalAmount / item.monthCount) || monthlyAmount > 1_000_000_000_000_000.0) return@withTransaction false
        cardInstallmentDao.update(item.copy(monthlyAmount = monthlyAmount))
        true
    }

    /** A monthly instalment settles existing card debt; it must never be booked as another expense. */
    suspend fun payInstallment(installmentId: Long, fromAccountId: Long): Boolean = database.withTransaction {
        val item = cardInstallmentDao.byId(installmentId) ?: return@withTransaction false
        val card = accountDao.getAccountById(item.cardId) ?: return@withTransaction false
        val source = accountDao.getAccountById(fromAccountId) ?: return@withTransaction false
        if (card.type != AccountType.CREDIT_CARD || source.id == card.id || source.type == AccountType.CREDIT_CARD || item.paidMonths >= item.monthCount) return@withTransaction false
        val amount = item.nextAmount()
        if (amount <= 0) return@withTransaction false
        val interest = item.nextInterest()
        if (interest > 0) {
            val interestCategoryId = categoryDao.getAllCategoriesOnce()
                .firstOrNull { it.name == "Lãi vay / lãi thẻ" && it.type == CategoryType.EXPENSE }?.id
            val interestTx = Transaction(
                type = TransactionType.EXPENSE,
                amount = interest,
                sourceAccountId = card.id,
                categoryId = interestCategoryId,
                note = "Lãi trả góp ${item.name} · kỳ ${item.paidMonths + 1}/${item.monthCount}"
            )
            applyBalanceEffect(interestTx, 1)
            transactionDao.insertTransaction(interestTx)
        }
        val tx = Transaction(
            type = TransactionType.TRANSFER,
            amount = amount,
            sourceAccountId = source.id,
            destinationAccountId = card.id,
            note = "Trả góp ${item.name} · kỳ ${item.paidMonths + 1}/${item.monthCount}"
        )
        applyBalanceEffect(tx, 1)
        transactionDao.insertTransaction(tx)
        cardInstallmentDao.update(item.copy(paidMonths = item.paidMonths + 1))
        true
    }

    fun transactionsForAccount(accountId: Long): Flow<List<Transaction>> = transactionDao.getTransactionsForAccount(accountId)

    suspend fun unarchiveAccount(id: Long) = accountDao.unarchiveAccountById(id)

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(monthYear)

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end)

    /**
     * Seeds default categories and a cash wallet on first run. Runs in one DB transaction so the
     * UI never sees a half-seeded database and a repeated call can't duplicate rows.
     */
    suspend fun ensureSeeded() = database.withTransaction {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(InitialCategories.getDefaultCategories())
        }
        if (accountDao.getAccountCount() == 0) {
            accountDao.insertAccount(
                Account(name = "Tiền mặt", type = AccountType.CASH, balance = 0.0, colorHex = "#10B981", iconName = "payments", bankId = "cash")
            )
        }
    }

    /** Re-adds default categories the user no longer has (matched by name + type). Never duplicates. */
    suspend fun restoreMissingDefaultCategories(): Int = database.withTransaction {
        val existing = categoryDao.getAllCategoriesOnce().map { it.name to it.type }.toSet()
        val missing = InitialCategories.getDefaultCategories().filter { (it.name to it.type) !in existing }
        if (missing.isNotEmpty()) categoryDao.insertCategories(missing)
        missing.size
    }

    /** Applies (sign = 1) or reverts (sign = -1) the balance effect of a paid transaction. */
    private suspend fun applyBalanceEffect(tx: Transaction, sign: Int) {
        if (tx.paymentStatus != PaymentStatus.PAID) return
        val amt = tx.amount * sign
        when (tx.type) {
            TransactionType.EXPENSE -> accountDao.adjustAccountBalance(tx.sourceAccountId, -amt)
            TransactionType.INCOME -> accountDao.adjustAccountBalance(tx.sourceAccountId, amt)
            TransactionType.TRANSFER -> {
                accountDao.adjustAccountBalance(tx.sourceAccountId, -amt)
                tx.destinationAccountId?.let { accountDao.adjustAccountBalance(it, amt) }
            }
            // Adjustments store the signed delta that was applied to the account.
            TransactionType.ADJUSTMENT -> accountDao.adjustAccountBalance(tx.sourceAccountId, adjustmentDelta(tx) * sign)
        }
    }

    /** Returns the new row id, or -1 when the transaction is invalid and nothing was saved. */
    suspend fun addTransaction(transaction: Transaction): Long = database.withTransaction {
        val selfTransfer = transaction.type == TransactionType.TRANSFER &&
            transaction.destinationAccountId == transaction.sourceAccountId
        if (transaction.amount <= 0.0 || selfTransfer) return@withTransaction -1L
        applyBalanceEffect(transaction, 1)
        transactionDao.insertTransaction(transaction)
    }

    /** Replaces an existing transaction: reverts the old balance effect, then applies the new one. */
    suspend fun updateTransaction(updated: Transaction) = database.withTransaction {
        val old = transactionDao.getTransactionById(updated.id) ?: return@withTransaction
        applyBalanceEffect(old, -1)
        applyBalanceEffect(updated, 1)
        transactionDao.updateTransaction(updated)
    }

    suspend fun confirmTransactionPayment(transactionId: Long) = database.withTransaction {
        val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
        if (tx.paymentStatus != PaymentStatus.PAID) {
            val paid = tx.copy(paymentStatus = PaymentStatus.PAID)
            applyBalanceEffect(paid, 1)
            transactionDao.updateTransaction(paid)
        }
    }

    suspend fun cancelTransactionPayment(transactionId: Long) = database.withTransaction {
        val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
        if (tx.paymentStatus == PaymentStatus.CANCELLED) return@withTransaction
        // Cancelling a transaction that was already paid must give its money back.
        applyBalanceEffect(tx, -1)
        transactionDao.updateTransaction(tx.copy(paymentStatus = PaymentStatus.CANCELLED))
    }

    suspend fun deleteTransaction(transaction: Transaction) = database.withTransaction {
        // Re-read so a stale UI copy can't revert the wrong amount or status.
        val tx = transactionDao.getTransactionById(transaction.id) ?: return@withTransaction
        applyBalanceEffect(tx, -1)
        transactionDao.deleteTransaction(tx)
    }

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    /**
     * Hard-deletes an account only when nothing references it; otherwise its history (and the
     * balances of transfer counterparts) would silently break. Returns false when it was kept.
     */
    suspend fun deleteAccountById(id: Long): Boolean = database.withTransaction {
        if (transactionDao.countTransactionsForAccount(id) > 0 || recurringBillDao.countBillsForAccount(id) > 0 || cardInstallmentDao.countForCard(id) > 0) {
            false
        } else {
            accountDao.deleteAccountById(id)
            true
        }
    }

    suspend fun archiveAccount(id: Long) = accountDao.archiveAccountById(id)

    suspend fun reconcileAccountBalance(accountId: Long, targetBalance: Double, reason: String = "Điều chỉnh số dư") = database.withTransaction {
        val account = accountDao.getAccountById(accountId) ?: return@withTransaction
        val diff = targetBalance - account.balance
        if (kotlin.math.abs(diff) > 0.001) {
            accountDao.setAccountBalance(accountId, targetBalance)
            // Signed amount, so deleting the adjustment reverts it exactly.
            transactionDao.insertTransaction(
                Transaction(
                    type = TransactionType.ADJUSTMENT,
                    amount = diff,
                    sourceAccountId = accountId,
                    note = reason,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun setCategoryPriority(categoryId: Long, isPriority: Boolean) =
        categoryDao.setCategoryPriority(categoryId, isPriority)

    suspend fun deleteCategoryIfUnused(category: Category): Boolean = database.withTransaction {
        if (categoryDao.countReferences(category.id) > 0 || categoryDao.getCategoryById(category.id) == null) {
            return@withTransaction false
        }
        categoryDao.deleteCategory(category)
        true
    }

    /** One budget per category per month: setting it again replaces the limit instead of duplicating. */
    suspend fun upsertBudget(categoryId: Long, amountLimit: Double, monthYear: String) = database.withTransaction {
        val existing = budgetDao.getBudgetForCategory(categoryId, monthYear)
        if (existing != null) {
            budgetDao.updateBudget(existing.copy(amountLimit = amountLimit))
        } else {
            budgetDao.insertBudget(Budget(categoryId = categoryId, amountLimit = amountLimit, monthYear = monthYear))
        }
    }

    /** Copies the most recent earlier month's budgets into [monthYear] when that month has none yet. */
    suspend fun carryOverBudgets(monthYear: String) = database.withTransaction {
        if (budgetDao.countBudgetsForMonth(monthYear) > 0) return@withTransaction
        val previousMonth = budgetDao.getLatestMonthBefore(monthYear) ?: return@withTransaction
        val previous = budgetDao.getBudgetsForMonthOnce(previousMonth)
        budgetDao.insertBudgets(previous.map { it.copy(id = 0, monthYear = monthYear) })
    }

    suspend fun deleteBudget(id: Long) = budgetDao.deleteBudgetById(id)

    suspend fun insertDebt(debt: Debt): Long = debtDao.insertDebt(debt)

    suspend fun recordDebtPayment(debt: Debt, paymentAmount: Double, accountId: Long) = database.withTransaction {
        val current = debtDao.getDebtById(debt.id) ?: debt
        val newRemaining = (current.remainingAmount - paymentAmount).coerceAtLeast(0.0)
        val isSettled = newRemaining <= 0.001
        debtDao.updateDebtRemaining(debt.id, newRemaining, isSettled)

        if (debt.type == DebtType.OWED_TO_ME) {
            // Someone paid me -> Income to account
            addTransaction(
                Transaction(
                    type = TransactionType.INCOME,
                    amount = paymentAmount,
                    sourceAccountId = accountId,
                    note = "${debt.personName} trả tiền nợ",
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            // I paid someone -> Expense from account
            addTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = paymentAmount,
                    sourceAccountId = accountId,
                    note = "Trả nợ cho ${debt.personName}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertGoal(goal: Goal): Long = goalDao.insertGoal(goal)

    suspend fun depositToGoal(goal: Goal, amount: Double, sourceAccountId: Long) = database.withTransaction {
        accountDao.adjustAccountBalance(sourceAccountId, -amount)
        goalDao.addDepositToGoal(goal.id, amount)
        // Saving is not spending: a transfer with no destination account keeps it out of expense reports.
        transactionDao.insertTransaction(
            Transaction(
                type = TransactionType.TRANSFER,
                amount = amount,
                sourceAccountId = sourceAccountId,
                note = "Nạp tiết kiệm: ${goal.name}",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertBill(bill: RecurringBill): Long = recurringBillDao.insertBill(bill)

    suspend fun deleteBill(bill: RecurringBill) = recurringBillDao.deleteBill(bill)

    suspend fun payBill(bill: RecurringBill) = database.withTransaction {
        addTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = bill.amount,
                sourceAccountId = bill.accountId,
                categoryId = bill.categoryId,
                note = "Thanh toán hóa đơn: ${bill.name}",
                timestamp = System.currentTimeMillis()
            )
        )
        recurringBillDao.updateBill(bill.copy(nextDueDate = nextDueDateAfter(bill.nextDueDate, bill.dayOfMonth)))
    }

    companion object {
        /** Older versions stored adjustments as an absolute amount with the sign only in the note. */
        fun adjustmentDelta(tx: Transaction): Double =
            if (tx.amount > 0 && tx.note.contains(": - ")) -tx.amount else tx.amount

        /** Same day-of-month next month, clamped to that month's length (31 → 28/29/30). */
        fun nextDueDateAfter(currentDue: Long, dayOfMonth: Int): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = currentDue
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
            }
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, maxDay))
            return cal.timeInMillis
        }
    }
}
