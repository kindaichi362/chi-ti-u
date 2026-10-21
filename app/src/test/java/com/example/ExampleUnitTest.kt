package com.example

import com.example.ai.NaturalLanguageSearchEngine
import com.example.ai.VoiceTransactionEngine
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.payment.vietqr.VietnameseBankRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val sampleAccounts = listOf(
        Account(id = 1, name = "Tiền mặt", type = AccountType.CASH, balance = 1000000.0, bankId = "cash"),
        Account(id = 2, name = "Vietcombank", type = AccountType.BANK, balance = 15000000.0, bankCode = "VCB", bankId = "vcb"),
        Account(id = 3, name = "MB Bank", type = AccountType.BANK, balance = 20000000.0, bankCode = "MB", bankId = "mb")
    )

    private val sampleCategories = listOf(
        Category(id = 1, name = "Ăn uống", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", emoji = "🍜"),
        Category(id = 2, name = "Xăng xe", type = CategoryType.EXPENSE, iconName = "local_gas_station", colorHex = "#F97316", emoji = "⛽"),
        Category(id = 3, name = "Lương", type = CategoryType.INCOME, iconName = "payments", colorHex = "#10B981", emoji = "💰")
    )

    @Test
    fun testVietnameseBankRegistry() {
        val vcb = VietnameseBankRegistry.findById("vcb")
        assertNotNull(vcb)
        assertEquals("Vietcombank", vcb?.shortName)

        val mb = VietnameseBankRegistry.findByNameOrKeyword("MB Bank")
        assertNotNull(mb)
        assertEquals("970422", mb?.bin)
    }

    @Test
    fun testVoiceTransactionEngineExpense() {
        val draft = VoiceTransactionEngine.processVoiceCommand(
            input = "Ăn sáng hết 50 nghìn bằng tiền mặt",
            currentDraft = null,
            availableAccounts = sampleAccounts,
            availableCategories = sampleCategories
        )

        assertNotNull(draft)
        assertEquals(TransactionType.EXPENSE, draft?.type)
        assertEquals(50000.0, draft?.amount ?: 0.0, 0.01)
        assertEquals(1L, draft?.accountId)
        assertEquals("Ăn uống", draft?.categoryName)
    }

    @Test
    fun testVoiceTransactionEngineRefinement() {
        val initialDraft = VoiceTransactionEngine.processVoiceCommand(
            input = "Ăn sáng hết 50k bằng tiền mặt",
            currentDraft = null,
            availableAccounts = sampleAccounts,
            availableCategories = sampleCategories
        )
        assertNotNull(initialDraft)

        val refinedDraft = VoiceTransactionEngine.processVoiceCommand(
            input = "Đổi thành 60 nghìn",
            currentDraft = initialDraft,
            availableAccounts = sampleAccounts,
            availableCategories = sampleCategories
        )

        assertNotNull(refinedDraft)
        assertEquals(60000.0, refinedDraft?.amount ?: 0.0, 0.01)
        assertEquals(1L, refinedDraft?.accountId)
    }

    @Test
    fun testNaturalLanguageSearch() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            Transaction(id = 1, type = TransactionType.EXPENSE, amount = 50000.0, sourceAccountId = 1, categoryId = 1, timestamp = now),
            Transaction(id = 2, type = TransactionType.EXPENSE, amount = 100000.0, sourceAccountId = 1, categoryId = 1, timestamp = now),
            Transaction(id = 3, type = TransactionType.EXPENSE, amount = 300000.0, sourceAccountId = 3, categoryId = 2, timestamp = now)
        )

        val result = NaturalLanguageSearchEngine.executeQuery(
            query = "Tháng này tôi tiêu ăn uống bao nhiêu?",
            allTransactions = txs,
            accounts = sampleAccounts,
            categories = sampleCategories
        )

        assertEquals(2, result.transactionCount)
        assertEquals(150000.0, result.totalAmount, 0.01)
        assertTrue(result.summaryText.contains("150.000"))
    }
}
