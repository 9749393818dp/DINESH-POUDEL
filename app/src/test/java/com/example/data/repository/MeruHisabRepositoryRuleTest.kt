package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserAccount
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeruHisabRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addAccountAndExecuteTransaction_authenticatedOwner_persistsAndUpdatesBalance() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = MeruHisabRepository(firestore, auth)

    val accResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addAccount("esewa", "My eSewa", 1000.0)
    }
    assertTrue(accResult.isSuccess)
    val accId = accResult.getOrThrow()

    val accounts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAccounts(uid).first { list -> list.any { it.id == accId } }
    }
    val createdAcc = accounts.first { it.id == accId }
    assertEquals(1000.0, createdAcc.balanceNpr, 0.01)

    val txResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.executeTransaction(
        type = "EXPENSE",
        title = "Vegetables",
        category = "🥬 तरकारी (Vegetables)",
        amount = 250.0,
        currency = "NPR",
        sourceAccount = createdAcc,
        note = "Fresh veggies",
      )
    }
    assertTrue(txResult.isSuccess)

    val updatedAccounts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAccounts(uid).first { list ->
        list.firstOrNull { it.id == accId }?.balanceNpr == 750.0
      }
    }
    assertEquals(750.0, updatedAccounts.first { it.id == accId }.balanceNpr, 0.01)

    val txs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { list -> list.isNotEmpty() }
    }
    assertTrue(txs.any { it.title == "Vegetables" && it.newBalanceNpr == 750.0 })
  }

  @Test
  fun preferencesGoalsKhataAndRecurring_authenticatedOwner_roundTrips() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = MeruHisabRepository(firestore, auth)

    val prefRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.savePreferences("ne", "NPR", false, "4321")
    }
    assertTrue(prefRes.isSuccess)

    val prefs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePreferences(uid).first { it.appPin == "4321" }
    }
    assertEquals("ne", prefs.lang)

    val goalRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addGoal("Motorcycle", 500000.0, "2084-12-30", "MONTHLY")
    }
    assertTrue(goalRes.isSuccess)

    val khataRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addKhata(
        type = "LEND",
        partyName = "Sita",
        itemDetail = "Cash lend",
        amountNpr = 1500.0,
        dueBsDate = "2083-08-25",
        dueAdDate = "2026-12-10",
        reminderEnabled = true,
        notes = "Call before evening",
      )
    }
    assertTrue("addKhata failed: ${khataRes.exceptionOrNull()}", khataRes.isSuccess)
    val khataId = khataRes.getOrThrow()

    val createdKhata = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKhatas(uid).first { list -> list.any { it.id == khataId } }
    }.first { it.id == khataId }
    assertEquals("2083-08-25", createdKhata.dueBsDate)
    assertEquals("2026-12-10", createdKhata.dueAdDate)
    assertTrue(createdKhata.reminderEnabled)
    assertEquals("Call before evening", createdKhata.notes)

    val paidRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.setKhataStatus(khataId, paid = true) }
    assertTrue("setKhataStatus(true) failed: ${paidRes.exceptionOrNull()}", paidRes.isSuccess)
    val paidKhata = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKhatas(uid).first { list -> list.firstOrNull { it.id == khataId }?.settled == true }
    }.first { it.id == khataId }
    assertTrue(paidKhata.paidAdDate.isNotBlank())

    val pendingRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.setKhataStatus(khataId, paid = false) }
    assertTrue("setKhataStatus(false) failed: ${pendingRes.exceptionOrNull()}", pendingRes.isSuccess)
    val pendingKhata = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKhatas(uid).first { list -> list.firstOrNull { it.id == khataId }?.settled == false }
    }.first { it.id == khataId }
    assertEquals("", pendingKhata.paidAdDate)

    val reminderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateKhataDueReminder(
        khataId = khataId,
        dueBsDate = "2083-09-01",
        dueAdDate = "2026-12-16",
        reminderEnabled = false,
        notes = "Extended due date",
      )
    }
    assertTrue("updateKhataDueReminder failed: ${reminderRes.exceptionOrNull()}", reminderRes.isSuccess)
    val updatedReminderKhata = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKhatas(uid).first { list ->
        list.firstOrNull { it.id == khataId }?.dueAdDate == "2026-12-16"
      }
    }.first { it.id == khataId }
    assertEquals(false, updatedReminderKhata.reminderEnabled)
    assertEquals("Extended due date", updatedReminderKhata.notes)

    val accId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addAccount("cash", "Cash", 5000.0).getOrThrow()
    }
    val acc = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeAccounts(uid).first { list -> list.any { it.id == accId } }
    }.first { it.id == accId }

    val settleTxRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.executeTransaction(
        type = "INCOME",
        title = "Sita Return",
        category = "🤝 सापटी / उधारो (Lending / Udharo)",
        amount = 1500.0,
        currency = "NPR",
        sourceAccount = acc,
        khataToSettle = updatedReminderKhata,
      )
    }
    assertTrue("executeTransaction(settle) failed: ${settleTxRes.exceptionOrNull()}", settleTxRes.isSuccess)
    val settledViaTx = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeKhatas(uid).first { list -> list.firstOrNull { it.id == khataId }?.settled == true }
    }.first { it.id == khataId }
    assertTrue(settledViaTx.paidBsDate.isNotBlank())

    val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteKhata(khataId) }
    assertTrue("deleteKhata failed: ${deleteRes.exceptionOrNull()}", deleteRes.isSuccess)

    val ruleRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addRecurringRule(
        type = "EXPENSE",
        title = "Room Rent",
        category = "🏠 कोठा भाडा (Room Rent)",
        amountNpr = 12000.0,
        accountId = accId,
        frequency = "MONTHLY",
        lastRunAdDate = "2026-11-16",
      )
    }
    assertTrue(ruleRes.isSuccess)
  }

  @Test
  fun observeAccounts_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = MeruHisabRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addAccount("nabil", "Alice Nabil", 5000.0).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = MeruHisabRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeAccounts(aliceUid).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull(caught)
    val firestoreException = caught!!.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  @Test
  fun observeAccounts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = MeruHisabRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAccounts("alice_uid").first()
      }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull(caught)
    val firestoreException = caught!!.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_meru@test.com"
    const val BOB_EMAIL = "bob_meru@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
