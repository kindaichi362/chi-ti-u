package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.screens.ScreenAIAssistant
import com.example.ui.screens.ScreenAccounts
import com.example.ui.screens.ScreenAddTransaction
import com.example.ui.screens.ScreenCategories
import com.example.ui.screens.ScreenDashboard
import com.example.ui.screens.ScreenDebtsAndGoals
import com.example.ui.screens.ScreenReports
import com.example.ui.screens.ScreenTransactions
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.ChiTieuThanhPhatTheme
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import com.example.ui.viewmodel.FinanceViewModel

enum class MainDestination(val label: String, val icon: ImageVector?, val tag: String) {
    DASHBOARD("Trang chủ", Icons.Default.Home, "tab_dashboard"),
    TRANSACTIONS("Giao dịch", Icons.AutoMirrored.Filled.ReceiptLong, "tab_transactions"),
    REPORTS("Báo cáo", Icons.Default.PieChart, "tab_reports"),
    ACCOUNTS("Tài khoản", Icons.Default.Wallet, "tab_accounts"),
    ADD_TRANSACTION("Thêm", null, "tab_add"),
    AI_ASSISTANT("Trợ lý AI", null, "tab_ai"),
    PLANS("Kế hoạch", null, "tab_plans"),
    CATEGORIES("Danh mục", null, "tab_categories")
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
            ChiTieuThanhPhatTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel, startDestination: MainDestination = MainDestination.DASHBOARD) {
    val backStack = remember { mutableStateListOf(MainDestination.DASHBOARD).apply { if (startDestination != MainDestination.DASHBOARD) add(startDestination) } }
    val currentDestination = backStack.lastOrNull() ?: MainDestination.DASHBOARD
    var showVoiceBottomSheet by remember { mutableStateOf(false) }
    // Which tab of the Reports screen to open (0 = analysis, 1 = budgets).
    var reportsTab by remember { mutableIntStateOf(0) }
    // Question to ask right away when the assistant opens from an insight card.
    var pendingQuestion by remember { mutableStateOf<String?>(null) }

    fun navigateTo(dest: MainDestination) {
        if (dest == MainDestination.DASHBOARD) {
            backStack.clear()
            backStack.add(MainDestination.DASHBOARD)
        } else if (dest in BOTTOM_TABS) {
            // Switching bottom tabs replaces the stack, so Back always goes tab → Home → exit
            // instead of replaying every tab the user tapped through.
            backStack.clear()
            backStack.add(MainDestination.DASHBOARD)
            backStack.add(dest)
        } else if (backStack.lastOrNull() != dest) {
            backStack.add(dest)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        } else if (backStack.lastOrNull() != MainDestination.DASHBOARD) {
            backStack.clear()
            backStack.add(MainDestination.DASHBOARD)
        }
    }

    BackHandler(enabled = backStack.size > 1 || currentDestination != MainDestination.DASHBOARD) {
        navigateBack()
    }

    val showBottomBar = currentDestination == MainDestination.DASHBOARD || currentDestination in BOTTOM_TABS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = OledBlack,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    current = currentDestination,
                    onSelect = { navigateTo(it) },
                    onAdd = { navigateTo(MainDestination.ADD_TRANSACTION) },
                    onVoice = { showVoiceBottomSheet = true }
                )
            }
        }
    ) { innerPadding ->
        val padded = Modifier.padding(innerPadding)
        when (currentDestination) {
            MainDestination.DASHBOARD -> ScreenDashboard(
                viewModel = viewModel,
                onNavigateToTransactions = { navigateTo(MainDestination.TRANSACTIONS) },
                onNavigateToAddTransaction = { navigateTo(MainDestination.ADD_TRANSACTION) },
                onNavigateToAccounts = { navigateTo(MainDestination.ACCOUNTS) },
                onNavigateToBudgets = { reportsTab = 1; navigateTo(MainDestination.REPORTS) },
                onNavigateToAiAssistant = { question -> pendingQuestion = question; navigateTo(MainDestination.AI_ASSISTANT) },
                onNavigateToPlans = { navigateTo(MainDestination.PLANS) },
                modifier = padded
            )
            MainDestination.TRANSACTIONS -> ScreenTransactions(
                viewModel = viewModel,
                onNavigateToAddTransaction = { navigateTo(MainDestination.ADD_TRANSACTION) },
                modifier = padded
            )
            MainDestination.REPORTS -> ScreenReports(
                viewModel = viewModel,
                selectedTab = reportsTab,
                onSelectTab = { reportsTab = it },
                onOpenCategories = { navigateTo(MainDestination.CATEGORIES) },
                modifier = padded
            )
            MainDestination.ACCOUNTS -> ScreenAccounts(
                viewModel = viewModel,
                onOpenPlans = { navigateTo(MainDestination.PLANS) },
                modifier = padded
            )
            MainDestination.ADD_TRANSACTION -> ScreenAddTransaction(
                viewModel = viewModel,
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
                modifier = padded
            )
            MainDestination.CATEGORIES -> ScreenCategories(
                viewModel = viewModel,
                onNavigateBack = { navigateBack() }
            )
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
 * Five slots: Home · Transactions · ➕ · Reports · Accounts. The centre button records a
 * transaction on tap and starts voice input on long-press, so the most frequent action is one
 * thumb-reach away on every main screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBar(
    current: MainDestination,
    onSelect: (MainDestination) -> Unit,
    onAdd: () -> Unit,
    onVoice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppleSurface1)
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.hairline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(MainDestination.DASHBOARD, current, onSelect)
            NavItem(MainDestination.TRANSACTIONS, current, onSelect)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Tone.accent)
                    .combinedClickable(
                        role = Role.Button,
                        onClickLabel = "Ghi giao dịch",
                        onLongClickLabel = "Nói để ghi",
                        onClick = onAdd,
                        onLongClick = onVoice
                    )
                    .testTag("fab_add_transaction"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ghi giao dịch (giữ để nói)", tint = Color.Black, modifier = Modifier.size(28.dp))
            }
            NavItem(MainDestination.REPORTS, current, onSelect)
            NavItem(MainDestination.ACCOUNTS, current, onSelect)
        }
    }
}

@Composable
private fun NavItem(dest: MainDestination, current: MainDestination, onSelect: (MainDestination) -> Unit) {
    val selected = dest == current
    val color = if (selected) Tone.accent else AppleTextSecondary
    Column(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(role = Role.Tab) { onSelect(dest) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(dest.tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(dest.icon!!, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(dest.label, fontSize = TextSize.xs, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
