package com.example

import com.example.ai.VietnameseNlpParser
import com.example.ai.VnText
import com.example.ai.VoiceTransactionEngine
import com.example.data.local.InitialCategories
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.payment.vietqr.VietQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class VietnameseParsingTest {

    private val accounts = listOf(
        Account(id = 1, name = "Tiền mặt", type = AccountType.CASH, balance = 0.0, bankId = "cash"),
        Account(id = 2, name = "Vietcombank", type = AccountType.BANK, balance = 0.0, bankCode = "VCB", bankId = "vcb"),
        Account(id = 3, name = "MoMo", type = AccountType.E_WALLET, balance = 0.0, bankCode = "MOMO", bankId = "momo"),
        Account(id = 4, name = "VPBank", type = AccountType.BANK, balance = 0.0, bankCode = "VPB", bankId = "vpbank")
    )

    // The real default list, with ids assigned like Room would.
    private val categories = InitialCategories.getDefaultCategories().mapIndexed { i, c -> c.copy(id = i + 1L) }

    private fun amountOf(text: String) = VnText.extractAmount(text)

    @Test
    fun amounts() {
        assertEquals(50_000.0, amountOf("ăn sáng 50k")!!, 0.1)
        assertEquals(180_000.0, amountOf("ăn sáng 180k")!!, 0.1)
        assertEquals(184_000.0, amountOf("mua đồ 184k")!!, 0.1)
        assertEquals(70_000.0, amountOf("cà phê 7 chục")!!, 0.1)
        assertEquals(70_000.0, amountOf("cà phê bảy chục")!!, 0.1)
        assertEquals(184_000.0, amountOf("một trăm tám mươi tư k")!!, 0.1)
        assertEquals(50_000.0, amountOf("50 nghìn")!!, 0.1)
        assertEquals(2_500_000.0, amountOf("2tr5")!!, 0.1)
        assertEquals(1_200_000.0, amountOf("1 triệu 2")!!, 0.1)
        assertEquals(2_500_000.0, amountOf("2,5 củ")!!, 0.1)
        assertEquals(1_500_000.0, amountOf("tiền nhà 1.500.000")!!, 0.1)
        assertEquals(350_000.0, amountOf("350000")!!, 0.1)
        assertEquals(500_000.0, amountOf("5 lít")!!, 0.1)
        assertEquals(35_000.0, amountOf("Ăn sáng 35")!!, 0.1)
        assertEquals(30_000_000.0, amountOf("lương 30 triệu 15/10")!!, 0.1)
        // Units glued to other words are not money units.
        assertEquals(45_000.0, amountOf("2 trà sữa 45k")!!, 0.1)
        assertEquals(40_000.0, amountOf("5 kg cam 40k")!!, 0.1)
    }

    @Test
    fun typeDetection_noFalseIncome() {
        assertEquals(TransactionType.EXPENSE, VnText.detectType("bánh mì 20k"))
        assertEquals(TransactionType.EXPENSE, VnText.detectType("mua thuốc 50k"))
        assertEquals(TransactionType.EXPENSE, VnText.detectType("thuê nhà 3tr"))
        assertEquals(TransactionType.EXPENSE, VnText.detectType("trả nợ cho Nam 500k"))
        assertEquals(TransactionType.EXPENSE, VnText.detectType("chuyển tiền cho mẹ 2tr"))
        assertEquals(TransactionType.EXPENSE, VnText.detectType("đi chuyến xe 100k"))
        assertEquals(TransactionType.INCOME, VnText.detectType("nhận lương 30tr"))
        assertEquals(TransactionType.INCOME, VnText.detectType("bán đồ cũ 300k"))
        assertEquals(TransactionType.INCOME, VnText.detectType("thu nợ anh Tuấn 1tr"))
        assertEquals(TransactionType.TRANSFER, VnText.detectType("chuyển 2tr từ vcb sang momo"))
        assertEquals(TransactionType.TRANSFER, VnText.detectType("rút tiền 1tr"))
    }

    @Test
    fun accounts_matchedByWordAndInOrder() {
        // "vi" inside "Vietcombank" must not pick the cash wallet.
        assertEquals(listOf(2L), VnText.matchAccountsInOrder("trả bằng vietcombank", accounts).map { it.id })
        assertEquals(listOf(3L, 2L), VnText.matchAccountsInOrder("chuyển từ momo sang vcb", accounts).map { it.id })
        assertEquals(listOf(4L), VnText.matchAccountsInOrder("quẹt vpbank 200k", accounts).map { it.id })
        assertTrue(VnText.matchAccountsInOrder("ăn phở 40k", accounts).isEmpty())
    }

    @Test
    fun typedParser() {
        val p = VietnameseNlpParser.parse("chuyển 2tr từ vcb sang momo", accounts, categories)!!
        assertEquals(TransactionType.TRANSFER, p.type)
        assertEquals(2L, p.sourceAccountId)
        assertEquals(3L, p.destinationAccountId)

        val coffee = VietnameseNlpParser.parse("cà phê 35k momo", accounts, categories)!!
        assertEquals(TransactionType.EXPENSE, coffee.type)
        assertEquals("Cà phê", coffee.categoryName)
        assertEquals(3L, coffee.sourceAccountId)

        val fuel = VietnameseNlpParser.parse("đổ xăng 70k", accounts, categories)!!
        assertEquals("Xăng xe", fuel.categoryName)

        val salary = VietnameseNlpParser.parse("nhận lương 30 triệu vcb", accounts, categories)!!
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals("Lương", salary.categoryName)

        // "thanh toán" contains "an" but is not food: unknown stays unknown.
        val unknown = VietnameseNlpParser.parse("thanh toán 200k", accounts, categories)!!
        assertNull(unknown.categoryId)

        assertNull(VietnameseNlpParser.parse("ăn sáng 30k", emptyList(), categories))
    }

    @Test
    fun voiceTransferNeedsTwoAccounts() {
        val transfer = VoiceTransactionEngine.processVoiceCommand("rút 1 triệu từ vcb sang tiền mặt", null, accounts, categories)!!
        assertEquals(TransactionType.TRANSFER, transfer.type)
        assertEquals(2L, transfer.accountId)
        assertEquals(1L, transfer.destinationAccountId)
        assertTrue(transfer.isComplete)

        val spend = VoiceTransactionEngine.processVoiceCommand("ăn trưa 45 nghìn tiền mặt", null, accounts, categories)!!
        assertEquals(TransactionType.EXPENSE, spend.type)
        assertEquals(1L, spend.accountId)
        assertEquals("Ăn trưa", spend.categoryName)
    }

    @Test
    fun vietQrCrcIsVerified() {
        val payload = VietQrParser.generateVietQrPayload("970436", "0123456789", 50_000.0, "an trua")
        val ok = VietQrParser.parse(payload)
        assertNotNull(ok)
        assertTrue(ok!!.crcValid)
        assertEquals(50_000.0, ok.amount!!, 0.1)

        // Tamper with the amount, keep the old checksum.
        val tampered = VietQrParser.parse(payload.replace("540550000", "540590000"))
        assertNotNull(tampered)
        assertFalse(tampered!!.crcValid)
    }

    @Test
    fun billDueDateKeepsDayOfMonth() {
        val jan31 = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 31, 9, 0, 0) }.timeInMillis
        val feb = Calendar.getInstance().apply { timeInMillis = FinanceRepository.nextDueDateAfter(jan31, 31) }
        assertEquals(Calendar.FEBRUARY, feb.get(Calendar.MONTH))
        assertEquals(28, feb.get(Calendar.DAY_OF_MONTH))
        val mar = Calendar.getInstance().apply { timeInMillis = FinanceRepository.nextDueDateAfter(feb.timeInMillis, 31) }
        assertEquals(31, mar.get(Calendar.DAY_OF_MONTH))
    }
}
