import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MeruHisabApp());
}

// ============================================================================
// १. विक्रम संवत् (Bikram Sambat) पात्रो कन्भर्टर (२०८० - २०९० वि.सं.)
// ============================================================================
class BsCalendar {
  static const Map<int, List<int>> bsMonthDays = {
    2080: [31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30],
    2081: [31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31],
    2082: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
    2083: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
    2084: [31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31],
    2085: [31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31],
    2086: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
    2087: [31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30],
    2088: [31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31],
    2089: [31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31],
    2090: [31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30],
  };

  static const List<String> nepaliMonths = [
    'बैशाख',
    'जेठ',
    'असार',
    'श्रावण',
    'भदौ',
    'असोज',
    'कार्तिक',
    'मंसिर',
    'पुस',
    'माघ',
    'फाल्गुन',
    'चैत'
  ];

  static const List<String> nepaliDays = [
    'आइतबार',
    'सोमबार',
    'मंगलबार',
    'बुधबार',
    'बिहीबार',
    'शुक्रबार',
    'शनिबार'
  ];

  static String toNepaliDigits(String input) {
    const en = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
    const ne = ['०', '१', '२', '३', '४', '५', '६', '७', '८', '९'];
    String out = input;
    for (int i = 0; i < 10; i++) {
      out = out.replaceAll(en[i], ne[i]);
    }
    return out;
  }

  static Map<String, dynamic> adToBs(DateTime ad) {
    final baseAd = DateTime(2023, 4, 14);
    final cleanAd = DateTime(ad.year, ad.month, ad.day);
    int diffDays = cleanAd.difference(baseAd).inDays;

    int bsYear = 2080;
    int bsMonth = 1;
    int bsDay = 1;

    if (diffDays >= 0) {
      while (diffDays > 0 && bsMonthDays.containsKey(bsYear)) {
        final daysInMonth = bsMonthDays[bsYear]![bsMonth - 1];
        if (diffDays >= daysInMonth) {
          diffDays -= daysInMonth;
          bsMonth++;
          if (bsMonth > 12) {
            bsMonth = 1;
            bsYear++;
          }
        } else {
          bsDay += diffDays;
          diffDays = 0;
        }
      }
    }

    final mStr = bsMonth.toString().padLeft(2, '0');
    final dStr = bsDay.toString().padLeft(2, '0');
    final weekdayName = nepaliDays[ad.weekday % 7];
    final formattedNep =
        '${toNepaliDigits('$bsYear')} ${nepaliMonths[bsMonth - 1]} ${toNepaliDigits('$bsDay')}, $weekdayName';
    final isoBs = '$bsYear-$mStr-$dStr';

    return {
      'year': bsYear,
      'month': bsMonth,
      'day': bsDay,
      'monthName': nepaliMonths[bsMonth - 1],
      'weekday': weekdayName,
      'formatted': formattedNep,
      'iso': isoBs,
    };
  }

  static int daysBetweenBs(String startBsIso, String endBsIso) {
    try {
      final s = startBsIso.split('-').map(int.parse).toList();
      final e = endBsIso.split('-').map(int.parse).toList();
      int y = s[0], m = s[1], d = s[2];
      int count = 0;
      while ((y < e[0] ||
              (y == e[0] && m < e[1]) ||
              (y == e[0] && m == e[1] && d < e[2])) &&
          count < 7300) {
        count++;
        d++;
        final maxD = (bsMonthDays[y] ??
            [30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30])[m - 1];
        if (d > maxD) {
          d = 1;
          m++;
          if (m > 12) {
            m = 1;
            y++;
          }
        }
      }
      return math.max(1, count);
    } catch (_) {
      return 365;
    }
  }
}

// ============================================================================
// २. नेपालका ६८ वित्तीय संस्था, डिजिटल वालेट र नगद (IME Pay हटाइएको)
// ============================================================================
class InstitutionCatalog {
  final String id;
  final String code;
  final String nameNp;
  final String nameEn;
  final String group; // 'A', 'B', 'C', 'WALLET', 'CASH'
  final Color color;

  const InstitutionCatalog({
    required this.id,
    required this.code,
    required this.nameNp,
    required this.nameEn,
    required this.group,
    required this.color,
  });

  static const List<InstitutionCatalog> all = [
    // क) 'क' वर्गका वाणिज्य बैंकहरू (२० वटा)
    InstitutionCatalog(id: 'nbl', code: 'NBL', nameNp: 'नेपाल बैंक लिमिटेड', nameEn: 'Nepal Bank Limited', group: 'A', color: Color(0xFF0D47A1)),
    InstitutionCatalog(id: 'rbb', code: 'RBB', nameNp: 'राष्ट्रिय वाणिज्य बैंक', nameEn: 'Rastriya Banijya Bank', group: 'A', color: Color(0xFF1565C0)),
    InstitutionCatalog(id: 'adbl', code: 'ADBL', nameNp: 'कृषि विकास बैंक', nameEn: 'Agricultural Development Bank', group: 'A', color: Color(0xFF2E7D32)),
    InstitutionCatalog(id: 'nabil', code: 'NABIL', nameNp: 'नबिल बैंक', nameEn: 'Nabil Bank', group: 'A', color: Color(0xFF00897B)),
    InstitutionCatalog(id: 'nimb', code: 'NIMB', nameNp: 'नेपाल इन्भेष्टमेन्ट मेगा बैंक', nameEn: 'Nepal Investment Mega Bank', group: 'A', color: Color(0xFF1A237E)),
    InstitutionCatalog(id: 'scb', code: 'SCB', nameNp: 'स्ट्यान्डर्ड चार्टर्ड बैंक नेपाल', nameEn: 'Standard Chartered Bank Nepal', group: 'A', color: Color(0xFF00796B)),
    InstitutionCatalog(id: 'hbl', code: 'HBL', nameNp: 'हिमालयन बैंक', nameEn: 'Himalayan Bank', group: 'A', color: Color(0xFF0277BD)),
    InstitutionCatalog(id: 'nsbi', code: 'NSBI', nameNp: 'नेपाल एसबीआई बैंक', nameEn: 'Nepal SBI Bank', group: 'A', color: Color(0xFF283593)),
    InstitutionCatalog(id: 'ebl', code: 'EBL', nameNp: 'एभरेष्ट बैंक', nameEn: 'Everest Bank', group: 'A', color: Color(0xFFC62828)),
    InstitutionCatalog(id: 'kbl', code: 'KBL', nameNp: 'कुमारी बैंक', nameEn: 'Kumari Bank', group: 'A', color: Color(0xFFD32F2F)),
    InstitutionCatalog(id: 'lsb', code: 'LSB', nameNp: 'लक्ष्मी सनराइज बैंक', nameEn: 'Laxmi Sunrise Bank', group: 'A', color: Color(0xFFEF6C00)),
    InstitutionCatalog(id: 'czbil', code: 'CZBIL', nameNp: 'सिटिजन्स बैंक इन्टरनेसनल', nameEn: 'Citizens Bank International', group: 'A', color: Color(0xFF00838F)),
    InstitutionCatalog(id: 'pcbl', code: 'PCBL', nameNp: 'प्राइम कमर्सियल बैंक', nameEn: 'Prime Commercial Bank', group: 'A', color: Color(0xFF1976D2)),
    InstitutionCatalog(id: 'nmb', code: 'NMB', nameNp: 'एनएमबि बैंक', nameEn: 'NMB Bank', group: 'A', color: Color(0xFF00695C)),
    InstitutionCatalog(id: 'prvu', code: 'PRVU', nameNp: 'प्रभु बैंक', nameEn: 'Prabhu Bank', group: 'A', color: Color(0xFFB71C1C)),
    InstitutionCatalog(id: 'sbl', code: 'SBL', nameNp: 'सिद्धार्थ बैंक', nameEn: 'Siddhartha Bank', group: 'A', color: Color(0xFFF9A825)),
    InstitutionCatalog(id: 'gbime', code: 'GBIME', nameNp: 'ग्लोबल आइएमई बैंक', nameEn: 'Global IME Bank', group: 'A', color: Color(0xFF0D47A1)),
    InstitutionCatalog(id: 'nica', code: 'NICA', nameNp: 'एनआईसी एसिया बैंक', nameEn: 'NIC Asia Bank', group: 'A', color: Color(0xFFD50000)),
    InstitutionCatalog(id: 'mbl', code: 'MBL', nameNp: 'माछापुच्छ्रे बैंक', nameEn: 'Machhapuchchhre Bank', group: 'A', color: Color(0xFFC62828)),
    InstitutionCatalog(id: 'sanima', code: 'SANIMA', nameNp: 'सानिमा बैंक', nameEn: 'Sanima Bank', group: 'A', color: Color(0xFF2E7D32)),

    // ख) 'ख' वर्गका विकास बैंकहरू (१७ वटा)
    InstitutionCatalog(id: 'mnbbl', code: 'MNBBL', nameNp: 'मुक्तिनाथ विकास बैंक', nameEn: 'Muktinath Bikas Bank', group: 'B', color: Color(0xFF1565C0)),
    InstitutionCatalog(id: 'gbbl', code: 'GBBL', nameNp: 'गरिमा विकास बैंक', nameEn: 'Garima Bikas Bank', group: 'B', color: Color(0xFF0277BD)),
    InstitutionCatalog(id: 'ksbbl', code: 'KSBBL', nameNp: 'कामना सेवा विकास बैंक', nameEn: 'Kamana Sewa Bikas Bank', group: 'B', color: Color(0xFF00838F)),
    InstitutionCatalog(id: 'mlbl', code: 'MLBL', nameNp: 'महालक्ष्मी विकास बैंक', nameEn: 'Mahalaxmi Bikas Bank', group: 'B', color: Color(0xFFC62828)),
    InstitutionCatalog(id: 'jbbl', code: 'JBBL', nameNp: 'ज्योति विकास बैंक', nameEn: 'Jyoti Bikas Bank', group: 'B', color: Color(0xFFE65100)),
    InstitutionCatalog(id: 'shine', code: 'SHINE', nameNp: 'साइन रेसुङ्गा डेभलपमेन्ट बैंक', nameEn: 'Shine Resunga Development Bank', group: 'B', color: Color(0xFF1976D2)),
    InstitutionCatalog(id: 'lbbl', code: 'LBBL', nameNp: 'लुम्बिनी विकास बैंक', nameEn: 'Lumbini Bikas Bank', group: 'B', color: Color(0xFF2E7D32)),
    InstitutionCatalog(id: 'sadbl', code: 'SADBL', nameNp: 'सांग्रिला डेभलपमेन्ट बैंक', nameEn: 'Shangri-la Development Bank', group: 'B', color: Color(0xFFEF6C00)),
    InstitutionCatalog(id: 'mdb', code: 'MDB', nameNp: 'मितेरी डेभलपमेन्ट बैंक', nameEn: 'Miteri Development Bank', group: 'B', color: Color(0xFF283593)),
    InstitutionCatalog(id: 'edbl', code: 'EDBL', nameNp: 'एक्सेल डेभलपमेन्ट बैंक', nameEn: 'Excel Development Bank', group: 'B', color: Color(0xFF388E3C)),
    InstitutionCatalog(id: 'krbl', code: 'KRBL', nameNp: 'कर्णाली डेभलपमेन्ट बैंक', nameEn: 'Karnali Development Bank', group: 'B', color: Color(0xFF0D47A1)),
    InstitutionCatalog(id: 'grdbl', code: 'GRDBL', nameNp: 'ग्रीन डेभलपमेन्ट बैंक', nameEn: 'Green Development Bank', group: 'B', color: Color(0xFF1B5E20)),
    InstitutionCatalog(id: 'sindu', code: 'SINDU', nameNp: 'सिन्धु विकास बैंक', nameEn: 'Sindhu Bikas Bank', group: 'B', color: Color(0xFFD32F2F)),
    InstitutionCatalog(id: 'sapdbl', code: 'SAPDBL', nameNp: 'सप्तकोशी डेभलपमेन्ट बैंक', nameEn: 'Saptakoshi Development Bank', group: 'B', color: Color(0xFF0288D1)),
    InstitutionCatalog(id: 'nabbc', code: 'NABBC', nameNp: 'नारायणी डेभलपमेन्ट बैंक', nameEn: 'Narayani Development Bank', group: 'B', color: Color(0xFFF57C00)),
    InstitutionCatalog(id: 'corbl', code: 'CORBL', nameNp: 'कर्पोरेट डेभलपमेन्ट बैंक', nameEn: 'Corporate Development Bank', group: 'B', color: Color(0xFF1565C0)),
    InstitutionCatalog(id: 'salapa', code: 'SALAPA', nameNp: 'सालपा विकास बैंक', nameEn: 'Salapa Bikas Bank', group: 'B', color: Color(0xFF2E7D32)),

    // ग) 'ग' वर्गका वित्त कम्पनीहरू (१५ वटा)
    InstitutionCatalog(id: 'gfcl', code: 'GFCL', nameNp: 'गुडविल फाइनान्स', nameEn: 'Goodwill Finance', group: 'C', color: Color(0xFF1565C0)),
    InstitutionCatalog(id: 'icfc', code: 'ICFC', nameNp: 'आईसीएफसी फाइनान्स', nameEn: 'ICFC Finance', group: 'C', color: Color(0xFFC62828)),
    InstitutionCatalog(id: 'mfil', code: 'MFIL', nameNp: 'मञ्जुश्री फाइनान्स', nameEn: 'Manjushree Finance', group: 'C', color: Color(0xFFEF6C00)),
    InstitutionCatalog(id: 'pfl', code: 'PFL', nameNp: 'पोखरा फाइनान्स', nameEn: 'Pokhara Finance', group: 'C', color: Color(0xFF0277BD)),
    InstitutionCatalog(id: 'sifc', code: 'SIFC', nameNp: 'श्री इन्भेष्टमेन्ट एण्ड फाइनान्स', nameEn: 'Shree Investment & Finance', group: 'C', color: Color(0xFF2E7D32)),
    InstitutionCatalog(id: 'cfcl', code: 'CFCL', nameNp: 'सेन्ट्रल फाइनान्स', nameEn: 'Central Finance', group: 'C', color: Color(0xFF1976D2)),
    InstitutionCatalog(id: 'profl', code: 'PROFL', nameNp: 'प्रोग्रेसिभ फाइनान्स', nameEn: 'Progressive Finance', group: 'C', color: Color(0xFF388E3C)),
    InstitutionCatalog(id: 'rlfl', code: 'RLFL', nameNp: 'रिलायन्स फाइनान्स', nameEn: 'Reliance Finance', group: 'C', color: Color(0xFF0D47A1)),
    InstitutionCatalog(id: 'bfc', code: 'BFC', nameNp: 'बेष्ट फाइनान्स', nameEn: 'Best Finance', group: 'C', color: Color(0xFFF9A825)),
    InstitutionCatalog(id: 'gmfil', code: 'GMFIL', nameNp: 'गुहेश्वरी मर्चेन्ट बैंकिङ एण्ड फाइनान्स', nameEn: 'Guheshwori Merchant Banking & Finance', group: 'C', color: Color(0xFFD32F2F)),
    InstitutionCatalog(id: 'sfcl', code: 'SFCL', nameNp: 'समृद्धि फाइनान्स', nameEn: 'Samriddhi Finance', group: 'C', color: Color(0xFF00796B)),
    InstitutionCatalog(id: 'jfl', code: 'JFL', nameNp: 'जानकी फाइनान्स', nameEn: 'Janaki Finance', group: 'C', color: Color(0xFF0288D1)),
    InstitutionCatalog(id: 'gufl', code: 'GUFL', nameNp: 'गोर्खाज फाइनान्स', nameEn: 'Gorkhas Finance', group: 'C', color: Color(0xFFB71C1C)),
    InstitutionCatalog(id: 'mpfl', code: 'MPFL', nameNp: 'मल्टिपर्पस फाइनान्स', nameEn: 'Multipurpose Finance', group: 'C', color: Color(0xFF283593)),
    InstitutionCatalog(id: 'nfs', code: 'NFS', nameNp: 'नेपाल फाइनान्स', nameEn: 'Nepal Finance', group: 'C', color: Color(0xFFC62828)),

    // घ) डिजिटल वालेटहरू (१५ वटा — IME Pay हटाइएको)
    InstitutionCatalog(id: 'esewa', code: 'eSewa', nameNp: 'इसेवा', nameEn: 'eSewa', group: 'WALLET', color: Color(0xFF60BB46)),
    InstitutionCatalog(id: 'khalti', code: 'Khalti', nameNp: 'खल्ती', nameEn: 'Khalti', group: 'WALLET', color: Color(0xFF5C2D91)),
    InstitutionCatalog(id: 'prabhupay', code: 'PrabhuPay', nameNp: 'प्रभु पे', nameEn: 'Prabhu Pay', group: 'WALLET', color: Color(0xFFD32F2F)),
    InstitutionCatalog(id: 'namastepay', code: 'NamastePay', nameNp: 'नमस्ते पे', nameEn: 'Namaste Pay', group: 'WALLET', color: Color(0xFF0277BD)),
    InstitutionCatalog(id: 'cellpay', code: 'CellPay', nameNp: 'सेलपे', nameEn: 'CellPay', group: 'WALLET', color: Color(0xFFEF6C00)),
    InstitutionCatalog(id: 'moru', code: 'Moru', nameNp: 'मोरु डिजिटल वालेट', nameEn: 'Moru Digital Wallet', group: 'WALLET', color: Color(0xFF7B1FA2)),
    InstitutionCatalog(id: 'mypay', code: 'MyPay', nameNp: 'माय पे', nameEn: 'MyPay', group: 'WALLET', color: Color(0xFF1976D2)),
    InstitutionCatalog(id: 'qpay', code: 'QPay', nameNp: 'क्यू पे', nameEn: 'QPay', group: 'WALLET', color: Color(0xFFC62828)),
    InstitutionCatalog(id: 'cgpay', code: 'CGPay', nameNp: 'सीजी पे', nameEn: 'CGPay', group: 'WALLET', color: Color(0xFFD50000)),
    InstitutionCatalog(id: 'gmepay', code: 'GME Pay', nameNp: 'जीएमई पे', nameEn: 'GME Pay', group: 'WALLET', color: Color(0xFF1565C0)),
    InstitutionCatalog(id: 'sajilopay', code: 'SajiloPay', nameNp: 'सजिलो पे', nameEn: 'Sajilo Pay', group: 'WALLET', color: Color(0xFF2E7D32)),
    InstitutionCatalog(id: 'digipay', code: 'DigiPay', nameNp: 'डिजी पे', nameEn: 'DigiPay', group: 'WALLET', color: Color(0xFF00838F)),
    InstitutionCatalog(id: 'paywell', code: 'Paywell', nameNp: 'पेवेल नेपाल', nameEn: 'Paywell Nepal', group: 'WALLET', color: Color(0xFFF57C00)),
    InstitutionCatalog(id: 'ncash', code: 'nCash', nameNp: 'एनक्यास', nameEn: 'nCash', group: 'WALLET', color: Color(0xFF388E3C)),
    InstitutionCatalog(id: 'hamropay', code: 'HamroPay', nameNp: 'हाम्रो पे', nameEn: 'Hamro Pay', group: 'WALLET', color: Color(0xFFB71C1C)),

    // ङ) नगद (Cash)
    InstitutionCatalog(id: 'cash', code: 'CASH', nameNp: 'नगद (खल्ती / घर)', nameEn: 'Cash in Hand', group: 'CASH', color: Color(0xFF1B5E20)),
  ];

