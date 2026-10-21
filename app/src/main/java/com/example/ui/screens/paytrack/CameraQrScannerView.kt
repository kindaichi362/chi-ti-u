package com.example.ui.screens.paytrack

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AppleOrange
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun CameraQrScannerView(
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCameraError: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (cameraErrorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cameraErrorMessage ?: "Camera không khả dụng",
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            var isScanning = true

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        if (!isScanning) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val result = decodeQrFromImageProxy(imageProxy)
                                        if (result != null && result.isNotBlank()) {
                                            isScanning = false
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onQrDetected(result)
                                            }
                                        }
                                        imageProxy.close()
                                    }
                                }

                            cameraProvider.unbindAll()

                            // Try back camera first, fallback to front or available camera
                            val selector = when {
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> CameraSelector.Builder().build()
                            }

                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val msg = "Không thể kết nối Camera: ${e.localizedMessage ?: "Thiết bị không hỗ trợ"}"
                            cameraErrorMessage = msg
                            onCameraError?.invoke(msg)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder reticle overlay (Apple style corner brackets)
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .border(2.dp, AppleOrange, RoundedCornerShape(20.dp))
            )
        }
    }
}

private fun decodeQrFromImageProxy(image: ImageProxy): String? {
    val plane = image.planes[0]
    val width = image.width
    val height = image.height
    val rowStride = plane.rowStride
    val buffer = plane.buffer

    // Respect rowStride to avoid diagonal image skew corruption
    val bytes = if (rowStride == width) {
        val b = ByteArray(buffer.remaining())
        buffer.get(b)
        b
    } else {
        val b = ByteArray(width * height)
        for (row in 0 until height) {
            val srcPos = row * rowStride
            val dstPos = row * width
            if (srcPos < buffer.capacity()) {
                buffer.position(srcPos)
                val toRead = minOf(width, buffer.remaining())
                buffer.get(b, dstPos, toRead)
            }
        }
        b
    }

    val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    val reader = MultiFormatReader().apply { setHints(hints) }

    return try {
        val source = PlanarYUVLuminanceSource(
            bytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        reader.decode(binaryBitmap).text
    } catch (_: Exception) {
        try {
            // Try inverted in case of dark mode or light on dark QR
            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
            reader.decode(binaryBitmap).text
        } catch (_: Exception) {
            null
        }
    }
}
