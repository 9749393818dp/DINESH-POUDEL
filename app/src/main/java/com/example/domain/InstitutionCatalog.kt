package com.example.domain

import androidx.compose.ui.graphics.Color

data class InstitutionInfo(
  val id: String,
  val code: String,
  val nameNp: String,
  val nameEn: String,
  val group: String, // 'A', 'B', 'C', 'WALLET', 'CASH'
  val color: Color,
)

object InstitutionCatalog {
  val all: List<InstitutionInfo> = listOf(
    // क) 'क' वर्गका वाणिज्य बैंकहरू (२० वटा)
    InstitutionInfo("nbl", "NBL", "नेपाल बैंक लिमिटेड", "Nepal Bank Limited", "A", Color(0xFF0D47A1)),
    InstitutionInfo("rbb", "RBB", "राष्ट्रिय वाणिज्य बैंक", "Rastriya Banijya Bank", "A", Color(0xFF1565C0)),
    InstitutionInfo("adbl", "ADBL", "कृषि विकास बैंक", "Agricultural Development Bank", "A", Color(0xFF2E7D32)),
    InstitutionInfo("nabil", "NABIL", "नबिल बैंक", "Nabil Bank", "A", Color(0xFF00897B)),
    InstitutionInfo("nimb", "NIMB", "नेपाल इन्भेष्टमेन्ट मेगा बैंक", "Nepal Investment Mega Bank", "A", Color(0xFF1A237E)),
    InstitutionInfo("scb", "SCB", "स्ट्यान्डर्ड चार्टर्ड बैंक नेपाल", "Standard Chartered Bank Nepal", "A", Color(0xFF00796B)),
    InstitutionInfo("hbl", "HBL", "हिमालयन बैंक", "Himalayan Bank", "A", Color(0xFF0277BD)),
    InstitutionInfo("nsbi", "NSBI", "नेपाल एसबीआई बैंक", "Nepal SBI Bank", "A", Color(0xFF283593)),
    InstitutionInfo("ebl", "EBL", "एभरेष्ट बैंक", "Everest Bank", "A", Color(0xFFC62828)),
    InstitutionInfo("kbl", "KBL", "कुमारी बैंक", "Kumari Bank", "A", Color(0xFFD32F2F)),
    InstitutionInfo("lsb", "LSB", "लक्ष्मी सनराइज बैंक", "Laxmi Sunrise Bank", "A", Color(0xFFEF6C00)),
    InstitutionInfo("czbil", "CZBIL", "सिटिजन्स बैंक इन्टरनेसनल", "Citizens Bank International", "A", Color(0xFF00838F)),
    InstitutionInfo("pcbl", "PCBL", "प्राइम कमर्सियल बैंक", "Prime Commercial Bank", "A", Color(0xFF1976D2)),
    InstitutionInfo("nmb", "NMB", "एनएमबि बैंक", "NMB Bank", "A", Color(0xFF00695C)),
    InstitutionInfo("prvu", "PRVU", "प्रभु बैंक", "Prabhu Bank", "A", Color(0xFFB71C1C)),
    InstitutionInfo("sbl", "SBL", "सिद्धार्थ बैंक", "Siddhartha Bank", "A", Color(0xFFF9A825)),
    InstitutionInfo("gbime", "GBIME", "ग्लोबल आइएमई बैंक", "Global IME Bank", "A", Color(0xFF0D47A1)),
    InstitutionInfo("nica", "NICA", "एनआईसी एसिया बैंक", "NIC Asia Bank", "A", Color(0xFFD50000)),
    InstitutionInfo("mbl", "MBL", "माछापुच्छ्रे बैंक", "Machhapuchchhre Bank", "A", Color(0xFFC62828)),
    InstitutionInfo("sanima", "SANIMA", "सानिमा बैंक", "Sanima Bank", "A", Color(0xFF2E7D32)),

    // ख) 'ख' वर्गका विकास बैंकहरू (१७ वटा)
    InstitutionInfo("mnbbl", "MNBBL", "मुक्तिनाथ विकास बैंक", "Muktinath Bikas Bank", "B", Color(0xFF1565C0)),
    InstitutionInfo("gbbl", "GBBL", "गरिमा विकास बैंक", "Garima Bikas Bank", "B", Color(0xFF0277BD)),
    InstitutionInfo("ksbbl", "KSBBL", "कामना सेवा विकास बैंक", "Kamana Sewa Bikas Bank", "B", Color(0xFF00838F)),
    InstitutionInfo("mlbl", "MLBL", "महालक्ष्मी विकास बैंक", "Mahalaxmi Bikas Bank", "B", Color(0xFFC62828)),
    InstitutionInfo("jbbl", "JBBL", "ज्योति विकास बैंक", "Jyoti Bikas Bank", "B", Color(0xFFE65100)),
    InstitutionInfo("shine", "SHINE", "साइन रेसुङ्गा डेभलपमेन्ट बैंक", "Shine Resunga Development Bank", "B", Color(0xFF1976D2)),
    InstitutionInfo("lbbl", "LBBL", "लुम्बिनी विकास बैंक", "Lumbini Bikas Bank", "B", Color(0xFF2E7D32)),
    InstitutionInfo("sadbl", "SADBL", "सांग्रिला डेभलपमेन्ट बैंक", "Shangri-la Development Bank", "B", Color(0xFFEF6C00)),
    InstitutionInfo("mdb", "MDB", "मितेरी डेभलपमेन्ट बैंक", "Miteri Development Bank", "B", Color(0xFF283593)),
    InstitutionInfo("edbl", "EDBL", "एक्सेल डेभलपमेन्ट बैंक", "Excel Development Bank", "B", Color(0xFF388E3C)),
    InstitutionInfo("krbl", "KRBL", "कर्णाली डेभलपमेन्ट बैंक", "Karnali Development Bank", "B", Color(0xFF0D47A1)),
    InstitutionInfo("grdbl", "GRDBL", "ग्रीन डेभलपमेन्ट बैंक", "Green Development Bank", "B", Color(0xFF1B5E20)),
    InstitutionInfo("sindu", "SINDU", "सिन्धु विकास बैंक", "Sindhu Bikas Bank", "B", Color(0xFFD32F2F)),
    InstitutionInfo("sapdbl", "SAPDBL", "सप्तकोशी डेभलपमेन्ट बैंक", "Saptakoshi Development Bank", "B", Color(0xFF0288D1)),
    InstitutionInfo("nabbc", "NABBC", "नारायणी डेभलपमेन्ट बैंक", "Narayani Development Bank", "B", Color(0xFFF57C00)),
    InstitutionInfo("corbl", "CORBL", "कर्पोरेट डेभलपमेन्ट बैंक", "Corporate Development Bank", "B", Color(0xFF1565C0)),
    InstitutionInfo("salapa", "SALAPA", "सालपा विकास बैंक", "Salapa Bikas Bank", "B", Color(0xFF2E7D32)),

    // ग) 'ग' वर्गका वित्त कम्पनीहरू (१५ वटा)
    InstitutionInfo("gfcl", "GFCL", "गुडविल फाइनान्स", "Goodwill Finance", "C", Color(0xFF1565C0)),
    InstitutionInfo("icfc", "ICFC", "आईसीएफसी फाइनान्स", "ICFC Finance", "C", Color(0xFFC62828)),
    InstitutionInfo("mfil", "MFIL", "मञ्जुश्री फाइनान्स", "Manjushree Finance", "C", Color(0xFFEF6C00)),
    InstitutionInfo("pfl", "PFL", "पोखरा फाइनान्स", "Pokhara Finance", "C", Color(0xFF0277BD)),
    InstitutionInfo("sifc", "SIFC", "श्री इन्भेष्टमेन्ट एण्ड फाइनान्स", "Shree Investment & Finance", "C", Color(0xFF2E7D32)),
    InstitutionInfo("cfcl", "CFCL", "सेन्ट्रल फाइनान्स", "Central Finance", "C", Color(0xFF1976D2)),
    InstitutionInfo("profl", "PROFL", "प्रोग्रेसिभ फाइनान्स", "Progressive Finance", "C", Color(0xFF388E3C)),
    InstitutionInfo("rlfl", "RLFL", "रिलायन्स फाइनान्स", "Reliance Finance", "C", Color(0xFF0D47A1)),
    InstitutionInfo("bfc", "BFC", "बेष्ट फाइनान्स", "Best Finance", "C", Color(0xFFF9A825)),
    InstitutionInfo("gmfil", "GMFIL", "गुहेश्वरी मर्चेन्ट बैंकिङ एण्ड फाइनान्स", "Guheshwori Merchant Banking & Finance", "C", Color(0xFFD32F2F)),
    InstitutionInfo("sfcl", "SFCL", "समृद्धि फाइनान्स", "Samriddhi Finance", "C", Color(0xFF00796B)),
    InstitutionInfo("jfl", "JFL", "जानकी फाइनान्स", "Janaki Finance", "C", Color(0xFF0288D1)),
    InstitutionInfo("gufl", "GUFL", "गोर्खाज फाइनान्स", "Gorkhas Finance", "C", Color(0xFFB71C1C)),
    InstitutionInfo("mpfl", "MPFL", "मल्टिपर्पस फाइनान्स", "Multipurpose Finance", "C", Color(0xFF283593)),
    InstitutionInfo("nfs", "NFS", "नेपाल फाइनान्स", "Nepal Finance", "C", Color(0xFFC62828)),

    // घ) डिजिटल वालेटहरू (१५ वटा — IME Pay हटाइएको)
    InstitutionInfo("esewa", "eSewa", "इसेवा", "eSewa", "WALLET", Color(0xFF60BB46)),
    InstitutionInfo("khalti", "Khalti", "खल्ती", "Khalti", "WALLET", Color(0xFF5C2D91)),
    InstitutionInfo("prabhupay", "PrabhuPay", "प्रभु पे", "Prabhu Pay", "WALLET", Color(0xFFD32F2F)),
    InstitutionInfo("namastepay", "NamastePay", "नमस्ते पे", "Namaste Pay", "WALLET", Color(0xFF0277BD)),
    InstitutionInfo("cellpay", "CellPay", "सेलपे", "CellPay", "WALLET", Color(0xFFEF6C00)),
    InstitutionInfo("moru", "Moru", "मोरु डिजिटल वालेट", "Moru Digital Wallet", "WALLET", Color(0xFF7B1FA2)),
    InstitutionInfo("mypay", "MyPay", "माय पे", "MyPay", "WALLET", Color(0xFF1976D2)),
    InstitutionInfo("qpay", "QPay", "क्यू पे", "QPay", "WALLET", Color(0xFFC62828)),
    InstitutionInfo("cgpay", "CGPay", "सीजी पे", "CGPay", "WALLET", Color(0xFFD50000)),
    InstitutionInfo("gmepay", "GME Pay", "जीएमई पे", "GME Pay", "WALLET", Color(0xFF1565C0)),
    InstitutionInfo("sajilopay", "SajiloPay", "सजिलो पे", "Sajilo Pay", "WALLET", Color(0xFF2E7D32)),
    InstitutionInfo("digipay", "DigiPay", "डिजी पे", "DigiPay", "WALLET", Color(0xFF00838F)),
    InstitutionInfo("paywell", "Paywell", "पेवेल नेपाल", "Paywell Nepal", "WALLET", Color(0xFFF57C00)),
    InstitutionInfo("ncash", "nCash", "एनक्यास", "nCash", "WALLET", Color(0xFF388E3C)),
    InstitutionInfo("hamropay", "HamroPay", "हाम्रो पे", "Hamro Pay", "WALLET", Color(0xFFB71C1C)),

    // ङ) नगद (Cash)
    InstitutionInfo("cash", "CASH", "नगद (खल्ती / घर)", "Cash in Hand", "CASH", Color(0xFF1B5E20)),
  )

  fun byId(id: String): InstitutionInfo = all.firstOrNull { it.id == id } ?: all.last()
}
