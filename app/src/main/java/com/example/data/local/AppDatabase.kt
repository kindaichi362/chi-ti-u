package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        Debt::class,
        Goal::class,
        RecurringBill::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringBillDao(): RecurringBillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'PAID'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN qrBankBin TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN qrAccountNumber TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN qrBeneficiaryName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN qrContent TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN emoji TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN bankId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'VND'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN groupName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN isPriority INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chi_tieu_thanh_phat_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(db: AppDatabase) {
                // 1. Initial 100+ Vietnamese Default Categories grouped by parent categories
                val defaultCategories = InitialCategories.getDefaultCategories()
                db.categoryDao().insertCategories(defaultCategories)

                // 2. Realistic Vietnamese Starter Accounts with specific bank branding
                val defaultAccounts = listOf(
                    Account(name = "Tiền mặt", type = AccountType.CASH, balance = 20000000.0, colorHex = "#10B981", iconName = "payments", bankId = "cash"),
                    Account(name = "Vietcombank", type = AccountType.BANK, balance = 35000000.0, bankCode = "VCB", bankId = "vcb", accountNumberMasked = "•••• 8899", colorHex = "#005C2B", iconName = "account_balance"),
                    Account(name = "BIDV", type = AccountType.BANK, balance = 18000000.0, bankCode = "BIDV", bankId = "bidv", accountNumberMasked = "•••• 3321", colorHex = "#0A3B82", iconName = "account_balance"),
                    Account(name = "VietinBank", type = AccountType.BANK, balance = 12000000.0, bankCode = "CTG", bankId = "vietinbank", accountNumberMasked = "•••• 4411", colorHex = "#A6192E", iconName = "account_balance"),
                    Account(name = "MB Bank", type = AccountType.BANK, balance = 42000000.0, bankCode = "MB", bankId = "mb", accountNumberMasked = "•••• 5522", colorHex = "#003882", iconName = "account_balance"),
                    Account(name = "Techcombank", type = AccountType.BANK, balance = 30000000.0, bankCode = "TCB", bankId = "tcb", accountNumberMasked = "•••• 1903", colorHex = "#E31837", iconName = "account_balance"),
                    Account(name = "VPBank", type = AccountType.BANK, balance = 15000000.0, bankCode = "VPB", bankId = "vpbank", accountNumberMasked = "•••• 7766", colorHex = "#00B14F", iconName = "account_balance"),
                    Account(name = "TPBank", type = AccountType.BANK, balance = 25000000.0, bankCode = "TPB", bankId = "tpbank", accountNumberMasked = "•••• 9988", colorHex = "#8E24AA", iconName = "account_balance"),
                    Account(name = "ACB", type = AccountType.BANK, balance = 14000000.0, bankCode = "ACB", bankId = "acb", accountNumberMasked = "•••• 2244", colorHex = "#0066B2", iconName = "account_balance"),
                    Account(name = "Agribank", type = AccountType.BANK, balance = 20000000.0, bankCode = "VBA", bankId = "agribank", accountNumberMasked = "•••• 6677", colorHex = "#800000", iconName = "account_balance"),
                    Account(name = "Thẻ tín dụng", type = AccountType.CREDIT_CARD, balance = -3500000.0, creditLimit = 30000000.0, colorHex = "#D97706", iconName = "credit_card", bankId = "credit"),
                    Account(name = "Ví MoMo", type = AccountType.E_WALLET, balance = 5500000.0, bankCode = "MOMO", bankId = "momo", accountNumberMasked = "•••• 0988", colorHex = "#A50064", iconName = "phone_android")
                )
                db.accountDao().insertAccounts(defaultAccounts)

                // 3. Initial Sample Budgets
                val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
                val defaultBudgets = listOf(
                    Budget(categoryId = 1, amountLimit = 8250000.0, monthYear = currentMonth),
                    Budget(categoryId = 2, amountLimit = 2100000.0, monthYear = currentMonth),
                    Budget(categoryId = 3, amountLimit = 5400000.0, monthYear = currentMonth)
                )
                db.budgetDao().insertBudgets(defaultBudgets)

                // 4. Sample Goal
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, 12)
                db.goalDao().insertGoal(
                    Goal(
                        name = "Quỹ dự phòng khẩn cấp",
                        targetAmount = 50000000.0,
                        currentAmount = 25000000.0,
                        monthlyTarget = 3000000.0,
                        targetDate = calendar.timeInMillis,
                        colorHex = "#0A5C36",
                        iconName = "savings"
                    )
                )

                // 5. Initial Sample Transactions matching user prompt
                val now = System.currentTimeMillis()
                val sampleTransactions = listOf(
                    Transaction(
                        type = com.example.data.model.TransactionType.EXPENSE,
                        amount = 50000.0,
                        sourceAccountId = 1,
                        categoryId = 1,
                        note = "Ăn sáng",
                        timestamp = now - 3600000 * 2,
                        paymentStatus = com.example.data.model.PaymentStatus.PAID
                    ),
                    Transaction(
                        type = com.example.data.model.TransactionType.EXPENSE,
                        amount = 300000.0,
                        sourceAccountId = 3, // MB Bank
                        categoryId = 2, // Xăng xe
                        note = "Đổ xăng",
                        timestamp = now - 3600000 * 5,
                        paymentStatus = com.example.data.model.PaymentStatus.PAID
                    ),
                    Transaction(
                        type = com.example.data.model.TransactionType.INCOME,
                        amount = 30000000.0,
                        sourceAccountId = 2, // Vietcombank
                        categoryId = 24, // Lương
                        note = "Nhận lương tháng",
                        timestamp = now - 86400000,
                        paymentStatus = com.example.data.model.PaymentStatus.PAID
                    )
                )
                for (tx in sampleTransactions) {
                    db.transactionDao().insertTransaction(tx)
                }
            }
        }
    }
}
