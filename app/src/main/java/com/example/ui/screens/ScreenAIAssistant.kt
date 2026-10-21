package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.components.InsightCard
import com.example.ui.components.Label
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import com.example.ui.viewmodel.FinanceViewModel

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender { USER, AI }

private val SUGGESTIONS = listOf(
    "Thẻ tín dụng của tôi thế nào?",
    "So với tháng trước thế nào?",
    "Ngân sách tháng này còn bao nhiêu?",
    "Tôi nên cắt giảm khoản nào?",
    "Dự báo chi tiêu cuối tháng",
    "Khoản nào tôi tiêu nhiều nhất?",
    "Tài sản ròng hiện tại là bao nhiêu?"
)

/** Renders the assistant's **bold** markers as bold text. */
private fun markdownBold(text: String): AnnotatedString = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)) { append(part) } else append(part)
    }
}

/**
 * The assistant: findings it noticed on its own at the top, then a chat that answers from the
 * user's real numbers (computed on the phone; nothing is sent anywhere).
 */
@Composable
fun ScreenAIAssistant(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    initialQuestion: String? = null,
    onInitialQuestionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val insights by viewModel.insights.collectAsState()
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Chào bạn 👋 Mình phân tích trên số liệu thật trong máy — không gửi dữ liệu đi đâu. Hỏi mình về chi tiêu, ngân sách, thẻ tín dụng hay mục tiêu tiết kiệm nhé."
            )
        )
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun ask(question: String) {
        if (question.isBlank()) return
        messages += ChatMessage(sender = MessageSender.USER, text = question.trim())
        messages += ChatMessage(sender = MessageSender.AI, text = viewModel.askAiAssistant(question))
        input = ""
    }

    // Asked from a card on the home screen: answer once the numbers are in.
    LaunchedEffect(initialQuestion, snapshot.monthlyExpense, snapshot.creditCards.size) {
        if (initialQuestion != null && (snapshot.monthlyExpense > 0 || snapshot.creditCards.isNotEmpty() || snapshot.monthlyIncome > 0)) {
            ask(initialQuestion)
            onInitialQuestionConsumed()
        }
    }
    LaunchedEffect(messages.size) { listState.animateScrollToItem(messages.size) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = AppleTextPrimary)
            }
            Box(Modifier.size(32.dp).clip(CircleShape).background(Tone.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Tone.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(Space.md))
            Column {
                Text("Trợ lý tài chính", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
                Label("Phân tích ngay trong máy")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Space.page, vertical = Space.md),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            if (insights.isNotEmpty()) {
                item {
                    Label("Mình để ý thấy")
                    Spacer(Modifier.height(Space.sm))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                        items(insights) { insight ->
                            InsightCard(insight, modifier = Modifier.width(260.dp)) { ask(insight.followUp) }
                        }
                    }
                }
            }
            itemsIndexed(messages, key = { _, m -> m.id }) { _, msg -> Bubble(msg) }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.page),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            modifier = Modifier.padding(bottom = Space.sm)
        ) {
            items(SUGGESTIONS) { q ->
                Text(
                    q,
                    fontSize = TextSize.xs,
                    color = AppleTextPrimary,
                    modifier = Modifier
                        .clip(Radius.xl)
                        .background(AppleSurface2)
                        .clickable { ask(q) }
                        .padding(horizontal = Space.md, vertical = Space.sm)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.page, end = Space.sm, bottom = Space.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Hỏi về chi tiêu, thẻ, ngân sách…", color = AppleTextSecondary) },
                singleLine = true,
                shape = Radius.xl,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Tone.accent,
                    unfocusedBorderColor = Tone.hairline,
                    focusedContainerColor = AppleSurface1,
                    unfocusedContainerColor = AppleSurface1
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { ask(input) }, enabled = input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = if (input.isNotBlank()) Tone.accent else AppleTextSecondary)
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val mine = msg.sender == MessageSender.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Text(
            text = if (mine) AnnotatedString(msg.text) else markdownBold(msg.text),
            fontSize = TextSize.sm,
            lineHeight = TextSize.sm * 1.45f,
            color = if (mine) Color.Black else Color(0xFFE5E5EA),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 4.dp,
                        bottomEnd = if (mine) 4.dp else 18.dp
                    )
                )
                .background(if (mine) Tone.accent else AppleSurface1)
                .padding(horizontal = Space.md + 2.dp, vertical = Space.md - 2.dp)
        )
    }
}

