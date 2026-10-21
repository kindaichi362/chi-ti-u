package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import com.example.ai.FinancialAdvisorEngine
import com.example.ai.Insight
import com.example.ai.InsightTone
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The one card style of the app: flat surface, hairline border, no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Tone.hairline,
    contentPadding: Dp = Space.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(Radius.lg)
        .background(AppleSurface1)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Surface(
        modifier = base,
        color = Color.Transparent,
        shape = Radius.lg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Block title with an optional text action on the right ("Xem tất cả"). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = TextSize.lg, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                fontSize = TextSize.sm,
                fontWeight = FontWeight.Medium,
                color = Tone.accent,
                modifier = Modifier.clip(Radius.sm).clickable(onClick = onAction).padding(vertical = Space.xs, horizontal = Space.xs)
            )
        }
    }
}

/** Small uppercase-free label above a figure. */
@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = AppleTextSecondary) {
    Text(text, fontSize = TextSize.xs, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Colour for a usage ratio: green under 70%, amber to 90%, orange to 100%, red above. */
fun thresholdColor(ratio: Double): Color = when {
    ratio > 1.0 -> Tone.danger
    ratio >= 0.9 -> Color(0xFFFF6B35)
    ratio >= 0.7 -> Color(0xFFFFD60A)
    else -> Tone.ok
}

@Composable
fun ThresholdBar(ratio: Double, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color = thresholdColor(ratio)) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(AppleSurface3)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** Two- or three-way switch, used for tabs inside a screen. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(AppleSurface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) AppleSurface3 else Color.Transparent)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = TextSize.sm, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) AppleTextPrimary else AppleTextSecondary)
            }
        }
    }
}

/** Round tile with an emoji (categories) or an icon (actions). */
@Composable
fun IconTile(color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp, emoji: String? = null, icon: ImageVector? = null) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            !emoji.isNullOrBlank() -> Text(emoji, fontSize = (size.value * 0.45f).sp)
            icon != null -> Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** A list row: leading tile, two lines of text, trailing value, optional chevron. */
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Space.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = AppleTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = TextSize.xs, color = AppleTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(Space.sm))
            trailing()
        }
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppleTextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

fun signedAmountText(tx: Transaction): String = when (tx.type) {
    TransactionType.EXPENSE -> "-" + formatVnd(tx.amount)
    TransactionType.INCOME -> "+" + formatVnd(tx.amount)
    TransactionType.TRANSFER -> formatVnd(tx.amount)
    TransactionType.ADJUSTMENT -> FinanceRepository.adjustmentDelta(tx).let { (if (it > 0) "+" else "") + formatVnd(it) }
}

fun amountColor(tx: Transaction): Color = when (tx.type) {
    TransactionType.EXPENSE -> AppleTextPrimary
    TransactionType.INCOME -> Tone.income
    TransactionType.TRANSFER -> AppleTextSecondary
    TransactionType.ADJUSTMENT -> AppleTextSecondary
}

/**
 * One transaction in any list. Expenses are white (the normal case), income green, transfers grey —
 * colour carries meaning instead of painting every amount red.
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
    val title = when {
        tx.type == TransactionType.TRANSFER -> if (destination != null) "${account?.name ?: "?"} → ${destination.name}" else tx.note.ifBlank { "Chuyển tiền" }
        tx.type == TransactionType.ADJUSTMENT -> "Điều chỉnh số dư"
        else -> category?.name ?: "Chưa phân loại"
    }
    val time = SimpleDateFormat(if (showDate) "dd/MM HH:mm" else "HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    val note = tx.note.takeIf { it.isNotBlank() && it != title && tx.type != TransactionType.TRANSFER }
    val subtitle = listOfNotNull(note, account?.name?.takeIf { tx.type != TransactionType.TRANSFER }, time).joinToString(" · ")
    val cancelled = tx.paymentStatus == PaymentStatus.CANCELLED

    ListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            when (tx.type) {
                TransactionType.TRANSFER -> IconTile(Tone.transfer, icon = Icons.Default.SwapHoriz)
                TransactionType.ADJUSTMENT -> IconTile(AppleTextSecondary, icon = Icons.Default.Tune)
                else -> IconTile(parseColorSafe(category?.colorHex ?: "#8E8E93"), emoji = category?.emoji ?: "🏷️")
            }
        },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    signedAmountText(tx),
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.SemiBold,
                    color = if (cancelled) AppleTextTertiary else amountColor(tx),
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null
                )
                when (tx.paymentStatus) {
                    PaymentStatus.PENDING_PAYMENT -> Text("Chờ QR", fontSize = TextSize.xs, color = Tone.warning, fontWeight = FontWeight.Medium)
                    PaymentStatus.CANCELLED -> Text("Đã hủy", fontSize = TextSize.xs, color = AppleTextTertiary)
                    else -> {}
                }
            }
        }
    )
}

fun toneColor(tone: InsightTone): Color = when (tone) {
    InsightTone.GOOD -> Tone.ok
    InsightTone.INFO -> Tone.transfer
    InsightTone.WARNING -> Tone.warning
    InsightTone.DANGER -> Tone.danger
}

/** An automatic finding from the assistant; tapping asks the assistant about it. */
@Composable
fun InsightCard(insight: Insight, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = toneColor(insight.tone)
    AppCard(modifier = modifier, onClick = onClick, borderColor = c.copy(alpha = 0.35f), contentPadding = Space.md + Space.xs) {
        Row(verticalAlignment = Alignment.Top) {
            Text(insight.emoji, fontSize = TextSize.lg)
            Spacer(Modifier.width(Space.sm))
            Column {
                Text(insight.title, fontSize = TextSize.sm, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Text(insight.body, fontSize = TextSize.xs, lineHeight = 17.sp, color = AppleTextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Empty list: one sentence of what goes here and the action that fills it. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Space.xxl, horizontal = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
        Spacer(Modifier.height(Space.xs))
        Text(body, fontSize = TextSize.sm, color = AppleTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            Box(
                modifier = Modifier
                    .clip(Radius.md)
                    .background(Tone.accent)
                    .clickable(onClick = onAction)
                    .padding(horizontal = Space.xl, vertical = Space.md)
            ) {
                Text(actionLabel, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
        }
    }
}

/** Compact money for tight spots: "1,2 tr", "350k". */
fun compactVnd(amount: Double): String = FinancialAdvisorEngine.compact(amount)

/** Thin divider between rows inside a card. */
@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Tone.hairline))
}
