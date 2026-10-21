package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.BudgetDao
import com.example.data.local.CategoryDao
import com.example.data.local.DebtDao
import com.example.data.local.GoalDao
import com.example.data.local.RecurringBillDao
import com.example.data.local.TransactionDao
import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Goal
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val debtDao: DebtDao,
    private val goalDao: GoalDao,
    private val recurringBillDao: RecurringBillDao
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val activeBills: Flow<List<RecurringBill>> = recurringBillDao.getActiveBills()

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(monthYear)

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end)

    suspend fun addTransaction(transaction: Transaction): Long {
        if (transaction.paymentStatus == PaymentStatus.PAID) {
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, -transaction.amount)
                }
                TransactionType.INCOME -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, transaction.amount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, -transaction.amount)
                    transaction.destinationAccountId?.let { destId ->
                        accountDao.adjustAccountBalance(destId, transaction.amount)
                    }
                }
                TransactionType.ADJUSTMENT -> {
                    // Adjustment already updates balance directly
                }
            }
        }
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun confirmTransactionPayment(transactionId: Long) {
        val tx = transactionDao.getTransactionById(transactionId) ?: return
        if (tx.paymentStatus != PaymentStatus.PAID) {
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    accountDao.adjustAccountBalance(tx.sourceAccountId, -tx.amount)
                }
                TransactionType.INCOME -> {
                    accountDao.adjustAccountBalance(tx.sourceAccountId, tx.amount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.adjustAccountBalance(tx.sourceAccountId, -tx.amount)
                    tx.destinationAccountId?.let { destId ->
                        accountDao.adjustAccountBalance(destId, tx.amount)
                    }
                }
                TransactionType.ADJUSTMENT -> {}
            }
            transactionDao.updateTransaction(tx.copy(paymentStatus = PaymentStatus.PAID))
        }
    }

    suspend fun cancelTransactionPayment(transactionId: Long) {
        val tx = transactionDao.getTransactionById(transactionId) ?: return
        transactionDao.updateTransaction(tx.copy(paymentStatus = PaymentStatus.CANCELLED))
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // Revert balance adjustment only if it was paid
        if (transaction.paymentStatus == PaymentStatus.PAID) {
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, transaction.amount)
                }
                TransactionType.INCOME -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, -transaction.amount)
                }
                TransactionType.TRANSFER -> {
                    accountDao.adjustAccountBalance(transaction.sourceAccountId, transaction.amount)
                    transaction.destinationAccountId?.let { destId ->
                        accountDao.adjustAccountBalance(destId, -transaction.amount)
                    }
                }
                TransactionType.ADJUSTMENT -> {
                    // Reversal not strictly necessary or handled by user adjustment
                }
            }
        }
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccountById(account.id)
    }

    suspend fun deleteAccountById(id: Long) = accountDao.deleteAccountById(id)

    suspend fun archiveAccount(id: Long) = accountDao.archiveAccountById(id)

    suspend fun reconcileAccountBalance(accountId: Long, targetBalance: Double, reason: String = "Điều chỉnh số dư") {
        val account = accountDao.getAccountById(accountId) ?: return
        val diff = targetBalance - account.balance
        if (kotlin.math.abs(diff) > 0.001) {
            accountDao.setAccountBalance(accountId, targetBalance)
            transactionDao.insertTransaction(
                Transaction(
                    type = TransactionType.ADJUSTMENT,
                    amount = kotlin.math.abs(diff),
                    sourceAccountId = accountId,
                    note = "$reason: ${if (diff > 0) "+ " else "- "}${kotlin.math.abs(diff)}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun insertCategories(categories: List<Category>) = categoryDao.insertCategories(categories)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun setCategoryPriority(categoryId: Long, isPriority: Boolean) =
        categoryDao.setCategoryPriority(categoryId, isPriority)

    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()

    suspend fun getAllCategoryNames(): List<String> = categoryDao.getAllCategoryNames()

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun deleteBudget(id: Long) = budgetDao.deleteBudgetById(id)

    suspend fun insertDebt(debt: Debt): Long = debtDao.insertDebt(debt)

    suspend fun recordDebtPayment(debt: Debt, paymentAmount: Double, accountId: Long) {
        val newRemaining = (debt.remainingAmount - paymentAmount).coerceAtLeast(0.0)
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

    suspend fun depositToGoal(goal: Goal, amount: Double, sourceAccountId: Long) {
        accountDao.adjustAccountBalance(sourceAccountId, -amount)
        goalDao.addDepositToGoal(goal.id, amount)
        transactionDao.insertTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = amount,
                sourceAccountId = sourceAccountId,
                note = "Nạp tiết kiệm: ${goal.name}",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertBill(bill: RecurringBill): Long = recurringBillDao.insertBill(bill)

    suspend fun payBill(bill: RecurringBill) {
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
        // Advance next due date by 1 month approx
        val nextDate = bill.nextDueDate + (30L * 86400000L)
        recurringBillDao.updateBill(bill.copy(nextDueDate = nextDate))
    }
}
