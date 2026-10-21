package com.example.data.local

import com.example.data.model.Category
import com.example.data.model.CategoryType

object InitialCategories {

    fun getDefaultCategories(): List<Category> {
        val list = mutableListOf<Category>()
        var order = 1

        // ==========================================
        // 💸 1. CHI TIỀN (EXPENSE)
        // ==========================================

        // 🍜 Ăn uống (Màu cam đào #F97316)
        val foodGroup = "Ăn uống"
        val foodColor = "#F97316"
        list.add(Category(name = "Ăn sáng", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = foodColor, emoji = "🍚", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Ăn trưa", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = foodColor, emoji = "🍱", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Ăn tối", type = CategoryType.EXPENSE, iconName = "restaurant", colorHex = foodColor, emoji = "🍲", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Cà phê", type = CategoryType.EXPENSE, iconName = "local_cafe", colorHex = foodColor, emoji = "☕", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Trà sữa / đồ uống", type = CategoryType.EXPENSE, iconName = "local_bar", colorHex = foodColor, emoji = "🧋", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Ăn ngoài", type = CategoryType.EXPENSE, iconName = "fastfood", colorHex = foodColor, emoji = "🍔", groupName = foodGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đặt đồ ăn", type = CategoryType.EXPENSE, iconName = "delivery_dining", colorHex = foodColor, emoji = "🛵", groupName = foodGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Mua thực phẩm", type = CategoryType.EXPENSE, iconName = "shopping_cart", colorHex = foodColor, emoji = "🛒", groupName = foodGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Ăn uống / liên hoan", type = CategoryType.EXPENSE, iconName = "celebration", colorHex = foodColor, emoji = "🍺", groupName = foodGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Nấu ăn", type = CategoryType.EXPENSE, iconName = "kitchen", colorHex = foodColor, emoji = "🍳", groupName = foodGroup, isPriority = false, displayOrder = order++))

        // 🏠 Sinh hoạt (Màu vàng hổ phách #EAB308)
        val livingGroup = "Sinh hoạt"
        val livingColor = "#EAB308"
        list.add(Category(name = "Tiền điện", type = CategoryType.EXPENSE, iconName = "bolt", colorHex = livingColor, emoji = "💡", groupName = livingGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Tiền nước", type = CategoryType.EXPENSE, iconName = "water_drop", colorHex = livingColor, emoji = "💧", groupName = livingGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Internet", type = CategoryType.EXPENSE, iconName = "wifi", colorHex = livingColor, emoji = "🌐", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Điện thoại", type = CategoryType.EXPENSE, iconName = "phone_android", colorHex = livingColor, emoji = "📱", groupName = livingGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Gas", type = CategoryType.EXPENSE, iconName = "local_fire_department", colorHex = livingColor, emoji = "🔥", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Dọn dẹp / vệ sinh", type = CategoryType.EXPENSE, iconName = "cleaning_services", colorHex = livingColor, emoji = "🧹", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thuê người giúp việc", type = CategoryType.EXPENSE, iconName = "person", colorHex = livingColor, emoji = "👩‍🍳", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Giặt là", type = CategoryType.EXPENSE, iconName = "local_laundry_service", colorHex = livingColor, emoji = "🧺", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Rác / phí môi trường", type = CategoryType.EXPENSE, iconName = "delete", colorHex = livingColor, emoji = "🗑️", groupName = livingGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phí chung cư", type = CategoryType.EXPENSE, iconName = "apartment", colorHex = livingColor, emoji = "🏢", groupName = livingGroup, isPriority = false, displayOrder = order++))

        // 🚗 Đi lại (Màu xanh dương #3B82F6)
        val transportGroup = "Đi lại"
        val transportColor = "#3B82F6"
        list.add(Category(name = "Xăng xe", type = CategoryType.EXPENSE, iconName = "local_gas_station", colorHex = transportColor, emoji = "⛽", groupName = transportGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Sạc xe điện", type = CategoryType.EXPENSE, iconName = "ev_station", colorHex = transportColor, emoji = "🔋", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Gửi xe", type = CategoryType.EXPENSE, iconName = "local_parking", colorHex = transportColor, emoji = "🅿️", groupName = transportGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Taxi", type = CategoryType.EXPENSE, iconName = "local_taxi", colorHex = transportColor, emoji = "🚕", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Xe ôm", type = CategoryType.EXPENSE, iconName = "two_wheeler", colorHex = transportColor, emoji = "🛵", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Xe buýt", type = CategoryType.EXPENSE, iconName = "directions_bus", colorHex = transportColor, emoji = "🚌", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tàu xe", type = CategoryType.EXPENSE, iconName = "train", colorHex = transportColor, emoji = "🚆", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Vé máy bay", type = CategoryType.EXPENSE, iconName = "flight", colorHex = transportColor, emoji = "✈️", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Sửa xe", type = CategoryType.EXPENSE, iconName = "build", colorHex = transportColor, emoji = "🔧", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Bảo dưỡng xe", type = CategoryType.EXPENSE, iconName = "car_repair", colorHex = transportColor, emoji = "🛠️", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Bảo hiểm xe", type = CategoryType.EXPENSE, iconName = "shield", colorHex = transportColor, emoji = "🛡️", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phí cầu đường", type = CategoryType.EXPENSE, iconName = "toll", colorHex = transportColor, emoji = "🧾", groupName = transportGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thuê xe", type = CategoryType.EXPENSE, iconName = "directions_car", colorHex = transportColor, emoji = "🚘", groupName = transportGroup, isPriority = false, displayOrder = order++))

        // 👶 Con cái (Màu hồng phấn #EC4899)
        val kidsGroup = "Con cái"
        val kidsColor = "#EC4899"
        list.add(Category(name = "Sữa", type = CategoryType.EXPENSE, iconName = "baby_changing_station", colorHex = kidsColor, emoji = "🍼", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Bỉm", type = CategoryType.EXPENSE, iconName = "child_care", colorHex = kidsColor, emoji = "👶", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Quần áo trẻ em", type = CategoryType.EXPENSE, iconName = "checkroom", colorHex = kidsColor, emoji = "👕", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ chơi", type = CategoryType.EXPENSE, iconName = "toys", colorHex = kidsColor, emoji = "🧸", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Học phí", type = CategoryType.EXPENSE, iconName = "school", colorHex = kidsColor, emoji = "🏫", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Sách / đồ dùng học tập", type = CategoryType.EXPENSE, iconName = "menu_book", colorHex = kidsColor, emoji = "📚", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Y tế cho con", type = CategoryType.EXPENSE, iconName = "medical_services", colorHex = kidsColor, emoji = "💊", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ dùng cho con", type = CategoryType.EXPENSE, iconName = "backpack", colorHex = kidsColor, emoji = "🎒", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Quà cho con", type = CategoryType.EXPENSE, iconName = "card_giftcard", colorHex = kidsColor, emoji = "🎁", groupName = kidsGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chi phí khác cho con", type = CategoryType.EXPENSE, iconName = "family_restroom", colorHex = kidsColor, emoji = "👨‍👩‍👦", groupName = kidsGroup, isPriority = false, displayOrder = order++))

        // 🏥 Sức khỏe (Màu xanh ngọc lục bảo #10B981)
        val healthGroup = "Sức khỏe"
        val healthColor = "#10B981"
        list.add(Category(name = "Khám bệnh", type = CategoryType.EXPENSE, iconName = "local_hospital", colorHex = healthColor, emoji = "🏥", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thuốc", type = CategoryType.EXPENSE, iconName = "medication", colorHex = healthColor, emoji = "💊", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Nha khoa", type = CategoryType.EXPENSE, iconName = "medical_services", colorHex = healthColor, emoji = "🦷", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Kính mắt", type = CategoryType.EXPENSE, iconName = "visibility", colorHex = healthColor, emoji = "👓", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Xét nghiệm", type = CategoryType.EXPENSE, iconName = "biotech", colorHex = healthColor, emoji = "🧪", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Khám định kỳ", type = CategoryType.EXPENSE, iconName = "health_and_safety", colorHex = healthColor, emoji = "🩺", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tập luyện / gym", type = CategoryType.EXPENSE, iconName = "fitness_center", colorHex = healthColor, emoji = "🏋️", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Yoga / thể thao", type = CategoryType.EXPENSE, iconName = "self_improvement", colorHex = healthColor, emoji = "🧘", groupName = healthGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Massage / trị liệu", type = CategoryType.EXPENSE, iconName = "spa", colorHex = healthColor, emoji = "💆", groupName = healthGroup, isPriority = false, displayOrder = order++))

        // 🏠 Nhà cửa (Màu tím violet #8B5CF6)
        val houseGroup = "Nhà cửa"
        val houseColor = "#8B5CF6"
        list.add(Category(name = "Nội thất", type = CategoryType.EXPENSE, iconName = "chair", colorHex = houseColor, emoji = "🛋️", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Sửa chữa nhà", type = CategoryType.EXPENSE, iconName = "handyman", colorHex = houseColor, emoji = "🧰", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ gia dụng", type = CategoryType.EXPENSE, iconName = "inventory_2", colorHex = houseColor, emoji = "🧹", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ phòng ngủ", type = CategoryType.EXPENSE, iconName = "bed", colorHex = houseColor, emoji = "🛏️", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ nhà bếp", type = CategoryType.EXPENSE, iconName = "soup_kitchen", colorHex = houseColor, emoji = "🍳", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đồ vệ sinh", type = CategoryType.EXPENSE, iconName = "sanitizer", colorHex = houseColor, emoji = "🧴", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền thuê nhà", type = CategoryType.EXPENSE, iconName = "real_estate_agent", colorHex = houseColor, emoji = "🏠", groupName = houseGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền đặt cọc nhà", type = CategoryType.EXPENSE, iconName = "key", colorHex = houseColor, emoji = "🔑", groupName = houseGroup, isPriority = false, displayOrder = order++))

        // 👕 Cá nhân (Màu ngọc lam #06B6D4)
        val personalGroup = "Cá nhân"
        val personalColor = "#06B6D4"
        list.add(Category(name = "Quần áo", type = CategoryType.EXPENSE, iconName = "checkroom", colorHex = personalColor, emoji = "👕", groupName = personalGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Giày dép", type = CategoryType.EXPENSE, iconName = "roller_skating", colorHex = personalColor, emoji = "👟", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Túi / ví", type = CategoryType.EXPENSE, iconName = "shopping_bag", colorHex = personalColor, emoji = "👜", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Mỹ phẩm", type = CategoryType.EXPENSE, iconName = "face_3", colorHex = personalColor, emoji = "💄", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Cắt tóc", type = CategoryType.EXPENSE, iconName = "content_cut", colorHex = personalColor, emoji = "💇", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Làm đẹp", type = CategoryType.EXPENSE, iconName = "brush", colorHex = personalColor, emoji = "💅", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chăm sóc cá nhân", type = CategoryType.EXPENSE, iconName = "spa", colorHex = personalColor, emoji = "🧴", groupName = personalGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phụ kiện", type = CategoryType.EXPENSE, iconName = "watch", colorHex = personalColor, emoji = "⌚", groupName = personalGroup, isPriority = false, displayOrder = order++))

        // 🎮 Giải trí (Màu tím tía #A855F7)
        val entertainGroup = "Giải trí"
        val entertainColor = "#A855F7"
        list.add(Category(name = "Xem phim", type = CategoryType.EXPENSE, iconName = "movie", colorHex = entertainColor, emoji = "🎬", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Game", type = CategoryType.EXPENSE, iconName = "sports_esports", colorHex = entertainColor, emoji = "🎮", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Karaoke", type = CategoryType.EXPENSE, iconName = "mic", colorHex = entertainColor, emoji = "🎤", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Âm nhạc", type = CategoryType.EXPENSE, iconName = "music_note", colorHex = entertainColor, emoji = "🎵", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thể thao", type = CategoryType.EXPENSE, iconName = "sports_soccer", colorHex = entertainColor, emoji = "⚽", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Vui chơi", type = CategoryType.EXPENSE, iconName = "attractions", colorHex = entertainColor, emoji = "🎡", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phim / truyền hình", type = CategoryType.EXPENSE, iconName = "tv", colorHex = entertainColor, emoji = "📺", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Vé sự kiện", type = CategoryType.EXPENSE, iconName = "confirmation_number", colorHex = entertainColor, emoji = "🎫", groupName = entertainGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Sở thích cá nhân", type = CategoryType.EXPENSE, iconName = "palette", colorHex = entertainColor, emoji = "🎨", groupName = entertainGroup, isPriority = false, displayOrder = order++))

        // ✈️ Du lịch (Màu xanh cánh đào #14B8A6)
        val travelGroup = "Du lịch"
        val travelColor = "#14B8A6"
        list.add(Category(name = "Vé máy bay du lịch", type = CategoryType.EXPENSE, iconName = "flight_takeoff", colorHex = travelColor, emoji = "✈️", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Khách sạn", type = CategoryType.EXPENSE, iconName = "hotel", colorHex = travelColor, emoji = "🏨", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thuê xe du lịch", type = CategoryType.EXPENSE, iconName = "car_rental", colorHex = travelColor, emoji = "🚗", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Ăn uống du lịch", type = CategoryType.EXPENSE, iconName = "restaurant_menu", colorHex = travelColor, emoji = "🍜", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Vé tham quan", type = CategoryType.EXPENSE, iconName = "local_activity", colorHex = travelColor, emoji = "🎫", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Mua sắm khi du lịch", type = CategoryType.EXPENSE, iconName = "shopping_basket", colorHex = travelColor, emoji = "🛍️", groupName = travelGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chi phí du lịch khác", type = CategoryType.EXPENSE, iconName = "luggage", colorHex = travelColor, emoji = "🧳", groupName = travelGroup, isPriority = false, displayOrder = order++))

        // 🎁 Quà tặng – Hiếu hỉ (Màu hồng đào san hô #F43F5E)
        val giftGroup = "Quà tặng – Hiếu hỉ"
        val giftColor = "#F43F5E"
        list.add(Category(name = "Quà tặng", type = CategoryType.EXPENSE, iconName = "card_giftcard", colorHex = giftColor, emoji = "🎁", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Sinh nhật", type = CategoryType.EXPENSE, iconName = "cake", colorHex = giftColor, emoji = "💐", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đám cưới", type = CategoryType.EXPENSE, iconName = "favorite", colorHex = giftColor, emoji = "💒", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tang lễ", type = CategoryType.EXPENSE, iconName = "nightlight_round", colorHex = giftColor, emoji = "🕯️", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thăm hỏi", type = CategoryType.EXPENSE, iconName = "people", colorHex = giftColor, emoji = "👨‍👩‍👧", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Mừng lễ / Tết", type = CategoryType.EXPENSE, iconName = "paid", colorHex = giftColor, emoji = "🧧", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Biếu bố mẹ", type = CategoryType.EXPENSE, iconName = "volunteer_activism", colorHex = giftColor, emoji = "❤️", groupName = giftGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Quan hệ / đối ngoại", type = CategoryType.EXPENSE, iconName = "handshake", colorHex = giftColor, emoji = "🤝", groupName = giftGroup, isPriority = false, displayOrder = order++))

        // 📚 Phát triển bản thân (Màu chàm #6366F1)
        val eduGroup = "Phát triển bản thân"
        val eduColor = "#6366F1"
        list.add(Category(name = "Sách", type = CategoryType.EXPENSE, iconName = "menu_book", colorHex = eduColor, emoji = "📚", groupName = eduGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Học phí", type = CategoryType.EXPENSE, iconName = "school", colorHex = eduColor, emoji = "🎓", groupName = eduGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Khóa học", type = CategoryType.EXPENSE, iconName = "laptop_chromebook", colorHex = eduColor, emoji = "💻", groupName = eduGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Học ngoại ngữ", type = CategoryType.EXPENSE, iconName = "translate", colorHex = eduColor, emoji = "🗣️", groupName = eduGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đào tạo nghề", type = CategoryType.EXPENSE, iconName = "business_center", colorHex = eduColor, emoji = "💼", groupName = eduGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Báo / tạp chí", type = CategoryType.EXPENSE, iconName = "newspaper", colorHex = eduColor, emoji = "📰", groupName = eduGroup, isPriority = false, displayOrder = order++))

        // 💼 Công việc / kinh doanh (Màu xanh cobalt #2563EB)
        val bizGroup = "Công việc / kinh doanh"
        val bizColor = "#2563EB"
        list.add(Category(name = "Quảng cáo", type = CategoryType.EXPENSE, iconName = "campaign", colorHex = bizColor, emoji = "📣", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phần mềm", type = CategoryType.EXPENSE, iconName = "terminal", colorHex = bizColor, emoji = "💻", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Vận chuyển", type = CategoryType.EXPENSE, iconName = "local_shipping", colorHex = bizColor, emoji = "📦", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Văn phòng phẩm", type = CategoryType.EXPENSE, iconName = "description", colorHex = bizColor, emoji = "🧾", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiếp khách", type = CategoryType.EXPENSE, iconName = "coffee", colorHex = bizColor, emoji = "👔", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Công tác", type = CategoryType.EXPENSE, iconName = "flight", colorHex = bizColor, emoji = "🚗", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chi phí nhân viên", type = CategoryType.EXPENSE, iconName = "groups", colorHex = bizColor, emoji = "💰", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thuê mặt bằng", type = CategoryType.EXPENSE, iconName = "store", colorHex = bizColor, emoji = "🏢", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Điện thoại công việc", type = CategoryType.EXPENSE, iconName = "call", colorHex = bizColor, emoji = "📞", groupName = bizGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phí ngân hàng công việc", type = CategoryType.EXPENSE, iconName = "account_balance", colorHex = bizColor, emoji = "💳", groupName = bizGroup, isPriority = false, displayOrder = order++))

        // 💳 Tài chính (Màu xám kim loại #64748B)
        val financeGroup = "Tài chính"
        val financeColor = "#64748B"
        list.add(Category(name = "Phí thẻ", type = CategoryType.EXPENSE, iconName = "credit_card", colorHex = financeColor, emoji = "💳", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Phí ngân hàng", type = CategoryType.EXPENSE, iconName = "account_balance", colorHex = financeColor, emoji = "🏦", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Trả nợ", type = CategoryType.EXPENSE, iconName = "money_off", colorHex = financeColor, emoji = "💸", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Đầu tư", type = CategoryType.EXPENSE, iconName = "trending_up", colorHex = financeColor, emoji = "📈", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiết kiệm", type = CategoryType.EXPENSE, iconName = "savings", colorHex = financeColor, emoji = "🏦", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chuyển tiền", type = CategoryType.EXPENSE, iconName = "swap_horiz", colorHex = financeColor, emoji = "💰", groupName = financeGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chi khác", type = CategoryType.EXPENSE, iconName = "more_horiz", colorHex = financeColor, emoji = "🔄", groupName = financeGroup, isPriority = false, displayOrder = order++))

        // ==========================================
        // 💰 2. THU TIỀN (INCOME)
        // ==========================================

        // 💼 Thu nhập (Màu xanh ngọc bích #059669)
        val incJobGroup = "Thu nhập"
        val incJobColor = "#059669"
        list.add(Category(name = "Lương", type = CategoryType.INCOME, iconName = "payments", colorHex = incJobColor, emoji = "💵", groupName = incJobGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Thưởng", type = CategoryType.INCOME, iconName = "redeem", colorHex = incJobColor, emoji = "🎁", groupName = incJobGroup, isPriority = true, displayOrder = order++))
        list.add(Category(name = "Thu nhập kinh doanh", type = CategoryType.INCOME, iconName = "storefront", colorHex = incJobColor, emoji = "💼", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Lợi nhuận kinh doanh", type = CategoryType.INCOME, iconName = "trending_up", colorHex = incJobColor, emoji = "🏪", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Hoa hồng", type = CategoryType.INCOME, iconName = "percent", colorHex = incJobColor, emoji = "💰", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền làm thêm", type = CategoryType.INCOME, iconName = "more_time", colorHex = incJobColor, emoji = "🤝", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thu nhập đầu tư", type = CategoryType.INCOME, iconName = "show_chart", colorHex = incJobColor, emoji = "📈", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền cho thuê nhà", type = CategoryType.INCOME, iconName = "home", colorHex = incJobColor, emoji = "🏠", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền cho thuê xe", type = CategoryType.INCOME, iconName = "directions_car", colorHex = incJobColor, emoji = "🚗", groupName = incJobGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Lãi tiền gửi", type = CategoryType.INCOME, iconName = "account_balance", colorHex = incJobColor, emoji = "💳", groupName = incJobGroup, isPriority = false, displayOrder = order++))

        // 💸 Tiền nhận (Màu xanh lá tươi #10B981)
        val incReceivedGroup = "Tiền nhận"
        val incReceivedColor = "#10B981"
        list.add(Category(name = "Người thân chuyển", type = CategoryType.INCOME, iconName = "family_restroom", colorHex = incReceivedColor, emoji = "👨‍👩‍👦", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Bạn bè chuyển", type = CategoryType.INCOME, iconName = "group", colorHex = incReceivedColor, emoji = "🤝", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền được hoàn", type = CategoryType.INCOME, iconName = "replay", colorHex = incReceivedColor, emoji = "💰", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Hoàn tiền mua hàng", type = CategoryType.INCOME, iconName = "shopping_cart", colorHex = incReceivedColor, emoji = "🛒", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Hoàn tiền thẻ", type = CategoryType.INCOME, iconName = "credit_card", colorHex = incReceivedColor, emoji = "💳", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền được tặng", type = CategoryType.INCOME, iconName = "card_giftcard", colorHex = incReceivedColor, emoji = "🎁", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thu hồi tiền đã cho vay", type = CategoryType.INCOME, iconName = "price_check", colorHex = incReceivedColor, emoji = "🔄", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền đặt cọc nhận lại", type = CategoryType.INCOME, iconName = "key", colorHex = incReceivedColor, emoji = "📦", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Thu nhập khác", type = CategoryType.INCOME, iconName = "attach_money", colorHex = incReceivedColor, emoji = "💵", groupName = incReceivedGroup, isPriority = false, displayOrder = order++))

        // ==========================================
        // 🔄 3. VAY / NỢ
        // ==========================================
        val debtGroup = "Vay / Nợ"
        val debtColor = "#D97706"
        list.add(Category(name = "Cho người khác vay", type = CategoryType.EXPENSE, iconName = "arrow_upward", colorHex = debtColor, emoji = "🤝", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Nhận tiền vay", type = CategoryType.INCOME, iconName = "arrow_downward", colorHex = debtColor, emoji = "💰", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Trả tiền vay", type = CategoryType.EXPENSE, iconName = "done", colorHex = debtColor, emoji = "💸", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Người khác trả nợ", type = CategoryType.INCOME, iconName = "done_all", colorHex = debtColor, emoji = "👤", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Trả khoản vay ngân hàng", type = CategoryType.EXPENSE, iconName = "account_balance", colorHex = debtColor, emoji = "🏦", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Trả thẻ tín dụng", type = CategoryType.EXPENSE, iconName = "credit_card", colorHex = debtColor, emoji = "💳", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Tiền lãi vay", type = CategoryType.EXPENSE, iconName = "trending_down", colorHex = debtColor, emoji = "📈", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Chuyển tiền giữa tài khoản", type = CategoryType.EXPENSE, iconName = "swap_horiz", colorHex = debtColor, emoji = "🔄", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Rút tiền mặt", type = CategoryType.EXPENSE, iconName = "atm", colorHex = debtColor, emoji = "💵", groupName = debtGroup, isPriority = false, displayOrder = order++))
        list.add(Category(name = "Nộp tiền vào ngân hàng", type = CategoryType.EXPENSE, iconName = "input", colorHex = debtColor, emoji = "🏦", groupName = debtGroup, isPriority = false, displayOrder = order++))

        return list
    }
}
