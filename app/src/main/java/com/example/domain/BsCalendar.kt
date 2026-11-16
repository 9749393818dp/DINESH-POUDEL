package com.example.domain

data class BsDateInfo(
  val year: Int,
  val month: Int,
  val day: Int,
  val monthName: String,
  val weekday: String,
  val formatted: String,
  val iso: String,
)

object BsCalendar {
  val bsMonthDays: Map<Int, List<Int>>
    get() = BsDateConverter.bsMonthDays

  val nepaliMonths: List<String>
    get() = BsDateConverter.nepaliMonths

  val nepaliDays: List<String>
    get() = BsDateConverter.nepaliDays

  fun toNepaliDigits(input: String): String = BsDateConverter.toNepaliDigits(input)

  fun adToBs(adTimeMillis: Long = System.currentTimeMillis()): BsDateInfo =
    BsDateConverter.adToBs(adTimeMillis)

  fun bsToAd(year: Int, month: Int, day: Int): AdDate =
    BsDateConverter.bsToAd(year, month, day)

  fun daysBetweenBs(startBsIso: String, endBsIso: String): Int =
    BsDateConverter.daysBetweenBs(startBsIso, endBsIso)
}
