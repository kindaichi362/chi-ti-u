package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VoicePhase { IDLE, LISTENING, RECOGNIZING }

/**
 * Voice entry for the transaction form. While the mic is open a clear recording window covers
 * the screen (pulsing mic, "Đang ghi âm", live transcript, Xong / Hủy), so it is obvious that
 * the phone is listening. The recognised sentence only fills the form; the user still taps Lưu.
 */
@Composable
fun InlineVoiceInput(trigger: Int, onPhrase: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var timer by remember { mutableStateOf<Job?>(null) }
    var phase by remember { mutableStateOf(VoicePhase.IDLE) }
    var speechStarted by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Đang chuẩn bị micro…") }

    fun finishWith(phrase: String?) {
        timer?.cancel()
        phase = VoicePhase.IDLE
        if (!phrase.isNullOrBlank()) {
            onPhrase(phrase)
            status = "Đã điền: \"$phrase\". Kiểm tra lại rồi bấm Lưu."
        } else status = "Chưa nghe rõ. Chạm micro để thử lại."
    }

    fun stopNow() {
        timer?.cancel()
        if (phase == VoicePhase.LISTENING) {
            phase = VoicePhase.RECOGNIZING
            recognizer?.stopListening()
        }
    }

    fun cancel() {
        timer?.cancel()
        recognizer?.cancel()
        phase = VoicePhase.IDLE
        partial = ""
        status = "Đã hủy ghi âm."
    }

    fun stopAfterSilence() {
        timer?.cancel()
        timer = scope.launch {
            delay(2500)
            if (phase == VoicePhase.LISTENING && speechStarted) stopNow()
        }
    }

    val fallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            finishWith(result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull())
        }
    }

    fun start() {
        timer?.cancel()
        recognizer?.destroy()
        recognizer = null
        partial = ""
        level = 0f
        speechStarted = false
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                fallback.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                })
            } catch (_: Exception) { status = "Không mở được nhận diện giọng nói." }
            return
        }
        try {
            val speech = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = speech
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { phase = VoicePhase.LISTENING }
                override fun onBeginningOfSpeech() { speechStarted = true; stopAfterSilence() }
                override fun onRmsChanged(rmsdB: Float) {
                    level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    if (speechStarted && rmsdB > 3.5f) stopAfterSilence()
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { timer?.cancel(); phase = VoicePhase.RECOGNIZING }
                override fun onError(error: Int) {
                    if (phase == VoicePhase.IDLE) return // cancelled by the user
                    if (partial.isNotBlank() && error in listOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) finishWith(partial)
                    else finishWith(null)
                }
                override fun onResults(results: Bundle?) {
                    finishWith(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: partial)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val phrase = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!phrase.isNullOrBlank() && phrase != partial) {
                        partial = phrase; speechStarted = true; stopAfterSilence()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            })
            phase = VoicePhase.LISTENING
        } catch (_: Exception) { phase = VoicePhase.IDLE; status = "Không bật được micro. Chạm micro để thử lại." }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else status = "Cần cấp quyền micro để nhập bằng giọng nói."
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
            else permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(Unit) {
        onDispose { timer?.cancel(); recognizer?.destroy() }
    }

    if (phase != VoicePhase.IDLE) {
        RecordingDialog(
            recognizing = phase == VoicePhase.RECOGNIZING,
            transcript = partial,
            level = level,
            onDone = { stopNow() },
            onCancel = { cancel() }
        )
    }

    // After recording: one line telling what was filled in (or why not).
    Row(
        Modifier.fillMaxWidth().background(AppPalette.accentSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp).testTag("inline_voice_status"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Mic, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(20.dp))
        Text(status, fontSize = TextSize.sm, color = AppPalette.textPrimary, maxLines = 3)
    }
}

/** Full-screen recording window: impossible to miss, with a live transcript and clear exits. */
@Composable
private fun RecordingDialog(recognizing: Boolean, transcript: String, level: Float, onDone: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RecordingPanel(recognizing, transcript, level, onDone, onCancel)
    }
}

/** Body of the recording window (separate so it can be previewed and tested). */
@Composable
internal fun RecordingPanel(recognizing: Boolean, transcript: String, level: Float, onDone: () -> Unit, onCancel: () -> Unit) {
    val p = AppPalette
        Column(
            modifier = Modifier
                .padding(horizontal = Space.xl)
                .fillMaxWidth()
                .clip(Radius.xl)
                .background(p.card)
                .padding(Space.xxl)
                .testTag("voice_recording_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Recording badge
            Row(
                modifier = Modifier.clip(Radius.pill).background(p.expenseSoft).padding(horizontal = Space.md, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val blink = rememberInfiniteTransition(label = "rec")
                val alpha by blink.animateFloat(1f, 0.25f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "recDot")
                Box(Modifier.size(10.dp).background(p.expense.copy(alpha = if (recognizing) 1f else alpha), CircleShape))
                Spacer(Modifier.width(Space.sm))
                Text(if (recognizing) "ĐANG XỬ LÝ" else "ĐANG GHI ÂM", fontSize = TextSize.xs, fontWeight = FontWeight.Bold, color = p.expense)
            }

            Spacer(Modifier.height(Space.xxl))

            // Mic that pulses and grows with the voice level
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val ring by pulse.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "ring")
                val voice by animateFloatAsState(if (recognizing) 0f else level, tween(120), label = "level")
                Box(Modifier.size(150.dp).scale(if (recognizing) 1f else ring).background(p.accent.copy(alpha = 0.10f), CircleShape))
                Box(Modifier.size(120.dp).scale(1f + voice * 0.25f).background(p.accent.copy(alpha = 0.18f), CircleShape))
                Box(Modifier.size(88.dp).background(p.accent, CircleShape), contentAlignment = Alignment.Center) {
                    if (recognizing) CircularProgressIndicator(color = p.onAccent, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    else Icon(Icons.Outlined.Mic, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(44.dp))
                }
            }

            Spacer(Modifier.height(Space.xl))
            Text(
                if (recognizing) "Đang nhận diện…" else "Mời bạn nói",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.textPrimary
            )
            Spacer(Modifier.height(Space.sm))
            Text(
                text = transcript.ifBlank { "Ví dụ: \"Ăn sáng 30 nghìn tiền mặt\"" },
                fontSize = if (transcript.isBlank()) TextSize.sm else 18.sp,
                fontWeight = if (transcript.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (transcript.isBlank()) p.textSecondary else p.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.heightIn(min = 48.dp)
            )
            Text("Tự dừng khi bạn ngừng nói khoảng 2 giây", fontSize = TextSize.xs, color = p.textTertiary, textAlign = TextAlign.Center)

            Spacer(Modifier.height(Space.xxl))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton("Hủy", onClick = onCancel, modifier = Modifier.weight(1f))
                PrimaryButton("Xong", onClick = onDone, enabled = !recognizing, modifier = Modifier.weight(1f))
            }
        }
    }
