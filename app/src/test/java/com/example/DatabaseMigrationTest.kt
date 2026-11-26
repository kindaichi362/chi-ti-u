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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun migration9To10KeepsExistingRows() {
        val file = context.getDatabasePath("debt_migration_test.db")
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { raw ->
            raw.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY, amount REAL NOT NULL)")
            raw.execSQL("CREATE TABLE debts (id INTEGER PRIMARY KEY, remainingAmount REAL NOT NULL)")
            raw.execSQL("INSERT INTO transactions VALUES (7, 125.0)")
            raw.execSQL("INSERT INTO debts VALUES (3, 500.0)")
        }
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("debt_migration_test.db")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                }).build()
        )
        helper.use {
            val db = it.writableDatabase
            AppDatabase.MIGRATION_9_10.migrate(db)
            db.query("SELECT amount, debtId FROM transactions WHERE id=7").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(125.0, cursor.getDouble(0), 0.001)
                assertTrue(cursor.isNull(1))
            }
            db.query("SELECT remainingAmount, accountId FROM debts WHERE id=3").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(500.0, cursor.getDouble(0), 0.001)
                assertTrue(cursor.isNull(1))
            }
        }
        file.delete()
    }

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
            // Old v1 defaults: two that merge into one new category, and a transfer-like one in use / unused.
            db.execSQL("INSERT INTO categories VALUES (4,'Taxi','EXPENSE','x','#000000',1,3,NULL,NULL,'Đi lại',0)")
            db.execSQL("INSERT INTO categories VALUES (5,'Xe ôm','EXPENSE','x','#000000',1,4,NULL,NULL,'Đi lại',0)")
            db.execSQL("INSERT INTO categories VALUES (6,'Rút tiền mặt','EXPENSE','x','#000000',1,5,NULL,NULL,'Vay / Nợ',0)")
            db.execSQL("INSERT INTO categories VALUES (7,'Đầu tư','EXPENSE','x','#000000',1,6,NULL,NULL,'Tài chính',0)")
            db.execSQL("INSERT INTO transactions VALUES (3,'EXPENSE',40000,1,NULL,4,3000,'Taxi','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO transactions VALUES (4,'EXPENSE',25000,1,NULL,5,4000,'Xe ôm','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO transactions VALUES (5,'EXPENSE',500000,1,NULL,6,5000,'ATM','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("UPDATE accounts SET bankId = 'vietinbank' WHERE id = 2")
            db.execSQL("INSERT INTO transactions VALUES (1,'EXPENSE',30000,1,NULL,3,1000,'Phở','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO transactions VALUES (2,'EXPENSE',20000,1,NULL,1,2000,'Bánh mì','',0,'PAID',NULL,NULL,NULL,NULL)")
            db.execSQL("INSERT INTO budgets VALUES (1,3,500000,'2026-10',1,1,1)")
            db.execSQL("INSERT INTO budgets VALUES (2,1,800000,'2026-10',1,1,1)")
            db.version = 4
        }
    }

    @Test
    fun migratesToLatest_cleansCategoriesAndKeepsHistory() = runBlocking {
        createV4Database()
        val db = AppDatabase.getDatabase(context)
        val repo = FinanceRepository(db)

        val categories = db.categoryDao().getAllCategoriesOnce()
        val byName = categories.groupBy { it.name }
        // No duplicate names left, every new default present, old merged names gone.
        assertEquals(categories.size, byName.size)
        assertTrue(byName.keys.containsAll(com.example.data.local.InitialCategories.getDefaultCategories().map { it.name }))
        assertFalse(byName.containsKey("Taxi") || byName.containsKey("Xe ôm") || byName.containsKey("Đầu tư"))
        // "Taxi" and "Xe ôm" history now sits in one category; the used "Rút tiền mặt" was kept.
        val taxiId = byName.getValue("Taxi / xe công nghệ").single().id
        assertEquals(taxiId, db.transactionDao().getTransactionById(3)!!.categoryId)
        assertEquals(taxiId, db.transactionDao().getTransactionById(4)!!.categoryId)
        assertEquals("Khác", byName.getValue("Rút tiền mặt").single().groupName)
        assertEquals("vtb", db.accountDao().getAccountById(2)!!.bankId)

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
