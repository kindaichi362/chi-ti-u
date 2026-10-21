package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.screens.ScreenAIAssistant
import com.example.ui.screens.ScreenAccounts
import com.example.ui.screens.ScreenAddTransaction
import com.example.ui.screens.ScreenAnalytics
import com.example.ui.screens.ScreenBudgets
import com.example.ui.screens.ScreenCategories
import com.example.ui.screens.ScreenDashboard
import com.example.ui.screens.ScreenTransactions
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleDarkBackground
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface4
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.ChiTieuThanhPhatTheme
import com.example.ui.theme.OledBlack
import com.example.ui.viewmodel.FinanceViewModel

enum class MainDestination(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Trang chủ", Icons.Default.Home, "tab_dashboard"),
    TRANSACTIONS("Giao dịch", Icons.Default.ReceiptLong, "tab_transactions"),
    ANALYTICS("Báo cáo", Icons.Default.PieChart, "tab_analytics"),
    BUDGETS("Ngân sách", Icons.Default.AccountBalanceWallet, "tab_budgets"),
    ADD_TRANSACTION("Thêm", Icons.Default.Add, "tab_add"),
    AI_ASSISTANT("Trợ lý AI", Icons.Default.Add, "tab_ai"),
    ACCOUNTS("Tài khoản", Icons.Default.Add, "tab_accounts"),
    CATEGORIES("Danh mục", Icons.Default.Category, "tab_categories")
}

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
fun MainAppScreen(viewModel: FinanceViewModel) {
    // Backstack implementation so pressing back returns through history to Home before exiting
    val backStack = remember { mutableStateListOf(MainDestination.DASHBOARD) }
    val currentDestination = backStack.lastOrNull() ?: MainDestination.DASHBOARD
    var showVoiceBottomSheet by remember { mutableStateOf(false) }

    fun navigateTo(dest: MainDestination) {
        if (dest == MainDestination.DASHBOARD) {
            backStack.clear()
            backStack.add(MainDestination.DASHBOARD)
        } else {
            if (backStack.lastOrNull() != dest) {
                backStack.add(dest)
            }
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

    // Intercept back button: go to previous screen, then to Home (DASHBOARD), only exit on Home
    BackHandler(enabled = backStack.size > 1 || currentDestination != MainDestination.DASHBOARD) {
        navigateBack()
    }

    // 4 Bottom Bar destinations (Removed Kế hoạch / PLANNING as requested)
    val bottomBarDestinations = listOf(
        MainDestination.DASHBOARD,
        MainDestination.TRANSACTIONS,
        MainDestination.ANALYTICS,
        MainDestination.BUDGETS
    )

    val showBottomBar = currentDestination in bottomBarDestinations

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = OledBlack,
        floatingActionButton = {
            if (showBottomBar) {
                // Voice button placed right beside the small + expense button with immediate speech start
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Voice Recording button (auto-starts listening immediately)
                    FloatingActionButton(
                        onClick = { showVoiceBottomSheet = true },
                        shape = CircleShape,
                        containerColor = AppleOrange,
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .testTag("fab_voice_transaction")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Nói để ghi chép",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nói",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // 2. Small + Add Transaction button
                    FloatingActionButton(
                        onClick = { navigateTo(MainDestination.ADD_TRANSACTION) },
                        shape = CircleShape,
                        containerColor = AppleSurface2,
                        contentColor = AppleOrange,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm giao dịch",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                // Apple Floating Capsule Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = AppleSurface2,
                        border = BorderStroke(1.dp, AppleBorder),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomBarDestinations.forEach { dest ->
                                val isSelected = currentDestination == dest
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AppleSurface4 else Color.Transparent)
                                        .clickable { navigateTo(dest) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag(dest.tag),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label,
                                            tint = if (isSelected) AppleOrange else AppleTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dest.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) AppleOrange else AppleTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            MainDestination.DASHBOARD -> {
                ScreenDashboard(
                    viewModel = viewModel,
                    onNavigateToTransactions = { navigateTo(MainDestination.TRANSACTIONS) },
                    onNavigateToAddTransaction = { navigateTo(MainDestination.ADD_TRANSACTION) },
                    onNavigateToAccounts = { navigateTo(MainDestination.ACCOUNTS) },
                    onNavigateToBudgets = { navigateTo(MainDestination.BUDGETS) },
                    onNavigateToAiAssistant = { navigateTo(MainDestination.AI_ASSISTANT) },
                    onNavigateToAnalytics = { navigateTo(MainDestination.ANALYTICS) },
                    onNavigateToCategories = { navigateTo(MainDestination.CATEGORIES) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainDestination.TRANSACTIONS -> {
                ScreenTransactions(
                    viewModel = viewModel,
                    onNavigateToAddTransaction = { navigateTo(MainDestination.ADD_TRANSACTION) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainDestination.BUDGETS -> {
                ScreenBudgets(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainDestination.ANALYTICS -> {
                ScreenAnalytics(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainDestination.ADD_TRANSACTION -> {
                ScreenAddTransaction(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
            MainDestination.AI_ASSISTANT -> {
                ScreenAIAssistant(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
            MainDestination.ACCOUNTS -> {
                ScreenAccounts(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
            MainDestination.CATEGORIES -> {
                ScreenCategories(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
        }
    }

    // Dedicated Voice Sheet with auto-start listening
    if (showVoiceBottomSheet) {
        VoiceTransactionBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVoiceBottomSheet = false },
            onTransactionSaved = { showVoiceBottomSheet = false },
            autoStart = true
        )
    }
}
