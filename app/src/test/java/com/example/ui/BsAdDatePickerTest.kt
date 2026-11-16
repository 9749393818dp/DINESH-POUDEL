package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.domain.AdDate
import com.example.domain.BsDate
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.ui.components.DualCalendarDatePickerDialog
import com.example.ui.theme.MeruHisabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BsAdDatePickerTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun `toggling between BS and AD keeps selected date synchronized and confirms converted values`() {
    // 2083-01-01 BS == 2026-04-14 AD
    val initialMillis = BsDateConverter.bsToAdMillis(2083, 1, 1)
    var confirmedBs: BsDate? = null
    var confirmedAd: AdDate? = null
    var confirmedFormat: CalendarFormat? = null

    composeRule.setContent {
      MeruHisabTheme {
        DualCalendarDatePickerDialog(
          initialAdMillis = initialMillis,
          initialFormat = CalendarFormat.BS,
          onDismiss = {},
          onConfirm = { _, bs, ad, fmt ->
            confirmedBs = bs
            confirmedAd = ad
            confirmedFormat = fmt
          },
        )
      }
    }

    // Initially in BS mode showing 2083 Baisakh 1 and AD equivalent 2026-04-14
    composeRule.onNodeWithTag("date_picker_primary_preview").assertTextContains("२०८३ बैशाख १", substring = true)
    composeRule.onNodeWithTag("date_picker_secondary_preview").assertTextContains("2026-04-14", substring = true)

    // Toggle to AD mode
    composeRule.onNodeWithTag("calendar_toggle_ad").performClick()
    composeRule.onNodeWithTag("date_picker_primary_preview").assertTextContains("Apr 14, 2026", substring = true)
    composeRule.onNodeWithTag("date_picker_secondary_preview").assertTextContains("२०८३-०१-०१", substring = true)

    // Pick day 20 in April 2026 (which is 2083-01-07 BS)
    composeRule.onNodeWithTag("date_picker_day_20")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithTag("date_picker_primary_preview").assertTextContains("Apr 20, 2026", substring = true)
    composeRule.onNodeWithTag("date_picker_secondary_preview").assertTextContains("२०८३-०१-०७", substring = true)

    // Toggle back to BS and verify 2083-01-07 is highlighted
    composeRule.onNodeWithTag("calendar_toggle_bs").performClick()
    composeRule.onNodeWithTag("date_picker_primary_preview").assertTextContains("२०८३ बैशाख ७", substring = true)

    // Confirm selection
    composeRule.onNodeWithTag("date_picker_confirm_button").performClick()
    assertNotNull(confirmedBs)
    assertEquals(BsDate(2083, 1, 7), confirmedBs)
    assertEquals(AdDate(2026, 4, 20), confirmedAd)
    assertEquals(CalendarFormat.BS, confirmedFormat)
  }
}
