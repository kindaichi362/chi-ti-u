package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Money side of borrowing / lending: which account moves, by how much, and what stays owed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DebtAccountFlowTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        db = AppDatabase.getDatabase(app)
        repo = FinanceRepository(db)
    }

    @After fun tearDown() = AppDatabase.resetForTests()

    private suspend fun balance(id: Long) = db.accountDao().getAccountById(id)!!.balance
    private suspend fun debt(id: Long) = db.debtDao().getDebtById(id)!!

    @Test fun borrowIntoOneAccountRepayFromAnother() = runBlocking {
        val cash = db.accountDao().insertAccount(Account(name = "Tiền mặt", type = AccountType.CASH, balance = 100_000.0))
        val bank = db.accountDao().insertAccount(Account(name = "MB", type = AccountType.BANK, balance = 2_000_000.0))
        val id = repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "Chị Lan", initialAmount = 1_000_000.0, remainingAmount = 1_000_000.0, accountId = cash))
        assertEquals(1_100_000.0, balance(cash), 0.001)
        assertEquals(2_000_000.0, balance(bank), 0.001)

        assertTrue(repo.recordDebtPayment(debt(id), 300_000.0, bank))
        assertTrue(repo.recordDebtPayment(debt(id), 200_000.0, cash))
        assertEquals(1_700_000.0, balance(bank), 0.001)
        assertEquals(900_000.0, balance(cash), 0.001)
        assertEquals(500_000.0, debt(id).remainingAmount, 0.001)
        assertFalse(debt(id).isSettled)

        val history = repo.transactionsForDebt(id).first()
        assertEquals(3, history.size)
        assertEquals(1, history.count { it.type == TransactionType.INCOME && it.sourceAccountId == cash })
        assertEquals(setOf(bank, cash), history.filter { it.type == TransactionType.EXPENSE }.map { it.sourceAccountId }.toSet())

        assertTrue(repo.recordDebtPayment(debt(id), 500_000.0, bank))
        assertTrue(debt(id).isSettled)
        assertEquals(1_200_000.0, balance(bank), 0.001)
    }

    @Test fun lendThenCollectInPartsAndUndoOne() = runBlocking {
        val bank = db.accountDao().insertAccount(Account(name = "VCB", type = AccountType.BANK, balance = 5_000_000.0))
        val id = repo.insertDebt(Debt(type = DebtType.OWED_TO_ME, personName = "Tuấn", initialAmount = 2_000_000.0, remainingAmount = 2_000_000.0, accountId = bank))
        assertEquals(3_000_000.0, balance(bank), 0.001)
        assertTrue(repo.recordDebtPayment(debt(id), 2_000_000.0, bank))
        assertTrue(debt(id).isSettled)
        assertEquals(5_000_000.0, balance(bank), 0.001)

        // Undoing a collection re-opens the debt and takes the money back out.
        val collected = repo.transactionsForDebt(id).first().first { it.type == TransactionType.INCOME }
        repo.deleteTransaction(collected)
        assertFalse(debt(id).isSettled)
        assertEquals(2_000_000.0, debt(id).remainingAmount, 0.001)
        assertEquals(3_000_000.0, balance(bank), 0.001)
    }

    @Test fun invalidDebtIsRefusedWithoutCrashOrMoneyMove() = runBlocking {
        val cash = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 100.0))
        assertEquals(-1L, repo.insertDebt(Debt(type = DebtType.I_OWE, personName = " ", initialAmount = 50.0, remainingAmount = 50.0, accountId = cash)))
        assertEquals(-1L, repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 0.0, remainingAmount = 0.0, accountId = cash)))
        assertEquals(-1L, repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 50.0, remainingAmount = 50.0, accountId = 999)))
        assertEquals(100.0, balance(cash), 0.001)
        assertTrue(repo.allDebts.first().isEmpty())
        assertTrue(repo.allTransactions.first().isEmpty())
    }

    @Test fun refusedRepaymentsChangeNothing() = runBlocking {
        val cash = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 0.0))
        val id = repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 500.0, remainingAmount = 500.0, accountId = cash))
        assertFalse(repo.recordDebtPayment(debt(id), 0.0, cash))
        assertFalse(repo.recordDebtPayment(debt(id), -10.0, cash))
        assertFalse(repo.recordDebtPayment(debt(id), 100.0, 999))
        assertEquals(500.0, balance(cash), 0.001)
        assertEquals(500.0, debt(id).remainingAmount, 0.001)
        assertEquals(1, repo.transactionsForDebt(id).first().size)
    }

    @Test fun cancellingOrConfirmingALoanMovementDoesNothing() = runBlocking {
        val cash = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 0.0))
        val id = repo.insertDebt(Debt(type = DebtType.I_OWE, personName = "An", initialAmount = 500.0, remainingAmount = 500.0, accountId = cash))
        val origin = repo.transactionsForDebt(id).first().single()
        repo.cancelTransactionPayment(origin.id)
        repo.confirmTransactionPayment(origin.id)
        assertEquals(500.0, balance(cash), 0.001)
        assertEquals(PaymentStatus.PAID, db.transactionDao().getTransactionById(origin.id)!!.paymentStatus)
        // And the normal transaction editor cannot rewrite it either.
        repo.updateTransaction(origin.copy(amount = 1.0))
        assertEquals(500.0, db.transactionDao().getTransactionById(origin.id)!!.amount, 0.001)
    }

    @Test fun loanMovementIsDatedWhenTheDebtWasCreated() = runBlocking {
        val cash = db.accountDao().insertAccount(Account(name = "Ví", type = AccountType.CASH, balance = 0.0))
        val created = System.currentTimeMillis() - 5L * 24 * 3600 * 1000
        val id = repo.insertDebt(Debt(type = DebtType.OWED_TO_ME, personName = "Bình", initialAmount = 100.0, remainingAmount = 100.0, accountId = cash, createdAt = created))
        assertEquals(created, repo.transactionsForDebt(id).first().single().timestamp)
        assertEquals(-100.0, balance(cash), 0.001)
    }
}
