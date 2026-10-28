package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.PaymentStatus
import com.example.data.model.RecurringBill
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.theme.ChiTieuThanhPhatTheme
import com.example.ui.components.CategoryChoiceChip
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/**
 * Renders the main screens with realistic data and writes PNGs to app/build/screenshots, so the
 * UI can be reviewed without a device. Not an assertion test: it fails only if a screen crashes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun daysAgo(d: Int, hour: Int = 12): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -d); set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 15)
    }.timeInMillis

    private fun seed(vm: FinanceViewModel) = runBlocking {
        val repo = vm.repository
        repo.ensureSeeded()
        val cats = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext()).categoryDao().getAllCategoriesOnce().associateBy { it.name }
        fun c(name: String) = cats.getValue(name).id
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val dueDay = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }.get(Calendar.DAY_OF_MONTH)

        val vcb = repo.insertAccount(Account(name = "Vietcombank", type = AccountType.BANK, balance = 0.0, bankId = "vcb", bankCode = "VCB", accountNumberMasked = "•••• 8899"))
        val mb = repo.insertAccount(Account(name = "MB Bank", type = AccountType.BANK, balance = 12_400_000.0, bankId = "mb", bankCode = "MB"))
        val momo = repo.insertAccount(Account(name = "Ví MoMo", type = AccountType.E_WALLET, balance = 850_000.0, bankId = "momo", bankCode = "MOMO"))
        val card = repo.insertAccount(Account(name = "Thẻ Techcombank Visa", type = AccountType.CREDIT_CARD, balance = 0.0, bankId = "tcb",
            accountNumberMasked = "•••• 4021", creditLimit = 50_000_000.0, statementDay = 20, dueDay = dueDay))
        repo.addInstallment(com.example.data.model.CardInstallment(
            cardId = card, name = "Tai nghe Sony", totalAmount = 6_500_000.0, monthlyAmount = 1_200_000.0, monthCount = 6,
            firstDueDate = daysAgo(-3)
        ))
        repo.reconcileAccountBalance(1, 1_250_000.0)

        fun tx(type: TransactionType, amount: Double, acc: Long, cat: String?, note: String, d: Int, h: Int = 12) = runBlocking {
            repo.addTransaction(Transaction(type = type, amount = amount, sourceAccountId = acc, categoryId = cat?.let(::c), note = note, timestamp = daysAgo(d, h)))
        }
        tx(TransactionType.INCOME, 32_000_000.0, vcb, "Lương", "Lương tháng", today - 1, 9)
        // Last month, for the comparison
        tx(TransactionType.EXPENSE, 2_100_000.0, vcb, "Đi chợ / siêu thị", "Siêu thị", today + 12)
        tx(TransactionType.EXPENSE, 900_000.0, momo, "Cà phê", "Cà phê nửa đầu tháng", today + 15)
        // This month
        tx(TransactionType.EXPENSE, 45_000.0, 1, "Ăn sáng", "Phở bò", 0, 7)
        tx(TransactionType.EXPENSE, 55_000.0, momo, "Cà phê", "Highlands", 0, 10)
        tx(TransactionType.EXPENSE, 6_500_000.0, card, "Đồ điện tử", "Tai nghe Sony WH-1000XM5 mua online", 1, 20)
        tx(TransactionType.EXPENSE, 350_000.0, card, "Nhà hàng / liên hoan", "Lẩu với team", 1, 19)
        tx(TransactionType.EXPENSE, 4_500_000.0, vcb, "Tiền thuê nhà", "Nhà tháng này", 2, 8)
        tx(TransactionType.EXPENSE, 80_000.0, 1, "Xăng xe", "Đổ xăng", 2, 18)
        tx(TransactionType.EXPENSE, 1_850_000.0, mb, "Đi chợ / siêu thị", "WinMart", 3, 17)
        tx(TransactionType.EXPENSE, 12_000_000.0, card, "Vé máy bay", "Vé Đà Nẵng cả nhà", 4, 21)
        tx(TransactionType.EXPENSE, 620_000.0, vcb, "Tiền điện", "EVN", 5, 9)
        repo.addTransaction(Transaction(type = TransactionType.TRANSFER, amount = 1_000_000.0, sourceAccountId = vcb, destinationAccountId = momo, timestamp = daysAgo(1, 8)))
        repo.addTransaction(Transaction(type = TransactionType.EXPENSE, amount = 320_000.0, sourceAccountId = mb, categoryId = c("Quần áo"), note = "QR shop quần áo", paymentStatus = PaymentStatus.PENDING_PAYMENT, timestamp = daysAgo(0, 11)))

        val month = FinanceViewModel.monthKeyOf(System.currentTimeMillis())
        repo.upsertBudget(c("Cà phê"), 600_000.0, month)
        repo.upsertBudget(c("Đi chợ / siêu thị"), 3_000_000.0, month)
        repo.upsertBudget(c("Nhà hàng / liên hoan"), 1_500_000.0, month)
        repo.upsertBudget(c("Xăng xe"), 800_000.0, month)
        repo.insertBill(RecurringBill(name = "Internet FPT", amount = 250_000.0, accountId = vcb, categoryId = c("Internet"), dayOfMonth = 25, nextDueDate = daysAgo(-2)))
        repo.insertDebt(Debt(type = DebtType.OWED_TO_ME, personName = "Anh Tuấn", initialAmount = 3_000_000.0, remainingAmount = 2_000_000.0))
    }

    private fun shoot(name: String, vm: FinanceViewModel, start: MainDestination, dark: Boolean = false, accountId: Long? = null) {
        compose.setContent { ChiTieuThanhPhatTheme(darkTheme = dark) { MainAppScreen(viewModel = vm, startDestination = start, startAccountId = accountId) } }
        // Robolectric pauses the main looper; pump it so Room flows reach the ViewModel state.
        repeat(40) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(50)
            compose.waitForIdle()
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { root.draw(android.graphics.Canvas(bmp)) }
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun run(dest: MainDestination, suffix: String = "", dark: Boolean = false, accountId: Long? = null) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val vm = FinanceViewModel(app)
        seed(vm)
        shoot(dest.name.lowercase() + suffix, vm, dest, dark, accountId)
    }

    @Test fun dashboard() = run(MainDestination.DASHBOARD)
    @Test fun dashboardExpenseOpensMonthTransactions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val vm = FinanceViewModel(app)
        seed(vm)
        shoot("dashboard_before_expense", vm, MainDestination.DASHBOARD)
        compose.onNodeWithTag("monthly_expense_card").performClick()
        compose.waitForIdle()
        assertEquals(TransactionType.EXPENSE, vm.txFilter.value.type)
        assertEquals(0, vm.txFilter.value.monthOffset)
        assertEquals(true, vm.txFilter.value.settledOnly)
        compose.onNodeWithText("Chi trong tháng").assertExists()
        compose.onNodeWithText("Giao dịch đã hoàn tất").assertExists()
    }
    @Test fun dashboardIncomeOpensMonthTransactions() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val vm = FinanceViewModel(app)
        seed(vm)
        shoot("dashboard_before_income", vm, MainDestination.DASHBOARD)
        compose.onNodeWithTag("monthly_income_card").performClick()
        compose.waitForIdle()
        assertEquals(TransactionType.INCOME, vm.txFilter.value.type)
        assertEquals(0, vm.txFilter.value.monthOffset)
        assertEquals(true, vm.txFilter.value.settledOnly)
        compose.onNodeWithText("Thu trong tháng").assertExists()
        compose.onNodeWithText("Giao dịch đã hoàn tất").assertExists()
    }
    @Test fun transactions() = run(MainDestination.TRANSACTIONS)
    @Test fun categoryChoiceIcon() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val vm = FinanceViewModel(app)
        seed(vm)
        val db = AppDatabase.getDatabase(app)
        val categories = runBlocking { db.categoryDao().getAllCategoriesOnce() }
        val breakfast = categories.first { it.name == "Ăn sáng" }
        compose.setContent {
            ChiTieuThanhPhatTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Row {
                    CategoryChoiceChip(category = breakfast, selected = true, onClick = {})
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("category_choice_${breakfast.id}").assertExists()
        compose.onNodeWithText("Ăn sáng").assertExists()
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { root.draw(android.graphics.Canvas(bmp)) }
        File(outDir, "category_choice_icon.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun reports() = run(MainDestination.REPORTS)
    @Test fun reportsCalendar() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        AppDatabase.resetForTests()
        app.deleteDatabase("chi_tieu_thanh_phat_db")
        val vm = FinanceViewModel(app)
        seed(vm)
        shoot("reports_before_calendar", vm, MainDestination.REPORTS)
        compose.onNodeWithText("Lịch thu chi").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Tổng thu").assertExists()
        compose.onNodeWithText("Tổng chi").assertExists()
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { root.draw(android.graphics.Canvas(bmp)) }
        File(outDir, "reports_calendar.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun accounts() = run(MainDestination.ACCOUNTS)
    @Test fun assistant() = run(MainDestination.AI_ASSISTANT)
    @Test fun add() = run(MainDestination.ADD_TRANSACTION)
    @Test fun categories() = run(MainDestination.CATEGORIES)
    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi") fun categoriesNarrow() = run(MainDestination.CATEGORIES, "_360")
    @Test fun plans() = run(MainDestination.PLANS)

    // Tall renders show the whole scrolling page in one image.
    @Test @Config(qualifiers = "w393dp-h2000dp-xxhdpi") fun dashboardFull() = run(MainDestination.DASHBOARD, "_full")
    @Test @Config(qualifiers = "w393dp-h2000dp-xxhdpi") fun accountsFull() = run(MainDestination.ACCOUNTS, "_full")
    @Test @Config(qualifiers = "w393dp-h1700dp-xxhdpi") fun transactionsFull() = run(MainDestination.TRANSACTIONS, "_full")
    @Test @Config(qualifiers = "w393dp-h2000dp-xxhdpi") fun reportsFull() = run(MainDestination.REPORTS, "_full")
    @Test @Config(qualifiers = "w393dp-h1900dp-xxhdpi") fun addFull() = run(MainDestination.ADD_TRANSACTION, "_full")
    @Test @Config(qualifiers = "w393dp-h2000dp-xxhdpi") fun dashboardDark() = run(MainDestination.DASHBOARD, "_dark", dark = true)
    @Test @Config(qualifiers = "w360dp-h780dp-xxhdpi") fun dashboardNarrow() = run(MainDestination.DASHBOARD, "_360")
    @Test fun settings() = run(MainDestination.SETTINGS)
    @Test @Config(qualifiers = "w393dp-h1800dp-xxhdpi") fun cardDetail() = run(MainDestination.ACCOUNT_DETAIL, "_card", accountId = 5)

    /** The recording window shown while the mic is open. */
    @Test fun voiceRecording() {
        compose.setContent {
            ChiTieuThanhPhatTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) { com.example.ui.components.RecordingPanel(false, "Ăn sáng 30 nghìn tiền mặt", 0.6f, {}, {}) }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val root = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { root.draw(android.graphics.Canvas(bmp)) }
        File(outDir, "voice_recording.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
