package com.example.data.model

import com.example.ui.MeruHisabUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KhataRecordTest {

  @Test
  fun `dueUrgency and daysUntilDue classify overdue, due today, upcoming, and paid records`() {
    val today = "2026-11-16"

    val overdue = KhataRecord(
      id = "k1",
      type = "LEND",
      partyName = "Ram",
      amountNpr = 1500.0,
      dueAdDate = "2026-11-13",
      dueBsDate = "2083-07-27",
      reminderEnabled = true,
      settled = false,
    )
    assertEquals(KhataStatus.PENDING, overdue.status())
    assertEquals(-3, overdue.daysUntilDue(today))
    assertEquals(KhataDueUrgency.OVERDUE, overdue.dueUrgency(today))

    val dueToday = KhataRecord(
      id = "k2",
      type = "BORROW",
      partyName = "Kirana Pasal",
      amountNpr = 800.0,
      dueBsDate = "2083-07-30", // 2026-11-16 AD (resolves via BsDateConverter fallback)
      dueAdDate = "",
      reminderEnabled = true,
      settled = false,
    )
    assertEquals("2026-11-16", dueToday.resolvedDueAdIso())
    assertEquals(0, dueToday.daysUntilDue(today))
    assertEquals(KhataDueUrgency.DUE_TODAY, dueToday.dueUrgency(today))

    val upcoming = KhataRecord(
      id = "k3",
      type = "LEND",
      partyName = "Sita",
      amountNpr = 2000.0,
      dueAdDate = "2026-11-19",
      reminderEnabled = true,
      settled = false,
    )
    assertEquals(3, upcoming.daysUntilDue(today))
    assertEquals(KhataDueUrgency.UPCOMING, upcoming.dueUrgency(today))

    val paid = overdue.copy(id = "k4", settled = true, paidAdDate = today)
    assertEquals(KhataStatus.PAID, paid.status())
    assertNull(paid.daysUntilDue(today))
    assertEquals(KhataDueUrgency.NONE, paid.dueUrgency(today))
  }

  @Test
  fun `uiState pending totals and dueReminderKhatas exclude settled and far future muted items`() {
    val today = "2026-11-16"
    val state = MeruHisabUiState(
      isLoading = false,
      khatas = listOf(
        KhataRecord(id = "1", type = "LEND", partyName = "A", amountNpr = 1000.0, dueAdDate = "2026-11-14", settled = false),
        KhataRecord(id = "2", type = "LEND", partyName = "B", amountNpr = 500.0, settled = true),
        KhataRecord(id = "3", type = "BORROW", partyName = "C", amountNpr = 400.0, dueAdDate = "2026-11-18", reminderEnabled = true, settled = false),
        KhataRecord(id = "4", type = "BORROW", partyName = "D", amountNpr = 300.0, dueAdDate = "2026-12-01", reminderEnabled = true, settled = false),
      ),
    )

    assertEquals(1000.0, state.pendingLendTotalNpr, 0.01)
    assertEquals(700.0, state.pendingBorrowTotalNpr, 0.01)

    val reminders = state.dueReminderKhatas(today)
    assertEquals(listOf("1", "3"), reminders.map { it.id })
    assertTrue(reminders.none { it.id == "2" || it.id == "4" })
  }
}