  static InstitutionCatalog byId(String id) {
    return all.firstWhere((e) => e.id == id, orElse: () => all.last);
  }
}

// ============================================================================
// ३. सम्पूर्ण खर्च (३०) र आम्दानी (२५) क्याटेगोरीहरू
// ============================================================================
class CategoryData {
  static const Map<String, List<Map<String, String>>> expenseGroups = {
    '१. भान्सा र खानपिन': [
      {'icon': '🌾', 'name': 'चामल (Rice)'},
      {'icon': '🍲', 'name': 'दाल (Dal / Lentils)'},
      {'icon': '🥬', 'name': 'तरकारी (Vegetables)'},
      {'icon': '🛒', 'name': 'किराना सामान (Groceries)'},
      {'icon': '🥛', 'name': 'दूध / दही (Milk & Curd)'},
      {'icon': '🍗', 'name': 'माछामासु (Meat & Fish)'},
      {'icon': '🔥', 'name': 'ग्यास (Gas)'},
      {'icon': '💧', 'name': 'पिउने पानी (Water)'},
      {'icon': '☕', 'name': 'खाजा / नास्ता (Snacks)'},
    ],
    '२. कोठा र मासिक बिल': [
      {'icon': '🏠', 'name': 'कोठा भाडा (Room Rent)'},
      {'icon': '💡', 'name': 'बिजुली बिल (Electricity Bill)'},
      {'icon': '📶', 'name': 'वाइफाइ / नेट (WiFi / Internet)'},
      {'icon': '📱', 'name': 'मोबाइल रिचार्ज (Mobile Recharge)'},
      {'icon': '🧼', 'name': 'साबुन / सरसफाइ (Soap & Cleaning)'},
      {'icon': '🛋️', 'name': 'घरायसी सामान (Household Items)'},
    ],
    '३. गाडी, पेट्रोल र मर्मत': [
      {'icon': '⛽', 'name': 'पेट्रोल (Petrol)'},
      {'icon': '🚌', 'name': 'गाडी भाडा (Bus / Auto / Pathao)'},
      {'icon': '🔧', 'name': 'मर्मत / सर्भिसिङ (Repair & Servicing)'},
    ],
    '४. उपचार, पढाइ र सपिङ': [
      {'icon': '💊', 'name': 'औषधि उपचार (Medical)'},
      {'icon': '📚', 'name': 'पढाइ / शिक्षा (Education)'},
      {'icon': '🛍️', 'name': 'सपिङ / लुगाकपडा (Shopping & Clothes)'},
      {'icon': '💇', 'name': 'सैलुन / पार्लर (Salon & Parlour)'},
    ],
    '५. चाडपर्व, बचत र अन्य': [
      {'icon': '🪔', 'name': 'चाडपर्व / पूजा (Festivals & Puja)'},
      {'icon': '🎁', 'name': 'उपहार / पाहुना (Gifts & Guests)'},
      {'icon': '🎡', 'name': 'घुमफिर (Outing)'},
      {'icon': '💰', 'name': 'बचत / बीमा (Savings & Insurance)'},
      {'icon': '🏦', 'name': 'ऋण / किस्ता (Loan / EMI)'},
      {'icon': '🤝', 'name': 'सापटी / उधारो (Lending / Udharo)'},
      {'icon': '🏛️', 'name': 'कर / नवीकरण (Tax & Renewal)'},
      {'icon': '📦', 'name': 'अन्य खर्च (Other Expenses)'},
    ],
  };

