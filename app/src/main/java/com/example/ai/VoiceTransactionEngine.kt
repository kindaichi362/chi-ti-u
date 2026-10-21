package com.example.ai

import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class VoiceTransactionDraft(
    val type: TransactionType,
    val amount: Double?,
    val currency: String = "VND",
    val categoryId: Long?,
    val categoryName: String?,
    val categoryEmoji: String?,
    val accountId: Long?,
    val accountName: String?,
    val bankId: String?,
    val destinationAccountId: Long? = null,
    val destinationAccountName: String? = null,
    val date: String,
    val note: String,
    val confidence: Float,
    val requiresConfirmation: Boolean,
    val clarificationQuestion: String? = null,
    val hasAmount: Boolean = false,
    val hasCategory: Boolean = false,
    val hasAccount: Boolean = false
) {
    val isComplete: Boolean
        get() = hasAmount && hasAccount && (amount != null && amount > 0) && (accountId != null) &&
            // Transfers have no category; they need a different destination account instead.
            if (type == TransactionType.TRANSFER) destinationAccountId != null && destinationAccountId != accountId
            else hasCategory && categoryId != null

    fun toJsonString(): String {
        return """
        {
          "type": "${type.name.lowercase()}",
          "amount": ${amount?.toLong() ?: 0},
          "currency": "$currency",
          "category_id": ${categoryId ?: "null"},
          "category_name": "${categoryName ?: ""}",
          "account_id": ${accountId ?: "null"},
          "account_name": "${accountName ?: ""}",
          "date": "$date",
          "note": "$note",
          "confidence": $confidence,
          "has_amount": $hasAmount,
          "has_category": $hasCategory,
          "has_account": $hasAccount,
          "is_complete": $isComplete
        }
        """.trimIndent()
    }
}

object VoiceTransactionEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun unaccent(text: String): String {
        val temp = Normalizer.normalize(text, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("")
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .lowercase(Locale.ROOT)
    }

    fun processVoiceCommand(
        input: String,
        currentDraft: VoiceTransactionDraft?,
        availableAccounts: List<Account>,
        availableCategories: List<Category>
    ): VoiceTransactionDraft? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // If existing draft is being refined
        if (currentDraft != null && isRefinementCommand(trimmed)) {
            return refineDraft(currentDraft, trimmed, availableAccounts, availableCategories)
        }

