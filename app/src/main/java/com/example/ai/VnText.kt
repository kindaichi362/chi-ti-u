package com.example.ai

import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import java.text.Normalizer
import java.util.Locale

/**
 * Shared Vietnamese text helpers for the typed and the voice transaction parsers.
 *
 * Everything matches on whole words: plain substring checks were the source of bugs such as
 * "bánh mì" being read as income ("bán"), "mua thuốc" as income ("thu") and almost any
 * sentence picking the cash wallet because "vi" appears inside "Vietcombank", "viện", "việc"...
 */
object VnText {

    private val diacritics = "\\p{InCombiningDiacriticalMarks}+".toRegex()

    fun unaccent(text: String): String =
        diacritics.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)

    /** True when [phrase] appears in [text] as whole word(s), comparing without accents. */
    fun hasWord(text: String, phrase: String): Boolean {
        val p = unaccent(phrase).trim()
        if (p.isEmpty()) return false
        return Regex("(?<![\\p{L}\\d])" + Regex.escape(p) + "(?![\\p{L}\\d])").containsMatchIn(unaccent(text))
    }

    /** Like [hasWord] but keeps accents, for words whose unaccented form is ambiguous (thu/thú/thư). */
    fun hasExactWord(text: String, phrase: String): Boolean {
        val p = phrase.lowercase(Locale.ROOT).trim()
        if (p.isEmpty()) return false
        return Regex("(?<![\\p{L}\\d])" + Regex.escape(p) + "(?![\\p{L}\\d])").containsMatchIn(text.lowercase(Locale.ROOT))
    }

    fun hasAnyWord(text: String, phrases: List<String>): Boolean = phrases.any { hasWord(text, it) }

    // Moving money between the user's own accounts. "chuyển tiền cho mẹ" alone is paying someone,
    // so a bare "chuyển" only counts together with "sang" (see detectType).
    private val transferWords = listOf("rút tiền", "bắn sang", "sang ví", "nạp ví", "nạp tiền vào")
    // Accent-sensitive on purpose: "thu" (collect) vs "thú" (pet) / "thư" (letter), "bán" vs "bàn".
    private val incomeExactWords = listOf(
        "lương", "nhận lương", "thưởng", "tiền thưởng", "trả tôi", "trả lại tôi", "trả mình", "thu nợ", "thu tiền",
        "thu vào", "nhận", "được cho", "được tặng", "bán", "tiền lãi", "lãi", "hoàn tiền", "cashback", "thu nhập"
    )

    fun detectType(text: String): TransactionType {
        val lower = text.lowercase(Locale.ROOT)
        val movesBetweenOwnAccounts = (hasExactWord(lower, "chuyển") || hasExactWord(lower, "rút")) && hasExactWord(lower, "sang")
        return when {
            transferWords.any { hasExactWord(lower, it) } || movesBetweenOwnAccounts -> TransactionType.TRANSFER
            // Confirming something ("xác nhận") is not receiving money.
            hasExactWord(lower, "xác nhận") && !hasExactWord(lower, "nhận lương") -> TransactionType.EXPENSE
            // "trả nợ cho X" / "trả lương cho Y" is money going out even though it mentions debt or salary.
            hasExactWord(lower, "trả nợ") || hasExactWord(lower, "trả lương") -> TransactionType.EXPENSE
            incomeExactWords.any { hasExactWord(lower, it) } -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private val bankAliases = mapOf(
        "vcb" to listOf("vcb", "vietcombank", "vietcom"),
        "tcb" to listOf("tcb", "techcombank", "techcom"),
        "mb" to listOf("mb", "mbbank", "mb bank"),
        "vpb" to listOf("vpb", "vpbank", "vp bank", "vp"),
        "vpbank" to listOf("vpb", "vpbank", "vp bank", "vp"),
        "bidv" to listOf("bidv"),
        "ctg" to listOf("ctg", "vietinbank", "viettinbank", "vietin"),
        "vietinbank" to listOf("ctg", "vietinbank", "viettinbank", "vietin"),
        "acb" to listOf("acb"),
        "tpb" to listOf("tpb", "tpbank", "tp bank"),
        "tpbank" to listOf("tpb", "tpbank", "tp bank"),
        "vba" to listOf("agribank", "agri"),
        "agribank" to listOf("agribank", "agri"),
        "momo" to listOf("momo"),
        "zalopay" to listOf("zalopay", "zalo pay"),
        "cake" to listOf("cake")
    )

    private fun aliasesOf(acc: Account): List<String> {
        val keys = listOfNotNull(acc.bankCode, acc.bankId).map { it.lowercase(Locale.ROOT) }
        val typeWords = when (acc.type) {
            AccountType.CASH -> listOf("tiền mặt", "ví tiền", "trong ví", "cash")
            AccountType.CREDIT_CARD -> listOf("thẻ tín dụng", "credit", "thẻ visa")
            else -> emptyList()
        }
        return (listOf(acc.name) + keys + keys.flatMap { bankAliases[it].orEmpty() } + typeWords).distinct()
    }

    /**
     * Accounts mentioned in [text], in the order they appear — so "từ VCB sang MoMo" yields
     * [VCB, MoMo] (source, destination) whatever order the accounts were created in.
     */
    fun matchAccountsInOrder(text: String, accounts: List<Account>): List<Account> {
        val plain = unaccent(text)
        return accounts.mapNotNull { acc ->
            val firstHit = aliasesOf(acc).mapNotNull { alias ->
                val a = unaccent(alias).trim()
                if (a.isEmpty()) null
                else Regex("(?<![\\p{L}\\d])" + Regex.escape(a) + "(?![\\p{L}\\d])").find(plain)?.range?.first
            }.minOrNull()
            firstHit?.let { acc to it }
        }.sortedBy { it.second }.map { it.first }
    }

    private const val NUM ="(\\d+(?:[.,]\\d+)?)"
    private const val END = "(?![\\p{L}\\d])"
    // A unit may be followed directly by digits ("2tr5"), but not by letters ("2 trà", "5 kg").
    private const val UNIT_END = "(?!\\p{L})"
    // The digits after a unit must end the number: "1 triệu 2" yes, "30 triệu 15/10" no.
    private const val TAIL_END = "(?![\\p{L}\\d/.,:])"
    private val millionRegex = Regex("$NUM\\s*(?:triệu|trieu|tr|củ|cu)$UNIT_END(?:\\s*(\\d{1,3})$TAIL_END)?")
    private val thousandRegex = Regex("$NUM\\s*(?:nghìn|nghin|ngàn|ngan|k)$UNIT_END(?:\\s*(\\d)$TAIL_END)?")
    private val litRegex = Regex("$NUM\\s*(?:lít|lit|xị|xi)$END")
    private val groupedRegex = Regex("(?<![\\d.,])(\\d{1,3}(?:[.,]\\d{3})+)(?![\\d.,])")
    private val plainRegex = Regex("(?<![\\d.,])(\\d{4,12})(?![\\d.,])")
    private val shortRegex = Regex("(?<![\\d.,])(\\d{1,3})$END")
    private val tensRegex = Regex("(?<![\\p{L}\\d])(\\d{1,2})\\s*(?:chục|chuc)$END")
    private val spokenAmountRegex = Regex(
        "(?<![\\p{L}\\d])((?:(?:mot|hai|ba|bon|tu|nam|lam|sau|bay|tam|chin|muoi|chuc|tram|linh|le)\\s+){1,6})(?:k|nghin|ngan|trieu|tr|cu|chuc)(?![\\p{L}\\d])"
    )

    private fun spokenNumber(words: List<String>): Int? {
        val digits = mapOf("mot" to 1, "hai" to 2, "ba" to 3, "bon" to 4, "tu" to 4,
            "nam" to 5, "lam" to 5, "sau" to 6, "bay" to 7, "tam" to 8, "chin" to 9)
        var total = 0
        var pending = 0
        for (word in words) {
            when (word) {
                "tram" -> { total += pending.coerceAtLeast(1) * 100; pending = 0 }
                "muoi", "chuc" -> { total += pending.coerceAtLeast(1) * 10; pending = 0 }
                "linh", "le" -> Unit
                else -> pending = digits[word] ?: return null
            }
        }
        return (total + pending).takeIf { it > 0 }
    }

    /**
     * Reads a VND amount from casual Vietnamese: "50k", "50 nghìn", "2tr5" (2.5M), "1 triệu 2"
     * (1.2M), "2,5 củ", "5 lít" (500k), "1.500.000", "350000", and a bare "35" (35k).
     * Units must stand alone, so "5 kg" or "2 trà sữa" are not read as money.
     */
    fun extractAmount(text: String): Double? {
        val lower = text.lowercase(Locale.ROOT)

        tensRegex.find(lower)?.let { return it.groupValues[1].toDouble() * 10_000 }
        val plainWords = unaccent(lower)
        spokenAmountRegex.find(plainWords)?.let { match ->
            val words = match.groupValues[1].trim().split(Regex("\\s+"))
            val value = spokenNumber(words)
            val unit = match.value.trim().substringAfterLast(' ')
            if (value != null) return value * when (unit) {
                "trieu", "tr", "cu" -> 1_000_000.0
                "chuc" -> 10_000.0
                else -> 1_000.0
            }
        }

        millionRegex.find(lower)?.let { m ->
            val base = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            val tail = m.groupValues[2]
            // "2tr5" = 2.5M, "1 triệu 2" = 1.2M, "1tr250" = 1.25M
            val fraction = if (tail.isNotEmpty() && !m.groupValues[1].contains('.') && !m.groupValues[1].contains(','))
                "0.$tail".toDouble() else 0.0
            return (base + fraction) * 1_000_000
        }
        thousandRegex.find(lower)?.let { m ->
            val base = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@let
            // "2k5" = 2,500
            val fraction = m.groupValues[2].takeIf { it.isNotEmpty() }?.let { "0.$it".toDouble() } ?: 0.0
            return (base + fraction) * 1_000
        }
        litRegex.find(lower)?.let { m ->
            return m.groupValues[1].replace(',', '.').toDoubleOrNull()?.times(100_000)
        }
        groupedRegex.find(lower)?.let { m ->
            return m.groupValues[1].replace(".", "").replace(",", "").toDoubleOrNull()
        }
        plainRegex.find(lower)?.let { m ->
            return m.groupValues[1].toDoubleOrNull()
        }
        // A bare small number is how people jot amounts in thousands: "Ăn sáng 35".
        shortRegex.findAll(lower).lastOrNull()?.let { m ->
            val n = m.groupValues[1].toDouble()
            if (n >= 1) return n * 1_000
        }
        return null
    }
}
