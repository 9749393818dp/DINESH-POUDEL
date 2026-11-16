package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class BsDateConverterTest {

  @Test
  fun `known BS new year anchor dates convert accurately to AD`() {
    assertEquals(AdDate(2023, 4, 14), BsDateConverter.bsToAd(2080, 1, 1))
    assertEquals(AdDate(2024, 4, 13), BsDateConverter.bsToAd(2081, 1, 1))
    assertEquals(AdDate(2025, 4, 14), BsDateConverter.bsToAd(2082, 1, 1))
    assertEquals(AdDate(2026, 4, 14), BsDateConverter.bsToAd(2083, 1, 1))
    assertEquals(AdDate(2028, 4, 14), BsDateConverter.bsToAd(2085, 1, 1))
    assertEquals(AdDate(2032, 4, 15), BsDateConverter.bsToAd(2089, 1, 1))
    assertEquals(AdDate(2034, 4, 15), BsDateConverter.bsToAd(2090, 12, 30))
  }

  @Test
  fun `round trip conversion is lossless for every day from 2080 to 2090 BS`() {
    var totalDaysChecked = 0
    for (year in BsDateConverter.MIN_BS_YEAR..BsDateConverter.MAX_BS_YEAR) {
      for (month in 1..12) {
        val daysInMonth = BsDateConverter.daysInBsMonth(year, month)
        for (day in 1..daysInMonth) {
          val bs = BsDate(year, month, day)
          val ad = BsDateConverter.bsToAd(bs)
          val roundTrip = BsDateConverter.adToBsDate(ad)
          assertEquals("Round-trip failed for $bs -> $ad", bs, roundTrip)
          totalDaysChecked++
        }
      }
    }
    assertTrue("Expected > 4000 days verified", totalDaysChecked > 4000)
  }

  @Test
  fun `epoch millis round trip is immune to DST timezone transitions`() {
    val losAngelesTz = TimeZone.getTimeZone("America/Los_Angeles")
    val kathmanduTz = TimeZone.getTimeZone("Asia/Kathmandu")

    val testDates = listOf(
      BsDate(2080, 11, 25), // around March US DST spring-forward
      BsDate(2081, 7, 18),  // around November US DST fall-back
      BsDate(2083, 7, 30),
      BsDate(2084, 12, 30),
    )

    for (bs in testDates) {
      val laMillis = BsDateConverter.bsToAdMillis(bs.year, bs.month, bs.day, losAngelesTz)
      val laAd = BsDateConverter.millisToAdDate(laMillis, losAngelesTz)
      assertEquals(bs, BsDateConverter.adToBsDate(laAd))

      val ktmMillis = BsDateConverter.bsToAdMillis(bs.year, bs.month, bs.day, kathmanduTz)
      val ktmAd = BsDateConverter.millisToAdDate(ktmMillis, kathmanduTz)
      assertEquals(bs, BsDateConverter.adToBsDate(ktmAd))
    }
  }

  @Test
  fun `validation rejects out of range and invalid dates`() {
    assertFalse(BsDateConverter.isValidBsDate(2079, 12, 30))
    assertFalse(BsDateConverter.isValidBsDate(2091, 1, 1))
    assertFalse(BsDateConverter.isValidBsDate(2083, 1, 32)) // Baisakh 2083 has 31 days
    assertTrue(BsDateConverter.isValidBsDate(2083, 3, 32))  // Ashadh 2083 has 32 days

    assertNull(BsDateConverter.bsToAdOrNull(2083, 1, 32))
    assertNull(BsDateConverter.parseBsIso("2083-13-01"))
    assertNotNull(BsDateConverter.bsIsoToAd("2083-03-32"))

    var threw = false
    try {
      BsDateConverter.bsToAd(2083, 1, 32)
    } catch (_: IllegalArgumentException) {
      threw = true
    }
    assertTrue(threw)
  }

  @Test
  fun `daysBetweenBs computes exact day count across BS months`() {
    assertEquals(365, BsDateConverter.daysBetweenBs("2080-01-01", "2081-01-01"))
    assertEquals(366, BsDateConverter.daysBetweenBs("2081-01-01", "2082-01-01"))
  }
}
