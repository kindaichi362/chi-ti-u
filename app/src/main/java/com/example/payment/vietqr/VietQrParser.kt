package com.example.payment.vietqr

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedVietQr(
    val rawPayload: String,
    val bankBin: String,
    val bankShortName: String,
    val bankFullName: String,
    val accountNumber: String,
    val beneficiaryName: String?,
    val amount: Double?,
    val transferContent: String?,
    val isDynamic: Boolean,
    val isMerchantQr: Boolean,
    val crcValid: Boolean
)

object VietQrParser {

    /**
     * Parses a QR code string payload into structured payment details.
     * Supports:
     * 1. Official EMVCo VietQR TLV standard (NAPAS Tag 38)
     * 2. VietQR.io / VietQR.me web URL format
     * 3. Custom format / fallback
     */
    fun parse(raw: String): ParsedVietQr? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Case 1: Standard EMVCo QR (Starts with "000201" or contains Tag 00)
        if (trimmed.startsWith("000201") || (trimmed.length > 10 && trimmed.substring(0, 4) == "0002")) {
            return parseEmvCoTlv(trimmed)
        }

        // Case 2: URL format (e.g., https://img.vietqr.io/image/970436-123456789-compact.png?amount=50000&addInfo=...)
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.contains("vietqr")) {
            val urlParsed = parseUrlFormat(trimmed)
            if (urlParsed != null) return urlParsed
        }

        // Case 3: Text pattern: "VCB 123456789 50000 An sang"
        return parseTextFallback(trimmed)
    }

    private fun parseEmvCoTlv(payload: String): ParsedVietQr? {
        val rootTlvs = parseTlvString(payload)
        if (rootTlvs.isEmpty()) return null

        val isDynamic = rootTlvs["01"] == "12"

        // Tag 38: NAPAS Merchant Account Information
        val tag38Value = rootTlvs["38"] ?: rootTlvs["26"]
        var bankBin = ""
        var accountNumber = ""
        var isMerchant = false

        if (tag38Value != null) {
            val subTlvs = parseTlvString(tag38Value)
            val guid = subTlvs["00"] // Usually A000000727 for NAPAS
            val beneficiaryBlock = subTlvs["01"]

            if (beneficiaryBlock != null) {
                // Beneficiary info is nested TLV
                val beneficiaryTlvs = parseTlvString(beneficiaryBlock)
                bankBin = beneficiaryTlvs["00"] ?: ""
                accountNumber = beneficiaryTlvs["01"] ?: ""
            }

            if (guid?.contains("A000000727", ignoreCase = true) == true) {
                isMerchant = false
            } else if (rootTlvs.containsKey("26")) {
                isMerchant = true
            }
        }

        // Tag 54: Amount
        val amountStr = rootTlvs["54"]
        val amount = amountStr?.toDoubleOrNull()

        // Tag 59: Merchant Name
        val merchantName = rootTlvs["59"]

        // Tag 62: Additional Data Field Template
        var transferContent: String? = null
        val tag62Value = rootTlvs["62"]
        if (tag62Value != null) {
            val additionalTlvs = parseTlvString(tag62Value)
            transferContent = additionalTlvs["08"] // Purpose of transaction
                ?: additionalTlvs["05"] // Reference label
                ?: additionalTlvs["01"] // Bill number
        }

        // Checksum Tag 63 (4 chars)
        val crc = rootTlvs["63"]
        val crcValid = !crc.isNullOrBlank()

        val bank = VietnameseBankRegistry.findByBin(bankBin)
        val bankShortName = bank?.shortName ?: if (bankBin.isNotBlank()) "Ngân hàng ($bankBin)" else "Ngân hàng"
        val bankFullName = bank?.fullName ?: bankShortName

        if (accountNumber.isBlank() && bankBin.isBlank()) {
            return null
        }

        return ParsedVietQr(
            rawPayload = payload,
            bankBin = bankBin,
            bankShortName = bankShortName,
            bankFullName = bankFullName,
            accountNumber = accountNumber,
            beneficiaryName = merchantName?.takeIf { it.isNotBlank() },
            amount = amount,
            transferContent = transferContent,
            isDynamic = isDynamic,
            isMerchantQr = isMerchant,
            crcValid = crcValid
        )
    }

    private fun parseUrlFormat(url: String): ParsedVietQr? {
        return try {
            val uri = Uri.parse(url)
            val path = uri.path ?: ""
            val segments = path.split("/").filter { it.isNotBlank() }

            var bankBin = ""
            var accountNumber = ""

            // Pattern: /image/{bin}-{acc}-compact.png or /{bin}/{acc}/{amount}/{content}
            for (segment in segments) {
                if (segment.contains("-")) {
                    val parts = segment.replace(".png", "").replace(".jpg", "").split("-")
                    if (parts.size >= 2) {
                        bankBin = parts[0]
                        accountNumber = parts[1]
                        break
                    }
                }
            }

            if (bankBin.isBlank() && segments.size >= 2) {
                val candidateBin = segments[0]
                if (candidateBin.all { it.isDigit() } && candidateBin.length >= 6) {
                    bankBin = candidateBin
                    accountNumber = segments[1]
                }
            }

            val amountStr = uri.getQueryParameter("amount")
            val amount = amountStr?.toDoubleOrNull()
            val addInfo = uri.getQueryParameter("addInfo")?.let {
                try {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                } catch (e: Exception) {
                    it
                }
            }

            val bank = VietnameseBankRegistry.findByBin(bankBin)
            val bankShortName = bank?.shortName ?: if (bankBin.isNotBlank()) "Ngân hàng ($bankBin)" else "Ngân hàng"

            if (accountNumber.isBlank()) return null

            ParsedVietQr(
                rawPayload = url,
                bankBin = bankBin,
                bankShortName = bankShortName,
                bankFullName = bank?.fullName ?: bankShortName,
                accountNumber = accountNumber,
                beneficiaryName = null,
                amount = amount,
                transferContent = addInfo,
                isDynamic = false,
                isMerchantQr = false,
                crcValid = true
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTextFallback(text: String): ParsedVietQr? {
        val tokens = text.split("\\s+".toRegex())
        if (tokens.size < 2) return null

        val firstToken = tokens[0]
        val bank = VietnameseBankRegistry.findByNameOrKeyword(firstToken) ?: return null
        val accountNumber = tokens.getOrNull(1) ?: return null
        if (!accountNumber.all { it.isDigit() || it.isLetter() }) return null

        val amount = tokens.getOrNull(2)?.toDoubleOrNull()
        val content = if (tokens.size > 3) tokens.subList(3, tokens.size).joinToString(" ") else null

        return ParsedVietQr(
            rawPayload = text,
            bankBin = bank.bin,
            bankShortName = bank.shortName,
            bankFullName = bank.fullName,
            accountNumber = accountNumber,
            beneficiaryName = null,
            amount = amount,
            transferContent = content,
            isDynamic = false,
            isMerchantQr = false,
            crcValid = true
        )
    }

    /**
     * Parses standard 2-digit ID + 2-digit length + value TLV string.
     */
    private fun parseTlvString(tlv: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0
        val len = tlv.length

        while (index + 4 <= len) {
            val tag = tlv.substring(index, index + 2)
            val lengthStr = tlv.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: break
            val valueStart = index + 4
            val valueEnd = valueStart + length

            if (valueEnd > len) break

            val value = tlv.substring(valueStart, valueEnd)
            result[tag] = value
            index = valueEnd
        }

        return result
    }

    /**
     * Generates a standard VietQR NAPAS TLV payload string for testing or demo purposes.
     */
    fun generateVietQrPayload(
        bankBin: String,
        accountNumber: String,
        amount: Double? = null,
        content: String? = null
    ): String {
        val sb = StringBuilder()

        // Tag 00: Format Indicator
        sb.append(formatTlv("00", "01"))

        // Tag 01: Point of Initiation Method (12 = Dynamic if amount given, 11 = Static)
        val initiationMethod = if (amount != null && amount > 0) "12" else "11"
        sb.append(formatTlv("01", initiationMethod))

        // Tag 38: NAPAS Beneficiary Sub-TLV
        val sub00 = formatTlv("00", bankBin)
        val sub01 = formatTlv("01", accountNumber)
        val beneficiaryData = formatTlv("00", "A000000727") + formatTlv("01", sub00 + sub01) + formatTlv("02", "QRIBFTTA")
        sb.append(formatTlv("38", beneficiaryData))

        // Tag 53: Currency (704 = VND)
        sb.append(formatTlv("53", "704"))

        // Tag 54: Amount
        if (amount != null && amount > 0) {
            val formattedAmount = String.format(java.util.Locale.US, "%.0f", amount)
            sb.append(formatTlv("54", formattedAmount))
        }

        // Tag 58: Country Code
        sb.append(formatTlv("58", "VN"))

        // Tag 62: Additional Data (Transfer Purpose)
        if (!content.isNullOrBlank()) {
            val normalizedContent = removeAccents(content)
            val additionalData = formatTlv("08", normalizedContent)
            sb.append(formatTlv("62", additionalData))
        }

        // Tag 63: CRC placeholder
        val rawWithoutCrc = sb.toString() + "6304"
        val crcValue = computeCrc16(rawWithoutCrc)
        return rawWithoutCrc + crcValue
    }

    private fun formatTlv(tag: String, value: String): String {
        val length = String.format("%02d", value.toByteArray(StandardCharsets.UTF_8).size)
        return "$tag$length$value"
    }

    private fun removeAccents(str: String): String {
        val nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        val pattern = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return pattern.replace(nfd, "")
            .replace("đ", "d")
            .replace("Đ", "D")
    }

    private fun computeCrc16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(StandardCharsets.ISO_8859_1)

        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) crc = crc xor polynomial
            }
        }
        crc = crc and 0xFFFF
        return String.format("%04X", crc)
    }
}
