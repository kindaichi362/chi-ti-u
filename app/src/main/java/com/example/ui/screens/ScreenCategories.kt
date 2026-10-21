package com.example.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import com.example.data.local.InitialCategories
import com.example.ui.theme.Tone
import com.example.ui.theme.TextSize
import com.example.ui.theme.Space
import com.example.ui.components.compactVnd
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.RowDivider
import com.example.ui.components.IconTile
import com.example.ui.components.AppCard
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.formatVnd
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleSurface4
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenCategories(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()

    var selectedTab by remember { mutableStateOf(CategoryType.EXPENSE) }
    var expandedParentId by remember { mutableStateOf<Long?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var parentForSubCategory by remember { mutableStateOf<Category?>(null) }

    // Dialog state for Add/Edit
    var inputName by remember { mutableStateOf("") }
    var inputEmoji by remember { mutableStateOf("🍜") }
    var inputColorHex by remember { mutableStateOf("#EF4444") }
    var inputParentId by remember { mutableStateOf<Long?>(null) }

    val filteredList = categories.filter { it.type == selectedTab }
    val rootCategories = filteredList.filter { it.parentId == null }

    val sampleColors = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4",
        "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899", "#78716C"
    )

    val sampleEmojis = listOf(
        "🍜", "🍳", "🍱", "🍲", "☕", "🥤", "⛽", "🛒", "🏠", "💡",
        "🚗", "💊", "🎓", "🎮", "✈️", "👨‍👩‍👧", "👶", "💳", "📱", "📦",
        "🎁", "💰", "📈", "📄", "🔧", "💼", "❤️"
    )

    // Calculate spent per category
    val currentMonth = FinanceViewModel.monthKeyOf(System.currentTimeMillis())
    // Totals per category computed once per data change instead of scanning every transaction
    // for every row. Only money that really moved counts: transfers, balance adjustments and
    // pending/cancelled QR payments are excluded.
    val spentByCategory = remember(transactions, currentMonth) {
        transactions
            .filter {
                it.countsInReports && it.categoryId != null &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME) &&
                    FinanceViewModel.monthKeyOf(it.timestamp) == currentMonth
            }
            .groupBy { it.categoryId!! }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    fun getCategorySpent(catId: Long): Double {
        // sum direct txs + subcategory txs
        val subCatIds = categories.filter { it.parentId == catId }.map { it.id }
        return (spentByCategory[catId] ?: 0.0) + subCatIds.sumOf { spentByCategory[it] ?: 0.0 }
    }

    fun getCategoryBudget(catId: Long): Double? {
        return budgets.firstOrNull { it.categoryId == catId && it.monthYear == currentMonth }?.amountLimit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản lý danh mục", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppleTextPrimary)
                        Text("${categories.size} danh mục • Hệ thống chuẩn", fontSize = 12.sp, color = AppleTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = AppleTextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.restoreDefaultCategories { added ->
                                val msg = if (added > 0) "Đã thêm lại $added danh mục mặc định" else "Đã có đủ danh mục mặc định"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Cập nhật 100+ danh mục", tint = AppleTextSecondary)
                    }
                    IconButton(
                        onClick = {
                            categoryToEdit = null
                            parentForSubCategory = null
                            inputName = ""
                            inputEmoji = if (selectedTab == CategoryType.EXPENSE) "🍜" else "💰"
                            inputColorHex = if (selectedTab == CategoryType.EXPENSE) "#EF4444" else "#10B981"
                            inputParentId = null
                            isAddingNewCategory = true
                        },
                        modifier = Modifier.testTag("btn_add_category")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm danh mục", tint = AppleOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OledBlack)
            )
        },
        containerColor = OledBlack
    ) { innerPadding ->
        fun openEditor(cat: Category) {
            categoryToEdit = cat
            inputName = cat.name
            inputEmoji = cat.emoji ?: "🍜"
            inputColorHex = cat.colorHex
            inputParentId = cat.parentId
            isAddingNewCategory = true
        }

        // Groups in the standard order; custom groups after them.
        val grouped = remember(filteredList) {
            filteredList.filter { it.parentId == null }
                .groupBy { it.groupName ?: "Khác" }
                .toList()
                .sortedBy { (g, _) -> InitialCategories.groupOrder.indexOf(g).let { if (it < 0) 99 else it } }
        }

        LazyColumn(
            modifier = modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            item {
                SegmentedTabs(
                    options = listOf(
                        "Chi tiêu (${categories.count { it.type == CategoryType.EXPENSE }})",
                        "Thu nhập (${categories.count { it.type == CategoryType.INCOME }})"
                    ),
                    selected = if (selectedTab == CategoryType.EXPENSE) 0 else 1,
                    onSelect = { selectedTab = if (it == 0) CategoryType.EXPENSE else CategoryType.INCOME }
                )
                Spacer(Modifier.height(Space.xs))
                Text("⭐ = hiện đầu tiên khi ghi giao dịch. Chạm một mục để sửa.", fontSize = TextSize.xs, color = AppleTextSecondary)
            }

            for ((groupName, cats) in grouped) {
                item(key = "g_$groupName") {
                    val groupSpent = cats.sumOf { getCategorySpent(it.id) }
                    Row(Modifier.fillMaxWidth().padding(top = Space.sm, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(parseColorSafe(cats.first().colorHex)))
                        Spacer(Modifier.width(Space.sm))
                        Text(groupName, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary, modifier = Modifier.weight(1f))
                        if (groupSpent > 0) Text("${if (selectedTab == CategoryType.EXPENSE) "Đã chi" else "Đã thu"} ${compactVnd(groupSpent)}", fontSize = TextSize.xs, color = AppleTextSecondary)
                    }
                    AppCard(contentPadding = Space.lg) {
                        val rows = cats.flatMap { root -> listOf(root) + categories.filter { it.parentId == root.id } }
                        rows.forEachIndexed { i, cat ->
                            if (i > 0) RowDivider()
                            val spent = getCategorySpent(cat.id)
                            val budget = getCategoryBudget(cat.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openEditor(cat) }
                                    .padding(vertical = Space.sm, horizontal = 0.dp)
                                    .padding(start = if (cat.parentId != null) Space.xxl else 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ui.components.CategoryTile(cat, size = 36.dp)
                                Spacer(Modifier.width(Space.md))
                                Column(Modifier.weight(1f)) {
                                    Text(cat.name, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = AppleTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val sub = when {
                                        budget != null && budget > 0 -> "${compactVnd(spent)} / ${compactVnd(budget)} ngân sách"
                                        spent > 0 -> "Tháng này ${compactVnd(spent)}"
                                        else -> null
                                    }
                                    if (sub != null) Text(sub, fontSize = TextSize.xs, color = if (budget != null && spent > budget) Tone.danger else AppleTextSecondary)
                                }
                                IconButton(onClick = { viewModel.toggleCategoryPriority(cat.id, !cat.isPriority) }) {
                                    Icon(
                                        imageVector = if (cat.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (cat.isPriority) "Bỏ ưu tiên" else "Ưu tiên",
                                        tint = if (cat.isPriority) Tone.accent else AppleTextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add / Edit Category Dialog
        if (isAddingNewCategory) {
            AlertDialog(
                onDismissRequest = { isAddingNewCategory = false },
                containerColor = AppleSurface2,
                title = {
                    Text(
                        text = when {
                            categoryToEdit != null -> "Chỉnh sửa danh mục"
                            inputParentId != null -> "Tạo danh mục con cho ${parentForSubCategory?.name ?: ""}"
                            else -> "Tạo danh mục mới"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Tên danh mục", fontSize = 13.sp) },
                            placeholder = { Text("Ví dụ: Cà phê sáng, Trà sữa...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppleOrange,
                                unfocusedBorderColor = AppleBorder
                            )
                        )

                        Text("Chọn Emoji đại diện:", fontSize = 12.sp, color = AppleTextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (em in sampleEmojis) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (inputEmoji == em) AppleOrange.copy(alpha = 0.3f) else AppleSurface3)
                                        .border(1.dp, if (inputEmoji == em) AppleOrange else Color.Transparent, CircleShape)
                                        .clickable { inputEmoji = em },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = em, fontSize = 16.sp)
                                }
                            }
                        }

                        Text("Chọn màu chủ đạo:", fontSize = 12.sp, color = AppleTextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (hex in sampleColors) {
                                val c = parseColorSafe(hex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            2.dp,
                                            if (inputColorHex == hex) Color.White else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { inputColorHex = hex }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputName.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (categoryToEdit != null) {
                                viewModel.updateCategory(
                                    categoryToEdit!!.copy(
                                        name = inputName,
                                        emoji = inputEmoji,
                                        colorHex = inputColorHex
                                    )
                                )
                                Toast.makeText(context, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addCategory(
                                    name = inputName,
                                    type = selectedTab,
                                    iconName = "folder",
                                    colorHex = inputColorHex,
                                    emoji = inputEmoji,
                                    parentId = inputParentId
                                )
                                Toast.makeText(context, "Đã tạo danh mục thành công!", Toast.LENGTH_SHORT).show()
                            }
                            isAddingNewCategory = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
                    ) {
                        Text("Lưu", color = com.example.ui.theme.OnAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isAddingNewCategory = false }) {
                        Text("Hủy", color = AppleTextSecondary)
                    }
                }
            )
        }
    }
}
