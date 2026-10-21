package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Account
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.Goal
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction

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
    version = 6,
    exportSchema = true
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

        /**
         * v5: merges the duplicate categories that the old first-launch race created (re-pointing
         * transactions, budgets and bills to the surviving row), keeps one budget per category per
         * month, and adds indexes for the queries the app runs constantly.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val keeperOf = "(SELECT MIN(c2.id) FROM categories c1 JOIN categories c2 ON c1.name = c2.name AND c1.type = c2.type WHERE c1.id = %s)"
                db.execSQL("UPDATE transactions SET categoryId = ${keeperOf.format("transactions.categoryId")} WHERE categoryId IN (SELECT id FROM categories)")
                db.execSQL("UPDATE budgets SET categoryId = ${keeperOf.format("budgets.categoryId")} WHERE categoryId IN (SELECT id FROM categories)")
                db.execSQL("UPDATE recurring_bills SET categoryId = ${keeperOf.format("recurring_bills.categoryId")} WHERE categoryId IN (SELECT id FROM categories)")
                db.execSQL("UPDATE categories SET parentId = ${keeperOf.format("categories.parentId")} WHERE parentId IN (SELECT id FROM categories)")
                db.execSQL("DELETE FROM categories WHERE id NOT IN (SELECT MIN(id) FROM categories GROUP BY name, type)")

                db.execSQL("DELETE FROM budgets WHERE id NOT IN (SELECT MAX(id) FROM budgets GROUP BY categoryId, monthYear)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_monthYear ON budgets (categoryId, monthYear)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_sourceAccountId ON transactions (sourceAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_destinationAccountId ON transactions (destinationAccountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions (categoryId)")
            }
        }

        /** Tests only: forget the open instance so the next call opens a fresh database file. */
        fun resetForTests() {
            synchronized(this) { INSTANCE?.close(); INSTANCE = null }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // No destructive fallback: for a money app, a missing migration must fail loudly
                // in testing rather than silently wipe every transaction on the user's phone.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chi_tieu_thanh_phat_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, CategoryCleanupMigration)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
