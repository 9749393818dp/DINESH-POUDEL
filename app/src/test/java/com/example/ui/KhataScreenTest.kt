package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.KhataRecord
import com.example.data.model.UserAccount
import com.example.data.model.UserPreferences
import com.example.domain.AdDate
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.ui.screens.KhataTab
import com.example.ui.theme.MeruHisabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KhataScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  private val fixedNowMillis = BsDateConverter.adToMillis(AdDate(2026, 11, 16))

  private val sampleKhatas = listOf(
    KhataRecord(
      id = "k_overdue",
      type = "LEND",
      partyName = "Ram Shrestha",
      itemDetail = "Cash lend",
      amountNpr = 2500.0,
      bsDate = "2083-07-15",
      adTimestamp = "2026-11-01 10:00:00",
      dueBsDate = "2083-07-28",
      dueAdDate = "2026-11-14",
      reminderEnabled = true,
      settled = false,
      notes = "Evening call",
    ),
    KhataRecord(
      id = "k_paid",
      type = "BORROW",
      partyName = "Sita Pasal",
      itemDetail = "Rice & oil",
      amountNpr = 1200.0,
      bsDate = "2083-07-10",
      adTimestamp = "2026-10-27 09:00:00",
      paidBsDate = "2083-07-20",
      paidAdDate = "2026-11-06",
      settled = true,
    ),
  )

  private val sampleState = MeruHisabUiState(
    isLoading = false,
    preferences = UserPreferences(lang = "ne", displayCurrency = "NPR"),
    accounts = listOf(UserAccount(id = "acc1", catalogId = "cash", customLabel = "Cash", balanceNpr = 5000.0)),
    khatas = sampleKhatas,
  )

  @Test
  fun `khata screen displays summary, due alert banner, status filters, and triggers paid pending actions`() {
    var settledId: String? = null
    var settledAcc: String? = null
    var reopenedId: String? = null
    var toggledBellId: String? = null

    composeRule.setContent {
      MeruHisabTheme {
        KhataTab(
          state = sampleState,
          nowMillis = fixedNowMillis,
          calendarFormat = CalendarFormat.BS,
          onCalendarFormatChange = {},
          onLendClick = {},
          onBorrowClick = {},
          onSettleKhata = { k, acc ->
            settledId = k.id
            settledAcc = acc
          },
          onMarkPending = { reopenedId = it.id },
          onToggleReminder = { toggledBellId = it.id },
          onUpdateDueReminder = { _, _, _, _ -> },
          onDeleteKhata = {},
        )
      }
    }

    composeRule.onNodeWithTag("khata_summary_card").assertIsDisplayed()
    composeRule.onNodeWithTag("khata_due_reminder_banner").assertIsDisplayed()
    composeRule.onNode(
      hasText("Ram Shrestha", substring = true) and hasAnyAncestor(hasTestTag("khata_due_reminder_banner"))
    ).assertIsDisplayed()

    // Filter to Paid and mark back Pending
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_search_input"))
    composeRule.onNodeWithTag("khata_status_filter_paid").performScrollTo().performClick()
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_card_k_paid"))
    composeRule.onNodeWithTag("khata_card_k_paid").assertIsDisplayed()
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("reopen_khata_k_paid"))
    composeRule.onNodeWithTag("reopen_khata_k_paid").performClick()
    assertEquals("k_paid", reopenedId)

    // Filter to Due Alerts, toggle bell, and Mark Paid via status-only option
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_search_input"))
    composeRule.onNodeWithTag("khata_status_filter_due").performScrollTo().performClick()
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_card_k_overdue"))
    composeRule.onNodeWithTag("khata_card_k_overdue").assertIsDisplayed()
    composeRule.onNodeWithTag("khata_reminder_bell_k_overdue").performClick()
    assertEquals("k_overdue", toggledBellId)

    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("settle_khata_k_overdue"))
    composeRule.onNodeWithTag("settle_khata_k_overdue").performClick()
    composeRule.onNodeWithTag("settle_mode_status_only").performClick()
    composeRule.onNodeWithTag("confirm_settle_khata_button").performClick()
    assertEquals("k_overdue", settledId)
    assertEquals(MeruHisabViewModel.STATUS_ONLY_ACCOUNT_ID, settledAcc)

    // Search filters by party name
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_search_input"))
    composeRule.onNodeWithTag("khata_status_filter_all").performScrollTo().performClick()
    composeRule.onNodeWithTag("khata_search_input").performTextInput("Sita")
    composeRule.onNodeWithTag("khata_lazy_column").performScrollToNode(hasTestTag("khata_card_k_paid"))
    composeRule.onNodeWithTag("khata_card_k_paid").assertIsDisplayed()
    composeRule.onNodeWithTag("khata_card_k_overdue").assertDoesNotExist()
  }
}
