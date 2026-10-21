package com.example

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Builds a v4 database the way the old app left it (duplicate categories from the first-launch
 * race, duplicate budgets) and checks that opening it with v5 keeps every transaction while
 * merging the duplicates — then exercises the balance bookkeeping on the migrated data.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun createV4Database() {
        val file = context.getDatabasePath("chi_tieu_thanh_phat_db")
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE accounts (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `balance` REAL NOT NULL, `bankCode` TEXT, `bankId` TEXT, `accountNumberMasked` TEXT, `creditLimit` REAL NOT NULL, `statementDay` INTEGER, `dueDay` INTEGER, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `currency` TEXT NOT NULL, `isExcludedFromNetWorth` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE categories (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL, `parentId` INTEGER, `emoji` TEXT, `groupName` TEXT, `isPriority` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE transactions (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `sourceAccountId` INTEGER NOT NULL, `destinationAccountId` INTEGER, `categoryId` INTEGER, `timestamp` INTEGER NOT NULL, `note` TEXT NOT NULL, `payeeOrPayer` TEXT NOT NULL, `isSplit` INTEGER NOT NULL, `paymentStatus` TEXT NOT NULL, `qrBankBin` TEXT, `qrAccountNumber` TEXT, `qrBeneficiaryName` TEXT, `qrContent` TEXT)")
            db.execSQL("CREATE TABLE budgets (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `amountLimit` REAL NOT NULL, `monthYear` TEXT NOT NULL, `alert70` INTEGER NOT NULL, `alert90` INTEGER NOT NULL, `alert100` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE debts (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `personName` TEXT NOT NULL, `phone` TEXT NOT NULL, `initialAmount` REAL NOT NULL, `remainingAmount` REAL NOT NULL, `dueDate` INTEGER, `note` TEXT NOT NULL, `isSettled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE goals (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `monthlyTarget` REAL NOT NULL, `targetDate` INTEGER, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE recurring_bills (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `dayOfMonth` INTEGER NOT NULL, `nextDueDate` INTEGER NOT NULL, `note` TEXT NOT NULL, `isActive` INTEGER NOT NULL)")

            db.execSQL("INSERT INTO accounts VALUES (1,'Tiền mặt','CASH',1000000,NULL,'cash',NULL,0,NULL,NULL,'#10B981','payments','VND',0,0,0)")
            db.execSQL("INSERT INTO accounts VALUES (2,'Vietcombank','BANK',5000000,'VCB','vcb',NULL,0,NULL,NULL,'#005C2B','account_balance','VND',0,0,0)")
            // "Ăn sáng" exists twice (ids 1 and 3) because of the old seeding race.
            db.execSQL("INSERT INTO categories VALUES (1,'Ăn sáng','EXPENSE','restaurant','#F97316',1,1,NULL,'🍚','Ăn uống',1)")
            db.execSQL("INSERT INTO categories VALUES (2,'Lương','INCOME','payments','#10B981',1,2,NULL,'💰',NULL,0)")
            db.execSQL("INSERT INTO categories VALUES (3,'Ăn sáng','EXPENSE','restaurant','#F97316',1,1,NULL,'🍚','Ăn uống',1)")
            db.execSQL("INSERT INTO transactions VALUES (1,'EXPENSE',30000,1,NULL,3,1000,'Phở','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO transactions VALUES (2,'EXPENSE',20000,1,NULL,1,2000,'Bánh mì','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO budgets VALUES (1,3,500000,'2026-10',1,1,1)")
            db.execSQL("INSERT INTO budgets VALUES (2,1,800000,'2026-10',1,1,1)")
            db.version = 4
        }
    }

    @Test
    fun migrates4to5_mergesDuplicatesAndKeepsHistory() = runBlocking {
        createV4Database()
        val db = AppDatabase.getDatabase(context)
        val repo = FinanceRepository(db)

        val categories = db.categoryDao().getAllCategoriesOnce()
        assertEquals(setOf("Ăn sáng", "Lương"), categories.map { it.name }.toSet())
        assertEquals(2, categories.size)

        // Both transactions survive and now point at the surviving "Ăn sáng" row (id 1).
        assertEquals(1L, db.transactionDao().getTransactionById(1)!!.categoryId)
        assertEquals(1L, db.transactionDao().getTransactionById(2)!!.categoryId)

        // One budget per category per month; the newest limit wins.
        val budgets = db.budgetDao().getBudgetsForMonthOnce("2026-10")
        assertEquals(1, budgets.size)
        assertEquals(800000.0, budgets.single().amountLimit, 0.1)

        // Bookkeeping on migrated data: add, edit, delete must leave balances consistent.
        val id = repo.addTransaction(Transaction(type = TransactionType.TRANSFER, amount = 200000.0, sourceAccountId = 2, destinationAccountId = 1))
        assertEquals(4_800_000.0, db.accountDao().getAccountById(2)!!.balance, 0.1)
        assertEquals(1_200_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)

        repo.updateTransaction(db.transactionDao().getTransactionById(id)!!.copy(amount = 500000.0))
        assertEquals(4_500_000.0, db.accountDao().getAccountById(2)!!.balance, 0.1)
        assertEquals(1_500_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)

        repo.deleteTransaction(db.transactionDao().getTransactionById(id)!!)
        assertEquals(5_000_000.0, db.accountDao().getAccountById(2)!!.balance, 0.1)
        assertEquals(1_000_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)

        // Self-transfers are rejected instead of corrupting data.
        assertEquals(-1L, repo.addTransaction(Transaction(type = TransactionType.TRANSFER, amount = 1.0, sourceAccountId = 1, destinationAccountId = 1)))

        // Reconcile then delete the adjustment: the balance goes back exactly.
        repo.reconcileAccountBalance(1, 900_000.0)
        assertEquals(900_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)
        val adjustment = db.transactionDao().getTransactionsForAccountOnce(1).first { it.type == TransactionType.ADJUSTMENT }
        repo.deleteTransaction(adjustment)
        assertEquals(1_000_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)

        // Cancelling an already-paid transaction refunds it.
        val paidId = repo.addTransaction(Transaction(type = TransactionType.EXPENSE, amount = 100_000.0, sourceAccountId = 1, paymentStatus = PaymentStatus.PAID))
        repo.cancelTransactionPayment(paidId)
        assertEquals(1_000_000.0, db.accountDao().getAccountById(1)!!.balance, 0.1)

        // An account with history cannot be hard-deleted.
        assertEquals(false, repo.deleteAccountById(1))

        // Budgets carry over to a month that has none.
        repo.carryOverBudgets("2026-11")
        assertEquals(1, db.budgetDao().getBudgetsForMonthOnce("2026-11").size)
        db.close()
    }
}
