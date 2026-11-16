package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AutoRecurringRule
import com.example.data.model.KhataRecord
import com.example.data.model.LedgerTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.UserAccount
import com.example.data.model.UserPreferences
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.domain.BsCalendar
import com.example.domain.CurrencyService
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeruHisabRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)
  private fun accountsCol(uid: String) = userDoc(uid).collection("accounts")
  private fun transactionsCol(uid: String) = userDoc(uid).collection("transactions")
  private fun goalsCol(uid: String) = userDoc(uid).collection("goals")
  private fun khatasCol(uid: String) = userDoc(uid).collection("khatas")
  private fun recurringCol(uid: String) = userDoc(uid).collection("recurring_rules")

  private fun <T> Flow<T>.logFirestoreErrors(
    uid: String,
    operationType: OperationType,
    path: String,
  ): Flow<T> = catch { error ->
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }

  fun observePreferences(userId: String): Flow<UserPreferences> = flow {
    val docRef = userDoc(userId)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserPreferences::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserPreferences(id = userId, userId = userId)
          } else {
            UserPreferences(id = userId, userId = userId)
          }
        }
        .logFirestoreErrors(userId, OperationType.GET, docRef.path)
    )
  }

  fun observeAccounts(userId: String): Flow<List<UserAccount>> = flow {
    val colRef = accountsCol(userId)
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            UserAccount::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeTransactions(userId: String): Flow<List<LedgerTransaction>> = flow {
    val colRef = transactionsCol(userId)
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            LedgerTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeGoals(userId: String): Flow<List<SavingsGoal>> = flow {
    val colRef = goalsCol(userId)
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SavingsGoal::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeKhatas(userId: String): Flow<List<KhataRecord>> = flow {
    val colRef = khatasCol(userId)
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            KhataRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  fun observeRecurringRules(userId: String): Flow<List<AutoRecurringRule>> = flow {
    val colRef = recurringCol(userId)
    emitAll(
      colRef.orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AutoRecurringRule::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        }
        .logFirestoreErrors(userId, OperationType.LIST, colRef.path)
    )
  }

  suspend fun savePreferences(
    lang: String,
    displayCurrency: String,
    hideBalances: Boolean,
    appPin: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    return try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        docRef.update(
          mapOf(
            "lang" to lang,
            "displayCurrency" to displayCurrency,
            "hideBalances" to hideBalances,
            "appPin" to appPin,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.set(
          mapOf(
            "userId" to uid,
            "lang" to lang,
            "displayCurrency" to displayCurrency,
            "hideBalances" to hideBalances,
            "appPin" to appPin,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addAccount(
    catalogId: String,
    customLabel: String,
    balanceNpr: Double,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = accountsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "catalogId" to catalogId,
      "customLabel" to customLabel.take(100),
      "balanceNpr" to balanceNpr,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAccount(accountId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = accountsCol(uid).document(accountId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun executeTransaction(
    type: String,
    title: String,
    category: String,
    amount: Double,
    currency: String,
    sourceAccount: UserAccount,
    destinationAccount: UserAccount? = null,
    note: String = "",
    goalToIncrement: SavingsGoal? = null,
    khataToSettle: KhataRecord? = null,
    nowMillis: Long = System.currentTimeMillis(),
  ): Result<LedgerTransaction> {
    val uid = requireUserId()
    val bs = BsCalendar.adToBs(nowMillis)
    val amountNpr = CurrencyService.toNpr(amount, currency)
    val nowDate = Date(nowMillis)
    val refNo = "MH-${SimpleDateFormat("yyMMddHHmmss", Locale.US).format(nowDate)}"
    val adStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(nowDate)

    val prevBal = sourceAccount.balanceNpr
    var newBal = prevBal

    when (type) {
      "INCOME", "BORROW" -> newBal = prevBal + amountNpr
      "EXPENSE", "SAVING", "LEND", "TRANSFER" -> newBal = prevBal - amountNpr
    }

    val txRef = transactionsCol(uid).document()
    val txPayload = mutableMapOf<String, Any>(
      "userId" to uid,
      "refNo" to refNo,
      "type" to type,
      "title" to title.take(160),
      "category" to category.take(120),
      "amountNpr" to amountNpr,
      "originalAmount" to amount,
      "originalCurrency" to currency,
      "accountId" to sourceAccount.id,
      "prevBalanceNpr" to prevBal,
      "newBalanceNpr" to newBal,
      "adTimestamp" to adStamp,
      "bsDate" to bs.iso,
      "bsYear" to bs.year.toLong(),
      "bsMonth" to bs.month.toLong(),
      "note" to note.take(500),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    if (destinationAccount != null) {
      txPayload["toAccountId"] = destinationAccount.id
    }

    return try {
      db.runBatch { batch ->
        batch.set(txRef, txPayload)
        if (sourceAccount.id != "CREDIT_ONLY") {
          batch.update(
            accountsCol(uid).document(sourceAccount.id),
            mapOf(
              "balanceNpr" to newBal,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (type == "TRANSFER" && destinationAccount != null) {
          batch.update(
            accountsCol(uid).document(destinationAccount.id),
            mapOf(
              "balanceNpr" to (destinationAccount.balanceNpr + amountNpr),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (goalToIncrement != null) {
          batch.update(
            goalsCol(uid).document(goalToIncrement.id),
            mapOf(
              "savedNpr" to (goalToIncrement.savedNpr + amountNpr),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (khataToSettle != null) {
          val paidAdIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nowDate)
          batch.update(
            khatasCol(uid).document(khataToSettle.id),
            mapOf(
              "settled" to true,
              "paidBsDate" to bs.iso,
              "paidAdDate" to paidAdIso,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()

      val createdTx = LedgerTransaction(
        id = txRef.id,
        userId = uid,
        refNo = refNo,
        type = type,
        title = title,
        category = category,
        amountNpr = amountNpr,
        originalAmount = amount,
        originalCurrency = currency,
        accountId = sourceAccount.id,
        toAccountId = destinationAccount?.id,
        prevBalanceNpr = prevBal,
        newBalanceNpr = newBal,
        adTimestamp = adStamp,
        bsDate = bs.iso,
        bsYear = bs.year.toLong(),
        bsMonth = bs.month.toLong(),
        note = note,
        createdAt = Timestamp.now(),
      )
      Result.success(createdTx)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      Result.failure(e)
    }
  }

  suspend fun addGoal(
    title: String,
    targetNpr: Double,
    targetBsDate: String,
    frequency: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = goalsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "title" to title.take(120),
      "targetNpr" to targetNpr,
      "savedNpr" to 0.0,
      "targetBsDate" to targetBsDate.take(20),
      "frequency" to frequency,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteGoal(goalId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = goalsCol(uid).document(goalId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addKhata(
    type: String,
    partyName: String,
    itemDetail: String,
    amountNpr: Double,
    nowMillis: Long = System.currentTimeMillis(),
    dueBsDate: String = "",
    dueAdDate: String = "",
    reminderEnabled: Boolean = false,
    notes: String = "",
  ): Result<String> {
    val uid = requireUserId()
    val bs = BsCalendar.adToBs(nowMillis)
    val adStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(nowMillis))
    val docRef = khatasCol(uid).document()
    val payload = hashMapOf<String, Any>(
      "userId" to uid,
      "type" to type,
      "partyName" to partyName.take(120),
      "itemDetail" to itemDetail.take(200),
      "amountNpr" to amountNpr,
      "bsDate" to bs.iso,
      "adTimestamp" to adStamp,
      "settled" to false,
      "dueBsDate" to dueBsDate.take(20),
      "dueAdDate" to dueAdDate.take(20),
      "reminderEnabled" to reminderEnabled,
      "paidBsDate" to "",
      "paidAdDate" to "",
      "notes" to notes.take(300),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun setKhataStatus(
    khataId: String,
    paid: Boolean,
    nowMillis: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = khatasCol(uid).document(khataId)
    val paidBs = if (paid) BsCalendar.adToBs(nowMillis).iso else ""
    val paidAd = if (paid) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis)) else ""
    return try {
      docRef.update(
        mapOf(
          "settled" to paid,
          "paidBsDate" to paidBs,
          "paidAdDate" to paidAd,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateKhataDueReminder(
    khataId: String,
    dueBsDate: String,
    dueAdDate: String,
    reminderEnabled: Boolean,
    notes: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = khatasCol(uid).document(khataId)
    return try {
      docRef.update(
        mapOf(
          "dueBsDate" to dueBsDate.take(20),
          "dueAdDate" to dueAdDate.take(20),
          "reminderEnabled" to reminderEnabled,
          "notes" to notes.take(300),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteKhata(khataId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = khatasCol(uid).document(khataId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addRecurringRule(
    type: String,
    title: String,
    category: String,
    amountNpr: Double,
    accountId: String,
    frequency: String,
    lastRunAdDate: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = recurringCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "type" to type,
      "title" to title.take(120),
      "category" to category.take(120),
      "amountNpr" to amountNpr,
      "accountId" to accountId,
      "frequency" to frequency,
      "lastRunAdDate" to lastRunAdDate,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markRecurringRuleExecuted(
    ruleId: String,
    lastRunAdDate: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = recurringCol(uid).document(ruleId)
    return try {
      docRef.update(
        mapOf(
          "lastRunAdDate" to lastRunAdDate,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteRecurringRule(ruleId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = recurringCol(uid).document(ruleId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }
}
