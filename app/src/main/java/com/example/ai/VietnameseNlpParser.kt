package com.example.ai

import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import java.util.Locale

data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long?,
    val categoryName: String?,
    val sourceAccountId: Long,
    val sourceAccountName: String,
    val destinationAccountId: Long? = null,
    val destinationAccountName: String? = null,
    val note: String,
    val confidenceScore: Float
)

object VietnameseNlpParser {

    fun parse(
        input: String,
        availableAccounts: List<Account>,
        availableCategories: List<Category>
    ): ParsedTransaction? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        // Without a real account the transaction could not be saved anyway.
        val defaultAccount = availableAccounts.firstOrNull() ?: return null

        // 1. Extract Amount
        val amount = VnText.extractAmount(trimmed)?.takeIf { it > 0 } ?: return null

        // 2. Determine Transaction Type
        var transactionType = VnText.detectType(trimmed)

        // 3. Match Accounts (in the order they are mentioned)
        val matchedAccounts = VnText.matchAccountsInOrder(trimmed, availableAccounts)

        val sourceAccount: Account
        var destAccount: Account? = null

        if (transactionType == TransactionType.TRANSFER) {
            sourceAccount = matchedAccounts.getOrNull(0) ?: defaultAccount
            destAccount = matchedAccounts.getOrNull(1)
                ?: availableAccounts.firstOrNull { it.id != sourceAccount.id }
            // A transfer needs two different accounts; with only one wallet it is just spending.
            if (destAccount == null) transactionType = TransactionType.EXPENSE
        } else {
            sourceAccount = matchedAccounts.firstOrNull() ?: defaultAccount
        }

        // 4. Match Category
        val matchedCategory = if (transactionType == TransactionType.TRANSFER) null
        else matchCategory(trimmed, transactionType, availableCategories)

        // 5. Clean Note
        val note = cleanNote(trimmed)

        return ParsedTransaction(
            type = transactionType,
            amount = amount,
            categoryId = matchedCategory?.id,
            categoryName = matchedCategory?.name,
            sourceAccountId = sourceAccount.id,
            sourceAccountName = sourceAccount.name,
            destinationAccountId = destAccount?.id,
            destinationAccountName = destAccount?.name,
            note = note,
            confidenceScore = if (matchedCategory != null) 0.95f else 0.6f
        )
    }

    private val fuelKeywords = listOf(
        "đổ xăng", "đỏ xăng", "đỗ xăng", "tiền xăng", "xăng xe", "xăng", "bơm xăng", "nạp xăng", "mua xăng", "cây xăng",
        "petrolimex", "pvoil", "pv oil", "dầu nhớt", "thay nhớt", "thay dầu", "nhớt"
    )

    // Keywords per category *name fragment*; the first existing category whose name contains the
    // fragment wins. Order matters: more specific groups first.
    private val keywordsMap = listOf(
        "xăng" to fuelKeywords,
        "gửi xe" to listOf("gửi xe", "vé gửi xe", "giữ xe"),
        "sửa xe" to listOf("sửa xe", "vá xe", "vá lốp", "thay lốp", "rửa xe"),
        "phí cầu đường" to listOf("cầu đường", "phí bot", "trạm bot", "vetc", "epass"),
        "taxi" to listOf("grab", "taxi", "xanh sm", "be car", "gojek"),
        "ăn sáng" to listOf("ăn sáng", "bánh mì", "xôi", "phở sáng"),
        "ăn trưa" to listOf("ăn trưa", "cơm trưa"),
        "ăn tối" to listOf("ăn tối", "cơm tối"),
        "cà phê" to listOf("cà phê", "cafe", "cafê", "coffee", "highlands", "starbucks"),
        "trà sữa" to listOf("trà sữa", "trà chanh", "nước ngọt", "sinh tố"),
        "ăn ngoài" to listOf("phở", "bún", "lẩu", "nướng", "hủ tiếu", "cơm", "nhà hàng", "quán"),
        "đặt đồ ăn" to listOf("shopeefood", "grabfood", "baemin", "đặt đồ ăn"),
        "mua thực phẩm" to listOf("đi chợ", "siêu thị", "winmart", "bách hóa xanh", "coopmart", "thực phẩm"),
        "tiền điện" to listOf("tiền điện", "evn", "hóa đơn điện"),
        "tiền nước" to listOf("tiền nước", "hóa đơn nước"),
        "internet" to listOf("internet", "wifi", "tiền mạng", "fpt", "viettel net"),
        "điện thoại" to listOf("nạp điện thoại", "thẻ cào", "cước điện thoại", "nạp card"),
        "mua sắm" to listOf("quần áo", "giày", "dép", "shopee", "lazada", "tiki", "mua sắm", "mỹ phẩm"),
        "giải trí" to listOf("xem phim", "cgv", "lotte cinema", "netflix", "spotify", "karaoke", "chơi game"),
        "thuốc" to listOf("thuốc", "nhà thuốc", "pharmacity", "long châu"),
        "khám" to listOf("bệnh viện", "khám bệnh", "nha khoa", "bác sĩ", "khám"),
        "học phí" to listOf("học phí", "tiền học", "khóa học"),
        // Broad fallbacks for users whose categories are coarse ("Ăn uống", "Đi lại", "Sức khỏe"...).
        "ăn uống" to listOf("ăn sáng", "ăn trưa", "ăn tối", "bánh mì", "xôi", "phở", "bún", "cơm", "lẩu", "nướng", "cà phê", "cafe", "trà sữa", "nhà hàng", "quán"),
        "đi lại" to listOf("grab", "taxi", "xe ôm", "xe buýt", "vé xe", "vé tàu", "máy bay", "gửi xe"),
        "xe" to fuelKeywords + listOf("gửi xe", "sửa xe", "rửa xe"),
        "sức khỏe" to listOf("thuốc", "bệnh viện", "khám", "nha khoa", "bác sĩ"),
        "hóa đơn" to listOf("hóa đơn", "tiền điện", "tiền nước", "internet", "wifi", "tiền nhà", "chung cư"),
        "lương" to listOf("lương", "nhận lương", "bắn lương"),
        "thưởng" to listOf("thưởng", "bonus"),
        "lãi" to listOf("tiền lãi", "lãi tiết kiệm", "lãi"),
        "bán" to listOf("bán đồ", "bán hàng", "thanh lý")
    )

    fun matchCategory(
        text: String,
        type: TransactionType,
        categories: List<Category>
    ): Category? {
        val wantedType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val targetCategories = categories.filter { it.type == wantedType }

        // 0. The sentence names a category directly ("Ăn sáng", "Tiền điện"...): longest name wins.
        for (cat in targetCategories.sortedByDescending { it.name.length }) {
            val names = cat.name.split("/").map { it.trim() }.filter { VnText.unaccent(it).length >= 3 }
            if (names.any { VnText.hasWord(text, it) }) return cat
        }

        // 1. Keyword rules mapped onto whatever matching category the user actually has.
        for ((fragment, keywords) in keywordsMap) {
            if (VnText.hasAnyWord(text, keywords)) {
                val found = targetCategories.firstOrNull { VnText.unaccent(it.name).contains(VnText.unaccent(fragment)) }
                if (found != null) return found
            }
        }

        // Unknown: leave it for the user to pick rather than guessing a wrong category.
        return null
    }

    private fun cleanNote(text: String): String {
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