  static const Map<String, List<Map<String, String>>> incomeGroups = {
    '💼 तलब र ज्याला': [
      {'icon': '💵', 'name': 'मासिक तलब (Monthly Salary)'},
      {'icon': '🔨', 'name': 'दैनिक ज्याला (Daily Wage)'},
      {'icon': '⏱️', 'name': 'ओभरटाइम र भत्ता (Overtime & Allowance)'},
    ],
    '🏪 व्यापार र व्यवसाय': [
      {'icon': '🛒', 'name': 'पसल / रिटेलिङबाट (Shop / Retail)'},
      {'icon': '🍽️', 'name': 'होटल / खाजा घरबाट (Hotel / Cafe)'},
      {'icon': '📦', 'name': 'अनलाइन व्यापारबाट (Online Store)'},
    ],
    '🚕 सवारी र ढुवानी': [
      {'icon': '🚌', 'name': 'गाडी / अटो चलाएर (Driving)'},
      {'icon': '🏍️', 'name': 'पठाओ / इनड्राइभबाट (Ride-Sharing)'},
      {'icon': '🚜', 'name': 'गाडी भाडाबाट (Vehicle Rent)'},
    ],
    '🏠 घर / सटर बहाल': [
      {'icon': '🛋️', 'name': 'कोठा / घर बहालबाट (Room / House Rent)'},
      {'icon': '🏬', 'name': 'सटर / गोदाम बहालबाट (Shutter / Godown Rent)'},
      {'icon': '🏞️', 'name': 'जग्गा भाडाबाट (Land Lease)'},
    ],
    '🔧 सीप र सेवा': [
      {'icon': '🛠️', 'name': 'मर्मत / वर्कसपबाट (Workshop / Repair)'},
      {'icon': '✂️', 'name': 'सिलाइ / टेलरिङबाट (Tailoring)'},
      {'icon': '💇', 'name': 'सैलुन / पार्लरबाट (Salon / Parlour)'},
    ],
    '🌾 कृषि र पशुपालन': [
      {'icon': '🥛', 'name': 'डेरी / दूध बेचेर (Dairy / Milk)'},
      {'icon': '🥬', 'name': 'तरकारी / फलफूल बेचेर (Vegetables & Fruits)'},
      {'icon': '🐔', 'name': 'कुखुरा / माछा बेचेर (Poultry & Fishery)'},
    ],
    '💻 अनलाइन र डिजिटल': [
      {'icon': '▶️', 'name': 'युट्युब / फेसबुकबाट (YouTube / Social Media)'},
      {'icon': '🌐', 'name': 'अनलाइन कामबाट (Freelancing)'},
    ],
    '📚 अध्यापन र ट्युसन': [
      {'icon': '✏️', 'name': 'ट्युसन पढाएर (Tuition)'},
      {'icon': '🎓', 'name': 'कोचिङ / तालिमबाट (Coaching / Training)'},
    ],
    '✈️ रेमिट्यान्स': [
      {'icon': '🌍', 'name': 'विदेशबाट (Foreign Remittance)'},
    ],
    '🎖️ पेन्सन र भत्ता': [
      {'icon': '👴', 'name': 'मासिक पेन्सन (Monthly Pension)'},
      {'icon': '🤝', 'name': 'वृद्ध / सामाजिक भत्ता (Social Allowance)'},
    ],
  };
}

// ============================================================================
// ४. बहु-मुद्रा (Multi-Currency) र बहु-भाषा (Multi-Language) प्रणाली
// ============================================================================
class CurrencyService {
  static const Map<String, Map<String, dynamic>> currencies = {
    'NPR': {'symbol': 'रु.', 'name': 'नेपाली रुपैयाँ (NPR)', 'rateFromNpr': 1.0},
    'USD': {'symbol': '\$', 'name': 'US Dollar (USD)', 'rateFromNpr': 0.00735},
    'QAR': {'symbol': 'QAR', 'name': 'कतारी रियाल (QAR)', 'rateFromNpr': 0.0267},
    'AED': {'symbol': 'AED', 'name': 'UAE Dirham (AED)', 'rateFromNpr': 0.0270},
    'SAR': {'symbol': 'SAR', 'name': 'साउदी रियाल (SAR)', 'rateFromNpr': 0.0276},
    'INR': {'symbol': '₹', 'name': 'भारतीय रुपैयाँ (INR)', 'rateFromNpr': 0.625},
    'EUR': {'symbol': '€', 'name': 'Euro (EUR)', 'rateFromNpr': 0.0068},
    'GBP': {'symbol': '£', 'name': 'British Pound (GBP)', 'rateFromNpr': 0.0058},
    'AUD': {'symbol': 'A\$', 'name': 'Australian Dollar (AUD)', 'rateFromNpr': 0.0112},
    'MYR': {'symbol': 'RM', 'name': 'Malaysian Ringgit (MYR)', 'rateFromNpr': 0.0325},
    'KRW': {'symbol': '₩', 'name': 'South Korean Won (KRW)', 'rateFromNpr': 10.15},
    'JPY': {'symbol': '¥', 'name': 'Japanese Yen (JPY)', 'rateFromNpr': 1.12},
  };

  static double toNpr(double amount, String fromCurrency) {
    final rate = (currencies[fromCurrency]?['rateFromNpr'] as double?) ?? 1.0;
    return rate == 0 ? amount : amount / rate;
  }

  static double fromNpr(double nprAmount, String toCurrency) {
    final rate = (currencies[toCurrency]?['rateFromNpr'] as double?) ?? 1.0;
    return nprAmount * rate;
  }

  static String format(double nprAmount, String displayCurrency,
      {bool hidden = false}) {
    if (hidden) return '••••••';
    final info = currencies[displayCurrency] ?? currencies['NPR']!;
    final converted = fromNpr(nprAmount, displayCurrency);
    final fmt = NumberFormat('#,##0.00');
    return '${info['symbol']} ${fmt.format(converted)}';
  }
}

class AppLocalizations {
  static const Map<String, Map<String, String>> strings = {
    'ne': {
      'app_title': 'meru hisab',
      'dev_credit': 'App Develop by: Dinesh Poudel Poudel',
      'total_balance': 'कुल मौज्दात (Total Balance)',
      'month_income': 'यो महिनाको आम्दानी',
      'month_expense': 'यो महिनाको खर्च',
      'my_accounts': 'मेरा बैंक तथा वालेटहरू',
      'add_account': '+ बैंक / वालेट थप्नुहोस्',
      'no_accounts':
          'कुनै खाता थपिएको छैन। तल थिचेर आफ्नो बैंक, वालेट वा नगद छान्नुहोस्।',
      'quick_actions': 'छिटो कारोबार (दायाँ सार्नुहोस् →)',
      'recent_tx': 'हालका कारोबार र Statement',
      'tab_home': 'ड्यासबोर्ड',
      'tab_statement': 'Statement',
      'tab_goals': 'लक्ष्य बचत',
      'tab_khata': 'सापटी/उधारो',
      'tab_settings': 'सेटिङ',
    },
    'en': {
      'app_title': 'meru hisab',
      'dev_credit': 'App Develop by: Dinesh Poudel Poudel',
      'total_balance': 'Total Net Balance',
      'month_income': 'This Month Income',
      'month_expense': 'This Month Expense',
      'my_accounts': 'My Banks & Wallets',
      'add_account': '+ Add Bank / Wallet / Cash',
      'no_accounts':
          'No accounts added yet. Tap below to choose your bank, wallet, or cash.',
      'quick_actions': 'Quick Actions (Swipe Right →)',
      'recent_tx': 'Recent Transactions & Statement',
      'tab_home': 'Dashboard',
      'tab_statement': 'Statement',
      'tab_goals': 'Savings Goal',
      'tab_khata': 'Lend/Borrow',
      'tab_settings': 'Settings',
    },
    'hi': {
      'app_title': 'meru hisab',
      'dev_credit': 'App Develop by: Dinesh Poudel Poudel',
      'total_balance': 'कुल शेष राशि',
      'month_income': 'इस महीने की आय',
      'month_expense': 'इस महीने का खर्च',
      'my_accounts': 'मेरे बैंक और वॉलेट',
      'add_account': '+ बैंक / वॉलेट जोड़ें',
      'no_accounts': 'अभी कोई खाता नहीं जोड़ा गया है।',
      'quick_actions': 'त्वरित लेनदेन (दाएं स्वाइप करें →)',
      'recent_tx': 'हालिया स्टेटमेंट',
      'tab_home': 'डैशबोर्ड',
      'tab_statement': 'स्टेटमेंट',
      'tab_goals': 'बचत लक्ष्य',
      'tab_khata': 'उधार खाता',
      'tab_settings': 'सेटिंग्स',
    },
    'ar': {
      'app_title': 'meru hisab',
      'dev_credit': 'App Develop by: Dinesh Poudel Poudel',
      'total_balance': 'إجمالي الرصيد',
      'month_income': 'دخل هذا الشهر',
      'month_expense': 'مصروفات هذا الشهر',
      'my_accounts': 'حساباتي البنكية والمحافظ',
      'add_account': '+ إضافة بنك / محفظة',
      'no_accounts': 'لم تتم إضافة أي حساب بعد.',
      'quick_actions': 'إجراءات سريعة (اسحب لليمين →)',
      'recent_tx': 'كشف الحساب الأخير',
      'tab_home': 'الرئيسية',
      'tab_statement': 'كشف حساب',
      'tab_goals': 'أهداف الادخار',
      'tab_khata': 'الديون',
      'tab_settings': 'الإعدادات',
    },
  };

  static String t(String lang, String key) {
    return strings[lang]?[key] ?? strings['ne']![key] ?? key;
  }
}

// ============================================================================
// ५. डाटा मोडेलहरू (Models)
// ============================================================================
class UserAccount {
  final String id;
  final String catalogId;
  final String customLabel;
  double balanceNpr;

  UserAccount({
    required this.id,
    required this.catalogId,
    required this.customLabel,
    required this.balanceNpr,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'catalogId': catalogId,
        'customLabel': customLabel,
        'balanceNpr': balanceNpr,
      };

  factory UserAccount.fromJson(Map<String, dynamic> json) => UserAccount(
        id: json['id'],
        catalogId: json['catalogId'],
        customLabel: json['customLabel'],
        balanceNpr: (json['balanceNpr'] as num).toDouble(),
      );
}

class LedgerTransaction {
  final String id;
  final String refNo;
  final String type;
  final String title;
  final String category;
  final double amountNpr;
  final double originalAmount;
  final String originalCurrency;
  final String accountId;
  final String? toAccountId;
  final double prevBalanceNpr;
  final double newBalanceNpr;
  final String adTimestamp;
  final String bsDate;
  final int bsYear;
  final int bsMonth;
  final String note;

  LedgerTransaction({
    required this.id,
    required this.refNo,
    required this.type,
    required this.title,
    required this.category,
    required this.amountNpr,
    required this.originalAmount,
    required this.originalCurrency,
    required this.accountId,
    this.toAccountId,
    required this.prevBalanceNpr,
    required this.newBalanceNpr,
    required this.adTimestamp,
    required this.bsDate,
    required this.bsYear,
    required this.bsMonth,
    required this.note,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'refNo': refNo,
        'type': type,
        'title': title,
        'category': category,
        'amountNpr': amountNpr,
        'originalAmount': originalAmount,
        'originalCurrency': originalCurrency,
        'accountId': accountId,
        'toAccountId': toAccountId,
        'prevBalanceNpr': prevBalanceNpr,
        'newBalanceNpr': newBalanceNpr,
        'adTimestamp': adTimestamp,
        'bsDate': bsDate,
        'bsYear': bsYear,
        'bsMonth': bsMonth,
        'note': note,
      };

  factory LedgerTransaction.fromJson(Map<String, dynamic> json) =>
      LedgerTransaction(
        id: json['id'],
        refNo: json['refNo'],
        type: json['type'],
        title: json['title'],
        category: json['category'],
        amountNpr: (json['amountNpr'] as num).toDouble(),
        originalAmount: (json['originalAmount'] as num).toDouble(),
        originalCurrency: json['originalCurrency'],
        accountId: json['accountId'],
        toAccountId: json['toAccountId'],
        prevBalanceNpr: (json['prevBalanceNpr'] as num).toDouble(),
        newBalanceNpr: (json['newBalanceNpr'] as num).toDouble(),
        adTimestamp: json['adTimestamp'],
        bsDate: json['bsDate'],
        bsYear: json['bsYear'],
        bsMonth: json['bsMonth'],
        note: json['note'] ?? '',
      );
}

class SavingsGoal {
  final String id;
  final String title;
  final double targetNpr;
  double savedNpr;
  final String targetBsDate;
  final String frequency;

  SavingsGoal({
    required this.id,
    required this.title,
    required this.targetNpr,
    required this.savedNpr,
    required this.targetBsDate,
    required this.frequency,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'targetNpr': targetNpr,
        'savedNpr': savedNpr,
        'targetBsDate': targetBsDate,
        'frequency': frequency,
      };

  factory SavingsGoal.fromJson(Map<String, dynamic> json) => SavingsGoal(
        id: json['id'],
        title: json['title'],
        targetNpr: (json['targetNpr'] as num).toDouble(),
        savedNpr: (json['savedNpr'] as num).toDouble(),
        targetBsDate: json['targetBsDate'],
        frequency: json['frequency'],
      );
}

class KhataRecord {
  final String id;
  final String type;
  final String partyName;
  final String itemDetail;
  final double amountNpr;
  final String bsDate;
  final String adTimestamp;
  bool settled;

