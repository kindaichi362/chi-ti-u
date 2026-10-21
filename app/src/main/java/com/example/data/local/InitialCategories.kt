package com.example.data.local

import com.example.data.model.Category
import com.example.data.model.CategoryType

/**
 * The default category tree: 16 groups, one place per kind of spending.
 *
 * Rules the list follows:
 * - No two categories mean the same thing ("Vé máy bay" vs "Vé máy bay du lịch" was one thing twice).
 * - Moving money between your own accounts, lending, saving, investing and paying a credit card
 *   are NOT spending; they are transfers or debts, so they are not categories here.
 * - Starred (isPriority) categories are the ones shown first when adding a transaction.
 */
object InitialCategories {

    private class Group(val name: String, val color: String, val type: CategoryType, val items: List<Item>)
    private class Item(val name: String, val emoji: String, val icon: String, val priority: Boolean = false)

    private val groups = listOf(
        Group("Ăn uống", "#F97316", CategoryType.EXPENSE, listOf(
            Item("Ăn sáng", "🍚", "restaurant", true),
            Item("Ăn trưa", "🍱", "restaurant", true),
            Item("Ăn tối", "🍲", "restaurant", true),
            Item("Cà phê", "☕", "local_cafe", true),
            Item("Trà sữa / đồ uống", "🧋", "local_cafe", true),
            Item("Đi chợ / siêu thị", "🛒", "shopping_cart", true),
            Item("Đặt đồ ăn", "🛵", "restaurant"),
            Item("Nhà hàng / liên hoan", "🍻", "dining")
        )),
        Group("Nhà ở", "#EAB308", CategoryType.EXPENSE, listOf(
            Item("Tiền thuê nhà", "🏠", "home", true),
            Item("Phí chung cư / quản lý", "🏢", "home"),
            Item("Tiền điện", "💡", "home", true),
            Item("Tiền nước", "💧", "home", true),
            Item("Internet", "🌐", "home"),
            Item("Điện thoại", "📱", "phone_android", true),
            Item("Gas", "🔥", "home"),
            Item("Rác / vệ sinh", "🧹", "home"),
            Item("Giúp việc", "👩‍🍳", "home"),
            Item("Giặt là", "🧺", "home"),
            Item("Sửa chữa nhà", "🔨", "home")
        )),
        Group("Đi lại", "#3B82F6", CategoryType.EXPENSE, listOf(
            Item("Xăng xe", "⛽", "local_gas_station", true),
            Item("Sạc xe điện", "🔋", "local_gas_station"),
            Item("Gửi xe", "🅿️", "local_gas_station", true),
            Item("Taxi / xe công nghệ", "🚕", "local_gas_station"),
            Item("Xe buýt / tàu", "🚌", "local_gas_station"),
            Item("Sửa / bảo dưỡng xe", "🔧", "local_gas_station"),
            Item("Bảo hiểm xe", "🛡️", "local_gas_station"),
            Item("Phí cầu đường", "🧾", "local_gas_station")
        )),
        Group("Mua sắm", "#06B6D4", CategoryType.EXPENSE, listOf(
            Item("Quần áo", "👕", "shopping_bag"),
            Item("Giày dép", "👟", "shopping_bag"),
            Item("Túi / phụ kiện", "👜", "shopping_bag"),
            Item("Mỹ phẩm", "💄", "shopping_bag"),
            Item("Đồ gia dụng / nội thất", "🛋️", "shopping_bag"),
            Item("Đồ điện tử", "💻", "shopping_bag")
        )),
        Group("Làm đẹp", "#D946EF", CategoryType.EXPENSE, listOf(
            Item("Cắt tóc", "💇", "shopping_bag"),
            Item("Làm đẹp / spa", "💅", "shopping_bag"),
            Item("Chăm sóc cá nhân", "🧴", "shopping_bag")
        )),
        Group("Sức khỏe", "#10B981", CategoryType.EXPENSE, listOf(
            Item("Khám bệnh", "🩺", "medical_services"),
            Item("Thuốc", "💊", "medical_services"),
            Item("Nha khoa", "🦷", "medical_services"),
            Item("Kính mắt", "👓", "medical_services"),
            Item("Thể thao / gym", "🏋️", "medical_services")
        )),
        Group("Giải trí", "#A855F7", CategoryType.EXPENSE, listOf(
            Item("Xem phim", "🎬", "movie"),
            Item("Ứng dụng / đăng ký", "📺", "movie"),
            Item("Game", "🎮", "movie"),
            Item("Karaoke / vui chơi", "🎤", "movie"),
            Item("Vé sự kiện", "🎫", "movie"),
            Item("Sở thích", "🎨", "movie")
        )),
        Group("Du lịch", "#14B8A6", CategoryType.EXPENSE, listOf(
            Item("Vé máy bay", "✈️", "card_giftcard"),
            Item("Khách sạn", "🏨", "home"),
            Item("Vé tham quan", "🎟️", "movie"),
            Item("Chi phí du lịch khác", "🧳", "card_giftcard")
        )),
        Group("Con cái", "#EC4899", CategoryType.EXPENSE, listOf(
            Item("Sữa / bỉm", "🍼", "redeem"),
            Item("Học phí cho con", "🏫", "school"),
            Item("Đồ dùng cho con", "🧸", "redeem"),
            Item("Sách vở cho con", "📚", "school"),
            Item("Y tế cho con", "💉", "medical_services"),
            Item("Chi khác cho con", "👶", "redeem")
        )),
        Group("Học tập", "#6366F1", CategoryType.EXPENSE, listOf(
            Item("Học phí / khóa học", "🎓", "school"),
            Item("Sách / báo", "📖", "school")
        )),
        Group("Hiếu hỉ – Quà tặng", "#F43F5E", CategoryType.EXPENSE, listOf(
            Item("Quà tặng", "🎁", "card_giftcard"),
            Item("Đám cưới", "💒", "card_giftcard"),
            Item("Ma chay / thăm hỏi", "🕯️", "card_giftcard"),
            Item("Lễ Tết", "🧧", "card_giftcard"),
            Item("Biếu bố mẹ", "👨‍👩‍👧", "card_giftcard")
        )),
        Group("Công việc", "#2563EB", CategoryType.EXPENSE, listOf(
            Item("Quảng cáo", "📣", "storefront"),
            Item("Phần mềm / dịch vụ", "🧩", "storefront"),
            Item("Vận chuyển", "🚚", "storefront"),
            Item("Văn phòng phẩm", "📎", "storefront"),
            Item("Tiếp khách", "🤝", "storefront"),
            Item("Công tác", "💼", "storefront"),
            Item("Lương nhân viên", "👥", "storefront"),
            Item("Thuê mặt bằng", "🏬", "storefront")
        )),
        Group("Phí & thuế", "#64748B", CategoryType.EXPENSE, listOf(
            Item("Phí ngân hàng / phí thẻ", "🏦", "account_balance"),
            Item("Lãi vay / lãi thẻ", "📉", "credit_card"),
            Item("Thuế", "🧾", "price_check")
        )),
        Group("Khác", "#78716C", CategoryType.EXPENSE, listOf(
            Item("Chi khác", "📦", "payments")
        )),
        Group("Thu nhập", "#059669", CategoryType.INCOME, listOf(
            Item("Lương", "💰", "payments", true),
            Item("Thưởng", "🎉", "redeem", true),
            Item("Làm thêm / freelance", "💼", "payments"),
            Item("Kinh doanh", "🏪", "storefront"),
            Item("Lãi tiết kiệm / đầu tư", "📈", "trending_up"),
            Item("Cho thuê", "🏘️", "home")
        )),
        Group("Tiền nhận", "#10B981", CategoryType.INCOME, listOf(
            Item("Được cho / tặng", "🎁", "redeem"),
            Item("Hoàn tiền", "💸", "price_check"),
            Item("Thu nhập khác", "➕", "payments")
        ))
    )

