package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserPreferences(
  @DocumentId val id: String = "",
  val userId: String = "",
  val lang: String = "ne",
  val displayCurrency: String = "NPR",
  val hideBalances: Boolean = false,
  val appPin: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val catalogId: String = "cash",
  val customLabel: String = "",
  val balanceNpr: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LedgerTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val refNo: String = "",
  val type: String = "EXPENSE",
  val title: String = "",
  val category: String = "",
  val amountNpr: Double = 0.0,
  val originalAmount: Double = 0.0,
  val originalCurrency: String = "NPR",
  val accountId: String = "",
  val toAccountId: String? = null,
  val prevBalanceNpr: Double = 0.0,
  val newBalanceNpr: Double = 0.0,
  val adTimestamp: String = "",
  val bsDate: String = "",
  val bsYear: Long = 2083L,
  val bsMonth: Long = 8L,
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SavingsGoal(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val targetNpr: Double = 0.0,
  val savedNpr: Double = 0.0,
  val targetBsDate: String = "2084-12-30",
  val frequency: String = "MONTHLY",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class KhataRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = "LEND",
  val partyName: String = "",
  val itemDetail: String = "",
  val amountNpr: Double = 0.0,
  val bsDate: String = "",
  val adTimestamp: String = "",
  val settled: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AutoRecurringRule(
  @DocumentId val id: String = "",
  val userId: String = "",
  val type: String = "EXPENSE",
  val title: String = "",
  val category: String = "",
  val amountNpr: Double = 0.0,
  val accountId: String = "",
  val frequency: String = "MONTHLY",
  val lastRunAdDate: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
