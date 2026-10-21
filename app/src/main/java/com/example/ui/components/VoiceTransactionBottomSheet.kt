package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.VoiceTransactionDraft
import com.example.ai.VoiceTransactionEngine
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionType
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleBorderSubtle
import com.example.ui.theme.AppleCyan
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleSurface4
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceTransactionBottomSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onTransactionSaved: () -> Unit,
    autoStart: Boolean = true
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var manualInputText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("🎙️ Đang lắng nghe... Nói câu bất kỳ (VD: Ăn sáng 50k tiền mặt, Đỏ xăng 50k vcb)") }
    var showFullCategoryPicker by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var silenceTimerJob by remember { mutableStateOf<Job?>(null) }

    fun resetSilenceTimer() {
        silenceTimerJob?.cancel()
        silenceTimerJob = coroutineScope.launch {
            delay(2000L) // 2 giây im lặng sau khi nói -> tự động kết thúc ghi âm
            if (isListening) {
                try {
                    speechRecognizer?.stopListening()
                } catch (_: Exception) {}
                isListening = false
                statusMessage = "Đã nhận âm thanh xong, đang xử lý thông tin..."
            }
        }
    }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var currentDraft by remember {
        mutableStateOf<VoiceTransactionDraft?>(
            VoiceTransactionDraft(
                type = TransactionType.EXPENSE,
                amount = null,
                categoryId = null,
                categoryName = null,
                categoryEmoji = null,
                accountId = null,
                accountName = null,
                bankId = null,
                date = todayDateStr,
                note = "",
                confidence = 0f,
                requiresConfirmation = true,
                hasAmount = false,
                hasCategory = false,
                hasAccount = false
            )
        )
    }

    fun saveCurrentDraft() {
        val draft = currentDraft ?: return
        if (!draft.isComplete) return
        val finalAmt = draft.amount ?: 0.0
        val finalAccId = draft.accountId ?: accounts.firstOrNull()?.id ?: 1L
        viewModel.addTransaction(
            type = draft.type,
            amount = finalAmt,
            sourceAccountId = finalAccId,
            destinationAccountId = draft.destinationAccountId,
            categoryId = draft.categoryId,
            note = draft.note.ifBlank { draft.categoryName ?: "Chi tiêu" },
            paymentStatus = PaymentStatus.PAID
        )
        Toast.makeText(context, "Đã lưu thành công: ${formatVnd(finalAmt)}!", Toast.LENGTH_SHORT).show()
        onTransactionSaved()
    }

    fun applyPhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isBlank()) return
        recognizedText = trimmed
        val draft = VoiceTransactionEngine.processVoiceCommand(
            input = trimmed,
            currentDraft = currentDraft,
            availableAccounts = accounts,
            availableCategories = categories
        )
        if (draft != null) {
            currentDraft = draft
            val missing = mutableListOf<String>()
            if (!draft.hasAmount) missing.add("Số tiền")
            if (!draft.hasCategory) missing.add("Danh mục")
            if (!draft.hasAccount) missing.add("Tài khoản chi")

            if (draft.isComplete) {
                statusMessage = "✅ Đầy đủ: Số tiền • Danh mục • Tài khoản. Vui lòng kiểm tra lại rồi bấm 'Xác nhận lưu'!"
            } else {
                statusMessage = "⚠️ Vui lòng chọn thêm: ${missing.joinToString(", ")}"
            }
        } else {
            statusMessage = "Đã nghe: \"$trimmed\". Bạn hãy chọn thêm bên dưới."
        }
    }

    // Google Voice Dialog fallback launcher
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                applyPhrase(spokenText)
            }
        }
    }

    fun startListening() {
        if (!hasAudioPermission) {
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói chi tiêu (VD: Đỏ xăng 50k vcb)")
                }
                try {
                    speechIntentLauncher.launch(intent)
                    statusMessage = "Đang mở hộp thoại giọng nói..."
                } catch (ex: Exception) {
                    statusMessage = "Bạn có thể gõ câu nói hoặc chọn câu mẫu bên dưới."
                }
                return
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói chi tiêu của bạn...")
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    statusMessage = "🎙️ Đang lắng nghe... Nói câu bất kỳ (tự dừng sau 2s không có tiếng)"
                }
                override fun onBeginningOfSpeech() {
                    statusMessage = "Đang thu âm câu nói..."
                    resetSilenceTimer()
                }
                override fun onRmsChanged(rmsdB: Float) {
                    if (rmsdB > 2.0f) {
                        resetSilenceTimer()
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    silenceTimerJob?.cancel()
                    isListening = false
                    statusMessage = "Đang phân tích 3 yếu tố qua AI..."
                }
                override fun onError(error: Int) {
                    silenceTimerJob?.cancel()
                    isListening = false
                    val errText = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Chưa nhận diện rõ câu nói. Hãy chạm Micro nói lại hoặc chọn gợi ý bên dưới."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Hết thời gian chờ. Bấm Micro để nói lại nhé!"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Lỗi mạng giọng nói. Bạn có thể chọn câu mẫu hoặc gõ văn bản."
                        SpeechRecognizer.ERROR_AUDIO -> "Lỗi micro. Kiểm tra thiết bị thu âm."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Dịch vụ bận, đang chuẩn bị lại..."
                        else -> "Chưa nghe rõ ($error). Bấm nói lại hoặc chọn gợi ý."
                    }
                    statusMessage = errText
                }
                override fun onResults(results: Bundle?) {
                    silenceTimerJob?.cancel()
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        applyPhrase(text)
                    } else {
                        statusMessage = "Chưa nhận diện được nội dung. Hãy thử lại!"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    resetSilenceTimer()
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        recognizedText = text
                        statusMessage = "Đang nghe: \"$text\"..."
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
            isListening = true
            statusMessage = "🎙️ Đang lắng nghe... Nói câu bất kỳ (tự dừng sau 2s im lặng)"
        } catch (e: Exception) {
            silenceTimerJob?.cancel()
            isListening = false
            statusMessage = "Không thể bật micro: ${e.message}. Hãy chọn gợi ý hoặc gõ bên dưới."
        }
    }

    fun stopListening() {
        silenceTimerJob?.cancel()
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
        statusMessage = "Đã dừng nghe. Bấm Micro để nói tiếp nếu cần."
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            startListening()
        } else {
            statusMessage = "Chưa cấp quyền micro. Bạn có thể chọn gợi ý hoặc gõ câu nói bên dưới."
        }
    }

    // Auto-start listening immediately when the bottom sheet opens
    LaunchedEffect(Unit) {
        if (autoStart) {
            if (hasAudioPermission) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            silenceTimerJob?.cancel()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 6 Commonly used categories for quick suggestion (Prioritizing Xăng xe & Ăn uống)
    val suggestedCategories = remember(categories) {
        val exp = categories.filter { it.type == com.example.data.model.CategoryType.EXPENSE }
        // Ensure "Xăng xe" is in top suggested
        val gas = exp.firstOrNull { it.id == 2L || it.name.contains("Xăng", ignoreCase = true) }
        val rest = exp.filter { it != gas }
        if (gas != null) {
            listOf(gas) + rest.take(5)
        } else {
            exp.take(6)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppleSurface1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nhập giọng nói thông minh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                    Text(
                        text = "Yêu cầu đủ 3 yếu tố: Giá tiền • Danh mục • Tài khoản",
                        fontSize = 12.sp,
                        color = AppleOrange
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = AppleTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Microphone Pulsing Visual Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AppleOrange.copy(alpha = 0.3f))
                    )
                }

                Surface(
                    onClick = {
                        if (isListening) {
                            stopListening()
                        } else {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                startListening()
                            }
                        }
                    },
                    shape = CircleShape,
                    color = if (isListening) AppleOrange else AppleSurface3,
                    border = BorderStroke(2.dp, if (isListening) AppleOrange else AppleBorder),
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("voice_mic_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (isListening) Color.Black else AppleOrange,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Badge
            Text(
                text = statusMessage,
                fontSize = 13.sp,
                fontWeight = if (isListening) FontWeight.Bold else FontWeight.Medium,
                color = if (isListening) AppleOrange else AppleTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (recognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$recognizedText\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleSurface2)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 3 REQUIRED ELEMENTS TRACKER CARD (Giá tiền • Danh mục • Tài khoản chi)
            // =========================================================================
            val draft = currentDraft
            if (draft != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_draft_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface2),
                    border = BorderStroke(
                        1.dp,
                        if (draft.isComplete) AppleGreen else AppleOrange.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        // Header Status of 3 Elements
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KIỂM ĐÍNH 3 YẾU TỐ:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextSecondary,
                                letterSpacing = 0.5.sp
                            )

                            if (draft.isComplete) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppleGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, AppleGreen)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AppleGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Đầy đủ 3/3",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleGreen
                                        )
                                    }
                                }
                            } else {
                                val count = (if (draft.hasAmount) 1 else 0) + (if (draft.hasCategory) 1 else 0) + (if (draft.hasAccount) 1 else 0)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AppleOrange.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, AppleOrange)
                                ) {
                                    Text(
                                        text = "Đã có $count/3 yếu tố",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // -------------------------------------------------------------
                        // 1. YẾU TỐ 1: GIÁ TIỀN (Amount)
                        // -------------------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Giá tiền:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            if (draft.hasAmount && draft.amount != null && draft.amount > 0) {
                                Text(
                                    text = formatVnd(draft.amount),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (draft.type) {
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.INCOME -> IncomeGreen
                                        else -> AppleCyan
                                    }
                                )
                            } else {
                                Text(
                                    text = "⚠️ Chưa có số tiền",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrange
                                )
                            }
                        }

                        // Gợi ý chọn nhanh số tiền nếu chưa có
                        if (!draft.hasAmount || draft.amount == null || draft.amount <= 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chọn nhanh số tiền:",
                                fontSize = 11.sp,
                                color = AppleTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val amountPresets = listOf(20000.0 to "20k", 50000.0 to "50k", 100000.0 to "100k", 200000.0 to "200k", 500000.0 to "500k")
                                for ((amt, label) in amountPresets) {
                                    Surface(
                                        onClick = {
                                            currentDraft = draft.copy(amount = amt, hasAmount = true)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppleSurface3,
                                        border = BorderStroke(1.dp, AppleBorderSubtle)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleOrange,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AppleBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // -------------------------------------------------------------
                        // 2. YẾU TỐ 2: DANH MỤC (Category)
                        // -------------------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "2. Danh mục:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            if (draft.hasCategory && draft.categoryId != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = draft.categoryEmoji ?: "🍜", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = draft.categoryName ?: "Chi tiêu",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AppleGreen, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text(
                                    text = "⚠️ Chưa chọn danh mục",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrange
                                )
                            }
                        }

                        // Gợi ý chọn danh mục (hiện lên luôn để người dùng chọn)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (draft.hasCategory) "Đổi danh mục khác:" else "Gợi ý chọn danh mục:",
                            fontSize = 11.sp,
                            color = AppleTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { showFullCategoryPicker = true },
                                shape = RoundedCornerShape(8.dp),
                                color = AppleOrange.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, AppleOrange)
                            ) {
                                Text(
                                    text = "📂 Xem tất cả (${categories.size})...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            suggestedCategories.forEach { cat ->
                                val isSelected = draft.categoryId == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        currentDraft = draft.copy(
                                            categoryId = cat.id,
                                            categoryName = cat.name,
                                            categoryEmoji = cat.emoji,
                                            hasCategory = true
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${cat.emoji ?: "🍜"} ${cat.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppleOrange,
                                        selectedLabelColor = Color.Black,
                                        containerColor = AppleSurface3,
                                        labelColor = AppleTextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AppleBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // -------------------------------------------------------------
                        // 3. YẾU TỐ 3: TÀI KHOẢN CHI (Account)
                        // -------------------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3. Tài khoản chi:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            if (draft.hasAccount && draft.accountId != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BankLogoBadge(bankId = draft.bankId, size = 20.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = draft.accountName ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AppleGreen, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text(
                                    text = "⚠️ Chưa chọn tài khoản chi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleOrange
                                )
                            }
                        }

                        // Gợi ý chọn tài khoản chi (hiện lên luôn để người dùng chọn)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (draft.hasAccount) "Đổi tài khoản nguồn:" else "Gợi ý chọn tài khoản chi:",
                            fontSize = 11.sp,
                            color = AppleTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            accounts.forEach { acc ->
                                val isSelected = draft.accountId == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        currentDraft = draft.copy(
                                            accountId = acc.id,
                                            accountName = acc.name,
                                            bankId = acc.bankId,
                                            hasAccount = true
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = acc.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        BankLogoBadge(bankId = acc.bankId, size = 16.dp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AppleOrange,
                                        selectedLabelColor = Color.Black,
                                        containerColor = AppleSurface3,
                                        labelColor = AppleTextPrimary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Ghi chú
                        Text(
                            text = "Ghi chú: ${draft.note}",
                            fontSize = 12.sp,
                            color = AppleTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // -------------------------------------------------------------
                        // NÚT LƯU GIAO DỊCH (Chỉ cho phép lưu khi đủ 3 yếu tố)
                        // -------------------------------------------------------------
                        if (draft.isComplete) {
                            Button(
                                onClick = {
                                    val finalAmt = draft.amount ?: 0.0
                                    val finalAccId = draft.accountId ?: accounts.first().id
                                    viewModel.addTransaction(
                                        type = draft.type,
                                        amount = finalAmt,
                                        sourceAccountId = finalAccId,
                                        destinationAccountId = draft.destinationAccountId,
                                        categoryId = draft.categoryId,
                                        note = draft.note,
                                        paymentStatus = PaymentStatus.PAID
                                    )
                                    Toast.makeText(context, "Đã lưu thành công: ${formatVnd(finalAmt)}!", Toast.LENGTH_SHORT).show()
                                    onTransactionSaved()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_save_voice_transaction"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Xác nhận lưu giao dịch",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👉 Vui lòng kiểm tra lại thông tin trước khi xác nhận lưu",
                                fontSize = 11.sp,
                                color = AppleTextSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val missing = mutableListOf<String>()
                            if (!draft.hasAmount) missing.add("Số tiền")
                            if (!draft.hasCategory) missing.add("Danh mục")
                            if (!draft.hasAccount) missing.add("Tài khoản chi")

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AppleOrange.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, AppleOrange.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AppleOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Vui lòng chọn thêm ${missing.joinToString(" và ")} ở trên để lưu.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleOrange
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Initial prompt when no draft yet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface2),
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 MẸO NÓI 1 CÂU NHẬN ĐỦ 3 YẾU TỐ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ví dụ: 'Đỏ xăng 50k vcb', 'Đổ xăng 70 nghìn tiền mặt', 'Ăn trưa 45k techcombank', 'Lương 25 triệu mb'",
                            fontSize = 13.sp,
                            color = AppleTextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Phrase Simulation Chips
            Text(
                text = if (currentDraft != null) "NÓI TIẾP ĐỂ BỔ SUNG NHANH:" else "CÂU NÓI MẪU ĐỦ 3 YẾU TỐ (CHẠM ĐỂ THỬ):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppleTextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val quickVoiceChips = if (currentDraft != null) {
                listOf(
                    "Bằng tiền mặt",
                    "Bằng Vietcombank",
                    "Bằng Techcombank",
                    "Đổi thành 70 nghìn",
                    "Đổi sang Xăng xe"
                )
            } else {
                listOf(
                    "Đỏ xăng 50k vcb",
                    "Đổ xăng 70k tiền mặt",
                    "Ăn phở 45k techcombank",
                    "Cà phê sáng 35k mb bank",
                    "Mua đồ siêu thị 320k vcb",
                    "Lương 25 triệu techcombank"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (chip in quickVoiceChips) {
                    Surface(
                        onClick = { applyPhrase(chip) },
                        shape = RoundedCornerShape(14.dp),
                        color = AppleSurface3,
                        border = BorderStroke(1.dp, AppleBorder),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            color = AppleTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Manual input text field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = { Text("Hoặc gõ câu nói (VD: Đỏ xăng 50k vcb)...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_manual_text_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleOrange,
                        unfocusedBorderColor = AppleBorder,
                        focusedContainerColor = AppleSurface2,
                        unfocusedContainerColor = AppleSurface2
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (manualInputText.isNotBlank()) {
                            applyPhrase(manualInputText)
                            manualInputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(AppleOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Gửi",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
