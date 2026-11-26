package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DebtPrincipalTest {
    @Test fun lendingAndDeletingRestoresBalance() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val accountId = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 1000.0))
        val id = repo.insertDebt(Debt(type = DebtType.OWED_TO_ME, personName = "Bình", initialAmount = 400.0, remainingAmount = 400.0, accountId = accountId))
        assertEquals(600.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        repo.recordDebtPayment(db.debtDao().getDebtById(id)!!, 100.0, accountId)
        assertEquals(700.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        val payment = repo.transactionsForDebt(id).first().first { it.note.startsWith("DEBT_PAYMENT:") }
        repo.deleteTransaction(payment)
        assertEquals(400.0, db.debtDao().getDebtById(id)!!.remainingAmount, 0.001)
        assertEquals(600.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        repo.deleteDebt(id)
        assertEquals(1000.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        AppDatabase.resetForTests()
    }

    @Test fun historicalDebtDoesNotChangeBalance() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val accountId = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 1000.0))
        repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 500.0, remainingAmount = 500.0))
        assertEquals(1000.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        assertTrue(repo.allTransactions.first().isEmpty())
        AppDatabase.resetForTests()
    }
    @Test fun borrowingAndRepaymentAffectBalanceButNotReports() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val accountId = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 0.0))
        val debtId = repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 500.0, remainingAmount = 500.0, accountId = accountId))
        assertEquals(500.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        repo.recordDebtPayment(db.debtDao().getDebtById(debtId)!!, 500.0, accountId)
        assertEquals(0.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        assertTrue(db.debtDao().getDebtById(debtId)!!.isSettled)
        assertTrue(repo.allTransactions.first().none { it.countsInReports })
        repo.recordDebtPayment(db.debtDao().getDebtById(debtId)!!, 1.0, accountId)
        assertEquals(0.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        AppDatabase.resetForTests()
    }

    @Test fun deletingTheLoanMovementKeepsTheDebtAsNotBooked() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val accountId = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 100.0))
        val id = repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 500.0, remainingAmount = 500.0, accountId = accountId))
        val past = System.currentTimeMillis() - 3L * 24 * 3600 * 1000
        assertTrue(repo.recordDebtPayment(db.debtDao().getDebtById(id)!!, 200.0, accountId, past))
        assertEquals(400.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        val txs = repo.transactionsForDebt(id).first()
        assertEquals(past, txs.first { it.type == TransactionType.EXPENSE }.timestamp)
        // Deleting the borrowed money (not a repayment) must not touch what is still owed.
        repo.deleteTransaction(txs.first { it.type == TransactionType.INCOME })
        val debt = db.debtDao().getDebtById(id)!!
        assertEquals(300.0, debt.remainingAmount, 0.001)
        assertNull(debt.accountId)
        assertEquals(-100.0, db.accountDao().getAccountById(accountId)!!.balance, 0.001)
        // Paying more than what is left is refused.
        assertFalse(repo.recordDebtPayment(debt, 301.0, accountId))
        AppDatabase.resetForTests()
    }

    @Test fun lendingDoesNotShrinkNetWorth() {
        val wallet = Account(id = 1, name = "Ví", type = AccountType.CASH, balance = 600.0)
        val lent = Debt(id = 1, type = DebtType.OWED_TO_ME, personName = "Bình", initialAmount = 400.0, remainingAmount = 400.0, accountId = 1)
        val borrowed = Debt(id = 2, type = DebtType.I_OWE, personName = "An", initialAmount = 100.0, remainingAmount = 100.0)
        val s = com.example.ai.FinancialAdvisorEngine.computeSnapshot(listOf(wallet), emptyList(), emptyList(), emptyList(), emptyList(), listOf(lent, borrowed))
        assertEquals(400.0, s.receivables, 0.001)
        assertEquals(900.0, s.netWorth, 0.001)
    }
}
