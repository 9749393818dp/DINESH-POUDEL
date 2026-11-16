package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AutoRecurringRule
import com.example.data.model.KhataDueUrgency
import com.example.data.model.KhataRecord
import com.example.data.model.LedgerTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.UserAccount
import com.example.data.model.UserPreferences
import com.example.data.repository.MeruHisabRepository
import com.example.domain.BsCalendar
import com.example.domain.BsDateConverter
import com.example.domain.CurrencyService
import com.example.domain.InstitutionCatalog
import com.example.domain.MathEvaluator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class MeruHisabUiState(
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
  val preferences: UserPreferences = UserPreferences(),
  val accounts: List<UserAccount> = emptyList(),
  val transactions: List<LedgerTransaction> = emptyList(),
  val goals: List<SavingsGoal> = emptyList(),
  val khatas: List<KhataRecord> = emptyList(),
  val recurringRules: List<AutoRecurringRule> = emptyList(),
) {
  val totalBalanceNpr: Double
    get() = accounts.sumOf { it.balanceNpr }

  fun currentMonthIncomeNpr(nowMillis: Long): Double {
    val bs = BsCalendar.adToBs(nowMillis)
    return transactions
      .filter { it.type == "INCOME" && it.bsYear == bs.year.toLong() && it.bsMonth == bs.month.toLong() }
      .sumOf { it.amountNpr }
  }

  fun currentMonthExpenseNpr(nowMillis: Long): Double {
    val bs = BsCalendar.adToBs(nowMillis)
    return transactions
      .filter { it.type == "EXPENSE" && it.bsYear == bs.year.toLong() && it.bsMonth == bs.month.toLong() }
      .sumOf { it.amountNpr }
  }

  val pendingLendTotalNpr: Double
    get() = khatas.filter { !it.settled && it.type == "LEND" }.sumOf { it.amountNpr }

  val pendingBorrowTotalNpr: Double
    get() = khatas.filter { !it.settled && it.type == "BORROW" }.sumOf { it.amountNpr }

  fun dueReminderKhatas(todayAdIso: String): List<KhataRecord> {
    return khatas
      .filter { khata ->
        if (khata.settled) return@filter false
        val urgency = khata.dueUrgency(todayAdIso)
        val days = khata.daysUntilDue(todayAdIso) ?: return@filter false
        urgency == KhataDueUrgency.OVERDUE ||
          urgency == KhataDueUrgency.DUE_TODAY ||
          (khata.reminderEnabled && days <= 3)
      }
      .sortedBy { it.daysUntilDue(todayAdIso) ?: Int.MAX_VALUE }
  }
}