    fun getDefaultCategories(): List<Category> {
        var order = 1
        return groups.flatMap { g ->
            g.items.map {
                Category(
                    name = it.name,
                    type = g.type,
                    iconName = it.icon,
                    colorHex = g.color,
                    emoji = it.emoji,
                    groupName = g.name,
                    isPriority = it.priority,
                    displayOrder = order++
                )
            }
        }
    }

    /** Groups in display order, so screens list them the same way everywhere. */
    val groupOrder: List<String> = groups.map { it.name }

    /**
     * Old default names (app v1.0) → the category they became. Several old names can land on the
     * same new one; their transactions, budgets and bills are merged by the v6 migration.
     */
    val legacyRenames: Map<String, String> = mapOf(
        "Mua thực phẩm" to "Đi chợ / siêu thị",
        "Nấu ăn" to "Đi chợ / siêu thị",
        "Ăn ngoài" to "Nhà hàng / liên hoan",
        "Ăn uống / liên hoan" to "Nhà hàng / liên hoan",
        "Phí chung cư" to "Phí chung cư / quản lý",
        "Dọn dẹp / vệ sinh" to "Rác / vệ sinh",
        "Rác / phí môi trường" to "Rác / vệ sinh",
        "Đồ vệ sinh" to "Rác / vệ sinh",
        "Thuê người giúp việc" to "Giúp việc",
        "Tiền đặt cọc nhà" to "Tiền thuê nhà",
        "Taxi" to "Taxi / xe công nghệ",
        "Xe ôm" to "Taxi / xe công nghệ",
        "Thuê xe" to "Taxi / xe công nghệ",
        "Xe buýt" to "Xe buýt / tàu",
        "Tàu xe" to "Xe buýt / tàu",
        "Sửa xe" to "Sửa / bảo dưỡng xe",
        "Bảo dưỡng xe" to "Sửa / bảo dưỡng xe",
        "Túi / ví" to "Túi / phụ kiện",
        "Phụ kiện" to "Túi / phụ kiện",
        "Nội thất" to "Đồ gia dụng / nội thất",
        "Đồ gia dụng" to "Đồ gia dụng / nội thất",
        "Đồ phòng ngủ" to "Đồ gia dụng / nội thất",
        "Đồ nhà bếp" to "Đồ gia dụng / nội thất",
        "Làm đẹp" to "Làm đẹp / spa",
        "Massage / trị liệu" to "Làm đẹp / spa",
        "Xét nghiệm" to "Khám bệnh",
        "Khám định kỳ" to "Khám bệnh",
        "Tập luyện / gym" to "Thể thao / gym",
        "Yoga / thể thao" to "Thể thao / gym",
        "Thể thao" to "Thể thao / gym",
        "Phim / truyền hình" to "Ứng dụng / đăng ký",
        "Âm nhạc" to "Ứng dụng / đăng ký",
        "Karaoke" to "Karaoke / vui chơi",
        "Vui chơi" to "Karaoke / vui chơi",
        "Sở thích cá nhân" to "Sở thích",
        "Vé máy bay du lịch" to "Vé máy bay",
        "Thuê xe du lịch" to "Chi phí du lịch khác",
        "Ăn uống du lịch" to "Chi phí du lịch khác",
        "Mua sắm khi du lịch" to "Chi phí du lịch khác",
        "Sữa" to "Sữa / bỉm",
        "Bỉm" to "Sữa / bỉm",
        "Quần áo trẻ em" to "Đồ dùng cho con",
        "Đồ chơi" to "Đồ dùng cho con",
        "Quà cho con" to "Đồ dùng cho con",
        "Sách / đồ dùng học tập" to "Sách vở cho con",
        "Chi phí khác cho con" to "Chi khác cho con",
        // "Học phí" existed twice (children + self) with the same name, so v5 already merged them
        // into one row; it becomes the children's one, which was listed first.
        "Học phí" to "Học phí cho con",
        "Khóa học" to "Học phí / khóa học",
        "Học ngoại ngữ" to "Học phí / khóa học",
        "Đào tạo nghề" to "Học phí / khóa học",
        "Sách" to "Sách / báo",
        "Báo / tạp chí" to "Sách / báo",
        "Sinh nhật" to "Quà tặng",
        "Quan hệ / đối ngoại" to "Quà tặng",
        "Tang lễ" to "Ma chay / thăm hỏi",
        "Thăm hỏi" to "Ma chay / thăm hỏi",
        "Mừng lễ / Tết" to "Lễ Tết",
        "Phần mềm" to "Phần mềm / dịch vụ",
        "Chi phí nhân viên" to "Lương nhân viên",
        "Điện thoại công việc" to "Điện thoại",
        "Phí ngân hàng công việc" to "Phí ngân hàng / phí thẻ",
        "Phí ngân hàng" to "Phí ngân hàng / phí thẻ",
        "Phí thẻ" to "Phí ngân hàng / phí thẻ",
        "Tiền lãi vay" to "Lãi vay / lãi thẻ",
        "Tiền làm thêm" to "Làm thêm / freelance",
        "Hoa hồng" to "Làm thêm / freelance",
        "Thu nhập kinh doanh" to "Kinh doanh",
        "Lợi nhuận kinh doanh" to "Kinh doanh",
        "Thu nhập đầu tư" to "Lãi tiết kiệm / đầu tư",
        "Lãi tiền gửi" to "Lãi tiết kiệm / đầu tư",
        "Tiền cho thuê nhà" to "Cho thuê",
        "Tiền cho thuê xe" to "Cho thuê",
        "Người thân chuyển" to "Được cho / tặng",
        "Bạn bè chuyển" to "Được cho / tặng",
        "Tiền được tặng" to "Được cho / tặng",
        "Tiền được hoàn" to "Hoàn tiền",
        "Hoàn tiền mua hàng" to "Hoàn tiền",
        "Hoàn tiền thẻ" to "Hoàn tiền",
        "Tiền đặt cọc nhận lại" to "Hoàn tiền"
    )

    /**
     * Old defaults that were really transfers / debts / savings, not income or spending. The
     * migration deletes them when nothing uses them; used ones are kept (history stays intact)
     * and moved to the "Khác" group so the user can re-file them.
     */
    val legacyNonSpending: Set<String> = setOf(
        "Trả nợ", "Đầu tư", "Tiết kiệm", "Chuyển tiền", "Cho người khác vay", "Nhận tiền vay",
        "Trả tiền vay", "Người khác trả nợ", "Trả khoản vay ngân hàng", "Trả thẻ tín dụng",
        "Chuyển tiền giữa tài khoản", "Rút tiền mặt", "Nộp tiền vào ngân hàng", "Thu hồi tiền đã cho vay"
    )
}
