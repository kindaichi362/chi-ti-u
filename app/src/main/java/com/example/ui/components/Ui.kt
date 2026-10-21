package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.FinancialAdvisorEngine
import com.example.ai.Insight
import com.example.ai.InsightTone
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.theme.AppPalette
import com.example.ui.theme.MoneyStyle
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Containers
// ---------------------------------------------------------------------------------------------

/** The one card style: white (or charcoal) surface, hairline border, no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Space.lg,
    color: Color = AppPalette.card,
    borderColor: Color = AppPalette.border,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.lg)
            .background(color)
            .border(1.dp, borderColor, Radius.lg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Deep-teal summary card at the top of a tab. Content colour defaults to white-on-teal. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = AppPalette
    CompositionLocalProvider(LocalContentColor provides p.onHero) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(Radius.xl)
                .background(p.hero)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(Space.xl),
            content = content
        )
    }
}

/** Block title with an optional action link on the right ("Xem tất cả →"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = TextSize.lg, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(Radius.sm)
                    .clickable(onClick = onAction)
                    .heightIn(min = Space.touch)
                    .padding(horizontal = Space.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = AppPalette.accent)
                Spacer(Modifier.width(Space.xs))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Small label above a figure. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = AppPalette.textSecondary) {
    Text(text, fontSize = TextSize.xs, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(AppPalette.borderSubtle))
}

// ---------------------------------------------------------------------------------------------
// Money
// ---------------------------------------------------------------------------------------------

/** An amount: tabular digits, never wraps between the number and "₫". */
@Composable
fun MoneyText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextSize.sm,
    color: Color = AppPalette.textPrimary,
    fontWeight: FontWeight = FontWeight.SemiBold,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = MoneyStyle,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textDecoration = textDecoration,
        textAlign = textAlign,
        modifier = modifier
    )
}

/** Big figure that steps down in size for long numbers, so billions still fit on a 360 dp phone. */
@Composable
fun HeroMoney(text: String, modifier: Modifier = Modifier, color: Color = LocalContentColor.current, base: TextUnit = TextSize.hero) {
    val size = when {
        text.length <= 13 -> base
        text.length <= 16 -> (base.value * 0.85f).sp
        else -> (base.value * 0.72f).sp
    }
    MoneyText(text, modifier = modifier, fontSize = size, color = color, fontWeight = FontWeight.Bold)
}

/** "1,2 tr", "350k" — for tight spots; spaces are no-break. */
fun compactVnd(amount: Double): String = FinancialAdvisorEngine.compact(amount).replace(" ", NBSP).replace("-", MINUS)

/** Expense in − red, income in + green, transfers neutral without sign. */
fun signedAmountText(tx: Transaction): String = when (tx.type) {
    TransactionType.EXPENSE -> MINUS + formatVnd(tx.amount)
    TransactionType.INCOME -> "+" + formatVnd(tx.amount)
    TransactionType.TRANSFER -> formatVnd(tx.amount)
    TransactionType.ADJUSTMENT -> FinanceRepository.adjustmentDelta(tx).let { (if (it > 0) "+" else "") + formatVnd(it) }
}

@Composable
fun amountColor(tx: Transaction): Color = when (tx.type) {
    TransactionType.EXPENSE -> AppPalette.expense
    TransactionType.INCOME -> AppPalette.income
    else -> AppPalette.textPrimary
}

// ---------------------------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------------------------

/** Budget usage colour: accent while healthy, amber from 80%, red when over. */
@Composable
fun thresholdColor(ratio: Double): Color = when {
    ratio > 1.0 -> AppPalette.danger
    ratio >= 0.8 -> AppPalette.warning
    else -> AppPalette.accent
}