        return parseNewTransaction(trimmed, availableAccounts, availableCategories, currentDraft)
    }

    private fun isRefinementCommand(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        return lower.startsWith("đổi") || lower.startsWith("thay") ||
                lower.startsWith("không phải") || lower.startsWith("sửa") ||
                lower.startsWith("chuyển sang") || lower.startsWith("thành") ||
                unaccented.startsWith("doi") || unaccented.startsWith("thay") ||
                unaccented.startsWith("khong phai") || unaccented.startsWith("sua") ||
                lower.contains("không phải") || unaccented.contains("khong phai")
    }

    private fun refineDraft(
        current: VoiceTransactionDraft,
        command: String,
        accounts: List<Account>,
        categories: List<Category>
    ): VoiceTransactionDraft {
        val lower = command.lowercase(Locale.ROOT)
        var updatedAmount = current.amount
        var updatedHasAmount = current.hasAmount
        var updatedAccountId = current.accountId
        var updatedAccountName = current.accountName
        var updatedBankId = current.bankId
        var updatedHasAccount = current.hasAccount
        var updatedCategoryId = current.categoryId
        var updatedCategoryName = current.categoryName
        var updatedCategoryEmoji = current.categoryEmoji
        var updatedHasCategory = current.hasCategory
        var updatedNote = current.note

        // 1. Refine Amount
        val parsedAmt = extractAmount(lower)
        if (parsedAmt != null && parsedAmt > 0) {
            updatedAmount = parsedAmt
            updatedHasAmount = true
        }

        // 2. Refine Account
        val matchedAcc = matchAccount(lower, accounts)
        if (matchedAcc != null) {
            updatedAccountId = matchedAcc.id
            updatedAccountName = matchedAcc.name
            updatedBankId = matchedAcc.bankId
            updatedHasAccount = true
        }

        // 3. Refine Category
        val matchedCat = matchCategory(lower, current.type, categories)
        if (matchedCat != null) {
            updatedCategoryId = matchedCat.id
            updatedCategoryName = matchedCat.name
            updatedCategoryEmoji = matchedCat.emoji
            updatedHasCategory = true
        }

        // 4. Refine Note
        if (lower.contains("ghi chú là") || lower.contains("ghi là")) {
            val notePart = command.substringAfter("ghi chú là").substringAfter("ghi là").trim()
            if (notePart.isNotBlank()) updatedNote = notePart
        }

        return current.copy(
            amount = updatedAmount,
            hasAmount = updatedHasAmount,
            accountId = updatedAccountId,
            accountName = updatedAccountName,
            bankId = updatedBankId,
            hasAccount = updatedHasAccount,
            categoryId = updatedCategoryId,
            categoryName = updatedCategoryName,
            categoryEmoji = updatedCategoryEmoji,
            hasCategory = updatedHasCategory,
            note = updatedNote,
            confidence = 0.98f,
            requiresConfirmation = true,
            clarificationQuestion = null
        )
    }

    private fun parseNewTransaction(
        text: String,
        accounts: List<Account>,
        categories: List<Category>,
        previousDraft: VoiceTransactionDraft? = null
    ): VoiceTransactionDraft {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        val todayStr = dateFormat.format(Date())

        // 1. Amount Extraction (Element 1/3)
        val extractedAmount = extractAmount(lower)
        val amount = extractedAmount ?: previousDraft?.amount
        val hasAmount = extractedAmount != null || (previousDraft?.hasAmount == true && amount != null && amount > 0)

        // 2. Transaction Type Detection. A transfer needs both accounts named ("rút VCB sang tiền mặt");
        // otherwise it is treated as spending from the named account instead of moving money to a guess.
        val mentionedAccounts = VnText.matchAccountsInOrder(text, accounts)
        val type = VnText.detectType(text).let {
            if (it == TransactionType.TRANSFER && mentionedAccounts.size < 2) TransactionType.EXPENSE else it
        }
        val destinationAccount = if (type == TransactionType.TRANSFER) mentionedAccounts[1] else null

        // 3. Category Evaluation (Element 2/3) - Priority for Fuel/Transportation
        val matchedCategoryResult = evaluateCategoryDetailed(lower, unaccented, type, categories)
        val categoryId = matchedCategoryResult?.first?.id ?: previousDraft?.categoryId
        val categoryName = matchedCategoryResult?.first?.name ?: previousDraft?.categoryName
        val categoryEmoji = matchedCategoryResult?.first?.emoji ?: previousDraft?.categoryEmoji
        val hasCategory = matchedCategoryResult != null || (previousDraft?.hasCategory == true && categoryId != null)
        val confidence = matchedCategoryResult?.second ?: if (hasCategory) 0.90f else 0.50f

        // 4. Account Evaluation (Element 3/3)
        val matchedAccount = matchAccount(lower, accounts)
        val accountId = matchedAccount?.id ?: previousDraft?.accountId
        val accountName = matchedAccount?.name ?: previousDraft?.accountName
        val bankId = matchedAccount?.bankId ?: previousDraft?.bankId
        val hasAccount = matchedAccount != null || (previousDraft?.hasAccount == true && accountId != null)

        val note = cleanVoiceNote(text, amount ?: 0.0, accountName ?: "", categoryName)

        return VoiceTransactionDraft(
            type = type,
            amount = amount,
            currency = "VND",
            categoryId = categoryId,
            categoryName = categoryName,
            categoryEmoji = categoryEmoji ?: if (categoryId != null) "🏷️" else null,
            accountId = accountId,
            accountName = accountName,
            bankId = bankId,
            destinationAccountId = destinationAccount?.id,
            destinationAccountName = destinationAccount?.name,
            date = todayStr,
            note = note.ifBlank { categoryName ?: "Chi tiêu" },
            confidence = confidence,
            requiresConfirmation = true,
            hasAmount = hasAmount,
            hasCategory = hasCategory,
            hasAccount = hasAccount
        )
    }

    // Amount, account and category rules are shared with the typed parser so both inputs agree.
    private fun extractAmount(text: String): Double? = VnText.extractAmount(text)?.takeIf { it > 0 }

    private fun matchAccount(text: String, accounts: List<Account>): Account? =
        VnText.matchAccountsInOrder(text, accounts).firstOrNull()

    @Suppress("UNUSED_PARAMETER")
    private fun evaluateCategoryDetailed(
        lower: String,
        unaccented: String,
        type: TransactionType,
        categories: List<Category>
    ): Pair<Category, Float>? =
        VietnameseNlpParser.matchCategory(lower, type, categories)?.let { it to 0.97f }


    private fun matchCategory(
        text: String,
        type: TransactionType,
        categories: List<Category>
    ): Category? {
        val lower = text.lowercase(Locale.ROOT)
        val unaccented = unaccent(text)
        return evaluateCategoryDetailed(lower, unaccented, type, categories)?.first
    }

    private fun cleanVoiceNote(
        rawText: String,
        amount: Double,
        accountName: String,
        categoryName: String?
    ): String {
        var cleaned = rawText
        val phrasesToRemove = listOf(
            "hết", "bằng", "qua", "từ", "vào", "tiền mặt", "bằng tiền mặt",
            "mb bank", "vietcombank", "techcombank", "momo", "vcb", "tcb"
        )
        for (p in phrasesToRemove) {
            cleaned = cleaned.replace(p, "", ignoreCase = true)
        }
        cleaned = cleaned.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        return cleaned.ifBlank { categoryName ?: "Chi tiêu" }
    }
}
