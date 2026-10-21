package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.screens.ScreenAIAssistant
import com.example.ui.screens.ScreenAccountDetail
import com.example.ui.screens.ScreenAccounts
import com.example.ui.screens.ScreenAddTransaction
import com.example.ui.screens.ScreenCategories
import com.example.ui.screens.ScreenDashboard
import com.example.ui.screens.ScreenDebtsAndGoals
import com.example.ui.screens.ScreenReports
import com.example.ui.screens.ScreenSettings
import com.example.ui.screens.ScreenTransactions
import com.example.ui.theme.AppPalette
import com.example.ui.theme.ChiTieuThanhPhatTheme
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.ThemeMode

enum class MainDestination(val label: String, val icon: ImageVector?, val tag: String) {
    DASHBOARD("Tổng quan", Icons.Outlined.Home, "tab_dashboard"),
    TRANSACTIONS("Giao dịch", Icons.AutoMirrored.Outlined.ReceiptLong, "tab_transactions"),
    REPORTS("Báo cáo", Icons.Outlined.PieChart, "tab_reports"),
    ACCOUNTS("Tài khoản", Icons.Outlined.AccountBalanceWallet, "tab_accounts"),
    ADD_TRANSACTION("Thêm", null, "tab_add"),
    AI_ASSISTANT("Trợ lý AI", null, "tab_ai"),
    PLANS("Kế hoạch", null, "tab_plans"),
    CATEGORIES("Danh mục", null, "tab_categories"),
    ACCOUNT_DETAIL("Tài khoản", null, "tab_account_detail"),
    SETTINGS("Cài đặt", null, "tab_settings")
}

private val BOTTOM_TABS = setOf(
    MainDestination.TRANSACTIONS,
    MainDestination.REPORTS,
    MainDestination.ACCOUNTS
)

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mode by viewModel.themeMode.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            ChiTieuThanhPhatTheme(darkTheme = dark) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel, startDestination: MainDestination = MainDestination.DASHBOARD, startAccountId: Long? = null) {
    val backStack = remember { mutableStateListOf(MainDestination.DASHBOARD).apply { if (startDestination != MainDestination.DASHBOARD) add(startDestination) } }
    val currentDestination = backStack.lastOrNull() ?: MainDestination.DASHBOARD
    var showVoiceBottomSheet by remember { mutableStateOf(false) }
    var reportsTab by rememberSaveable { mutableStateOf(0) }
    var pendingQuestion by remember { mutableStateOf<String?>(null) }
    var openAccountId by rememberSaveable { mutableStateOf(startAccountId) }
    // Type the add screen opens with ("Chuyển tiền" on the Accounts tab opens it as a transfer).
    var addType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var addSourceAccountId by remember { mutableStateOf<Long?>(null) }

    fun navigateTo(dest: MainDestination) {
        when {
            dest == MainDestination.DASHBOARD -> { backStack.clear(); backStack.add(MainDestination.DASHBOARD) }
            dest in BOTTOM_TABS -> {
                // Switching tabs replaces the stack: Back goes tab → Overview → exit.
                backStack.clear(); backStack.add(MainDestination.DASHBOARD); backStack.add(dest)
            }
            backStack.lastOrNull() != dest -> backStack.add(dest)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
        else if (backStack.lastOrNull() != MainDestination.DASHBOARD) { backStack.clear(); backStack.add(MainDestination.DASHBOARD) }
    }

    fun openAdd(type: TransactionType = TransactionType.EXPENSE, sourceAccountId: Long? = null) {
        addType = type
        addSourceAccountId = sourceAccountId
        navigateTo(MainDestination.ADD_TRANSACTION)
    }

    BackHandler(enabled = backStack.size > 1 || currentDestination != MainDestination.DASHBOARD) { navigateBack() }

    val showBottomBar = currentDestination == MainDestination.DASHBOARD || currentDestination in BOTTOM_TABS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppPalette.background,
        // Voice entry is always one tap away on every main tab.
        floatingActionButton = {
            if (showBottomBar) VoiceButton(onClick = { showVoiceBottomSheet = true })
        },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    current = currentDestination,
                    onSelect = { navigateTo(it) },
                    onAdd = { openAdd() },
                    onVoice = { showVoiceBottomSheet = true }
                )
            }
        }
    ) { innerPadding ->
        // On tablets the content keeps a phone-like reading width, centred.
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            val content = Modifier.fillMaxSize().widthIn(max = 720.dp).statusBarsPadding()
            when (currentDestination) {
                MainDestination.DASHBOARD -> ScreenDashboard(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navigateTo(MainDestination.TRANSACTIONS) },
                    onNavigateToAddTransaction = { openAdd() },
                    onNavigateToAccounts = { navigateTo(MainDestination.ACCOUNTS) },
                    onOpenAccount = { openAccountId = it; navigateTo(MainDestination.ACCOUNT_DETAIL) },
                    onNavigateToBudgets = { reportsTab = 1; navigateTo(MainDestination.REPORTS) },
                    onNavigateToAiAssistant = { q -> pendingQuestion = q; navigateTo(MainDestination.AI_ASSISTANT) },
                    onNavigateToPlans = { navigateTo(MainDestination.PLANS) },
                    modifier = content
                )
                MainDestination.TRANSACTIONS -> ScreenTransactions(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { openAdd() },
                    onVoice = { showVoiceBottomSheet = true },
                    modifier = content
                )
                MainDestination.REPORTS -> ScreenReports(
                    viewModel = viewModel,
                    selectedTab = reportsTab,
                    onSelectTab = { reportsTab = it },
                    onOpenCategories = { navigateTo(MainDestination.CATEGORIES) },
                    modifier = content
                )
                MainDestination.ACCOUNTS -> ScreenAccounts(
                    viewModel = viewModel,
                    onOpenAccount = { openAccountId = it; navigateTo(MainDestination.ACCOUNT_DETAIL) },
                    onTransfer = { openAdd(TransactionType.TRANSFER) },
                    onOpenPlans = { navigateTo(MainDestination.PLANS) },
                    onOpenSettings = { navigateTo(MainDestination.SETTINGS) },
                    modifier = content
                )
                MainDestination.ACCOUNT_DETAIL -> ScreenAccountDetail(
                    viewModel = viewModel,
                    accountId = openAccountId,
                    onBack = { navigateBack() },
                    onAddTransaction = { type, accId -> openAdd(type, accId) },
                    modifier = content
                )
                MainDestination.ADD_TRANSACTION -> ScreenAddTransaction(
                    viewModel = viewModel,
                    initialType = addType,
                    initialSourceAccountId = addSourceAccountId,
                    onNavigateBack = { navigateBack() }
                )
                MainDestination.AI_ASSISTANT -> ScreenAIAssistant(
                    viewModel = viewModel,
                    initialQuestion = pendingQuestion,
                    onInitialQuestionConsumed = { pendingQuestion = null },
                    onNavigateBack = { navigateBack() }
                )
                MainDestination.PLANS -> ScreenDebtsAndGoals(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() },
                    modifier = content
                )
                MainDestination.CATEGORIES -> ScreenCategories(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
                MainDestination.SETTINGS -> ScreenSettings(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onOpenCategories = { navigateTo(MainDestination.CATEGORIES) },
                    onOpenPlans = { navigateTo(MainDestination.PLANS) },
                    modifier = content
                )
            }
        }
    }

    if (showVoiceBottomSheet) {
        VoiceTransactionBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVoiceBottomSheet = false },
            onTransactionSaved = { showVoiceBottomSheet = false },
            autoStart = true
        )
    }
}

