package com.example.ui.screens

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
    val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
    val monthlyTxs = transactions.filter {
        val txMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(it.date))
        txMonth == currentMonth
    }

    fun getCategorySpent(catId: Long): Double {
        // sum direct txs + subcategory txs
        val subCatIds = categories.filter { it.parentId == catId }.map { it.id }
        return monthlyTxs.filter { it.categoryId == catId || subCatIds.contains(it.categoryId) }
            .sumOf { it.amount }
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
                            viewModel.restoreDefaultCategories()
                            Toast.makeText(context, "Đã cập nhật toàn bộ 100+ danh mục chuẩn!", Toast.LENGTH_SHORT).show()
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Row: Chi tiêu | Thu nhập
            TabRow(
                selectedTabIndex = if (selectedTab == CategoryType.EXPENSE) 0 else 1,
                containerColor = AppleSurface1,
                contentColor = AppleOrange,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, AppleBorder, RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == CategoryType.EXPENSE,
                    onClick = { selectedTab = CategoryType.EXPENSE },
                    text = {
                        Text(
                            text = "Chi tiêu (${categories.count { it.type == CategoryType.EXPENSE }})",
                            fontWeight = if (selectedTab == CategoryType.EXPENSE) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == CategoryType.EXPENSE) AppleOrange else AppleTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == CategoryType.INCOME,
                    onClick = { selectedTab = CategoryType.INCOME },
                    text = {
                        Text(
                            text = "Thu nhập (${categories.count { it.type == CategoryType.INCOME }})",
                            fontWeight = if (selectedTab == CategoryType.INCOME) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == CategoryType.INCOME) AppleGreen else AppleTextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(rootCategories, key = { it.id }) { cat ->
                    val subList = categories.filter { it.parentId == cat.id }
                    val isExpanded = expandedParentId == cat.id
                    val spent = getCategorySpent(cat.id)
                    val budget = getCategoryBudget(cat.id)
                    val percentSpent = if (budget != null && budget > 0) ((spent / budget) * 100).toInt() else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                        border = BorderStroke(1.dp, AppleBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(
                                    category = cat,
                                    size = 44.dp,
                                    isCircle = false
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AppleTextPrimary
                                        )
                                        if (subList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = AppleSurface3
                                            ) {
                                                Text(
                                                    text = "${subList.size} con",
                                                    fontSize = 10.sp,
                                                    color = AppleTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Đã chi: ${formatVnd(spent)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selectedTab == CategoryType.EXPENSE) AppleOrange else AppleGreen
                                    )
                                }

                                if (percentSpent != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (percentSpent > 100) AppleRed.copy(alpha = 0.15f) else AppleOrange.copy(alpha = 0.15f)
                                            )
                                            .border(1.dp, if (percentSpent > 100) AppleRed else AppleOrange, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$percentSpent%",
                                            color = if (percentSpent > 100) AppleRed else AppleOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Priority Star Button (Toggle Ưu tiên)
                                IconButton(
                                    onClick = {
                                        val newPriority = !cat.isPriority
                                        viewModel.toggleCategoryPriority(cat.id, newPriority)
                                        Toast.makeText(
                                            context,
                                            if (newPriority) "Đã thêm \"${cat.name}\" vào mục ưu tiên ⭐" else "Đã bỏ ưu tiên \"${cat.name}\"",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (cat.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Ưu tiên",
                                        tint = if (cat.isPriority) AppleOrange else AppleTextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = {
                                        categoryToEdit = cat
                                        inputName = cat.name
                                        inputEmoji = cat.emoji ?: "🍜"
                                        inputColorHex = cat.colorHex
                                        inputParentId = cat.parentId
                                        isAddingNewCategory = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa", tint = AppleTextSecondary, modifier = Modifier.size(16.dp))
                                }

                                // Expand / Collapse subcategories
                                if (subList.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            expandedParentId = if (isExpanded) null else cat.id
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Mở rộng",
                                            tint = AppleTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Subcategories Tree View
                            AnimatedVisibility(visible = isExpanded && subList.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, start = 20.dp)
                                ) {
                                    subList.forEach { sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "└── ", color = AppleTextSecondary, fontSize = 13.sp)
                                            Text(text = sub.emoji ?: "•", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sub.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AppleTextPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // Priority Star for Subcategory
                                            IconButton(
                                                onClick = {
                                                    val newPriority = !sub.isPriority
                                                    viewModel.toggleCategoryPriority(sub.id, newPriority)
                                                    Toast.makeText(
                                                        context,
                                                        if (newPriority) "Đã thêm \"${sub.name}\" vào ưu tiên ⭐" else "Đã bỏ ưu tiên \"${sub.name}\"",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (sub.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Ưu tiên mục con",
                                                    tint = if (sub.isPriority) AppleOrange else AppleTextSecondary.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    categoryToEdit = sub
                                                    inputName = sub.name
                                                    inputEmoji = sub.emoji ?: "🍳"
                                                    inputColorHex = sub.colorHex
                                                    inputParentId = sub.parentId
                                                    isAddingNewCategory = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa danh mục con", tint = AppleTextSecondary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Add subcategory button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    onClick = {
                                        parentForSubCategory = cat
                                        categoryToEdit = null
                                        inputName = ""
                                        inputEmoji = "🍳"
                                        inputColorHex = cat.colorHex
                                        inputParentId = cat.id
                                        isAddingNewCategory = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppleSurface3
                                ) {
                                    Text(
                                        text = "+ Thêm danh mục con",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        Text("Lưu", color = Color.Black, fontWeight = FontWeight.Bold)
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
