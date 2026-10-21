package com.example.payment.vietqr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.ui.components.formatVnd

data class InstalledBankApp(
    val bank: VietnameseBank,
    val isInstalled: Boolean,
    val packageName: String
)

object BankLauncherHelper {

    /**
     * Checks which Vietnamese banking apps or e-wallets are installed on the device.
     */
    fun getInstalledBankingApps(context: Context): List<InstalledBankApp> {
        val pm = context.packageManager
        return VietnameseBankRegistry.BANKS.map { bank ->
            var installed = false
            try {
                pm.getPackageInfo(bank.primaryPackage, 0)
                installed = true
            } catch (e: Exception) {
                installed = false
            }
            InstalledBankApp(bank, installed, bank.primaryPackage)
        }
    }

    /**
     * Copies structured payment details to Android system clipboard so the user can easily paste in their banking app.
     */
    fun copyPaymentInfoToClipboard(
        context: Context,
        bankName: String,
        accountNumber: String,
        amount: Double,
        content: String
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val textToCopy = "$accountNumber"
        val fullSummary = "Ngân hàng: $bankName\nSTK: $accountNumber\nSố tiền: ${formatVnd(amount)}\nNội dung: $content"

        val clip = ClipData.newPlainText("Thông tin chuyển khoản", textToCopy)
        clipboard?.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "Đã sao chép STK: $accountNumber\nSố tiền: ${formatVnd(amount)}",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Attempts to open the bank's application via Deep Link or Android Package Intent.
     * Returns true if successfully launched, false if fallback is needed.
     */
    fun launchBankApp(
        context: Context,
        bank: VietnameseBank,
        accountNumber: String,
        amount: Double,
        content: String
    ): Boolean {
        // 1. Copy details to clipboard automatically
        copyPaymentInfoToClipboard(context, bank.shortName, accountNumber, amount, content)

        val pm = context.packageManager

        // 2. Try launching via Explicit Package Intent
        try {
            val launchIntent = pm.getLaunchIntentForPackage(bank.primaryPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        } catch (e: Exception) {
            // Continue to custom scheme
        }

        // 3. Try custom scheme (if available)
        if (!bank.customScheme.isNullOrBlank()) {
            try {
                val schemeUri = Uri.parse("${bank.customScheme}://app")
                val schemeIntent = Intent(Intent.ACTION_VIEW, schemeUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (schemeIntent.resolveActivity(pm) != null) {
                    context.startActivity(schemeIntent)
                    return true
                }
            } catch (e: Exception) {
                // Continue to fallback
            }
        }

        // 4. Fallback: Prompt user to open Google Play Store to install or use installed banking app
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${bank.primaryPackage}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
            return true
        } catch (e: Exception) {
            try {
                val webPlayStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${bank.primaryPackage}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webPlayStoreIntent)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "Không thể mở ứng dụng ${bank.shortName}", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }
}