/**
 * Overview · Transactions · ＋ · Reports · Accounts, fixed at the bottom on every tab. The centre
 * button opens the add screen; long-press starts voice input.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBar(
    current: MainDestination,
    onSelect: (MainDestination) -> Unit,
    onAdd: () -> Unit,
    onVoice: () -> Unit
) {
    val p = AppPalette
    Column(Modifier.fillMaxWidth().background(p.card).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.border))
        Row(
            modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(MainDestination.DASHBOARD, current, onSelect, Modifier.weight(1f))
            NavItem(MainDestination.TRANSACTIONS, current, onSelect, Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(p.accent, CircleShape)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = "Thêm giao dịch",
                            onLongClickLabel = "Nói để ghi",
                            onClick = onAdd,
                            onLongClick = onVoice
                        )
                        .testTag("fab_add_transaction"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch (giữ để nói)", tint = p.onAccent, modifier = Modifier.size(30.dp))
                }
            }
            NavItem(MainDestination.REPORTS, current, onSelect, Modifier.weight(1f))
            NavItem(MainDestination.ACCOUNTS, current, onSelect, Modifier.weight(1f))
        }
    }
}

/** Floating "Nói" pill above the bottom bar: opens voice entry (the user reviews before saving). */
@Composable
private fun VoiceButton(onClick: () -> Unit) {
    val p = AppPalette
    Row(
        modifier = Modifier
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(p.card)
            .border(1.dp, p.border, CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Nhập nhanh bằng giọng nói", onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(start = 14.dp, end = 18.dp)
            .testTag("fab_voice_transaction"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).background(p.accent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("Nói", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
    }
}

@Composable
private fun NavItem(dest: MainDestination, current: MainDestination, onSelect: (MainDestination) -> Unit, modifier: Modifier) {
    val selected = dest == current
    val color = if (selected) AppPalette.accent else AppPalette.textSecondary
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(role = Role.Tab) { onSelect(dest) }
            .testTag(dest.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(dest.icon!!, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(dest.label, fontSize = TextSize.xs, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}
