package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.VpsBackup
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CardInstallment
import com.example.data.model.Category
import com.example.data.model.CategoryType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VpsBackupTest {
    @Test fun snapshotRestoresAllTablesAndRejectsChangedSchema() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val db = AppDatabase.getDatabase(app)
        val accounts = db.accountDao()
        val categories = db.categoryDao()
        accounts.insertAccount(Account(name = "Tiền mặt", type = AccountType.CASH, balance = 125_000.0))
        val cardId = accounts.insertAccount(Account(name = "Thẻ tín dụng", type = AccountType.CREDIT_CARD, balance = -600_000.0))
        db.cardInstallmentDao().insert(CardInstallment(cardId = cardId, name = "Điện thoại", totalAmount = 600_000.0, monthCount = 3, firstDueDate = 1_790_000_000_000))
        categories.insertCategory(Category(name = "Ăn sáng", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = "#F97316"))
        val dragon = com.example.dragon.data.DragonRepository(db)
        dragon.ensurePet()
        dragon.grant("first_tx:2026-10-28", "MISSION", "Ghi khoản đầu tiên", 20, 5)
        val backup = VpsBackup(app)
        val snapshot = backup.createSnapshot()
        assertEquals(10, JSONObject(String(snapshot)).getInt("databaseVersion"))

        accounts.setAccountBalance(1, 9_000.0)
        categories.insertCategory(Category(name = "Tạm", type = CategoryType.EXPENSE, iconName = "home", colorHex = "#123456"))
        backup.restoreSnapshot(snapshot)
        assertEquals(125_000.0, accounts.getAccountById(1)!!.balance, 0.0)
        assertEquals(1, categories.getCategoryCount())
        assertEquals(1, db.cardInstallmentDao().forCard(cardId).first().size)
        assertEquals(20L, db.dragonDao().totalXpOnce())
        assertEquals(5L, db.dragonDao().coinsOnce())

        val v9 = JSONObject(String(snapshot)).put("databaseVersion", 9)
        v9.getJSONObject("tables").getJSONArray("transactions").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("debtId") }
        v9.getJSONObject("tables").getJSONArray("debts").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("accountId") }
        backup.restoreSnapshot(v9.toString().toByteArray())

        // An old (v8) backup restores the finance tables and leaves the dragon alone.
        dragon.grant("care:2026-10-28:1", "CARE", "Vuốt ve", 1, 0)
        val v8 = JSONObject(String(snapshot)).put("databaseVersion", 8)
        for (legacy in listOf(v8)) {
            val tables = legacy.getJSONObject("tables")
            for (i in 0 until tables.getJSONArray("transactions").length()) tables.getJSONArray("transactions").getJSONObject(i).remove("debtId")
            for (i in 0 until tables.getJSONArray("debts").length()) tables.getJSONArray("debts").getJSONObject(i).remove("accountId")
        }
        v8.getJSONObject("tables").remove("dragon_pet"); v8.getJSONObject("tables").remove("dragon_ledger")
        backup.restoreSnapshot(v8.toString().toByteArray())
        assertEquals(21L, db.dragonDao().totalXpOnce())

        val previous = JSONObject(String(snapshot)).put("databaseVersion", 7)
        previous.getJSONObject("tables").getJSONArray("transactions").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("debtId") }
        previous.getJSONObject("tables").getJSONArray("debts").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("accountId") }
        previous.getJSONObject("tables").getJSONArray("card_installments").getJSONObject(0).remove("monthlyAmount")
        previous.getJSONObject("tables").remove("dragon_pet"); previous.getJSONObject("tables").remove("dragon_ledger")
        backup.restoreSnapshot(previous.toString().toByteArray())
        assertEquals(null, db.cardInstallmentDao().forCard(cardId).first().single().monthlyAmount)

        val old = JSONObject(String(snapshot)).put("databaseVersion", 6)
        old.getJSONObject("tables").getJSONArray("transactions").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("debtId") }
        old.getJSONObject("tables").getJSONArray("debts").let { rows -> for (i in 0 until rows.length()) rows.getJSONObject(i).remove("accountId") }
        old.getJSONObject("tables").remove("card_installments")
        old.getJSONObject("tables").remove("dragon_pet"); old.getJSONObject("tables").remove("dragon_ledger")
        backup.restoreSnapshot(old.toString().toByteArray())
        assertEquals(0, db.cardInstallmentDao().forCard(cardId).first().size)
        assertEquals(125_000.0, accounts.getAccountById(1)!!.balance, 0.0)

        val broken = JSONObject(String(snapshot)).put("databaseVersion", 999).toString().toByteArray()
        try {
            backup.restoreSnapshot(broken)
            throw AssertionError("invalid snapshot accepted")
        } catch (_: IllegalArgumentException) {
            assertEquals(125_000.0, accounts.getAccountById(1)!!.balance, 0.0)
        }
        AppDatabase.resetForTests()
    }
}
