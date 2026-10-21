package com.example.payment.vietqr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object QrCodeDecoder {

    /**
     * Decodes a QR code string from an Android Bitmap using ZXing core.
     */
    fun decodeBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()

        return try {
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: NotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a QR code from a local image Uri (e.g., from Android Photo Picker).
     */
    fun decodeUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                decodeBitmap(bitmap)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
