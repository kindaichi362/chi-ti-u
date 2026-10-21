package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.ChildFriendly
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Toll
import androidx.compose.material.icons.outlined.Toys
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.Category

/** One outlined icon per default category (no emoji in the UI). */
private val byName: Map<String, ImageVector> = mapOf(
    "Ăn sáng" to Icons.Outlined.BakeryDining,
    "Ăn trưa" to Icons.Outlined.LunchDining,
    "Ăn tối" to Icons.Outlined.DinnerDining,
    "Cà phê" to Icons.Outlined.LocalCafe,
    "Trà sữa / đồ uống" to Icons.Outlined.EmojiFoodBeverage,
    "Đi chợ / siêu thị" to Icons.Outlined.ShoppingCart,
    "Đặt đồ ăn" to Icons.Outlined.DeliveryDining,
    "Nhà hàng / liên hoan" to Icons.Outlined.Restaurant,
    "Tiền thuê nhà" to Icons.Outlined.Home,
    "Phí chung cư / quản lý" to Icons.Outlined.Apartment,
    "Tiền điện" to Icons.Outlined.Lightbulb,
    "Tiền nước" to Icons.Outlined.WaterDrop,
    "Internet" to Icons.Outlined.Wifi,
    "Điện thoại" to Icons.Outlined.PhoneAndroid,
    "Gas" to Icons.Outlined.LocalFireDepartment,
    "Rác / vệ sinh" to Icons.Outlined.CleaningServices,
    "Giúp việc" to Icons.Outlined.Person,
    "Giặt là" to Icons.Outlined.LocalLaundryService,
    "Sửa chữa nhà" to Icons.Outlined.Handyman,
    "Xăng xe" to Icons.Outlined.LocalGasStation,
    "Sạc xe điện" to Icons.Outlined.EvStation,
    "Gửi xe" to Icons.Outlined.LocalParking,
    "Taxi / xe công nghệ" to Icons.Outlined.LocalTaxi,
    "Xe buýt / tàu" to Icons.Outlined.DirectionsBus,
    "Sửa / bảo dưỡng xe" to Icons.Outlined.Build,
    "Bảo hiểm xe" to Icons.Outlined.Shield,
    "Phí cầu đường" to Icons.Outlined.Toll,
    "Quần áo" to Icons.Outlined.Checkroom,
    "Giày dép" to Icons.Outlined.ShoppingBag,
    "Túi / phụ kiện" to Icons.Outlined.ShoppingBag,
    "Mỹ phẩm" to Icons.Outlined.Face,
    "Đồ gia dụng / nội thất" to Icons.Outlined.Chair,
    "Đồ điện tử" to Icons.Outlined.Devices,
    "Cắt tóc" to Icons.Outlined.ContentCut,
    "Làm đẹp / spa" to Icons.Outlined.Spa,
    "Chăm sóc cá nhân" to Icons.Outlined.Spa,
    "Khám bệnh" to Icons.Outlined.MedicalServices,
    "Thuốc" to Icons.Outlined.Medication,
    "Nha khoa" to Icons.Outlined.Healing,
    "Kính mắt" to Icons.Outlined.Visibility,
    "Thể thao / gym" to Icons.Outlined.FitnessCenter,
    "Xem phim" to Icons.Outlined.Movie,
    "Ứng dụng / đăng ký" to Icons.Outlined.Subscriptions,
    "Game" to Icons.Outlined.SportsEsports,
    "Karaoke / vui chơi" to Icons.Outlined.Mic,
    "Vé sự kiện" to Icons.Outlined.ConfirmationNumber,
    "Sở thích" to Icons.Outlined.Palette,
    "Vé máy bay" to Icons.Outlined.Flight,
    "Khách sạn" to Icons.Outlined.Hotel,
    "Vé tham quan" to Icons.Outlined.Museum,
    "Chi phí du lịch khác" to Icons.Outlined.Luggage,
    "Sữa / bỉm" to Icons.Outlined.ChildCare,
    "Học phí cho con" to Icons.Outlined.School,
    "Đồ dùng cho con" to Icons.Outlined.Toys,
    "Sách vở cho con" to Icons.AutoMirrored.Outlined.MenuBook,
    "Y tế cho con" to Icons.Outlined.LocalHospital,
    "Chi khác cho con" to Icons.Outlined.ChildFriendly,
    "Học phí / khóa học" to Icons.Outlined.School,
    "Sách / báo" to Icons.AutoMirrored.Outlined.MenuBook,
    "Quà tặng" to Icons.Outlined.CardGiftcard,
    "Đám cưới" to Icons.Outlined.Favorite,
    "Ma chay / thăm hỏi" to Icons.Outlined.LocalFlorist,
    "Lễ Tết" to Icons.Outlined.Celebration,
    "Biếu bố mẹ" to Icons.Outlined.FamilyRestroom,
    "Quảng cáo" to Icons.Outlined.Campaign,
    "Phần mềm / dịch vụ" to Icons.Outlined.Extension,
    "Vận chuyển" to Icons.Outlined.LocalShipping,
    "Văn phòng phẩm" to Icons.Outlined.AttachFile,
    "Tiếp khách" to Icons.Outlined.Groups,
    "Công tác" to Icons.Outlined.BusinessCenter,
    "Lương nhân viên" to Icons.Outlined.Badge,
    "Thuê mặt bằng" to Icons.Outlined.Storefront,
    "Phí ngân hàng / phí thẻ" to Icons.Outlined.AccountBalance,
    "Lãi vay / lãi thẻ" to Icons.Outlined.Percent,
    "Thuế" to Icons.AutoMirrored.Outlined.ReceiptLong,
    "Chi khác" to Icons.Outlined.Category,
    "Lương" to Icons.Outlined.Payments,
    "Thưởng" to Icons.Outlined.EmojiEvents,
    "Làm thêm / freelance" to Icons.Outlined.Work,
    "Kinh doanh" to Icons.Outlined.Storefront,
    "Lãi tiết kiệm / đầu tư" to Icons.AutoMirrored.Outlined.TrendingUp,
    "Cho thuê" to Icons.Outlined.Cottage,
    "Được cho / tặng" to Icons.Outlined.Redeem,
    "Hoàn tiền" to Icons.Outlined.CurrencyExchange,
    "Thu nhập khác" to Icons.Outlined.Paid
)

