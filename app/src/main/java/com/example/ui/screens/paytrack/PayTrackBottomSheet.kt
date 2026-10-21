package com.example.ui.screens.paytrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.payment.vietqr.BankLauncherHelper
import com.example.payment.vietqr.ParsedVietQr
import com.example.payment.vietqr.QrCodeDecoder
import com.example.payment.vietqr.VietQrParser
import com.example.payment.vietqr.VietnameseBank
import com.example.payment.vietqr.VietnameseBankRegistry
import com.example.ui.components.formatVnd
import com.example.ui.components.parseColorSafe
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
import com.example.ui.theme.OledBlack

enum class PayTrackStep {
    SCANNING,
    CONFIRM_DETAILS,
    WAITING_RECONCILIATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayTrackBottomSheet(
    initialAmount: Double,
    initialNote: String,
    selectedCategory: Category?,
    selectedAccount: Account?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (Double, String, Long?, Long, ParsedVietQr) -> Unit,
    onPaymentPendingSaved: (Double, String, Long?, Long, ParsedVietQr) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentStep by remember { mutableStateOf(PayTrackStep.SCANNING) }
    var parsedQrData by remember { mutableStateOf<ParsedVietQr?>(null) }

    // Editable fields for confirmation
    var confirmedAmount by remember { mutableStateOf(if (initialAmount > 0) initialAmount else 50000.0) }
    var confirmedNote by remember { mutableStateOf(if (initialNote.isNotBlank()) initialNote else "Ăn sáng") }
    var chosenSourceAccount by remember { mutableStateOf(selectedAccount ?: accounts.firstOrNull()) }
    var chosenBankToLaunch by remember { mutableStateOf<VietnameseBank?>(null) }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Chưa cấp quyền Camera. Bạn có thể chọn ảnh QR từ thư viện hoặc dán chuỗi.", Toast.LENGTH_LONG).show()
        }
    }

    // Photo picker (Android zero-permission visual media picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decodedString = QrCodeDecoder.decodeUri(context, uri)
            if (decodedString != null) {
                val parsed = VietQrParser.parse(decodedString)
                if (parsed != null) {
                    parsedQrData = parsed
                    if (parsed.amount != null && parsed.amount > 0) {
                        confirmedAmount = parsed.amount
                    }
                    if (!parsed.transferContent.isNullOrBlank()) {
                        confirmedNote = parsed.transferContent
                    }
                    chosenBankToLaunch = VietnameseBankRegistry.findByBin(parsed.bankBin) ?: VietnameseBankRegistry.BANKS.first()
                    currentStep = PayTrackStep.CONFIRM_DETAILS
                } else {
                    Toast.makeText(context, "Mã QR hợp lệ nhưng không phải định dạng thanh toán VietQR", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Không thể đọc mã QR từ hình ảnh này", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppleSurface1,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (currentStep) {
                            PayTrackStep.SCANNING -> "Quét mã QR thanh toán"
                            PayTrackStep.CONFIRM_DETAILS -> "Xác nhận thanh toán"
                            PayTrackStep.WAITING_RECONCILIATION -> "Đối soát giao dịch"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                    Text(
                        text = "PAY & TRACK • Chi tiêu & Thanh toán 1 chạm",
                        fontSize = 12.sp,
                        color = AppleOrange
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppleSurface2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = AppleTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = AppleBorder, thickness = 1.dp)

            when (currentStep) {
                PayTrackStep.SCANNING -> {
                    ScanningStepContent(
                        hasCameraPermission = hasCameraPermission,
                        onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onQrDetected = { payload ->
                            val parsed = VietQrParser.parse(payload)
                            if (parsed != null) {
                                parsedQrData = parsed
                                if (parsed.amount != null && parsed.amount > 0) {
                                    confirmedAmount = parsed.amount
                                }
                                if (!parsed.transferContent.isNullOrBlank()) {
                                    confirmedNote = parsed.transferContent
                                }
                                chosenBankToLaunch = VietnameseBankRegistry.findByBin(parsed.bankBin) ?: VietnameseBankRegistry.BANKS.first()
                                currentStep = PayTrackStep.CONFIRM_DETAILS
                            } else {
                                Toast.makeText(context, "Mã không hợp lệ hoặc chưa hỗ trợ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                PayTrackStep.CONFIRM_DETAILS -> {
                    val qr = parsedQrData
                    if (qr != null) {
                        ConfirmDetailsStepContent(
                            qr = qr,
                            amount = confirmedAmount,
                            note = confirmedNote,
                            selectedAccount = chosenSourceAccount,
                            accounts = accounts,
                            chosenBank = chosenBankToLaunch ?: VietnameseBankRegistry.BANKS.first(),
                            onBankSelected = { chosenBankToLaunch = it },
                            onAccountSelected = { chosenSourceAccount = it },
                            onAmountChanged = { confirmedAmount = it },
                            onNoteChanged = { confirmedNote = it },
                            onProceedToBank = {
                                val bank = chosenBankToLaunch ?: VietnameseBankRegistry.BANKS.first()
                                val acc = chosenSourceAccount ?: accounts.first()

                                // Launch Banking App & copy details to clipboard
                                BankLauncherHelper.launchBankApp(
                                    context = context,
                                    bank = bank,
                                    accountNumber = qr.accountNumber,
                                    amount = confirmedAmount,
                                    content = confirmedNote
                                )

                                // Transition to reconciliation step
                                currentStep = PayTrackStep.WAITING_RECONCILIATION
                            }
                        )
                    }
                }

                PayTrackStep.WAITING_RECONCILIATION -> {
                    val qr = parsedQrData
                    if (qr != null) {
                        ReconciliationStepContent(
                            qr = qr,
                            amount = confirmedAmount,
                            note = confirmedNote,
                            accountName = chosenSourceAccount?.name ?: "Ví tiền",
                            categoryName = selectedCategory?.name ?: "Ăn uống",
                            bankName = chosenBankToLaunch?.shortName ?: qr.bankShortName,
                            onConfirmPaid = {
                                val accId = chosenSourceAccount?.id ?: accounts.first().id
                                onPaymentConfirmed(
                                    confirmedAmount,
                                    confirmedNote,
                                    selectedCategory?.id,
                                    accId,
                                    qr
                                )
                                onDismiss()
                            },
                            onSavePending = {
                                val accId = chosenSourceAccount?.id ?: accounts.first().id
                                onPaymentPendingSaved(
                                    confirmedAmount,
                                    confirmedNote,
                                    selectedCategory?.id,
                                    accId,
                                    qr
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningStepContent(
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickPhoto: () -> Unit,
    onQrDetected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Camera Viewport or Fallback
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AppleSurface2)
                .border(1.dp, AppleBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                CameraQrScannerView(
                    onQrDetected = onQrDetected,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = AppleOrange,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cần quyền Camera để quét mã VietQR",
                        color = AppleTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
                    ) {
                        Text("Cấp quyền Camera", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options: Pick from Gallery or Paste Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onPickPhoto,
                shape = RoundedCornerShape(14.dp),
                color = AppleSurface2,
                border = BorderStroke(1.dp, AppleBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = AppleCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chọn ảnh QR",
                        color = AppleTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dán chuỗi QR / Nhập mã thủ công
        var manualQrText by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualQrText,
                onValueChange = { manualQrText = it },
                placeholder = { Text("Dán link hoặc mã VietQR...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
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
            Button(
                onClick = {
                    if (manualQrText.isNotBlank()) {
                        onQrDetected(manualQrText.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleOrange)
            ) {
                Text("Quét", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Demo Test Presets for Instant Evaluation & Testing
        Text(
            text = "⚡ QUÉT NHANH MÃ MẪU (TEST 1 CHẠM TRÊN GIẢ LẬP):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppleTextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        val demoPresets = listOf(
            Triple("Quán Phở Bò Cồ (VCB)", 50000.0, "An sang pho bo"),
            Triple("Cà phê Highlands (TCB)", 45000.0, "Ca phe buoi sang"),
            Triple("Cơm văn phòng (MBBank)", 35000.0, "Com trua van phong"),
            Triple("Tạp hóa WinMart (TPBank)", 120000.0, "Mua do sinh hoat")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            demoPresets.forEach { (name, amount, content) ->
                val bankBin = when {
                    name.contains("VCB") -> "970436"
                    name.contains("TCB") -> "970407"
                    name.contains("MBBank") -> "970422"
                    else -> "970423"
                }
                Surface(
                    onClick = {
                        val payload = VietQrParser.generateVietQrPayload(
                            bankBin = bankBin,
                            accountNumber = "10" + (10000000..99999999).random(),
                            amount = amount,
                            content = content
                        )
                        onQrDetected(payload)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = AppleSurface3,
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleOrange
                        )
                        Text(
                            text = "${formatVnd(amount)} • $content",
                            fontSize = 11.sp,
                            color = AppleTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDetailsStepContent(
    qr: ParsedVietQr,
    amount: Double,
    note: String,
    selectedAccount: Account?,
    accounts: List<Account>,
    chosenBank: VietnameseBank,
    onBankSelected: (VietnameseBank) -> Unit,
    onAccountSelected: (Account) -> Unit,
    onAmountChanged: (Double) -> Unit,
    onNoteChanged: (String) -> Unit,
    onProceedToBank: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Beneficiary Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppleSurface2),
            border = BorderStroke(1.dp, AppleBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THÔNG TIN NGƯỜI NHẬN (VIETQR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleOrange,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        color = AppleSurface4,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (qr.isDynamic) "QR Động" else "QR Tĩnh",
                            fontSize = 10.sp,
                            color = AppleCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bank Name & Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseColorSafe(chosenBank.brandColorHex).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = parseColorSafe(chosenBank.brandColorHex),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = qr.bankShortName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppleTextPrimary
                        )
                        Text(
                            text = qr.bankFullName,
                            fontSize = 11.sp,
                            color = AppleTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Number with copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleSurface3)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Số tài khoản thụ hưởng",
                            fontSize = 11.sp,
                            color = AppleTextSecondary
                        )
                        Text(
                            text = qr.accountNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            BankLauncherHelper.copyPaymentInfoToClipboard(
                                context,
                                qr.bankShortName,
                                qr.accountNumber,
                                amount,
                                note
                            )
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép STK",
                            tint = AppleOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!qr.beneficiaryName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tên thụ hưởng: ${qr.beneficiaryName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleGreen
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppleTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tên chủ tài khoản sẽ hiển thị chính xác khi mở App Ngân hàng",
                            fontSize = 11.sp,
                            color = AppleTextSecondary
                        )
                    }
                }
            }
        }

        // Amount Input Box
        Column {
            Text(
                text = "Số tiền thanh toán (VNĐ)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = String.format(java.util.Locale.US, "%.0f", amount),
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    val parsed = clean.toDoubleOrNull() ?: 0.0
                    onAmountChanged(parsed)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleOrange,
                    unfocusedBorderColor = AppleBorder,
                    focusedContainerColor = AppleSurface2,
                    unfocusedContainerColor = AppleSurface2,
                    focusedTextColor = AppleTextPrimary,
                    unfocusedTextColor = AppleTextPrimary
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleOrange
                )
            )
        }

        // Note Input Box
        Column {
            Text(
                text = "Nội dung chuyển khoản",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleOrange,
                    unfocusedBorderColor = AppleBorder,
                    focusedContainerColor = AppleSurface2,
                    unfocusedContainerColor = AppleSurface2,
                    focusedTextColor = AppleTextPrimary,
                    unfocusedTextColor = AppleTextPrimary
                )
            )
        }

        // Source Account Selector (Ví chi tiêu trong app)
        Column {
            Text(
                text = "Trừ tiền từ ví trong app:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isSelected = selectedAccount?.id == acc.id
                    Surface(
                        onClick = { onAccountSelected(acc) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AppleSurface4 else AppleSurface2,
                        border = BorderStroke(1.dp, if (isSelected) AppleOrange else AppleBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = acc.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AppleOrange else AppleTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatVnd(acc.balance),
                                fontSize = 11.sp,
                                color = AppleTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Bank App To Launch Chooser
        Column {
            Text(
                text = "Chọn App Ngân hàng của bạn để mở & thanh toán:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppleTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VietnameseBankRegistry.BANKS.take(8).forEach { bank ->
                    val isSelected = chosenBank.bin == bank.bin
                    Surface(
                        onClick = { onBankSelected(bank) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) AppleSurface4 else AppleSurface2,
                        border = BorderStroke(1.dp, if (isSelected) AppleCyan else AppleBorder)
                    ) {
                        Text(
                            text = bank.shortName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppleCyan else AppleTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Proceed Button
        Button(
            onClick = onProceedToBank,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = AppleOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_proceed_to_bank")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mở ${chosenBank.shortName} & Thanh toán",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ReconciliationStepContent(
    qr: ParsedVietQr,
    amount: Double,
    note: String,
    accountName: String,
    categoryName: String,
    bankName: String,
    onConfirmPaid: () -> Unit,
    onSavePending: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppleOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppleOrange,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bạn đã hoàn tất thanh toán?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppleTextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sau khi xác nhận mã PIN/FaceID trên App $bankName, hãy chọn trạng thái để ứng dụng cập nhật vào sổ chi tiêu.",
            fontSize = 13.sp,
            color = AppleTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppleSurface2),
            border = BorderStroke(1.dp, AppleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Số tiền:", fontSize = 13.sp, color = AppleTextSecondary)
                    Text(
                        formatVnd(amount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleOrange
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Danh mục:", fontSize = 13.sp, color = AppleTextSecondary)
                    Text(categoryName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nội dung:", fontSize = 13.sp, color = AppleTextSecondary)
                    Text(note, fontSize = 13.sp, color = AppleTextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Người nhận:", fontSize = 13.sp, color = AppleTextSecondary)
                    Text(
                        "${qr.bankShortName} • ${qr.accountNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary: Đã thanh toán thành công
        Button(
            onClick = onConfirmPaid,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = AppleGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_confirm_paid")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Đã thanh toán thành công",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary: Chưa thanh toán / Lưu tạm
        OutlinedButton(
            onClick = onSavePending,
            shape = CircleShape,
            border = BorderStroke(1.dp, AppleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_save_pending")
        ) {
            Text(
                text = "Chưa thanh toán / Lưu tạm vào sổ",
                color = AppleTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
