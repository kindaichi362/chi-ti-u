package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CardInstallment
import com.example.data.model.TransactionType
import com.example.data.model.nextAmount
import com.example.data.model.nextDueDate
import com.example.data.model.nextInterest
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class CardInstallmentTest {
    @Test fun monthlyPaymentMovesExistingDebtWithoutNewExpense() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val bank = db.accountDao().insertAccount(Account(name = "Ngân hàng", type = AccountType.BANK, balance = 1_000_000.0))
        val card = db.accountDao().insertAccount(Account(name = "Thẻ", type = AccountType.CREDIT_CARD, balance = -1_000_000.0))
        val id = repo.addInstallment(CardInstallment(cardId = card, name = "Điện thoại", totalAmount = 1_000_000.0, monthCount = 3, firstDueDate = 1_790_000_000_000))

        assertTrue(repo.payInstallment(id, bank))
        assertEquals(666_667.0, db.accountDao().getAccountById(bank)!!.balance, 0.0)
        assertEquals(-666_667.0, db.accountDao().getAccountById(card)!!.balance, 0.0)
        val tx = db.transactionDao().getTransactionsForAccountOnce(card).single()
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertEquals(333_333.0, tx.amount, 0.0)
        assertEquals(1, db.cardInstallmentDao().forCard(card).first().single().paidMonths)
        assertFalse(repo.deleteAccountById(card))

        assertTrue(repo.payInstallment(id, bank))
        assertTrue(repo.payInstallment(id, bank))
        assertFalse(repo.payInstallment(id, bank))
        assertEquals(0.0, db.accountDao().getAccountById(bank)!!.balance, 0.0)
        assertEquals(0.0, db.accountDao().getAccountById(card)!!.balance, 0.0)
        assertEquals(3, db.cardInstallmentDao().forCard(card).first().single().paidMonths)
        AppDatabase.resetForTests()
    }

    @Test fun clampsMonthlyDueDateAndRounding() {
        val first = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 31, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val plan = CardInstallment(cardId = 1, name = "Máy tính", totalAmount = 1_000_000.0, monthCount = 3, firstDueDate = first)
        assertEquals(333_333.0, plan.nextAmount(), 0.0)
        val second = plan.copy(paidMonths = 1)
        val due = Calendar.getInstance().apply { timeInMillis = second.nextDueDate()!! }
        assertEquals(Calendar.FEBRUARY, due.get(Calendar.MONTH))
        assertEquals(28, due.get(Calendar.DAY_OF_MONTH))
        assertEquals(333_334.0, plan.copy(paidMonths = 2).nextAmount(), 0.0)
    }

    @Test fun interestIsExpenseAndMonthlyTransferPaysCard() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val bank = db.accountDao().insertAccount(Account(name = "Ngân hàng", type = AccountType.BANK, balance = 2_000_000.0))
        val card = db.accountDao().insertAccount(Account(name = "Thẻ", type = AccountType.CREDIT_CARD, balance = -1_200_000.0))
        val plan = CardInstallment(cardId = card, name = "Máy tính", totalAmount = 1_200_000.0, monthlyAmount = 410_000.0, monthCount = 3, firstDueDate = 1_790_000_000_000)
        val id = repo.addInstallment(plan)
        assertEquals(410_000.0, plan.nextAmount(), 0.0)
        assertEquals(10_000.0, plan.nextInterest(), 0.0)
        assertTrue(repo.payInstallment(id, bank))
        assertEquals(1_590_000.0, db.accountDao().getAccountById(bank)!!.balance, 0.0)
        assertEquals(-800_000.0, db.accountDao().getAccountById(card)!!.balance, 0.0)
        val history = db.transactionDao().getTransactionsForAccountOnce(card)
        assertEquals(2, history.size)
        assertEquals(10_000.0, history.single { it.type == TransactionType.EXPENSE }.amount, 0.0)
        assertEquals(410_000.0, history.single { it.type == TransactionType.TRANSFER }.amount, 0.0)
        assertTrue(repo.updateInstallmentMonthlyAmount(id, 420_000.0))
        assertTrue(repo.payInstallment(id, bank))
        assertTrue(repo.payInstallment(id, bank))
        assertEquals(750_000.0, db.accountDao().getAccountById(bank)!!.balance, 0.0)
        assertEquals(0.0, db.accountDao().getAccountById(card)!!.balance, 0.0)
        assertEquals(50_000.0, db.transactionDao().getTransactionsForAccountOnce(card).filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }, 0.0)
        AppDatabase.resetForTests()
    }
}