  KhataRecord({
    required this.id,
    required this.type,
    required this.partyName,
    required this.itemDetail,
    required this.amountNpr,
    required this.bsDate,
    required this.adTimestamp,
    this.settled = false,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'type': type,
        'partyName': partyName,
        'itemDetail': itemDetail,
        'amountNpr': amountNpr,
        'bsDate': bsDate,
        'adTimestamp': adTimestamp,
        'settled': settled,
      };

  factory KhataRecord.fromJson(Map<String, dynamic> json) => KhataRecord(
        id: json['id'],
        type: json['type'],
        partyName: json['partyName'],
        itemDetail: json['itemDetail'],
        amountNpr: (json['amountNpr'] as num).toDouble(),
        bsDate: json['bsDate'],
        adTimestamp: json['adTimestamp'],
        settled: json['settled'] ?? false,
      );
}

class AutoRecurringRule {
  final String id;
  final String type;
  final String title;
  final String category;
  final double amountNpr;
  final String accountId;
  final String frequency;
  String lastRunAdDate;

  AutoRecurringRule({
    required this.id,
    required this.type,
    required this.title,
    required this.category,
    required this.amountNpr,
    required this.accountId,
    required this.frequency,
    required this.lastRunAdDate,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'type': type,
        'title': title,
        'category': category,
        'amountNpr': amountNpr,
        'accountId': accountId,
        'frequency': frequency,
        'lastRunAdDate': lastRunAdDate,
      };

  factory AutoRecurringRule.fromJson(Map<String, dynamic> json) =>
      AutoRecurringRule(
        id: json['id'],
        type: json['type'],
        title: json['title'],
        category: json['category'],
        amountNpr: (json['amountNpr'] as num).toDouble(),
        accountId: json['accountId'],
        frequency: json['frequency'],
        lastRunAdDate: json['lastRunAdDate'],
      );
}

// ============================================================================
// ६. मुख्य एप विजेट र स्टेट व्यवस्थापन (meru hisab)
// ============================================================================
class MeruHisabApp extends StatelessWidget {
  const MeruHisabApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'meru hisab',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1B5E20),
          primary: const Color(0xFF1B5E20),
          secondary: const Color(0xFF2E7D32),
          surface: const Color(0xFFF4F7F5),
        ),
        scaffoldBackgroundColor: const Color(0xFFF4F7F5),
        cardTheme: CardThemeData(
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
            side: BorderSide(color: Colors.grey.shade200),
          ),
          color: Colors.white,
        ),
      ),
      home: const MainShellScreen(),
    );
  }
}

class MainShellScreen extends StatefulWidget {
  const MainShellScreen({super.key});

  @override
  State<MainShellScreen> createState() => _MainShellScreenState();
}

class _MainShellScreenState extends State<MainShellScreen> {
  int _selectedIndex = 0;
  DateTime _now = DateTime.now();
  String _lastKnownAdDate = DateFormat('yyyy-MM-dd').format(DateTime.now());
  Timer? _clockTimer;

  List<UserAccount> _accounts = [];
  List<LedgerTransaction> _transactions = [];
  List<SavingsGoal> _goals = [];
  List<KhataRecord> _khatas = [];
  List<AutoRecurringRule> _recurringRules = [];

