package com.example.payment.vietqr

data class VietnameseBank(
    val id: String,
    val bin: String,
    val shortName: String,
    val fullName: String,
    val primaryPackage: String,
    val customScheme: String?,
    val brandColorHex: String,
    val secondaryColorHex: String = "#FFFFFF",
    val website: String = "https://napas.com.vn",
    val supportedPaymentMethods: List<String> = listOf("vietqr", "deeplink", "napastrans")
)

object VietnameseBankRegistry {
    val CASH_ENTRY = VietnameseBank(
        id = "cash",
        bin = "000000",
        shortName = "Tiền mặt",
        fullName = "Tiền mặt trong ví",
        primaryPackage = "",
        customScheme = null,
        brandColorHex = "#10B981",
        secondaryColorHex = "#059669",
        website = "",
        supportedPaymentMethods = listOf("cash")
    )

    val BANKS: List<VietnameseBank> = listOf(
        VietnameseBank(
            id = "vcb",
            bin = "970436",
            shortName = "Vietcombank",
            fullName = "Ngân hàng Ngoại Thương Việt Nam",
            primaryPackage = "com.VCB",
            customScheme = "vcbdigibank",
            brandColorHex = "#005C2B",
            secondaryColorHex = "#78BE20",
            website = "https://vietcombank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink", "app2app")
        ),
        VietnameseBank(
            id = "mb",
            bin = "970422",
            shortName = "MB Bank",
            fullName = "Ngân hàng Quân Đội",
            primaryPackage = "com.mbmobile",
            customScheme = "mbmobile",
            brandColorHex = "#003882",
            secondaryColorHex = "#E31837",
            website = "https://mbbank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink", "app2app")
        ),
        VietnameseBank(
            id = "tcb",
            bin = "970407",
            shortName = "Techcombank",
            fullName = "Ngân hàng Kỹ Thương Việt Nam",
            primaryPackage = "vn.com.techcombank.bb.app",
            customScheme = "tcb",
            brandColorHex = "#E31837",
            secondaryColorHex = "#000000",
            website = "https://techcombank.com",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "bidv",
            bin = "970418",
            shortName = "BIDV",
            fullName = "Ngân hàng Đầu tư và Phát triển VN",
            primaryPackage = "com.vnpay.bidv",
            customScheme = "bidvsmartbanking",
            brandColorHex = "#006A4E",
            secondaryColorHex = "#FFCC00",
            website = "https://bidv.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "vpb",
            bin = "970432",
            shortName = "VPBank",
            fullName = "Ngân hàng Việt Nam Thịnh Vượng",
            primaryPackage = "com.vnpay.vpbankonline",
            customScheme = "vpbankneo",
            brandColorHex = "#009639",
            secondaryColorHex = "#E31837",
            website = "https://vpbank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "acb",
            bin = "970416",
            shortName = "ACB",
            fullName = "Ngân hàng Á Châu",
            primaryPackage = "mobile.acb.com.vn",
            customScheme = "acbone",
            brandColorHex = "#004B87",
            secondaryColorHex = "#00A3E0",
            website = "https://acb.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "vtb",
            bin = "970415",
            shortName = "VietinBank",
            fullName = "Ngân hàng Công Thương Việt Nam",
            primaryPackage = "com.vietinbank.ipay",
            customScheme = "vietinbankipay",
            brandColorHex = "#005697",
            secondaryColorHex = "#CC0000",
            website = "https://vietinbank.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "vba",
            bin = "970405",
            shortName = "Agribank",
            fullName = "Ngân hàng Nông nghiệp & PTNT VN",
            primaryPackage = "com.vnpay.agribank",
            customScheme = "agribankemobile",
            brandColorHex = "#800000",
            secondaryColorHex = "#D4AF37",
            website = "https://agribank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "stb",
            bin = "970403",
            shortName = "Sacombank",
            fullName = "Ngân hàng Sài Gòn Thương Tín",
            primaryPackage = "com.vnpay.msacombank",
            customScheme = "sacombankpay",
            brandColorHex = "#004B91",
            secondaryColorHex = "#F58220",
            website = "https://sacombank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "tpb",
            bin = "970423",
            shortName = "TPBank",
            fullName = "Ngân hàng Tiên Phong",
            primaryPackage = "com.tpb.mb.gprsandroid",
            customScheme = "tpbank",
            brandColorHex = "#79237D",
            secondaryColorHex = "#FF8200",
            website = "https://tpb.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "vib",
            bin = "970441",
            shortName = "VIB",
            fullName = "Ngân hàng Quốc Tế Việt Nam",
            primaryPackage = "com.vn.vib.mobileapp",
            customScheme = "myvib",
            brandColorHex = "#004B91",
            secondaryColorHex = "#00B0FF",
            website = "https://vib.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "ocb",
            bin = "970448",
            shortName = "OCB",
            fullName = "Ngân hàng Phương Đông",
            primaryPackage = "com.vnpay.ocb",
            customScheme = "ocbomni",
            brandColorHex = "#008853",
            secondaryColorHex = "#F39200",
            website = "https://ocb.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "shb",
            bin = "970443",
            shortName = "SHB",
            fullName = "Ngân hàng Sài Gòn - Hà Nội",
            primaryPackage = "vn.shb.mbanking",
            customScheme = "shbbank",
            brandColorHex = "#ED6B25",
            secondaryColorHex = "#004B87",
            website = "https://shb.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "hdb",
            bin = "970437",
            shortName = "HDBank",
            fullName = "Ngân hàng Phát triển TP.HCM",
            primaryPackage = "com.vnpay.hdbank",
            customScheme = "hdbank",
            brandColorHex = "#E41B13",
            secondaryColorHex = "#FFD100",
            website = "https://hdbank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "msb",
            bin = "970426",
            shortName = "MSB",
            fullName = "Ngân hàng Hàng Hải Việt Nam",
            primaryPackage = "com.msb.mb",
            customScheme = "msb",
            brandColorHex = "#EB2227",
            secondaryColorHex = "#000000",
            website = "https://msb.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "eib",
            bin = "970431",
            shortName = "Eximbank",
            fullName = "Ngân hàng Xuất Nhập Khẩu Việt Nam",
            primaryPackage = "com.vnpay.eximbank",
            customScheme = "eximbank",
            brandColorHex = "#004A99",
            secondaryColorHex = "#00AEEF",
            website = "https://eximbank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "seap",
            bin = "970440",
            shortName = "SeABank",
            fullName = "Ngân hàng Đông Nam Á",
            primaryPackage = "vn.com.seabank.mb1",
            customScheme = "seabank",
            brandColorHex = "#C8102E",
            secondaryColorHex = "#000000",
            website = "https://seabank.com.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink")
        ),
        VietnameseBank(
            id = "momo",
            bin = "971005",
            shortName = "MoMo",
            fullName = "Ví điện tử MoMo",
            primaryPackage = "com.mservice.momotransfer",
            customScheme = "momo",
            brandColorHex = "#A50064",
            secondaryColorHex = "#FF4081",
            website = "https://momo.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink", "wallet")
        ),
        VietnameseBank(
            id = "zalopay",
            bin = "971001",
            shortName = "ZaloPay",
            fullName = "Ví điện tử ZaloPay",
            primaryPackage = "vn.com.vng.zalopay",
            customScheme = "zalopay",
            brandColorHex = "#0068FF",
            secondaryColorHex = "#00C300",
            website = "https://zalopay.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink", "wallet")
        ),
        VietnameseBank(
            id = "viettelmoney",
            bin = "971011",
            shortName = "Viettel Money",
            fullName = "Hệ sinh thái tài chính số Viettel Money",
            primaryPackage = "com.bplus.vtpay",
            customScheme = "viettelmoney",
            brandColorHex = "#E60000",
            secondaryColorHex = "#000000",
            website = "https://viettelmoney.vn",
            supportedPaymentMethods = listOf("vietqr", "deeplink", "wallet")
        )
    )

    fun findByBin(bin: String): VietnameseBank? {
        val cleanBin = bin.trim()
        return BANKS.firstOrNull { it.bin == cleanBin }
    }

    fun findById(id: String): VietnameseBank? {
        val cleanId = id.trim().lowercase()
        if (cleanId == "cash") return CASH_ENTRY
        return BANKS.firstOrNull { it.id == cleanId }
    }

    fun findByNameOrKeyword(keyword: String): VietnameseBank? {
        val lower = keyword.lowercase().trim()
        if (lower.contains("tiền mặt") || lower.contains("cash")) return CASH_ENTRY
        return BANKS.firstOrNull {
            it.id == lower ||
            it.shortName.lowercase().contains(lower) ||
            it.fullName.lowercase().contains(lower) ||
            it.primaryPackage.lowercase().contains(lower)
        }
    }
}
