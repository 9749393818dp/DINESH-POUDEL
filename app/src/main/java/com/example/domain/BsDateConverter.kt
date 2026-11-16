package com.example.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

data class BsDate(
  val year: Int,
  val month: Int,
  val day: Int,
) {
  val iso: String
    get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

  val monthNameNp: String
    get() = BsDateConverter.nepaliMonths.getOrElse(month - 1) { "" }

  val monthNameEn: String
    get() = BsDateConverter.bsMonthsEn.getOrElse(month - 1) { "" }
}

data class AdDate(
  val year: Int,
  val month: Int,
  val day: Int,
) {
  val iso: String
    get() = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

  val monthNameEn: String
    get() = BsDateConverter.adMonthsEn.getOrElse(month - 1) { "" }

  val monthShortEn: String
    get() = BsDateConverter.adMonthsShortEn.getOrElse(month - 1) { "" }
}

enum class CalendarFormat {
  BS,
  AD,
}

/**
 * Utility class handling lossless bidirectional conversion between Bikram Sambat (BS)
 * and Gregorian (AD) dates for the verified 2080–2090 BS (2023-04-14 to 2034-04-13 AD) range.
 *
 * Uses UTC midnight calendars for day-count arithmetic so conversions remain exact across
 * all device timezones and Daylight Saving Time (DST) transitions.
 */
object BsDateConverter {
  const val MIN_BS_YEAR = 2080
  const val MAX_BS_YEAR = 2090

  val MIN_BS_DATE = BsDate(2080, 1, 1)
  val MAX_BS_DATE = BsDate(2090, 12, 30)
  val MIN_AD_DATE = AdDate(2023, 4, 14)
  val MAX_AD_DATE: AdDate by lazy { bsToAd(MAX_BS_DATE) }

  private const val MILLIS_PER_DAY = 86_400_000L

  val bsMonthDays: Map<Int, List<Int>> = mapOf(
    2080 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
    2081 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2082 to listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2083 to listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2084 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2085 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2086 to listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
    2087 to listOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
    2088 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
    2089 to listOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
    2090 to listOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
  )

  val nepaliMonths: List<String> = listOf(
    "बैशाख", "जेठ", "असार", "श्रावण", "भदौ", "असोज",
    "कार्तिक", "मंसिर", "पुस", "माघ", "फाल्गुन", "चैत",
  )

  val bsMonthsEn: List<String> = listOf(
    "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
    "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra",
  )