private val byGroup: Map<String, ImageVector> = mapOf(
    "Ăn uống" to Icons.Outlined.Restaurant,
    "Nhà ở" to Icons.Outlined.Home,
    "Đi lại" to Icons.Outlined.DirectionsBus,
    "Mua sắm" to Icons.Outlined.ShoppingBag,
    "Làm đẹp" to Icons.Outlined.Spa,
    "Sức khỏe" to Icons.Outlined.MedicalServices,
    "Giải trí" to Icons.Outlined.Movie,
    "Du lịch" to Icons.Outlined.Luggage,
    "Con cái" to Icons.Outlined.ChildCare,
    "Học tập" to Icons.Outlined.School,
    "Hiếu hỉ – Quà tặng" to Icons.Outlined.CardGiftcard,
    "Công việc" to Icons.Outlined.BusinessCenter,
    "Phí & thuế" to Icons.Outlined.Receipt,
    "Thu nhập" to Icons.Outlined.Payments,
    "Tiền nhận" to Icons.Outlined.Redeem
)

private val byIconName: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Outlined.Restaurant, "dining" to Icons.Outlined.Restaurant,
    "local_cafe" to Icons.Outlined.LocalCafe, "shopping_cart" to Icons.Outlined.ShoppingCart,
    "local_gas_station" to Icons.Outlined.LocalGasStation, "shopping_bag" to Icons.Outlined.ShoppingBag,
    "home" to Icons.Outlined.Home, "movie" to Icons.Outlined.Movie, "medical_services" to Icons.Outlined.MedicalServices,
    "school" to Icons.Outlined.School, "card_giftcard" to Icons.Outlined.CardGiftcard, "payments" to Icons.Outlined.Payments,
    "redeem" to Icons.Outlined.Redeem, "storefront" to Icons.Outlined.Storefront, "trending_up" to Icons.AutoMirrored.Outlined.TrendingUp,
    "price_check" to Icons.Outlined.Paid, "credit_card" to Icons.Outlined.Percent, "account_balance" to Icons.Outlined.AccountBalance,
    "phone_android" to Icons.Outlined.PhoneAndroid, "savings" to Icons.Outlined.Savings
)

/** The icon for a category: its own, else its group's, else a generic one. */
fun categoryIcon(category: Category?): ImageVector {
    if (category == null) return Icons.Outlined.Category
    return byName[category.name]
        ?: category.groupName?.let { byGroup[it] }
        ?: byIconName[category.iconName]
        ?: Icons.Outlined.Category
}