class MeruHisabViewModel(
  private val repository: MeruHisabRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
  val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

  private val _pinUnlockedThisSession = MutableStateFlow(false)
  val pinUnlockedThisSession: StateFlow<Boolean> = _pinUnlockedThisSession.asStateFlow()

  private val _latestSlipTx = MutableStateFlow<LedgerTransaction?>(null)
  val latestSlipTx: StateFlow<LedgerTransaction?> = _latestSlipTx.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private var lastCheckedAdDate: String = dateFormatter.format(Date())
  private var recurringCheckInProgress = false

  private fun <T> Flow<T>.safeCatch(defaultValue: T, label: String): Flow<T> = catch { error ->
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error observing $label", rootError)
    _feedbackMessage.value = rootError.localizedMessage ?: "Failed to sync $label"
    emit(defaultValue)
  }

  private val baseStreams = combine(
    repository.observePreferences(currentUserId).safeCatch(UserPreferences(userId = currentUserId), "preferences"),
    repository.observeAccounts(currentUserId).safeCatch(emptyList(), "accounts"),
    repository.observeTransactions(currentUserId).safeCatch(emptyList(), "transactions"),
    repository.observeGoals(currentUserId).safeCatch(emptyList(), "goals"),
    repository.observeKhatas(currentUserId).safeCatch(emptyList(), "khatas"),
  ) { prefs, accounts, txs, goals, khatas ->
    MeruHisabUiState(
      isLoading = false,
      preferences = prefs,
      accounts = accounts,
      transactions = txs,
      goals = goals,
      khatas = khatas,
    )
  }

  val uiState: StateFlow<MeruHisabUiState> = combine(
    baseStreams,
    repository.observeRecurringRules(currentUserId).safeCatch(emptyList(), "recurring_rules"),
  ) { state, rules ->
    state.copy(recurringRules = rules)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = MeruHisabUiState(isLoading = true),
  )

  init {
    viewModelScope.launch {
      while (true) {
        val current = System.currentTimeMillis()
        _nowMillis.value = current
        val todayStr = dateFormatter.format(Date(current))
        if (todayStr != lastCheckedAdDate) {
          lastCheckedAdDate = todayStr
          processAutoRecurring(current)
        }
        delay(1000L)
      }
    }

    viewModelScope.launch {
      uiState.collect { state ->
        if (!state.isLoading && state.recurringRules.isNotEmpty() && state.accounts.isNotEmpty()) {
          processAutoRecurring(_nowMillis.value)
        }
      }
    }
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun showSlip(tx: LedgerTransaction) {
    _latestSlipTx.value = tx
  }

  fun dismissSlip() {
    _latestSlipTx.value = null
  }

  fun unlockPin(enteredPin: String): Boolean {
    val expected = uiState.value.preferences.appPin
    return if (enteredPin == expected) {
      _pinUnlockedThisSession.value = true
      true
    } else {
      false
    }
  }

  fun setLanguage(lang: String) {
    val prefs = uiState.value.preferences
    viewModelScope.launch {
      repository.savePreferences(
        lang = lang,
        displayCurrency = prefs.displayCurrency,
        hideBalances = prefs.hideBalances,
        appPin = prefs.appPin,
      )
    }
  }

  fun setDisplayCurrency(currency: String) {
    val prefs = uiState.value.preferences
    viewModelScope.launch {
      repository.savePreferences(
        lang = prefs.lang,
        displayCurrency = currency,
        hideBalances = prefs.hideBalances,
        appPin = prefs.appPin,
      )
    }
  }

  fun toggleHideBalances() {
    val prefs = uiState.value.preferences
    viewModelScope.launch {
      repository.savePreferences(
        lang = prefs.lang,
        displayCurrency = prefs.displayCurrency,
        hideBalances = !prefs.hideBalances,
        appPin = prefs.appPin,
      )
    }
  }

  fun savePin(pinInput: String) {
    val trimmed = pinInput.trim()
    val validPin = if (trimmed.length == 4 && trimmed.all { it.isDigit() }) trimmed else ""
    val prefs = uiState.value.preferences
    viewModelScope.launch {
      val res = repository.savePreferences(
        lang = prefs.lang,
        displayCurrency = prefs.displayCurrency,
        hideBalances = prefs.hideBalances,
        appPin = validPin,
      )
      if (res.isSuccess) {
        _pinUnlockedThisSession.value = true
        _feedbackMessage.value = if (validPin.isEmpty()) {
          "PIN लक हटाइयो।"
        } else {
          "PIN सेटिङ सुरक्षित गरियो।"
        }
      } else {
        _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save PIN"
      }
    }
  }

  fun addAccount(catalogId: String, customLabelInput: String, initialBalanceInput: String) {
    val info = InstitutionCatalog.byId(catalogId)
    val label = customLabelInput.trim().ifEmpty { info.nameNp }
    val rawAmt = initialBalanceInput.toDoubleOrNull() ?: 0.0
    val displayCurrency = uiState.value.preferences.displayCurrency
    val nprAmt = CurrencyService.toNpr(rawAmt, displayCurrency)
    viewModelScope.launch {
      val res = repository.addAccount(catalogId = catalogId, customLabel = label, balanceNpr = nprAmt)
      if (res.isFailure) {
        _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to add account"
      }
    }
  }

  fun deleteAccount(accountId: String) {
    viewModelScope.launch {
      repository.deleteAccount(accountId)
    }
  }

  fun executeIncomeOrExpense(
    type: String,
    category: String,
    amountExpression: String,
    entryCurrency: String,
    accountId: String,
    note: String,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val amount = MathEvaluator.evaluate(amountExpression)
    if (amount <= 0.0) return
    val sourceAccount = uiState.value.accounts.firstOrNull { it.id == accountId } ?: return
    val title = note.trim().ifEmpty { category }
    viewModelScope.launch {
      val res = repository.executeTransaction(
        type = type,
        title = title,
        category = category,
        amount = amount,
        currency = entryCurrency,
        sourceAccount = sourceAccount,
        note = note.trim(),
        nowMillis = nowMillis,
      )
      res.onSuccess { tx -> _latestSlipTx.value = tx }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Transaction failed" }
    }
  }

  fun executeTransfer(
    fromAccountId: String,
    toAccountId: String,
    amountInput: String,
    note: String,
  ) {
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    if (amount <= 0.0 || fromAccountId == toAccountId) return
    val accounts = uiState.value.accounts
    val fromAcc = accounts.firstOrNull { it.id == fromAccountId } ?: return
    val toAcc = accounts.firstOrNull { it.id == toAccountId } ?: return
    val currency = uiState.value.preferences.displayCurrency
    viewModelScope.launch {
      val res = repository.executeTransaction(
        type = "TRANSFER",
        title = "${fromAcc.customLabel} → ${toAcc.customLabel}",
        category = "🔄 आन्तरिक ट्रान्सफर (Transfer)",
        amount = amount,
        currency = currency,
        sourceAccount = fromAcc,
        destinationAccount = toAcc,
        note = note.trim(),
      )
      res.onSuccess { tx -> _latestSlipTx.value = tx }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Transfer failed" }
    }
  }

  fun addGoal(title: String, targetInput: String, targetBsDate: String, frequency: String) {
    val tAmt = targetInput.toDoubleOrNull() ?: 0.0
    if (title.trim().isEmpty() || tAmt <= 0.0) return
    val currency = uiState.value.preferences.displayCurrency
    val targetNpr = CurrencyService.toNpr(tAmt, currency)
    viewModelScope.launch {
      val res = repository.addGoal(
        title = title.trim(),
        targetNpr = targetNpr,
        targetBsDate = targetBsDate.trim().ifEmpty { "2084-12-30" },
        frequency = frequency,
      )
      if (res.isFailure) {
        _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to create goal"
      }
    }
  }

  fun depositToGoal(goalId: String, accountId: String, amountInput: String) {
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    if (amount <= 0.0) return
    val goal = uiState.value.goals.firstOrNull { it.id == goalId } ?: return
    val account = uiState.value.accounts.firstOrNull { it.id == accountId } ?: return
    val currency = uiState.value.preferences.displayCurrency
    viewModelScope.launch {
      val res = repository.executeTransaction(
        type = "SAVING",
        title = "बचत: ${goal.title}",
        category = "💰 बचत / बीमा (Savings & Insurance)",
        amount = amount,
        currency = currency,
        sourceAccount = account,
        note = "लक्ष्य बचत जम्मा",
        goalToIncrement = goal,
      )
      res.onSuccess { tx -> _latestSlipTx.value = tx }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Saving deposit failed" }
    }
  }

  fun deleteGoal(goalId: String) {
    viewModelScope.launch { repository.deleteGoal(goalId) }
  }

  fun addKhata(
    type: String,
    partyName: String,
    itemDetail: String,
    amountInput: String,
    accountId: String,
    nowMillis: Long = System.currentTimeMillis(),
    dueDateMillis: Long? = null,
    reminderEnabled: Boolean = false,
    notes: String = "",
  ) {
    val amount = amountInput.toDoubleOrNull() ?: 0.0
    if (partyName.trim().isEmpty() || amount <= 0.0) return
    val currency = uiState.value.preferences.displayCurrency
    val nprVal = CurrencyService.toNpr(amount, currency)
    val detail = itemDetail.trim().ifEmpty { "नगद/सामान" }
    val dueAd = dueDateMillis?.let { BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(it)) }
    val dueAdIso = dueAd?.iso ?: ""
    val dueBsIso = dueAd?.let { BsDateConverter.adToBsDate(it).iso } ?: ""

    viewModelScope.launch {
      val addRes = repository.addKhata(
        type = type,
        partyName = partyName.trim(),
        itemDetail = detail,
        amountNpr = nprVal,
        nowMillis = nowMillis,
        dueBsDate = dueBsIso,
        dueAdDate = dueAdIso,
        reminderEnabled = reminderEnabled && dueDateMillis != null,
        notes = notes.trim(),
      )
      if (addRes.isFailure) {
        _feedbackMessage.value = addRes.exceptionOrNull()?.localizedMessage ?: "Failed to save khata"
        return@launch
      }
      if (accountId != "CREDIT_ONLY") {
        val acc = uiState.value.accounts.firstOrNull { it.id == accountId } ?: return@launch
        val txRes = repository.executeTransaction(
          type = type,
          title = if (type == "LEND") "सापटी दिएको: ${partyName.trim()}" else "सापटी/उधारो लिएको: ${partyName.trim()}",
          category = "🤝 सापटी / उधारो (Lending / Udharo)",
          amount = amount,
          currency = currency,
          sourceAccount = acc,
          note = detail,
          nowMillis = nowMillis,
        )
        txRes.onSuccess { tx -> _latestSlipTx.value = tx }
      } else {
        _feedbackMessage.value = "सापटी/उधारो खाता सुरक्षित गरियो।"
      }
    }
  }

  fun settleKhata(khata: KhataRecord, accountId: String) {
    viewModelScope.launch {
      if (accountId == STATUS_ONLY_ACCOUNT_ID) {
        val res = repository.setKhataStatus(khata.id, paid = true)
        res.onSuccess {
          _feedbackMessage.value = "${khata.partyName} को हिसाब भुक्तानी (Paid) चिन्ह लगाइयो।"
        }.onFailure {
          _feedbackMessage.value = it.localizedMessage ?: "Failed to update khata status"
        }
        return@launch
      }
      val acc = uiState.value.accounts.firstOrNull { it.id == accountId } ?: return@launch
      val txType = if (khata.type == "LEND") "INCOME" else "EXPENSE"
      val title = if (khata.type == "LEND") "सापटी फिर्ता: ${khata.partyName}" else "उधारो भुक्तानी: ${khata.partyName}"
      val res = repository.executeTransaction(
        type = txType,
        title = title,
        category = "🤝 सापटी / उधारो (Lending / Udharo)",
        amount = khata.amountNpr,
        currency = "NPR",
        sourceAccount = acc,
        note = khata.itemDetail,
        khataToSettle = khata,
      )
      res.onSuccess { tx -> _latestSlipTx.value = tx }
        .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Failed to settle khata" }
    }
  }

  fun markKhataPending(khata: KhataRecord) {
    viewModelScope.launch {
      val res = repository.setKhataStatus(khata.id, paid = false)
      res.onSuccess {
        _feedbackMessage.value = "${khata.partyName} को हिसाब पुनः बाँकी (Pending) मा राखियो।"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to mark pending"
      }
    }
  }

  fun toggleKhataReminder(khata: KhataRecord) {
    if (!khata.hasDueDate()) return
    val nextEnabled = !khata.reminderEnabled
    viewModelScope.launch {
      val res = repository.updateKhataDueReminder(
        khataId = khata.id,
        dueBsDate = khata.dueBsDate,
        dueAdDate = khata.dueAdDate,
        reminderEnabled = nextEnabled,
        notes = khata.notes,
      )
      res.onSuccess {
        _feedbackMessage.value = if (nextEnabled) {
          "${khata.partyName} को देय मिति रिमाइन्डर चालु गरियो।"
        } else {
          "${khata.partyName} को रिमाइन्डर बन्द गरियो।"
        }
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to update reminder"
      }
    }
  }

  fun updateKhataDueReminder(
    khataId: String,
    dueDateMillis: Long?,
    reminderEnabled: Boolean,
    notes: String,
  ) {
    val dueAd = dueDateMillis?.let { BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(it)) }
    val dueAdIso = dueAd?.iso ?: ""
    val dueBsIso = dueAd?.let { BsDateConverter.adToBsDate(it).iso } ?: ""
    viewModelScope.launch {
      val res = repository.updateKhataDueReminder(
        khataId = khataId,
        dueBsDate = dueBsIso,
        dueAdDate = dueAdIso,
        reminderEnabled = reminderEnabled && dueDateMillis != null,
        notes = notes.trim(),
      )
      res.onSuccess {
        _feedbackMessage.value = if (dueDateMillis != null) {
          "देय मिति रिमाइन्डर अद्यावधिक गरियो।"
        } else {
          "देय मिति रिमाइन्डर हटाइयो।"
        }
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to save reminder"
      }
    }
  }

  fun deleteKhata(khataId: String) {
    viewModelScope.launch {
      val res = repository.deleteKhata(khataId)
      res.onSuccess {
        _feedbackMessage.value = "खाता रेकर्ड मेटाइयो।"
      }.onFailure {
        _feedbackMessage.value = it.localizedMessage ?: "Failed to delete khata"
      }
    }
  }

  fun addRecurringRule(
    type: String,
    title: String,
    amountNprInput: String,
    accountId: String,
    frequency: String,
  ) {
    val amt = amountNprInput.toDoubleOrNull() ?: 0.0
    if (title.trim().isEmpty() || amt <= 0.0) return
    val category = if (type == "INCOME") "💵 मासिक तलब (Monthly Salary)" else "🏠 कोठा भाडा (Room Rent)"
    val todayStr = dateFormatter.format(Date())
    viewModelScope.launch {
      val res = repository.addRecurringRule(
        type = type,
        title = title.trim(),
        category = category,
        amountNpr = amt,
        accountId = accountId,
        frequency = frequency,
        lastRunAdDate = todayStr,
      )
      if (res.isFailure) {
        _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save rule"
      }
    }
  }

  fun runRecurringRuleNow(rule: AutoRecurringRule) {
    val acc = uiState.value.accounts.firstOrNull { it.id == rule.accountId } ?: return
    val todayStr = dateFormatter.format(Date())
    viewModelScope.launch {
      repository.markRecurringRuleExecuted(rule.id, todayStr)
      val res = repository.executeTransaction(
        type = rule.type,
        title = "${rule.title} (Auto)",
        category = rule.category,
        amount = rule.amountNpr,
        currency = "NPR",
        sourceAccount = acc,
        note = "स्वचालित कारोबार (${rule.frequency})",
      )
      res.onSuccess { tx -> _latestSlipTx.value = tx }
    }
  }

  fun deleteRecurringRule(ruleId: String) {
    viewModelScope.launch { repository.deleteRecurringRule(ruleId) }
  }

  private fun processAutoRecurring(nowMillis: Long) {
    if (recurringCheckInProgress) return
    val rules = uiState.value.recurringRules
    val accounts = uiState.value.accounts
    if (rules.isEmpty() || accounts.isEmpty()) return

    val todayStr = dateFormatter.format(Date(nowMillis))
    recurringCheckInProgress = true

    viewModelScope.launch {
      try {
        for (rule in rules) {
          if (rule.lastRunAdDate == todayStr) continue
          val lastDate = try {
            dateFormatter.parse(rule.lastRunAdDate)
          } catch (_: Exception) {
            null
          } ?: continue
          val diffDays = TimeUnit.MILLISECONDS.toDays(nowMillis - lastDate.time)
          val due = when (rule.frequency) {
            "DAILY" -> diffDays >= 1
            "WEEKLY" -> diffDays >= 7
            "MONTHLY" -> diffDays >= 30
            else -> false
          }
          val acc = accounts.firstOrNull { it.id == rule.accountId }
          if (due && acc != null) {
            repository.markRecurringRuleExecuted(rule.id, todayStr)
            val res = repository.executeTransaction(
              type = rule.type,
              title = "${rule.title} (Auto)",
              category = rule.category,
              amount = rule.amountNpr,
              currency = "NPR",
              sourceAccount = acc,
              note = "स्वचालित कारोबार (${rule.frequency})",
              nowMillis = nowMillis,
            )
            res.onSuccess { tx -> _latestSlipTx.value = tx }
          }
        }
      } finally {
        recurringCheckInProgress = false
      }
    }
  }

  companion object {
    const val STATUS_ONLY_ACCOUNT_ID = "STATUS_ONLY"
    private const val TAG = "MeruHisabVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