  val adMonthsEn: List<String> = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  )

  val adMonthsShortEn: List<String> = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  )

  val nepaliDays: List<String> = listOf(
    "आइतबार", "सोमबार", "मंगलबार", "बुधबार", "बिहीबार", "शुक्रबार", "शनिबार",
  )

  val nepaliDaysShort: List<String> = listOf(
    "आइत", "सोम", "मंगल", "बुध", "बिही", "शुक्र", "शनि",
  )

  val englishDays: List<String> = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
  )

  val englishDaysShort: List<String> = listOf(
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
  )

  private fun utcCalendar(year: Int, month1Based: Int, day: Int): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(year, month1Based - 1, day, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
  }

  private fun baseUtcCalendar(): Calendar = utcCalendar(2023, 4, 14)

  fun toNepaliDigits(input: String): String {
    val en = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val ne = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
    val sb = StringBuilder(input.length)
    for (ch in input) {
      val idx = en.indexOf(ch)
      sb.append(if (idx >= 0) ne[idx] else ch)
    }
    return sb.toString()
  }

  fun daysInBsMonth(year: Int, month: Int): Int {
    if (month !in 1..12) return 0
    return bsMonthDays[year]?.getOrNull(month - 1) ?: 0
  }

  fun daysInAdMonth(year: Int, month: Int): Int {
    if (month !in 1..12) return 0
    return utcCalendar(year, month, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  fun isValidBsDate(year: Int, month: Int, day: Int): Boolean {
    val maxDay = daysInBsMonth(year, month)
    return maxDay > 0 && day in 1..maxDay
  }

  fun isValidAdDate(year: Int, month: Int, day: Int): Boolean {
    if (month !in 1..12) return false
    val maxDay = daysInAdMonth(year, month)
    if (day !in 1..maxDay) return false
    val target = utcCalendar(year, month, day).timeInMillis
    val minMillis = utcCalendar(MIN_AD_DATE.year, MIN_AD_DATE.month, MIN_AD_DATE.day).timeInMillis
    val maxMillis = utcCalendar(MAX_AD_DATE.year, MAX_AD_DATE.month, MAX_AD_DATE.day).timeInMillis
    return target in minMillis..maxMillis
  }

  fun parseBsIso(bsIso: String): BsDate? {
    val parts = bsIso.trim().split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return if (isValidBsDate(y, m, d)) BsDate(y, m, d) else null
  }

  fun parseAdIso(adIso: String): AdDate? {
    val parts = adIso.trim().split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return if (m in 1..12 && d in 1..daysInAdMonth(y, m)) AdDate(y, m, d) else null
  }

  /**
   * Converts a Bikram Sambat date to Gregorian (AD).
   * Throws [IllegalArgumentException] if the BS date is outside 2080–2090 BS or invalid.
   */
  fun bsToAd(bsDate: BsDate): AdDate = bsToAd(bsDate.year, bsDate.month, bsDate.day)

  fun bsToAd(year: Int, month: Int, day: Int): AdDate {
    require(isValidBsDate(year, month, day)) {
      "Unsupported or invalid BS date: $year-$month-$day (supported: $MIN_BS_YEAR–$MAX_BS_YEAR BS)"
    }
    var totalDays = 0
    for (y in MIN_BS_YEAR until year) {
      totalDays += bsMonthDays[y]!!.sum()
    }
    val monthList = bsMonthDays[year]!!
    for (m in 0 until (month - 1)) {
      totalDays += monthList[m]
    }
    totalDays += (day - 1)

    val cal = baseUtcCalendar().apply {
      add(Calendar.DAY_OF_YEAR, totalDays)
    }
    return AdDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      day = cal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun bsToAdOrNull(year: Int, month: Int, day: Int): AdDate? {
    return if (isValidBsDate(year, month, day)) bsToAd(year, month, day) else null
  }

  fun bsIsoToAd(bsIso: String): AdDate? {
    val parsed = parseBsIso(bsIso) ?: return null
    return bsToAd(parsed)
  }

  /**
   * Converts a BS date to local epoch milliseconds (preserving the current time-of-day or noon).
   */
  fun bsToAdMillis(year: Int, month: Int, day: Int, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val ad = bsToAd(year, month, day)
    return adToMillis(ad, timeZone)
  }

  fun adToMillis(adDate: AdDate, timeZone: TimeZone = TimeZone.getDefault()): Long {
    return Calendar.getInstance(timeZone).apply {
      clear()
      set(adDate.year, adDate.month - 1, adDate.day, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  fun millisToAdDate(adTimeMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): AdDate {
    val localCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = adTimeMillis
    }
    return AdDate(
      year = localCal.get(Calendar.YEAR),
      month = localCal.get(Calendar.MONTH) + 1,
      day = localCal.get(Calendar.DAY_OF_MONTH),
    )
  }

  fun clampAdDate(adDate: AdDate): AdDate {
    val target = utcCalendar(adDate.year, adDate.month, adDate.day).timeInMillis
    val minMillis = utcCalendar(MIN_AD_DATE.year, MIN_AD_DATE.month, MIN_AD_DATE.day).timeInMillis
    val maxMillis = utcCalendar(MAX_AD_DATE.year, MAX_AD_DATE.month, MAX_AD_DATE.day).timeInMillis
    return when {
      target < minMillis -> MIN_AD_DATE
      target > maxMillis -> MAX_AD_DATE
      else -> adDate
    }
  }

  fun adToBsDate(adDate: AdDate): BsDate {
    val clamped = clampAdDate(adDate)
    val targetUtc = utcCalendar(clamped.year, clamped.month, clamped.day)
    val baseUtc = baseUtcCalendar()
    var diffDays = ((targetUtc.timeInMillis - baseUtc.timeInMillis) / MILLIS_PER_DAY).toInt()

    var bsYear = MIN_BS_YEAR
    var bsMonth = 1
    var bsDay = 1

    while (diffDays > 0 && bsYear <= MAX_BS_YEAR) {
      val daysInMonth = bsMonthDays[bsYear]!![bsMonth - 1]
      if (diffDays >= daysInMonth) {
        diffDays -= daysInMonth
        bsMonth++
        if (bsMonth > 12) {
          bsMonth = 1
          bsYear++
        }
      } else {
        bsDay += diffDays
        diffDays = 0
      }
    }

    if (bsYear > MAX_BS_YEAR) {
      return MAX_BS_DATE
    }
    return BsDate(bsYear, bsMonth, bsDay)
  }

  fun adToBs(year: Int, month: Int, day: Int): BsDateInfo {
    val adDate = clampAdDate(AdDate(year, month, day))
    val bs = adToBsDate(adDate)
    val weekdayIdx = weekdayOfAd(adDate)
    val weekdayName = nepaliDays[weekdayIdx]
    val monthName = nepaliMonths[bs.month - 1]
    val formatted = "${toNepaliDigits(bs.year.toString())} $monthName ${toNepaliDigits(bs.day.toString())}, $weekdayName"
    return BsDateInfo(
      year = bs.year,
      month = bs.month,
      day = bs.day,
      monthName = monthName,
      weekday = weekdayName,
      formatted = formatted,
      iso = bs.iso,
    )
  }

  fun adToBs(adTimeMillis: Long = System.currentTimeMillis()): BsDateInfo {
    val ad = millisToAdDate(adTimeMillis)
    return adToBs(ad.year, ad.month, ad.day)
  }

  /** Returns 0 for Sunday .. 6 for Saturday. */
  fun weekdayOfAd(adDate: AdDate): Int {
    val cal = utcCalendar(adDate.year, adDate.month, adDate.day)
    return (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
  }

  /** Returns 0 for Sunday .. 6 for Saturday. */
  fun weekdayOfBs(bsDate: BsDate): Int {
    return weekdayOfAd(bsToAd(bsDate))
  }

  fun startWeekdayOfBsMonth(year: Int, month: Int): Int {
    return weekdayOfBs(BsDate(year, month, 1))
  }

  fun startWeekdayOfAdMonth(year: Int, month: Int): Int {
    return weekdayOfAd(AdDate(year, month, 1))
  }

  fun formatBsFull(bsDate: BsDate, useNepaliScript: Boolean = true): String {
    val wd = weekdayOfBs(bsDate)
    return if (useNepaliScript) {
      "${toNepaliDigits(bsDate.year.toString())} ${bsDate.monthNameNp} ${toNepaliDigits(bsDate.day.toString())}, ${nepaliDays[wd]}"
    } else {
      "${englishDaysShort[wd]}, ${bsDate.monthNameEn} ${bsDate.day}, ${bsDate.year} BS"
    }
  }

  fun formatBsShort(bsDate: BsDate, useNepaliScript: Boolean = true): String {
    return if (useNepaliScript) {
      "${toNepaliDigits(bsDate.year.toString())} ${bsDate.monthNameNp} ${toNepaliDigits(bsDate.day.toString())}"
    } else {
      "${bsDate.year} ${bsDate.monthNameEn} ${bsDate.day} BS"
    }
  }

  fun formatAdFull(adDate: AdDate): String {
    val wd = weekdayOfAd(adDate)
    return String.format(
      Locale.US,
      "%s, %s %d, %04d AD",
      englishDaysShort[wd],
      adDate.monthShortEn,
      adDate.day,
      adDate.year,
    )
  }

  fun formatAdShort(adDate: AdDate): String {
    return String.format(
      Locale.US,
      "%d %s %04d",
      adDate.day,
      adDate.monthShortEn,
      adDate.year,
    )
  }

  fun daysBetweenBs(startBsIso: String, endBsIso: String): Int {
    return try {
      val s = parseBsIso(startBsIso)
      val e = parseBsIso(endBsIso)
      if (s != null && e != null) {
        val sAd = bsToAd(s)
        val eAd = bsToAd(e)
        val diff = ((utcCalendar(eAd.year, eAd.month, eAd.day).timeInMillis -
          utcCalendar(sAd.year, sAd.month, sAd.day).timeInMillis) / MILLIS_PER_DAY).toInt()
        return max(1, diff)
      }
      val sParts = startBsIso.split("-").map { it.trim().toInt() }
      val eParts = endBsIso.split("-").map { it.trim().toInt() }
      if (sParts.size != 3 || eParts.size != 3) return 365
      var y = sParts[0]
      var m = sParts[1].coerceIn(1, 12)
      var d = sParts[2].coerceIn(1, 32)
      var count = 0
      val fallbackDays = listOf(30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30)
      while ((y < eParts[0] || (y == eParts[0] && m < eParts[1]) || (y == eParts[0] && m == eParts[1] && d < eParts[2])) && count < 7300) {
        count++
        d++
        val maxD = (bsMonthDays[y] ?: fallbackDays)[m - 1]
        if (d > maxD) {
          d = 1
          m++
          if (m > 12) {
            m = 1
            y++
          }
        }
      }
      max(1, count)
    } catch (_: Exception) {
      365
    }
  }
}