@Composable
fun ThresholdBar(
    ratio: Double,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = thresholdColor(ratio),
    trackColor: Color = AppPalette.surface3
) {
    Box(modifier = modifier.fillMaxWidth().height(height).clip(CircleShape).background(trackColor)) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------------------------

/** Segmented switch; the selected segment is filled with the accent. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    Row(
        modifier = modifier.fillMaxWidth().clip(Radius.md).background(p.surface2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) p.accent else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = TextSize.sm, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) p.onAccent else p.textSecondary, maxLines = 1)
            }
        }
    }
}

/** Pill chip for filters; filled with the accent when selected. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val p = AppPalette
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(Radius.pill)
            .background(if (selected) p.accent else p.surface2)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = if (selected) p.onAccent else p.textPrimary, maxLines = 1)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val p = AppPalette
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(Radius.md)
            .background(if (enabled) p.accent else p.surface3)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val c = if (enabled) p.onAccent else p.textTertiary
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = c, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val p = AppPalette
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(Radius.md)
            .background(p.card)
            .border(1.dp, p.border, Radius.md)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.sm))
        }
        Text(text, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textPrimary, maxLines = 1)
    }
}

/** Rounded-square icon on a soft background (categories, actions, account types). */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(modifier = modifier.size(size).clip(shape).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Category icon tile: expense on blush, income on mint, coloured by meaning (not per category). */
@Composable
fun CategoryTile(category: Category?, modifier: Modifier = Modifier, size: Dp = 40.dp, type: CategoryType? = category?.type) {
    val p = AppPalette
    val income = type == CategoryType.INCOME
    IconTile(
        icon = categoryIcon(category),
        tint = if (income) p.income else p.expense,
        background = if (income) p.accentSoft else p.expenseSoft,
        modifier = modifier,
        size = size
    )
}

/** Month picker pill ("Tháng 10, 2026 ▾"); offers the last [monthsBack] months. */
@Composable
fun MonthPill(selectedOffset: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, monthsBack: Int = 12) {
    val p = AppPalette
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .clip(Radius.md)
                .background(p.card)
                .border(1.dp, p.border, Radius.md)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(monthLabel(selectedOffset), fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.textPrimary, maxLines = 1)
            Spacer(Modifier.width(Space.xs))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Chọn tháng", tint = p.textSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = p.card) {
            for (offset in 0 downTo -monthsBack + 1) {
                DropdownMenuItem(
                    text = {
                        Text(
                            monthLabel(offset) + if (offset == 0) " (tháng này)" else "",
                            fontWeight = if (offset == selectedOffset) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (offset == selectedOffset) p.accent else p.textPrimary
                        )
                    },
                    onClick = { onSelect(offset); open = false }
                )
            }
        }
    }
}

/** "‹ Tháng 10, 2026 ›" stepper; the next arrow stops at the current month. */
@Composable
fun MonthStepper(selectedOffset: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onSelect(selectedOffset - 1) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước", tint = p.textPrimary)
        }
        Text(monthLabel(selectedOffset), fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = { onSelect(selectedOffset + 1) }, enabled = selectedOffset < 0) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau", tint = if (selectedOffset < 0) p.textPrimary else p.textTertiary.copy(alpha = 0.4f))
        }
    }
}

