package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.IconTile
import com.example.ui.components.ListRow
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.ThemeMode
import com.example.data.backup.VpsBackup
import kotlinx.coroutines.launch

/** Settings: appearance, hidden balances, archived accounts, and shortcuts to category / plan management. */
@Composable
fun ScreenSettings(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPlans: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val context = LocalContext.current
    val mode by viewModel.themeMode.collectAsState()
    val hidden by viewModel.balancesHidden.collectAsState()
    val archived by viewModel.archivedAccounts.collectAsState()
    val backup = remember(context) { VpsBackup(context) }
    val scope = rememberCoroutineScope()
    var backupToken by remember { mutableStateOf(backup.token) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Khôi phục từ VPS?") },
            text = { Text("Toàn bộ dữ liệu hiện tại trên điện thoại sẽ được thay bằng bản sao lưu mới nhất. App sẽ giữ một bản dự phòng trên máy trước khi thay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    backup.token = backupToken
                    backupBusy = true
                    scope.launch {
                        backupStatus = try {
                            backup.downloadAndRestore()
                            "Đã khôi phục dữ liệu từ VPS"
                        } catch (e: Exception) { "Khôi phục thất bại: ${e.message ?: "Lỗi không rõ"}" }
                        backupBusy = false
                    }
                }) { Text("Khôi phục") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Hủy") } }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(p.background),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.xs)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = p.textPrimary) }
                Text("Quản lý & cài đặt", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = p.textPrimary)
            }
        }

        item {
            SectionHeader("Giao diện")
            Spacer(Modifier.height(Space.xs))
            AppCard {
                SegmentedTabs(
                    options = ThemeMode.entries.map { it.label },
                    selected = ThemeMode.entries.indexOf(mode),
                    onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) }
                )
                RowDivider(Modifier.padding(vertical = Space.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Visibility, tint = p.accent, background = p.accentSoft)
                    Column(Modifier.weight(1f).padding(start = Space.md)) {
                        Text("Ẩn số dư", fontSize = TextSize.base, fontWeight = FontWeight.Medium, color = p.textPrimary)
                        Text("Che số tiền ở Tổng quan và Tài khoản", fontSize = TextSize.xs, color = p.textSecondary)
                    }
                    Switch(
                        checked = hidden,
                        onCheckedChange = { viewModel.toggleBalancesHidden() },
                        colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = p.onAccent)
                    )
                }
            }
        }

        item {
            SectionHeader("Tài khoản đã ẩn")
            Spacer(Modifier.height(Space.xs))
            AppCard(contentPadding = Space.lg) {
                if (archived.isEmpty()) {
                    Text("Không có tài khoản nào bị ẩn.", fontSize = TextSize.sm, color = p.textSecondary, modifier = Modifier.padding(vertical = Space.sm))
                } else {
                    archived.forEachIndexed { i, acc ->
                        if (i > 0) RowDivider()
                        ListRow(
                            title = acc.name,
                            subtitle = "Số dư " + formatVnd(acc.balance),
                            leading = { AccountLogo(acc, size = 28.dp) },
                            trailing = {
                                TextButton(onClick = {
                                    viewModel.unarchiveAccount(acc.id)
                                    Toast.makeText(context, "Đã hiện lại ${acc.name}", Toast.LENGTH_SHORT).show()
                                }) { Text("Hiện lại", color = p.accent, fontWeight = FontWeight.SemiBold) }
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionHeader("Dữ liệu")
            Spacer(Modifier.height(Space.xs))
            AppCard(contentPadding = Space.lg) {
                ListRow(
                    title = "Danh mục thu chi",
                    leading = { IconTile(Icons.Outlined.Category, tint = p.accent, background = p.accentSoft) },
                    showChevron = true,
                    onClick = onOpenCategories
                )
                RowDivider()
                ListRow(
                    title = "Sổ nợ, mục tiêu & hóa đơn",
                    leading = { IconTile(Icons.AutoMirrored.Outlined.EventNote, tint = p.accent, background = p.accentSoft) },
                    showChevron = true,
                    onClick = onOpenPlans
                )
                RowDivider()
                ListRow(
                    title = "Thêm lại danh mục mặc định đã xóa",
                    leading = { IconTile(Icons.Outlined.RestartAlt, tint = p.accent, background = p.accentSoft) },
                    onClick = {
                        viewModel.restoreDefaultCategories { added ->
                            Toast.makeText(context, if (added > 0) "Đã thêm lại $added danh mục" else "Đã có đủ danh mục mặc định", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item {
            SectionHeader("Cập nhật ứng dụng")
            AppCard {
                Text("Phiên bản hiện tại ${com.example.BuildConfig.VERSION_NAME}", fontSize = TextSize.sm, color = p.textSecondary)
                Spacer(Modifier.height(Space.sm))
                Button(onClick = onCheckUpdate) { Text("Kiểm tra bản mới") }
            }
        }

        item {
            SectionHeader("Sao lưu VPS")
            Spacer(Modifier.height(Space.xs))
            AppCard {
                Text("Sao lưu thủ công dữ liệu của điện thoại này lên VPS.", fontSize = TextSize.sm, color = p.textSecondary)
                Spacer(Modifier.height(Space.md))
                OutlinedTextField(
                    value = backupToken,
                    onValueChange = { backupToken = it },
                    label = { Text("Mã kết nối VPS") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Space.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Button(enabled = !backupBusy && backupToken.isNotBlank(), onClick = {
                        backup.token = backupToken
                        backupBusy = true
                        scope.launch {
                            backupStatus = try {
                                "Đã lưu bản sao ${backup.upload()}"
                            } catch (e: Exception) { "Sao lưu thất bại: ${e.message ?: "Lỗi không rõ"}" }
                            backupBusy = false
                        }
                    }) { Text("Sao lưu ngay") }
                    OutlinedButton(enabled = !backupBusy && backupToken.isNotBlank(), onClick = { confirmRestore = true }) {
                        Text("Khôi phục")
                    }
                }
                if (backupStatus.isNotBlank()) {
                    Spacer(Modifier.height(Space.sm))
                    Text(backupStatus, fontSize = TextSize.sm, color = p.textSecondary)
                }
            }
        }
    }
}

