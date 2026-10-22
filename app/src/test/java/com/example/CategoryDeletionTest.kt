package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CategoryDeletionTest {
    @Test fun unusedCategoryCanBeDeletedButReferencedCategoryCannot() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val repo = FinanceRepository(db)
        val accountId = db.accountDao().insertAccount(Account(name = "Tiền mặt", type = AccountType.CASH, balance = 100_000.0))
        val unused = Category(name = "Tạm", type = CategoryType.EXPENSE, iconName = "custom_other", colorHex = "#F97316", isDefault = false)
        val unusedId = db.categoryDao().insertCategory(unused)
        assertTrue(repo.deleteCategoryIfUnused(unused.copy(id = unusedId)))

        val used = Category(name = "Ăn sáng", type = CategoryType.EXPENSE, iconName = "custom_food", colorHex = "#F97316")
        val usedId = db.categoryDao().insertCategory(used)
        repo.addTransaction(Transaction(type = TransactionType.EXPENSE, amount = 20_000.0, sourceAccountId = accountId, categoryId = usedId))
        assertFalse(repo.deleteCategoryIfUnused(used.copy(id = usedId)))
        assertTrue(db.categoryDao().getCategoryById(usedId) != null)
        AppDatabase.resetForTests()
    }
}