fun monthLabel(offset: Int): String {
    val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
    return "Tháng ${c.get(Calendar.MONTH) + 1}, ${c.get(Calendar.YEAR)}"
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

/** A list row: leading tile, title + subtitle, trailing value, optional chevron. Min 56 dp tall. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val p = AppPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = TextSize.base, fontWeight = FontWeight.Medium, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = TextSize.xs, color = p.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.sm))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(Space.xs))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

private fun isToday(ts: Long): Boolean {
    val a = Calendar.getInstance()
    val b = Calendar.getInstance().apply { timeInMillis = ts }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/**
 * One transaction in any list: category tile, name, "time · account" (date too when it is not
 * today, unless the list is already grouped by day), signed amount on the right.
 */
@Composable
fun TransactionRow(
    tx: Transaction,
    category: Category?,
    account: Account?,
    destination: Account? = null,
    showDate: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val p = AppPalette
    val title = when (tx.type) {
        TransactionType.TRANSFER -> if (destination != null) "${account?.name ?: "?"} → ${destination.name}" else tx.note.ifBlank { "Chuyển tiền" }
        TransactionType.ADJUSTMENT -> "Đối soát số dư"
        else -> category?.name ?: tx.note.ifBlank { "Chưa phân loại" }
    }
    val time = SimpleDateFormat(if (showDate && !isToday(tx.timestamp)) "dd/MM/yyyy · HH:mm" else "HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    // Adjustments carry an automatic reason as note; it adds nothing next to the title.
    val note = tx.note.takeIf { it.isNotBlank() && it != title && tx.type != TransactionType.TRANSFER && tx.type != TransactionType.ADJUSTMENT }
    val subtitle = listOfNotNull(time, account?.name?.takeIf { tx.type != TransactionType.TRANSFER }, note).joinToString(" · ")
    val cancelled = tx.paymentStatus == PaymentStatus.CANCELLED

    ListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            when (tx.type) {
                TransactionType.TRANSFER -> IconTile(Icons.Outlined.SwapHoriz, p.transfer, p.surface2)
                TransactionType.ADJUSTMENT -> IconTile(Icons.Outlined.Tune, p.textSecondary, p.surface2)
                else -> CategoryTile(category, type = if (tx.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE)
            }
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(
                    signedAmountText(tx),
                    color = if (cancelled) p.textTertiary else amountColor(tx),
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null
                )
                when (tx.paymentStatus) {
                    PaymentStatus.PENDING_PAYMENT -> Text("Chờ xác nhận QR", fontSize = TextSize.xs, color = p.warning, fontWeight = FontWeight.Medium)
                    PaymentStatus.CANCELLED -> Text("Đã hủy", fontSize = TextSize.xs, color = p.textTertiary)
                    else -> {}
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Feedback
// ---------------------------------------------------------------------------------------------

@Composable
fun toneColor(tone: InsightTone): Color = when (tone) {
    InsightTone.GOOD -> AppPalette.income
    InsightTone.INFO -> AppPalette.transfer
    InsightTone.WARNING -> AppPalette.warning
    InsightTone.DANGER -> AppPalette.danger
}

fun toneIcon(tone: InsightTone): ImageVector = when (tone) {
    InsightTone.GOOD -> Icons.Outlined.CheckCircle
    InsightTone.INFO -> Icons.Outlined.Info
    InsightTone.WARNING -> Icons.Outlined.WarningAmber
    InsightTone.DANGER -> Icons.Outlined.ReportProblem
}

/** An automatic finding from the assistant; tapping asks the assistant about it. */
@Composable
fun InsightCard(insight: Insight, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = toneColor(insight.tone)
    AppCard(modifier = modifier, onClick = onClick, contentPadding = Space.lg) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(toneIcon(insight.tone), tint = c, background = c.copy(alpha = 0.12f), size = 32.dp, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.width(Space.md))
            Column {
                Text(insight.title, fontSize = TextSize.sm, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Text(insight.body, fontSize = TextSize.xs, lineHeight = 17.sp, color = AppPalette.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Empty list: one line of what goes here and the action that fills it. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val p = AppPalette
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Space.xxl, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            IconTile(icon, tint = p.textSecondary, background = p.surface2, size = 48.dp, shape = CircleShape)
            Spacer(Modifier.height(Space.md))
        }
        Text(title, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(body, fontSize = TextSize.sm, color = p.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            Text(
                actionLabel,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.SemiBold,
                color = p.accent,
                modifier = Modifier.clip(Radius.md).clickable(onClick = onAction).heightIn(min = Space.touch).padding(horizontal = Space.md, vertical = Space.md)
            )
        }
    }
}

/** Big two-up action button used under hero cards ("Chuyển tiền", "Đối soát số dư"). */
@Composable
fun ActionTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    Surface(
        onClick = onClick,
        shape = Radius.lg,
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(Space.sm))
            Text(label, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textPrimary, maxLines = 1)
        }
    }
}