  String _lang = 'ne';
  String _displayCurrency = 'NPR';
  bool _hideBalances = false;
  String? _appPin;
  bool _isLocked = false;
  final TextEditingController _pinVerifyCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadAllData();
    _clockTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      final current = DateTime.now();
      final todayStr = DateFormat('yyyy-MM-dd').format(current);
      if (mounted) {
        setState(() {
          _now = current;
        });
      }
      if (todayStr != _lastKnownAdDate) {
        _lastKnownAdDate = todayStr;
        _processAutoRecurring(current);
      }
    });
  }

  @override
  void dispose() {
    _clockTimer?.cancel();
    _pinVerifyCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadAllData() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() {
      _lang = prefs.getString('mh_lang') ?? 'ne';
      _displayCurrency = prefs.getString('mh_currency') ?? 'NPR';
      _hideBalances = prefs.getBool('mh_hide') ?? false;
      _appPin = prefs.getString('mh_pin');
      _isLocked = _appPin != null && _appPin!.isNotEmpty;

      final accRaw = prefs.getString('mh_accounts');
      if (accRaw != null) {
        _accounts = (jsonDecode(accRaw) as List)
            .map((e) => UserAccount.fromJson(e))
            .toList();
      }

      final txRaw = prefs.getString('mh_transactions');
      if (txRaw != null) {
        _transactions = (jsonDecode(txRaw) as List)
            .map((e) => LedgerTransaction.fromJson(e))
            .toList();
      }

      final goalRaw = prefs.getString('mh_goals');
      if (goalRaw != null) {
        _goals = (jsonDecode(goalRaw) as List)
            .map((e) => SavingsGoal.fromJson(e))
            .toList();
      } else {
        _goals = [
          SavingsGoal(
            id: 'goal_bike',
            title: 'मोटरसाइकल किन्ने (Motorcycle)',
            targetNpr: 500000,
            savedNpr: 0,
            targetBsDate: '2084-12-30',
            frequency: 'MONTHLY',
          ),
        ];
      }

      final khataRaw = prefs.getString('mh_khatas');
      if (khataRaw != null) {
        _khatas = (jsonDecode(khataRaw) as List)
            .map((e) => KhataRecord.fromJson(e))
            .toList();
      }

      final recRaw = prefs.getString('mh_recurring');
      if (recRaw != null) {
        _recurringRules = (jsonDecode(recRaw) as List)
            .map((e) => AutoRecurringRule.fromJson(e))
            .toList();
      }
    });

    await _processAutoRecurring(DateTime.now());
  }

  Future<void> _saveAllData() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('mh_lang', _lang);
    await prefs.setString('mh_currency', _displayCurrency);
    await prefs.setBool('mh_hide', _hideBalances);
    if (_appPin != null && _appPin!.isNotEmpty) {
      await prefs.setString('mh_pin', _appPin!);
    } else {
      await prefs.remove('mh_pin');
    }
    await prefs.setString(
        'mh_accounts', jsonEncode(_accounts.map((e) => e.toJson()).toList()));
    await prefs.setString('mh_transactions',
        jsonEncode(_transactions.map((e) => e.toJson()).toList()));
    await prefs.setString(
        'mh_goals', jsonEncode(_goals.map((e) => e.toJson()).toList()));
    await prefs.setString(
        'mh_khatas', jsonEncode(_khatas.map((e) => e.toJson()).toList()));
    await prefs.setString('mh_recurring',
        jsonEncode(_recurringRules.map((e) => e.toJson()).toList()));
  }

  Future<void> _processAutoRecurring(DateTime now) async {
    final todayStr = DateFormat('yyyy-MM-dd').format(now);
    bool executedAny = false;

    for (final rule in _recurringRules) {
      if (rule.lastRunAdDate == todayStr) continue;
      final lastDate = DateTime.tryParse(rule.lastRunAdDate) ?? now;
      final diffDays = now.difference(lastDate).inDays;

      bool due = false;
      if (rule.frequency == 'DAILY' && diffDays >= 1) due = true;
      if (rule.frequency == 'WEEKLY' && diffDays >= 7) due = true;
      if (rule.frequency == 'MONTHLY' && diffDays >= 30) due = true;

      if (due && _accounts.any((a) => a.id == rule.accountId)) {
        rule.lastRunAdDate = todayStr;
        executedAny = true;
        _executeTransaction(
          type: rule.type,
          title: '${rule.title} (Auto)',
          category: rule.category,
          amount: rule.amountNpr,
          currency: 'NPR',
          accountId: rule.accountId,
          note: 'स्वचालित कारोबार (${rule.frequency})',
          showSlip: true,
        );
      }
    }
    if (executedAny) {
      await _saveAllData();
    }
  }

  void _executeTransaction({
    required String type,
    required String title,
    required String category,
    required double amount,
    required String currency,
    required String accountId,
    String? toAccountId,
    String note = '',
    bool showSlip = true,
  }) {
    final now = DateTime.now();
    final bs = BsCalendar.adToBs(now);
    final amountNpr = CurrencyService.toNpr(amount, currency);
    final refNo = 'MH-${DateFormat('yyMMddHHmmss').format(now)}';
    final adStamp = DateFormat('yyyy-MM-dd HH:mm:ss').format(now);

    final accIndex = _accounts.indexWhere((a) => a.id == accountId);
    if (accIndex == -1) return;

    final prevBal = _accounts[accIndex].balanceNpr;
    double newBal = prevBal;

    if (type == 'INCOME') {
      newBal = prevBal + amountNpr;
      _accounts[accIndex].balanceNpr = newBal;
    } else if (type == 'EXPENSE' || type == 'SAVING' || type == 'LEND') {
      newBal = prevBal - amountNpr;
      _accounts[accIndex].balanceNpr = newBal;
    } else if (type == 'BORROW') {
      if (accountId != 'CREDIT_ONLY') {
        newBal = prevBal + amountNpr;
        _accounts[accIndex].balanceNpr = newBal;
      }
    } else if (type == 'TRANSFER' && toAccountId != null) {
      final toIndex = _accounts.indexWhere((a) => a.id == toAccountId);
      newBal = prevBal - amountNpr;
      _accounts[accIndex].balanceNpr = newBal;
      if (toIndex != -1) {
        _accounts[toIndex].balanceNpr += amountNpr;
      }
    }

    final tx = LedgerTransaction(
      id: DateTime.now().microsecondsSinceEpoch.toString(),
      refNo: refNo,
      type: type,
      title: title,
      category: category,
      amountNpr: amountNpr,
      originalAmount: amount,
      originalCurrency: currency,
      accountId: accountId,
      toAccountId: toAccountId,
      prevBalanceNpr: prevBal,
      newBalanceNpr: newBal,
      adTimestamp: adStamp,
      bsDate: bs['iso'],
      bsYear: bs['year'],
      bsMonth: bs['month'],
      note: note,
    );

    setState(() {
      _transactions.insert(0, tx);
    });
    _saveAllData();

    if (showSlip && mounted) {
      _showInstantStatementSlip(tx);
    }
  }

  void _showInstantStatementSlip(LedgerTransaction tx) {
    final acc = _accounts.firstWhere(
      (a) => a.id == tx.accountId,
      orElse: () => UserAccount(
          id: 'credit',
          catalogId: 'cash',
          customLabel: 'उधारो खाता',
          balanceNpr: 0),
    );
    final isCredit = tx.type == 'INCOME' || tx.type == 'BORROW';

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Row(
          children: [
            Icon(Icons.receipt_long, color: Color(0xFF1B5E20)),
            SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'meru hisab - Statement रसिद',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  Text(
                    'App Develop by: Dinesh Poudel Poudel',
                    style: TextStyle(fontSize: 10, color: Colors.grey),
                  ),
                ],
              ),
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _slipRow('Ref No:', tx.refNo),
              _slipRow('वि.सं. मिति:', BsCalendar.toNepaliDigits(tx.bsDate)),
              _slipRow('लाइभ समय (AD):', tx.adTimestamp),
              const Divider(),
              _slipRow('प्रकार:', tx.type),
              _slipRow('विवरण:', tx.title),
              _slipRow('क्याटेगोरी:', tx.category),
              _slipRow('खाता:', acc.customLabel),
              const Divider(),
              _slipRow('अघिल्लो मौज्दात:',
                  CurrencyService.format(tx.prevBalanceNpr, _displayCurrency)),
              _slipRow(
                'कारोबार रकम:',
                '${isCredit ? '+' : '-'} ${CurrencyService.format(tx.amountNpr, _displayCurrency)}',
                valueColor:
                    isCredit ? Colors.green.shade700 : Colors.red.shade700,
                bold: true,
              ),
              if (tx.originalCurrency != _displayCurrency)
                _slipRow('मूल प्रविष्टि:',
                    '${tx.originalCurrency} ${tx.originalAmount.toStringAsFixed(2)}'),
              _slipRow(
                'नयाँ बाँकी मौज्दात:',
                CurrencyService.format(tx.newBalanceNpr, _displayCurrency),
                bold: true,
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              setState(() => _selectedIndex = 1);
            },
            child: const Text('पूरा Statement हेर्नुहोस्'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('ठीक छ'),
          ),
        ],
      ),
    );
  }

  Widget _slipRow(String label, String value,
      {Color? valueColor, bool bold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: TextStyle(
                fontSize: 13,
                fontWeight: bold ? FontWeight.bold : FontWeight.w500,
                color: valueColor ?? Colors.black87,
              ),
            ),
          ),
        ],
      ),
    );
  }

  double get _totalBalanceNpr =>
      _accounts.fold(0.0, (sum, acc) => sum + acc.balanceNpr);

  double get _currentMonthIncomeNpr {
    final currentBs = BsCalendar.adToBs(_now);
    return _transactions
        .where((t) =>
            t.type == 'INCOME' &&
            t.bsYear == currentBs['year'] &&
            t.bsMonth == currentBs['month'])
        .fold(0.0, (sum, t) => sum + t.amountNpr);
  }

  double get _currentMonthExpenseNpr {
    final currentBs = BsCalendar.adToBs(_now);
    return _transactions
        .where((t) =>
            t.type == 'EXPENSE' &&
            t.bsYear == currentBs['year'] &&
            t.bsMonth == currentBs['month'])
        .fold(0.0, (sum, t) => sum + t.amountNpr);
  }

  double _evaluateMathExpression(String input) {
    try {
      final clean =
          input.replaceAll(' ', '').replaceAll('×', '*').replaceAll('÷', '/');
      if (clean.isEmpty) return 0.0;
      final addParts = clean.split('+');
      double sum = 0.0;
      for (final addPart in addParts) {
        if (addPart.isEmpty) continue;
        final subParts = addPart.split('-');
        double subVal = _evalMulDiv(subParts[0]);
        for (int i = 1; i < subParts.length; i++) {
          subVal -= _evalMulDiv(subParts[i]);
        }
        sum += subVal;
      }
      return sum.isNaN || sum.isInfinite ? 0.0 : sum;
    } catch (_) {
      return double.tryParse(input) ?? 0.0;
    }
  }

  double _evalMulDiv(String expr) {
    if (expr.isEmpty) return 0.0;
    final mulParts = expr.split('*');
    double res = 1.0;
    for (final m in mulParts) {
      if (m.contains('/')) {
        final divParts = m.split('/');
        double dVal = double.tryParse(divParts[0]) ?? 0.0;
        for (int i = 1; i < divParts.length; i++) {
          final divisor = double.tryParse(divParts[i]) ?? 1.0;
          if (divisor != 0) dVal /= divisor;
        }
        res *= dVal;
      } else {
        res *= double.tryParse(m) ?? 0.0;
      }
    }
    return res;
  }

  @override
  Widget build(BuildContext context) {
    if (_isLocked) {
      return Scaffold(
        backgroundColor: const Color(0xFF1B5E20),
        body: Center(
          child: Card(
            margin: const EdgeInsets.all(24),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.lock_outline,
                      size: 48, color: Color(0xFF1B5E20)),
                  const SizedBox(height: 10),
                  const Text('meru hisab - PIN Lock',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const Text('App Develop by: Dinesh Poudel Poudel',
                      style: TextStyle(fontSize: 11, color: Colors.grey)),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _pinVerifyCtrl,
                    keyboardType: TextInputType.number,
                    maxLength: 4,
                    obscureText: true,
                    textAlign: TextAlign.center,
                    decoration: const InputDecoration(
                      hintText: '४ अंकको PIN हान्नुहोस्',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () {
                        if (_pinVerifyCtrl.text == _appPin) {
                          setState(() {
                            _isLocked = false;
                            _pinVerifyCtrl.clear();
                          });
                        } else {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content:
                                    Text('गलत PIN! पुनः प्रयास गर्नुहोस्।')),
                          );
                        }
                      },
                      child: const Text('खोल्नुहोस् (Unlock)'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    }

    final bsToday = BsCalendar.adToBs(_now);
    final liveTimeStr = DateFormat('HH:mm:ss').format(_now);
    final adDateStr = DateFormat('yyyy-MM-dd').format(_now);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xFF1B5E20),
        foregroundColor: Colors.white,
        titleSpacing: 12,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(AppLocalizations.t(_lang, 'app_title'),
                    style: const TextStyle(
                        fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    AppLocalizations.t(_lang, 'dev_credit'),
                    style: const TextStyle(
                        fontSize: 10,
                        color: Colors.amberAccent,
                        fontWeight: FontWeight.w500),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            Text(
              '${bsToday['formatted']} | $adDateStr $liveTimeStr',
              style: const TextStyle(fontSize: 11, color: Colors.white70),
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
        actions: [
          PopupMenuButton<String>(
            tooltip: 'मुद्रा छान्नुहोस्',
            onSelected: (val) {
              setState(() => _displayCurrency = val);
              _saveAllData();
            },
            itemBuilder: (_) => CurrencyService.currencies.keys
                .map((code) => PopupMenuItem(
                      value: code,
                      child: Text(CurrencyService.currencies[code]!['name']),
                    ))
                .toList(),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              margin: const EdgeInsets.symmetric(vertical: 10),
              decoration: BoxDecoration(
                color: Colors.white24,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(_displayCurrency,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 12)),
            ),
          ),
          const SizedBox(width: 4),
          PopupMenuButton<String>(
            icon: const Icon(Icons.language),
            tooltip: 'भाषा छान्नुहोस्',
            onSelected: (val) {
              setState(() => _lang = val);
              _saveAllData();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'ne', child: Text('🇳🇵 नेपाली')),
              PopupMenuItem(value: 'en', child: Text('🇬🇧 English')),
              PopupMenuItem(value: 'hi', child: Text('🇮🇳 हिन्दी')),
              PopupMenuItem(value: 'ar', child: Text('🇶🇦 العربية')),
            ],
          ),
        ],
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          _buildDashboardTab(),
          _buildStatementTab(),
          _buildGoalsTab(),
          _buildKhataTab(),
          _buildSettingsTab(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (i) => setState(() => _selectedIndex = i),
        destinations: [
          NavigationDestination(
              icon: const Icon(Icons.dashboard_outlined),
              selectedIcon: const Icon(Icons.dashboard),
              label: AppLocalizations.t(_lang, 'tab_home')),
          NavigationDestination(
              icon: const Icon(Icons.receipt_long_outlined),
              selectedIcon: const Icon(Icons.receipt_long),
              label: AppLocalizations.t(_lang, 'tab_statement')),
          NavigationDestination(
              icon: const Icon(Icons.savings_outlined),
              selectedIcon: const Icon(Icons.savings),
              label: AppLocalizations.t(_lang, 'tab_goals')),
          NavigationDestination(
              icon: const Icon(Icons.handshake_outlined),
              selectedIcon: const Icon(Icons.handshake),
              label: AppLocalizations.t(_lang, 'tab_khata')),
          NavigationDestination(
              icon: const Icon(Icons.settings_outlined),
              selectedIcon: const Icon(Icons.settings),
              label: AppLocalizations.t(_lang, 'tab_settings')),
        ],
      ),
    );
  }

  // ==========================================================================
  // ट्याब १: ड्यासबोर्ड
  // ==========================================================================
  Widget _buildDashboardTab() {
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF1B5E20), Color(0xFF2E7D32)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(22),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF1B5E20).withValues(alpha: 0.28),
                blurRadius: 12,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    AppLocalizations.t(_lang, 'total_balance'),
                    style: const TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                  IconButton(
                    icon: Icon(
                      _hideBalances ? Icons.visibility_off : Icons.visibility,
                      color: Colors.white,
                      size: 20,
                    ),
                    onPressed: () {
                      setState(() => _hideBalances = !_hideBalances);
                      _saveAllData();
                    },
                  ),
                ],
              ),
              Text(
                CurrencyService.format(_totalBalanceNpr, _displayCurrency,
                    hidden: _hideBalances),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (_displayCurrency != 'NPR' && !_hideBalances)
                Text(
                  '≈ रु. ${NumberFormat('#,##0.00').format(_totalBalanceNpr)} NPR',
                  style: const TextStyle(color: Colors.white70, fontSize: 12),
                ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.arrow_downward,
                                  color: Colors.lightGreenAccent, size: 16),
                              const SizedBox(width: 4),
                              Expanded(
                                child: Text(
                                  AppLocalizations.t(_lang, 'month_income'),
                                  style: const TextStyle(
                                      color: Colors.white70, fontSize: 11),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            CurrencyService.format(
                                _currentMonthIncomeNpr, _displayCurrency,
                                hidden: _hideBalances),
                            style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 15),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              const Icon(Icons.arrow_upward,
                                  color: Colors.orangeAccent, size: 16),
                              const SizedBox(width: 4),
                              Expanded(
                                child: Text(
                                  AppLocalizations.t(_lang, 'month_expense'),
                                  style: const TextStyle(
                                      color: Colors.white70, fontSize: 11),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            CurrencyService.format(
                                _currentMonthExpenseNpr, _displayCurrency,
                                hidden: _hideBalances),
                            style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 15),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),

        Text(
          AppLocalizations.t(_lang, 'quick_actions'),
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 196,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final colWidth = (constraints.maxWidth - 12) / 2;
              return ListView(
                scrollDirection: Axis.horizontal,
                physics: const BouncingScrollPhysics(),
                children: [
                  SizedBox(
                    width: colWidth,
                    child: Column(
                      children: [
                        Expanded(
                          child: _quickActionCard(
                            label: 'Income',
                            icon: Icons.arrow_downward_rounded,
                            iconColor: const Color(0xFF2E7D32),
                            onTap: () => _openIncomeExpenseSheet('INCOME'),
                          ),
                        ),
                        const SizedBox(height: 10),
                        Expanded(
                          child: _quickActionCard(
                            label: 'Transfer',
                            icon: Icons.swap_horiz_rounded,
                            iconColor: Colors.white,
                            filledGreen: true,
                            onTap: _openTransferSheet,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 10),
                  SizedBox(
                    width: colWidth,
                    child: Column(
                      children: [
                        Expanded(
                          child: _quickActionCard(
                            label: 'Expense',
                            icon: Icons.arrow_upward_rounded,
                            iconColor: const Color(0xFFD32F2F),
                            onTap: () => _openIncomeExpenseSheet('EXPENSE'),
                          ),
                        ),
                        const SizedBox(height: 10),
                        Expanded(
                          child: _quickActionCard(
                            label: 'Lend',
                            icon: Icons.north_east_rounded,
                            iconColor: const Color(0xFFD84315),
                            onTap: () => _openKhataSheet('LEND'),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 10),
                  SizedBox(
                    width: colWidth,
                    child: Column(
                      children: [
                        Expanded(
                          child: _quickActionCard(
                            label: 'Saving',
                            icon: Icons.savings_outlined,
                            iconColor: const Color(0xFFEF6C00),
                            onTap: _openSavingDepositSheet,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Expanded(
                          child: _quickActionCard(
                            label: 'Borrow',
                            icon: Icons.south_west_rounded,
                            iconColor: const Color(0xFF2E7D32),
                            onTap: () => _openKhataSheet('BORROW'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
        ),
        const SizedBox(height: 18),

        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              AppLocalizations.t(_lang, 'my_accounts'),
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            ),
            TextButton.icon(
              onPressed: _openAddAccountSheet,
              icon: const Icon(Icons.add_circle_outline, size: 18),
              label: Text(AppLocalizations.t(_lang, 'add_account')),
            ),
          ],
        ),
        if (_accounts.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  const Icon(Icons.account_balance_wallet_outlined,
                      size: 38, color: Colors.grey),
                  const SizedBox(height: 8),
                  Text(
                    AppLocalizations.t(_lang, 'no_accounts'),
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
                  ),
                  const SizedBox(height: 10),
                  FilledButton.icon(
                    onPressed: _openAddAccountSheet,
                    icon: const Icon(Icons.add),
                    label: Text(AppLocalizations.t(_lang, 'add_account')),
                  ),
                ],
              ),
            ),
          )
        else
          SizedBox(
            height: 92,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _accounts.length,
              separatorBuilder: (_, __) => const SizedBox(width: 10),
              itemBuilder: (_, idx) {
                final acc = _accounts[idx];
                final info = InstitutionCatalog.byId(acc.catalogId);
                return Container(
                  width: 165,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border:
                        Border.all(color: info.color.withValues(alpha: 0.35)),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: info.color,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              info.code,
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 10),
                            ),
                          ),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              acc.customLabel,
                              style: const TextStyle(
                                  fontSize: 12, fontWeight: FontWeight.w600),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                      Text(
                        CurrencyService.format(
                            acc.balanceNpr, _displayCurrency,
                            hidden: _hideBalances),
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: info.color,
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        const SizedBox(height: 18),

        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              AppLocalizations.t(_lang, 'recent_tx'),
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            ),
            TextButton(
              onPressed: () => setState(() => _selectedIndex = 1),
              child: const Text('सबै हेर्नुहोस्'),
            ),
          ],
        ),
        if (_transactions.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 20),
            child: Center(
              child: Text('अहिलेसम्म कुनै कारोबार दर्ता भएको छैन।',
                  style: TextStyle(color: Colors.grey)),
            ),
          )
        else
          ..._transactions.take(6).map((tx) => _transactionTile(tx)),

        const SizedBox(height: 16),
        Center(
          child: Text(
            'meru hisab • ${AppLocalizations.t(_lang, 'dev_credit')}',
            style: TextStyle(
                fontSize: 11,
                color: Colors.grey.shade600,
                fontWeight: FontWeight.w500),
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }

  Widget _quickActionCard({
    required String label,
    required IconData icon,
    required Color iconColor,
    required VoidCallback onTap,
    bool filledGreen = false,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        decoration: BoxDecoration(
          color: filledGreen ? const Color(0xFF1B6B28) : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: filledGreen ? const Color(0xFF1B6B28) : Colors.grey.shade300,
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 28, color: iconColor),
            const SizedBox(height: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: filledGreen ? Colors.white : const Color(0xFF2B2F38),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _transactionTile(LedgerTransaction tx) {
    final isCredit = tx.type == 'INCOME' || tx.type == 'BORROW';
    final acc = _accounts.firstWhere(
      (a) => a.id == tx.accountId,
      orElse: () => UserAccount(
          id: 'x', catalogId: 'cash', customLabel: 'उधारो/अन्य', balanceNpr: 0),
    );
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        onTap: () => _showInstantStatementSlip(tx),
        leading: CircleAvatar(
          backgroundColor:
              isCredit ? Colors.green.shade50 : Colors.red.shade50,
          child: Icon(
            isCredit ? Icons.arrow_downward : Icons.arrow_upward,
            color: isCredit ? Colors.green.shade700 : Colors.red.shade700,
            size: 18,
          ),
        ),
        title: Text(tx.title,
            style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
        subtitle: Text(
          '${BsCalendar.toNepaliDigits(tx.bsDate)} • ${acc.customLabel}\n${tx.adTimestamp}',
          style: const TextStyle(fontSize: 11),
        ),
        isThreeLine: true,
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              '${isCredit ? '+' : '-'}${CurrencyService.format(tx.amountNpr, _displayCurrency, hidden: _hideBalances)}',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 13,
                color: isCredit ? Colors.green.shade700 : Colors.red.shade700,
              ),
            ),
            Text(
              'Bal: ${CurrencyService.format(tx.newBalanceNpr, _displayCurrency, hidden: _hideBalances)}',
              style: const TextStyle(fontSize: 10, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }

  // ==========================================================================
  // ट्याब २: पूर्ण Statement र खर्च विश्लेषण चार्ट
  // ==========================================================================
  Widget _buildStatementTab() {
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('आम्दानी बनाम खर्च विश्लेषण (Live Chart)',
                    style:
                        TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 12),
                SizedBox(
                  height: 130,
                  child: CustomPaint(
                    size: const Size(double.infinity, 130),
                    painter: IncomeExpenseBarPainter(
                      income: _currentMonthIncomeNpr,
                      expense: _currentMonthExpenseNpr,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('आधिकारिक Statement (${_transactions.length} कारोबार)',
                style:
                    const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
            OutlinedButton.icon(
              onPressed: () {
                if (_transactions.isNotEmpty) {
                  _showInstantStatementSlip(_transactions.first);
                }
              },
              icon: const Icon(Icons.receipt, size: 16),
              label: const Text('पछिल्लो रसिद'),
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (_transactions.isEmpty)
          const Center(
            child: Padding(
              padding: EdgeInsets.all(32),
              child: Text('कुनै Statement उपलब्ध छैन।'),
            ),
          )
        else
          ..._transactions.map((tx) => _transactionTile(tx)),
      ],
    );
  }

  // ==========================================================================
  // ट्याब ३: लक्ष्य बचत योजना (Savings Goal & Target Planner)
  // ==========================================================================
  Widget _buildGoalsTab() {
    final todayBs = BsCalendar.adToBs(_now)['iso'] as String;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text('लक्ष्य बचत योजना (Target Planner)',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            FilledButton.icon(
              onPressed: _openCreateGoalSheet,
              icon: const Icon(Icons.add, size: 18),
              label: const Text('नयाँ लक्ष्य'),
            ),
          ],
        ),
        const SizedBox(height: 12),
        ..._goals.map((g) {
          final remainingNpr = math.max(0.0, g.targetNpr - g.savedNpr);
          final daysLeft = BsCalendar.daysBetweenBs(todayBs, g.targetBsDate);
          final weeksLeft = math.max(1, (daysLeft / 7).ceil());
          final monthsLeft = math.max(1, (daysLeft / 30).ceil());

          final dailyNeed = remainingNpr / daysLeft;
          final weeklyNeed = remainingNpr / weeksLeft;
          final monthlyNeed = remainingNpr / monthsLeft;
          final progress =
              g.targetNpr > 0 ? (g.savedNpr / g.targetNpr).clamp(0.0, 1.0) : 0.0;

          return Card(
            margin: const EdgeInsets.only(bottom: 14),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(g.title,
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 16)),
                      ),
                      Chip(
                        label: Text(
                            'लक्ष्य वि.सं. ${BsCalendar.toNepaliDigits(g.targetBsDate)}',
                            style: const TextStyle(fontSize: 11)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(
                    value: progress,
                    minHeight: 10,
                    borderRadius: BorderRadius.circular(8),
                    color: const Color(0xFF1B5E20),
                    backgroundColor: Colors.grey.shade200,
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                          'जम्मा: ${CurrencyService.format(g.savedNpr, _displayCurrency)}'),
                      Text(
                          'लक्ष्य: ${CurrencyService.format(g.targetNpr, _displayCurrency)}',
                          style: const TextStyle(fontWeight: FontWeight.bold)),
                    ],
                  ),
                  const Divider(height: 20),
                  const Text('एकपटकमा कति बचत गर्ने? (तुलनात्मक दर):',
                      style:
                          TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _rateBox(
                          'दैनिक (Daily)', dailyNeed, g.frequency == 'DAILY'),
                      const SizedBox(width: 6),
                      _rateBox('साप्ताहिक (Weekly)', weeklyNeed,
                          g.frequency == 'WEEKLY'),
                      const SizedBox(width: 6),
                      _rateBox('मासिक (Monthly)', monthlyNeed,
                          g.frequency == 'MONTHLY'),
                    ],
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: () =>
                          _openSavingDepositSheet(preselectedGoal: g),
                      icon: const Icon(Icons.savings),
                      label: const Text('+ यो लक्ष्यमा बचत जम्मा गर्नुहोस्'),
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _rateBox(String label, double nprAmount, bool active) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: active ? const Color(0xFFE8F5E9) : Colors.grey.shade100,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: active ? const Color(0xFF1B5E20) : Colors.grey.shade300,
          ),
        ),
        child: Column(
          children: [
            Text(label,
                style: const TextStyle(fontSize: 10),
                textAlign: TextAlign.center),
            const SizedBox(height: 4),
            Text(
              CurrencyService.format(nprAmount, _displayCurrency),
              style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  // ==========================================================================
  // ट्याब ४: सापटी (Lend) र उधारो सामान (Borrow / Udharo) खाता
  // ==========================================================================
  Widget _buildKhataTab() {
    final lends = _khatas.where((k) => k.type == 'LEND').toList();
    final borrows = _khatas.where((k) => k.type == 'BORROW').toList();

    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFD84315)),
                onPressed: () => _openKhataSheet('LEND'),
                icon: const Icon(Icons.north_east),
                label: const Text('+ सापटी दिएको (Lend)'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFF1B5E20)),
                onPressed: () => _openKhataSheet('BORROW'),
                icon: const Icon(Icons.south_west),
                label: const Text('+ उधारो लिएको (Borrow)'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        const Text('हामीसँग कसले कति सापटी लगेको छ (Lend):',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
        const SizedBox(height: 8),
        if (lends.isEmpty)
          const Text('कुनै सापटी रेकर्ड छैन।',
              style: TextStyle(color: Colors.grey))
        else
          ...lends.map((k) => _khataCard(k)),
        const SizedBox(height: 18),
        const Text('हामीले कहाँ उधारो सामान / सापटी लिएका छौँ (Borrow):',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
        const SizedBox(height: 8),
        if (borrows.isEmpty)
          const Text('कुनै उधारो रेकर्ड छैन।',
              style: TextStyle(color: Colors.grey))
        else
          ...borrows.map((k) => _khataCard(k)),
      ],
    );
  }

  Widget _khataCard(KhataRecord k) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        title: Text('${k.partyName} (${k.itemDetail})',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              decoration: k.settled ? TextDecoration.lineThrough : null,
            )),
        subtitle: Text('वि.सं. ${BsCalendar.toNepaliDigits(k.bsDate)}'),
        trailing: k.settled
            ? const Chip(label: Text('फर्स्योट भयो'))
            : ElevatedButton(
                onPressed: () => _settleKhataRecord(k),
                child: Text(
                    'फर्स्योट (${CurrencyService.format(k.amountNpr, _displayCurrency)})'),
              ),
      ),
    );
  }

  void _settleKhataRecord(KhataRecord k) {
    if (_accounts.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('पहिले कम्तीमा एउटा बैंक वा नगद खाता थप्नुहोस्।')),
      );
      return;
    }
    String selectedAcc = _accounts.first.id;
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('${k.partyName} को हिसाब फर्स्योट'),
        content: DropdownButtonFormField<String>(
          initialValue: selectedAcc,
          decoration:
              const InputDecoration(labelText: 'कुन खाताबाट मिलान गर्ने?'),
          items: _accounts
              .map((a) =>
                  DropdownMenuItem(value: a.id, child: Text(a.customLabel)))
              .toList(),
          onChanged: (v) => selectedAcc = v ?? selectedAcc,
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx), child: const Text('रद्द')),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              setState(() => k.settled = true);
              _executeTransaction(
                type: k.type == 'LEND' ? 'INCOME' : 'EXPENSE',
                title: k.type == 'LEND'
                    ? 'सापटी फिर्ता: ${k.partyName}'
                    : 'उधारो भुक्तानी: ${k.partyName}',
                category: '🤝 सापटी / उधारो (Lending / Udharo)',
                amount: k.amountNpr,
                currency: 'NPR',
                accountId: selectedAcc,
                note: k.itemDetail,
              );
            },
            child: const Text('फर्स्योट गर्नुहोस्'),
          ),
        ],
      ),
    );
  }

  // ==========================================================================
  // ट्याब ५: सेटिङ, स्वचालित कारोबार, PIN र Developer क्रेडिट
  // ==========================================================================
  Widget _buildSettingsTab() {
    final pinCtrl = TextEditingController(text: _appPin ?? '');
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        const Card(
          color: Color(0xFFE8F5E9),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: Color(0xFF1B5E20),
              child: Icon(Icons.verified_user, color: Colors.white),
            ),
            title: Text(
              'meru hisab',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17),
            ),
            subtitle: Text(
              'App Develop by: Dinesh Poudel Poudel\nVersion 1.0.0 • Offline & Auto-Statement Ready',
              style: TextStyle(fontSize: 12),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('स्वचालित कारोबार नियम (Auto-Recurring & Statement)',
                    style:
                        TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 6),
                const Text(
                  'मासिक तलब, कोठा भाडा वा दैनिक/मासिक बचत सेट गर्नुहोस्—दिन परिवर्तन हुनासाथ स्वतः Statement बन्छ।',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 10),
                FilledButton.icon(
                  onPressed: _openAddRecurringSheet,
                  icon: const Icon(Icons.autorenew),
                  label: const Text('+ नयाँ स्वचालित कारोबार थप्नुहोस्'),
                ),
                ..._recurringRules.map((r) => ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${r.title} (${r.frequency})'),
                      subtitle: Text(
                          '${r.type} • ${CurrencyService.format(r.amountNpr, _displayCurrency)}'),
                      trailing: IconButton(
                        icon: const Icon(Icons.play_arrow, color: Colors.green),
                        tooltip: 'अहिले तुरुन्त चलाउनुहोस्',
                        onPressed: () {
                          _executeTransaction(
                            type: r.type,
                            title: '${r.title} (Auto)',
                            category: r.category,
                            amount: r.amountNpr,
                            currency: 'NPR',
                            accountId: r.accountId,
                            note: 'स्वचालित कारोबार',
                          );
                        },
                      ),
                    )),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('एप सुरक्षा (४-अंकको PIN Lock)',
                    style:
                        TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                const SizedBox(height: 10),
                TextField(
                  controller: pinCtrl,
                  keyboardType: TextInputType.number,
                  maxLength: 4,
                  decoration: const InputDecoration(
                    labelText:
                        '४ अंकको PIN सेट गर्नुहोस् (हटाउन खाली राख्नुहोस्)',
                    border: OutlineInputBorder(),
                  ),
                ),
                FilledButton(
                  onPressed: () {
                    setState(() {
                      _appPin = pinCtrl.text.trim().length == 4
                          ? pinCtrl.text.trim()
                          : null;
                    });
                    _saveAllData();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                          content: Text('PIN सेटिङ सुरक्षित गरियो।')),
                    );
                  },
                  child: const Text('PIN सुरक्षित गर्नुहोस्'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  // ==========================================================================
  // मोडल सिटहरू (Modals)
  // ==========================================================================
  void _openAddAccountSheet() {
    String selectedGroup = 'A';
    String selectedCatalogId = InstitutionCatalog.all.first.id;
    final amountCtrl = TextEditingController(text: '0');
    final labelCtrl = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) {
          final filtered = InstitutionCatalog.all
              .where((e) => e.group == selectedGroup)
              .toList();
          if (!filtered.any((e) => e.id == selectedCatalogId)) {
            selectedCatalogId = filtered.first.id;
          }
          final currentInst = InstitutionCatalog.byId(selectedCatalogId);

          return Padding(
            padding: EdgeInsets.only(
              left: 16,
              right: 16,
              top: 16,
              bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
            ),
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                      'बैंक / डिजिटल वालेट / नगद थप्नुहोस् (६८ विकल्प)',
                      style:
                          TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 6,
                    children: [
                      ChoiceChip(
                        label: const Text("'क' वाणिज्य (२०)"),
                        selected: selectedGroup == 'A',
                        onSelected: (_) =>
                            setModalState(() => selectedGroup = 'A'),
                      ),
                      ChoiceChip(
                        label: const Text("'ख' विकास (१७)"),
                        selected: selectedGroup == 'B',
                        onSelected: (_) =>
                            setModalState(() => selectedGroup = 'B'),
                      ),
                      ChoiceChip(
                        label: const Text("'ग' फाइनान्स (१५)"),
                        selected: selectedGroup == 'C',
                        onSelected: (_) =>
                            setModalState(() => selectedGroup = 'C'),
                      ),
                      ChoiceChip(
                        label: const Text('डिजिटल वालेट (१५)'),
                        selected: selectedGroup == 'WALLET',
                        onSelected: (_) =>
                            setModalState(() => selectedGroup = 'WALLET'),
                      ),
                      ChoiceChip(
                        label: const Text('नगद (Cash)'),
                        selected: selectedGroup == 'CASH',
                        onSelected: (_) =>
                            setModalState(() => selectedGroup = 'CASH'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    key: ValueKey('inst_$selectedGroup'),
                    initialValue: selectedCatalogId,
                    isExpanded: true,
                    decoration:
                        const InputDecoration(labelText: 'संस्था छान्नुहोस्'),
                    items: filtered
                        .map((inst) => DropdownMenuItem(
                              value: inst.id,
                              child: Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: inst.color,
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    child: Text(inst.code,
                                        style: const TextStyle(
                                            color: Colors.white, fontSize: 10)),
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                        '${inst.nameNp} (${inst.nameEn})',
                                        overflow: TextOverflow.ellipsis),
                                  ),
                                ],
                              ),
                            ))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) {
                        setModalState(() => selectedCatalogId = v);
                      }
                    },
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: labelCtrl,
                    decoration: InputDecoration(
                      labelText: 'खाताको नाम (ऐच्छिक)',
                      hintText: currentInst.nameNp,
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: amountCtrl,
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(
                      labelText: 'सुरुवाती मौज्दात ($_displayCurrency)',
                    ),
                  ),
                  const SizedBox(height: 14),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () {
                        final rawAmt = double.tryParse(amountCtrl.text) ?? 0.0;
                        final nprAmt =
                            CurrencyService.toNpr(rawAmt, _displayCurrency);
                        setState(() {
                          _accounts.add(UserAccount(
                            id: DateTime.now()
                                .microsecondsSinceEpoch
                                .toString(),
                            catalogId: selectedCatalogId,
                            customLabel: labelCtrl.text.trim().isEmpty
                                ? currentInst.nameNp
                                : labelCtrl.text.trim(),
                            balanceNpr: nprAmt,
                          ));
                        });
                        _saveAllData();
                        Navigator.pop(ctx);
                      },
                      child: const Text('खाता सुरक्षित गर्नुहोस्'),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  void _openIncomeExpenseSheet(String type) {
    if (_accounts.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content:
                Text('पहिले "+ बैंक / वालेट थप्नुहोस्" बाट खाता बनाउनुहोस्।')),
      );
      _openAddAccountSheet();
      return;
    }

    final groups = type == 'INCOME'
        ? CategoryData.incomeGroups
        : CategoryData.expenseGroups;
    String selectedGroup = groups.keys.first;
    String selectedCat =
        '${groups[selectedGroup]!.first['icon']} ${groups[selectedGroup]!.first['name']}';
    String selectedAcc = _accounts.first.id;
    String entryCurrency = _displayCurrency;
    final exprCtrl = TextEditingController();
    final noteCtrl = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) {
          final cats = groups[selectedGroup]!;
          final catLabels =
              cats.map((c) => '${c['icon']} ${c['name']}').toList();
          if (!catLabels.contains(selectedCat)) {
            selectedCat = catLabels.first;
          }
          final calculatedVal = _evaluateMathExpression(exprCtrl.text);

          return Padding(
            padding: EdgeInsets.only(
              left: 16,
              right: 16,
              top: 16,
              bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
            ),
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    type == 'INCOME'
                        ? 'नयाँ आम्दानी थप्नुहोस् (Income)'
                        : 'नयाँ खर्च थप्नुहोस् (Expense)',
                    style: const TextStyle(
                        fontSize: 17, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 10),
                  DropdownButtonFormField<String>(
                    initialValue: selectedGroup,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'मुख्य समूह'),
                    items: groups.keys
                        .map((g) => DropdownMenuItem(value: g, child: Text(g)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) {
                        setModalState(() {
                          selectedGroup = v;
                          selectedCat =
                              '${groups[v]!.first['icon']} ${groups[v]!.first['name']}';
                        });
                      }
                    },
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<String>(
                    key: ValueKey('cat_$selectedGroup'),
                    initialValue: selectedCat,
                    isExpanded: true,
                    decoration: const InputDecoration(labelText: 'क्याटेगोरी'),
                    items: catLabels
                        .map((label) =>
                            DropdownMenuItem(value: label, child: Text(label)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) setModalState(() => selectedCat = v);
                    },
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        flex: 2,
                        child: TextField(
                          controller: exprCtrl,
                          onChanged: (_) => setModalState(() {}),
                          decoration: const InputDecoration(
                            labelText: 'रकम / रसिद क्याल्कुलेटर',
                            hintText: 'उदा: 150+80+45',
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          initialValue: entryCurrency,
                          decoration: const InputDecoration(labelText: 'मुद्रा'),
                          items: CurrencyService.currencies.keys
                              .map((c) =>
                                  DropdownMenuItem(value: c, child: Text(c)))
                              .toList(),
                          onChanged: (v) {
                            if (v != null) {
                              setModalState(() => entryCurrency = v);
                            }
                          },
                        ),
                      ),
                    ],
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      'गणना गरिएको कुल: $entryCurrency ${calculatedVal.toStringAsFixed(2)} (≈ रु. ${CurrencyService.toNpr(calculatedVal, entryCurrency).toStringAsFixed(2)})',
                      style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF1B5E20)),
                    ),
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<String>(
                    initialValue: selectedAcc,
                    decoration:
                        const InputDecoration(labelText: 'बैंक / वालेट / नगद'),
                    items: _accounts
                        .map((a) => DropdownMenuItem(
                            value: a.id, child: Text(a.customLabel)))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) setModalState(() => selectedAcc = v);
                    },
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: noteCtrl,
                    decoration:
                        const InputDecoration(labelText: 'टिप्पणी / पसलको नाम'),
                  ),
                  const SizedBox(height: 14),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () {
                        if (calculatedVal <= 0) return;
                        Navigator.pop(ctx);
                        _executeTransaction(
                          type: type,
                          title: noteCtrl.text.trim().isEmpty
                              ? selectedCat
                              : noteCtrl.text.trim(),
                          category: selectedCat,
                          amount: calculatedVal,
                          currency: entryCurrency,
                          accountId: selectedAcc,
                          note: noteCtrl.text.trim(),
                        );
                      },
                      child:
                          const Text('सेभ गर्नुहोस् र Statement निकाल्नुहोस्'),
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  void _openTransferSheet() {
    if (_accounts.length < 2) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text(
                'रकम सार्न कम्तीमा २ वटा खाता (जस्तै: बैंक र इसेवा) थप्नुहोस्।')),
      );
      return;
    }
    String fromAcc = _accounts[0].id;
    String toAcc = _accounts[1].id;
    final amtCtrl = TextEditingController();
    final noteCtrl = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('बैंक / वालेट ट्रान्सफर (Self Transfer)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                initialValue: fromAcc,
                decoration:
                    const InputDecoration(labelText: 'पठाउने खाता (From)'),
                items: _accounts
                    .map((a) => DropdownMenuItem(
                        value: a.id, child: Text(a.customLabel)))
                    .toList(),
                onChanged: (v) => setModalState(() => fromAcc = v!),
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<String>(
                initialValue: toAcc,
                decoration: const InputDecoration(labelText: 'पाउने खाता (To)'),
                items: _accounts
                    .map((a) => DropdownMenuItem(
                        value: a.id, child: Text(a.customLabel)))
                    .toList(),
                onChanged: (v) => setModalState(() => toAcc = v!),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: amtCtrl,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration:
                    InputDecoration(labelText: 'रकम ($_displayCurrency)'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: noteCtrl,
                decoration: const InputDecoration(labelText: 'कैफियत (Note)'),
              ),
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final val = double.tryParse(amtCtrl.text) ?? 0;
                    if (val <= 0 || fromAcc == toAcc) return;
                    final fromName = _accounts
                        .firstWhere((a) => a.id == fromAcc)
                        .customLabel;
                    final toName =
                        _accounts.firstWhere((a) => a.id == toAcc).customLabel;
                    Navigator.pop(ctx);
                    _executeTransaction(
                      type: 'TRANSFER',
                      title: '$fromName → $toName',
                      category: '🔄 आन्तरिक ट्रान्सफर (Transfer)',
                      amount: val,
                      currency: _displayCurrency,
                      accountId: fromAcc,
                      toAccountId: toAcc,
                      note: noteCtrl.text.trim(),
                    );
                  },
                  child: const Text('ट्रान्सफर पूरा गर्नुहोस्'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openCreateGoalSheet() {
    final titleCtrl = TextEditingController();
    final targetCtrl = TextEditingController();
    final dateCtrl = TextEditingController(text: '2084-12-30');
    String freq = 'MONTHLY';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('नयाँ बचत लक्ष्य थप्नुहोस्',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              TextField(
                controller: titleCtrl,
                decoration: const InputDecoration(
                    labelText: 'लक्ष्यको नाम (उदा: मोटरसाइकल)'),
              ),
              TextField(
                controller: targetCtrl,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                    labelText: 'लक्ष्य रकम ($_displayCurrency)'),
              ),
              TextField(
                controller: dateCtrl,
                decoration: const InputDecoration(
                    labelText: 'लक्ष्य वि.सं. मिति (YYYY-MM-DD)'),
              ),
              DropdownButtonFormField<String>(
                initialValue: freq,
                decoration: const InputDecoration(labelText: 'बचत गर्ने तरिका'),
                items: const [
                  DropdownMenuItem(value: 'DAILY', child: Text('दैनिक (Daily)')),
                  DropdownMenuItem(
                      value: 'WEEKLY', child: Text('साप्ताहिक (Weekly)')),
                  DropdownMenuItem(
                      value: 'MONTHLY', child: Text('मासिक (Monthly)')),
                ],
                onChanged: (v) => setModalState(() => freq = v!),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final tAmt = double.tryParse(targetCtrl.text) ?? 0;
                    if (titleCtrl.text.trim().isEmpty || tAmt <= 0) return;
                    setState(() {
                      _goals.add(SavingsGoal(
                        id: DateTime.now().microsecondsSinceEpoch.toString(),
                        title: titleCtrl.text.trim(),
                        targetNpr:
                            CurrencyService.toNpr(tAmt, _displayCurrency),
                        savedNpr: 0,
                        targetBsDate: dateCtrl.text.trim(),
                        frequency: freq,
                      ));
                    });
                    _saveAllData();
                    Navigator.pop(ctx);
                  },
                  child: const Text('लक्ष्य सुरक्षित गर्नुहोस्'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openSavingDepositSheet({SavingsGoal? preselectedGoal}) {
    if (_accounts.isEmpty || _goals.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('पहिले खाता र बचत लक्ष्य तयार गर्नुहोस्।')),
      );
      return;
    }
    String selectedGoalId = preselectedGoal?.id ?? _goals.first.id;
    String selectedAccId = _accounts.first.id;
    final amtCtrl = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('लक्ष्य बचत जम्मा गर्नुहोस् (Saving)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              DropdownButtonFormField<String>(
                initialValue: selectedGoalId,
                decoration: const InputDecoration(labelText: 'लक्ष्य छान्नुहोस्'),
                items: _goals
                    .map((g) =>
                        DropdownMenuItem(value: g.id, child: Text(g.title)))
                    .toList(),
                onChanged: (v) => setModalState(() => selectedGoalId = v!),
              ),
              DropdownButtonFormField<String>(
                initialValue: selectedAccId,
                decoration:
                    const InputDecoration(labelText: 'कुन खाताबाट बचत गर्ने?'),
                items: _accounts
                    .map((a) => DropdownMenuItem(
                        value: a.id, child: Text(a.customLabel)))
                    .toList(),
                onChanged: (v) => setModalState(() => selectedAccId = v!),
              ),
              TextField(
                controller: amtCtrl,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                    labelText: 'बचत रकम ($_displayCurrency)'),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final val = double.tryParse(amtCtrl.text) ?? 0;
                    if (val <= 0) return;
                    final goal =
                        _goals.firstWhere((g) => g.id == selectedGoalId);
                    final nprVal =
                        CurrencyService.toNpr(val, _displayCurrency);
                    setState(() {
                      goal.savedNpr += nprVal;
                    });
                    Navigator.pop(ctx);
                    _executeTransaction(
                      type: 'SAVING',
                      title: 'बचत: ${goal.title}',
                      category: '💰 बचत / बीमा (Savings & Insurance)',
                      amount: val,
                      currency: _displayCurrency,
                      accountId: selectedAccId,
                      note: 'लक्ष्य बचत जम्मा',
                    );
                  },
                  child: const Text('बचत जम्मा गर्नुहोस्'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openKhataSheet(String type) {
    if (type == 'LEND' && _accounts.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('सापटी दिन पहिले कम्तीमा एउटा खाता थप्नुहोस्।')),
      );
      _openAddAccountSheet();
      return;
    }

    final partyCtrl = TextEditingController();
    final itemCtrl = TextEditingController();
    final amtCtrl = TextEditingController();
    String selectedAcc =
        _accounts.isNotEmpty ? _accounts.first.id : 'CREDIT_ONLY';

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                type == 'LEND'
                    ? 'कसलाई सापटी दिनुभयो? (Lend)'
                    : 'कहाँबाट उधारो सामान / सापटी लिनुभयो? (Borrow)',
                style:
                    const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
              ),
              TextField(
                controller: partyCtrl,
                decoration: InputDecoration(
                    labelText: type == 'LEND'
                        ? 'व्यक्तिको नाम'
                        : 'पसल वा व्यक्तिको नाम'),
              ),
              TextField(
                controller: itemCtrl,
                decoration: const InputDecoration(
                    labelText:
                        'सामानको विवरण / कारण (उदा: चामल, तेल, सापटी)'),
              ),
              TextField(
                controller: amtCtrl,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration:
                    InputDecoration(labelText: 'रकम ($_displayCurrency)'),
              ),
              DropdownButtonFormField<String>(
                initialValue: selectedAcc,
                decoration: const InputDecoration(labelText: 'खाता प्रभाव'),
                items: [
                  if (type == 'BORROW')
                    const DropdownMenuItem(
                        value: 'CREDIT_ONLY',
                        child: Text(
                            'उधारो सामान मात्र (बैंक ब्यालेन्स नबढाउने)')),
                  ..._accounts.map((a) => DropdownMenuItem(
                      value: a.id, child: Text(a.customLabel))),
                ],
                onChanged: (v) => setModalState(() => selectedAcc = v!),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final val = double.tryParse(amtCtrl.text) ?? 0;
                    if (partyCtrl.text.trim().isEmpty || val <= 0) return;
                    final now = DateTime.now();
                    final bs = BsCalendar.adToBs(now);
                    final nprVal =
                        CurrencyService.toNpr(val, _displayCurrency);
                    setState(() {
                      _khatas.insert(
                        0,
                        KhataRecord(
                          id: DateTime.now()
                              .microsecondsSinceEpoch
                              .toString(),
                          type: type,
                          partyName: partyCtrl.text.trim(),
                          itemDetail: itemCtrl.text.trim().isEmpty
                              ? 'नगद/सामान'
                              : itemCtrl.text.trim(),
                          amountNpr: nprVal,
                          bsDate: bs['iso'],
                          adTimestamp:
                              DateFormat('yyyy-MM-dd HH:mm:ss').format(now),
                        ),
                      );
                    });
                    Navigator.pop(ctx);
                    if (selectedAcc != 'CREDIT_ONLY') {
                      _executeTransaction(
                        type: type,
                        title: type == 'LEND'
                            ? 'सापटी दिएको: ${partyCtrl.text.trim()}'
                            : 'सापटी/उधारो लिएको: ${partyCtrl.text.trim()}',
                        category: '🤝 सापटी / उधारो (Lending / Udharo)',
                        amount: val,
                        currency: _displayCurrency,
                        accountId: selectedAcc,
                        note: itemCtrl.text.trim(),
                      );
                    } else {
                      _saveAllData();
                    }
                  },
                  child: const Text('रेकर्ड सुरक्षित गर्नुहोस्'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openAddRecurringSheet() {
    if (_accounts.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('पहिले कम्तीमा एउटा बैंक वा नगद खाता थप्नुहोस्।')),
      );
      return;
    }
    String type = 'EXPENSE';
    String freq = 'MONTHLY';
    String accId = _accounts.first.id;
    final titleCtrl = TextEditingController();
    final amtCtrl = TextEditingController();

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('स्वचालित कारोबार थप्नुहोस्',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              DropdownButtonFormField<String>(
                initialValue: type,
                items: const [
                  DropdownMenuItem(
                      value: 'INCOME', child: Text('आम्दानी (Income)')),
                  DropdownMenuItem(
                      value: 'EXPENSE', child: Text('खर्च (Expense)')),
                ],
                onChanged: (v) => setModalState(() => type = v!),
              ),
              TextField(
                controller: titleCtrl,
                decoration: const InputDecoration(
                    labelText: 'शीर्षक (उदा: कोठा भाडा / तलब)'),
              ),
              TextField(
                controller: amtCtrl,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'रकम (NPR)'),
              ),
              DropdownButtonFormField<String>(
                initialValue: freq,
                items: const [
                  DropdownMenuItem(value: 'DAILY', child: Text('दैनिक (Daily)')),
                  DropdownMenuItem(
                      value: 'WEEKLY', child: Text('साप्ताहिक (Weekly)')),
                  DropdownMenuItem(
                      value: 'MONTHLY', child: Text('मासिक (Monthly)')),
                ],
                onChanged: (v) => setModalState(() => freq = v!),
              ),
              DropdownButtonFormField<String>(
                initialValue: accId,
                items: _accounts
                    .map((a) => DropdownMenuItem(
                        value: a.id, child: Text(a.customLabel)))
                    .toList(),
                onChanged: (v) => setModalState(() => accId = v!),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () {
                    final amt = double.tryParse(amtCtrl.text) ?? 0;
                    if (titleCtrl.text.trim().isEmpty || amt <= 0) return;
                    setState(() {
                      _recurringRules.add(AutoRecurringRule(
                        id: DateTime.now().microsecondsSinceEpoch.toString(),
                        type: type,
                        title: titleCtrl.text.trim(),
                        category: type == 'INCOME'
                            ? '💵 मासिक तलब (Monthly Salary)'
                            : '🏠 कोठा भाडा (Room Rent)',
                        amountNpr: amt,
                        accountId: accId,
                        frequency: freq,
                        lastRunAdDate:
                            DateFormat('yyyy-MM-dd').format(DateTime.now()),
                      ));
                    });
                    _saveAllData();
                    Navigator.pop(ctx);
                  },
                  child: const Text('नियम सुरक्षित गर्नुहोस्'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ============================================================================
// ७. कस्टम पेंटर: आम्दानी र खर्चको तुलनात्मक बार चार्ट
// ============================================================================
class IncomeExpenseBarPainter extends CustomPainter {
  final double income;
  final double expense;

  IncomeExpenseBarPainter({required this.income, required this.expense});

  @override
  void paint(Canvas canvas, Size size) {
    final maxVal = math.max(1.0, math.max(income, expense));
    final barWidth = size.width * 0.24;
    final maxBarHeight = size.height - 30;

    final incPaint = Paint()..color = const Color(0xFF2E7D32);
    final expPaint = Paint()..color = const Color(0xFFD32F2F);
    final bgPaint = Paint()..color = Colors.grey.shade200;

    final incX = size.width * 0.22;
    final expX = size.width * 0.56;

    canvas.drawRRect(
      RRect.fromRectAndRadius(Rect.fromLTWH(incX, 10, barWidth, maxBarHeight),
          const Radius.circular(10)),
      bgPaint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(Rect.fromLTWH(expX, 10, barWidth, maxBarHeight),
          const Radius.circular(10)),
      bgPaint,
    );

    final incH = (income / maxVal) * maxBarHeight;
    final expH = (expense / maxVal) * maxBarHeight;

    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTWH(incX, 10 + (maxBarHeight - incH), barWidth, incH),
          const Radius.circular(10)),
      incPaint,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
          Rect.fromLTWH(expX, 10 + (maxBarHeight - expH), barWidth, expH),
          const Radius.circular(10)),
      expPaint,
    );

    final tpInc = TextPainter(
      text: const TextSpan(
          text: 'आम्दानी',
          style: TextStyle(color: Colors.black87, fontSize: 12)),
      textDirection: ui.TextDirection.ltr,
    )..layout();
    tpInc.paint(
        canvas, Offset(incX + (barWidth - tpInc.width) / 2, size.height - 16));

    final tpExp = TextPainter(
      text: const TextSpan(
          text: 'खर्च',
          style: TextStyle(color: Colors.black87, fontSize: 12)),
      textDirection: ui.TextDirection.ltr,
    )..layout();
    tpExp.paint(
        canvas, Offset(expX + (barWidth - tpExp.width) / 2, size.height - 16));
  }

  @override
  bool shouldRepaint(covariant IncomeExpenseBarPainter oldDelegate) =>
      oldDelegate.income != income || oldDelegate.expense != expense;
}
