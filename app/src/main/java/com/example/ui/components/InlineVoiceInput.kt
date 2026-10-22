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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppPalette
import com.example.ui.theme.TextSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Voice capture stays inside the transaction form. Silence finishes recognition; Save confirms it. */
@Composable
fun InlineVoiceInput(trigger: Int, onPhrase: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var timer by remember { mutableStateOf<Job?>(null) }
    var listening by remember { mutableStateOf(false) }
    var speechStarted by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Đang chuẩn bị micro…") }

    fun stopAfterSilence() {
        timer?.cancel()
        timer = scope.launch {
            delay(2500)
            if (listening && speechStarted) {
                listening = false
                status = "Đang nhận diện…"
                recognizer?.stopListening()
            }
        }
    }

    val fallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!phrase.isNullOrBlank()) { onPhrase(phrase); status = "Đã điền vào biểu mẫu. Kiểm tra rồi lưu." }
        }
    }

    fun start() {
        timer?.cancel()
        recognizer?.destroy()
        recognizer = null
        partial = ""
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
                override fun onReadyForSpeech(params: Bundle?) { listening = true; status = "Đang nghe · tự dừng sau 2–3 giây im lặng" }
                override fun onBeginningOfSpeech() { speechStarted = true; stopAfterSilence() }
                override fun onRmsChanged(rmsdB: Float) { if (speechStarted && rmsdB > 3.5f) stopAfterSilence() }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { timer?.cancel(); listening = false; status = "Đang nhận diện…" }
                override fun onError(error: Int) {
                    timer?.cancel(); listening = false
                    if (partial.isNotBlank() && error in listOf(SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        onPhrase(partial); status = "Đã điền vào biểu mẫu. Kiểm tra rồi lưu."
                    } else status = "Chưa nghe rõ. Chạm mic để thử lại."
                }
                override fun onResults(results: Bundle?) {
                    timer?.cancel(); listening = false
                    val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: partial
                    if (phrase.isNotBlank()) { onPhrase(phrase); status = "Đã điền vào biểu mẫu. Kiểm tra rồi lưu." }
                    else status = "Chưa nghe rõ. Chạm mic để thử lại."
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val phrase = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!phrase.isNullOrBlank() && phrase != partial) {
                        partial = phrase; speechStarted = true; stopAfterSilence()
                        status = "Đang nghe: $phrase"
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
            listening = true
            status = "Đang nghe · tự dừng sau 2–3 giây im lặng"
        } catch (_: Exception) { listening = false; status = "Không bật được micro. Chạm mic để thử lại." }
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

    Row(
        Modifier.fillMaxWidth().background(AppPalette.accentSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp).testTag("inline_voice_status"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Outlined.Mic, contentDescription = null, tint = AppPalette.accent, modifier = Modifier.size(20.dp))
        Column {
            Text(if (listening) "Đang ghi âm" else "Nhập giọng nói", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppPalette.textPrimary)
            Text(status, fontSize = TextSize.xs, color = AppPalette.textSecondary, maxLines = 2)
        }
    }
}
