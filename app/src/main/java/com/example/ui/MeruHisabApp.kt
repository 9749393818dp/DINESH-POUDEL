package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.SavingsGoal
import com.example.data.repository.MeruHisabRepository
import com.example.domain.AppLocalizations
import com.example.domain.BsCalendar
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.domain.CurrencyService
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AddAccountBottomSheet
import com.example.ui.components.AddRecurringBottomSheet
import com.example.ui.components.CreateGoalBottomSheet
import com.example.ui.components.DualCalendarDatePickerDialog
import com.example.ui.components.IncomeExpenseBottomSheet
import com.example.ui.components.KhataBottomSheet
import com.example.ui.components.PinLockScreen
import com.example.ui.components.SavingDepositBottomSheet
import com.example.ui.components.StatementSlipDialog
import com.example.ui.components.TransferBottomSheet
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.GoalsTab
import com.example.ui.screens.KhataTab
import com.example.ui.screens.SettingsTab
import com.example.ui.screens.StatementTab
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MarigoldAccent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun FirebaseAuth.authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation() {
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = { /* AuthStateListener updates currentUser */ })
  } else {
    key(user.uid) {
      MainShellRoute(
        currentUserId = user.uid,
        userEmail = user.email,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShellRoute(
  currentUserId: String,
  userEmail: String?,
) {
  val viewModel: MeruHisabViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        MeruHisabViewModel(MeruHisabRepository(db), currentUserId)
      }
    },
  )

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()
  val pinUnlocked by viewModel.pinUnlockedThisSession.collectAsStateWithLifecycle()
  val latestSlipTx by viewModel.latestSlipTx.collectAsStateWithLifecycle()
  val feedbackMsg by viewModel.feedbackMessage.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(feedbackMsg) {
    if (feedbackMsg != null) {
      snackbarHostState.showSnackbar(feedbackMsg!!)
      viewModel.clearFeedback()
    }
  }

  val hasPin = state.preferences.appPin.length == 4
  if (!state.isLoading && hasPin && !pinUnlocked) {
    PinLockScreen(onVerifyPin = viewModel::unlockPin)
    return
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  var showAddAccountSheet by remember { mutableStateOf(false) }
  var incomeExpenseType by remember { mutableStateOf<String?>(null) }
  var showTransferSheet by remember { mutableStateOf(false) }
  var showCreateGoalSheet by remember { mutableStateOf(false) }
  var savingGoalPreselected by remember { mutableStateOf<SavingsGoal?>(null) }
  var showSavingDepositSheet by remember { mutableStateOf(false) }
  var khataSheetType by remember { mutableStateOf<String?>(null) }
  var showAddRecurringSheet by remember { mutableStateOf(false) }
  var showCalendarConverterDialog by remember { mutableStateOf(false) }
  var calendarFormat by remember { mutableStateOf(CalendarFormat.BS) }

  var currencyMenuExpanded by remember { mutableStateOf(false) }
  var langMenuExpanded by remember { mutableStateOf(false) }

  val lang = state.preferences.lang
  val displayCurrency = state.preferences.displayCurrency
  val bsToday = remember(nowMillis / 60000L) { BsCalendar.adToBs(nowMillis) }
  val adFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
  val adTimeStr = remember(nowMillis / 1000L) { adFormatter.format(Date(nowMillis)) }

  val navItems = listOf(
    Triple(AppLocalizations.t(lang, "tab_home"), Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Triple(AppLocalizations.t(lang, "tab_statement"), Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    Triple(AppLocalizations.t(lang, "tab_goals"), Icons.Filled.Savings, Icons.Outlined.Savings),
    Triple(AppLocalizations.t(lang, "tab_khata"), Icons.Filled.Handshake, Icons.Outlined.Handshake),
    Triple(AppLocalizations.t(lang, "tab_settings"), Icons.Filled.Settings, Icons.Outlined.Settings),
  )

  fun openIncomeOrExpense(type: String) {
    if (state.accounts.isEmpty()) {
      scope.launchSnack(snackbarHostState, "पहिले \"+ बैंक / वालेट थप्नुहोस्\" बाट खाता बनाउनुहोस्।")
      showAddAccountSheet = true
    } else {
      incomeExpenseType = type
    }
  }

  fun openTransfer() {
    if (state.accounts.size < 2) {
      scope.launchSnack(snackbarHostState, "रकम सार्न कम्तीमा २ वटा खाता (जस्तै: बैंक र इसेवा) थप्नुहोस्।")
    } else {
      showTransferSheet = true
    }
  }

  fun openSavingDeposit(goal: SavingsGoal? = null) {
    if (state.accounts.isEmpty() || state.goals.isEmpty()) {
      scope.launchSnack(snackbarHostState, "पहिले खाता र बचत लक्ष्य तयार गर्नुहोस्।")
      if (state.accounts.isEmpty()) showAddAccountSheet = true else showCreateGoalSheet = true
    } else {
      savingGoalPreselected = goal
      showSavingDepositSheet = true
    }
  }

  fun openKhata(type: String) {
    if (type == "LEND" && state.accounts.isEmpty()) {
      scope.launchSnack(snackbarHostState, "सापटी दिन पहिले कम्तीमा एउटा खाता थप्नुहोस्।")
      showAddAccountSheet = true
    } else {
      khataSheetType = type
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = EmeraldPrimary,
          titleContentColor = Color.White,
          actionIconContentColor = Color.White,
        ),
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = AppLocalizations.t(lang, "app_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = AppLocalizations.t(lang, "dev_credit"),
                fontSize = 10.sp,
                color = MarigoldAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = "${bsToday.formatted} | $adTimeStr",
              fontSize = 11.sp,
              color = Color.White.copy(alpha = 0.80f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showCalendarConverterDialog = true },
            modifier = Modifier.testTag("open_calendar_picker_button"),
          ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = "BS / AD Calendar & Date Converter")
          }
          Box {
            TextButton(
              onClick = { currencyMenuExpanded = true },
              modifier = Modifier.testTag("currency_menu_button"),
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.22f),
              ) {
                Text(
                  text = displayCurrency,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
            DropdownMenu(
              expanded = currencyMenuExpanded,
              onDismissRequest = { currencyMenuExpanded = false },
            ) {
              CurrencyService.currencies.values.forEach { c ->
                DropdownMenuItem(
                  text = { Text(c.name) },
                  onClick = {
                    currencyMenuExpanded = false
                    viewModel.setDisplayCurrency(c.code)
                  },
                )
              }
            }
          }

          Box {
            IconButton(
              onClick = { langMenuExpanded = true },
              modifier = Modifier.testTag("language_menu_button"),
            ) {
              Icon(Icons.Filled.Language, contentDescription = "Language")
            }
            DropdownMenu(
              expanded = langMenuExpanded,
              onDismissRequest = { langMenuExpanded = false },
            ) {
              listOf(
                "ne" to "🇳🇵 नेपाली",
                "en" to "🇬🇧 English",
                "hi" to "🇮🇳 हिन्दी",
                "ar" to "🇶🇦 العربية",
              ).forEach { (code, label) ->
                DropdownMenuItem(
                  text = { Text(label) },
                  onClick = {
                    langMenuExpanded = false
                    viewModel.setLanguage(code)
                  },
                )
              }
            }
          }
        },
      )
    },
    bottomBar = {
      BoxWithConstraints {
        if (maxWidth < 720.dp) {
          NavigationBar {
            navItems.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
              NavigationBarItem(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                icon = {
                  Icon(
                    imageVector = if (selectedTab == index) selectedIcon else unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("nav_tab_$index"),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
    ) {
      val useRail = maxWidth >= 720.dp
      Row(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            navItems.forEachIndexed { index, (label, selectedIcon, unselectedIcon) ->
              NavigationRailItem(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                icon = {
                  Icon(
                    imageVector = if (selectedTab == index) selectedIcon else unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label, maxLines = 1) },
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          when (selectedTab) {
            0 -> DashboardTab(
              state = state,
              nowMillis = nowMillis,
              onToggleHide = viewModel::toggleHideBalances,
              onAddAccount = { showAddAccountSheet = true },
              onIncomeClick = { openIncomeOrExpense("INCOME") },
              onExpenseClick = { openIncomeOrExpense("EXPENSE") },
              onTransferClick = { openTransfer() },
              onSavingClick = { openSavingDeposit(null) },
              onLendClick = { openKhata("LEND") },
              onBorrowClick = { openKhata("BORROW") },
              onViewAllStatements = { selectedTab = 1 },
              onTransactionClick = viewModel::showSlip,
            )

            1 -> StatementTab(
              state = state,
              nowMillis = nowMillis,
              calendarFormat = calendarFormat,
              onCalendarFormatChange = { calendarFormat = it },
              onTransactionClick = viewModel::showSlip,
            )

            2 -> GoalsTab(
              state = state,
              nowMillis = nowMillis,
              onCreateGoalClick = { showCreateGoalSheet = true },
              onQuickCreatePreset = { t, amt, dt, freq ->
                viewModel.addGoal(t, amt.toString(), dt, freq)
              },
              onDepositToGoalClick = { goal -> openSavingDeposit(goal) },
              onDeleteGoal = viewModel::deleteGoal,
            )

            3 -> KhataTab(
              state = state,
              onLendClick = { openKhata("LEND") },
              onBorrowClick = { openKhata("BORROW") },
              onSettleKhata = viewModel::settleKhata,
              onNeedAccountWarning = {
                scope.launchSnack(snackbarHostState, "पहिले कम्तीमा एउटा बैंक वा नगद खाता थप्नुहोस्।")
              },
            )

            4 -> SettingsTab(
              state = state,
              userEmail = userEmail,
              onAddRecurringClick = {
                if (state.accounts.isEmpty()) {
                  scope.launchSnack(snackbarHostState, "पहिले कम्तीमा एउटा बैंक वा नगद खाता थप्नुहोस्।")
                } else {
                  showAddRecurringSheet = true
                }
              },
              onRunRuleNow = viewModel::runRecurringRuleNow,
              onDeleteRule = viewModel::deleteRecurringRule,
              onSavePin = viewModel::savePin,
              onSignOut = {
                signOutUser(credentialManager, scope)
              },
            )
          }
        }
      }
    }
  }

  // Modal Sheets & Statement Receipt Slip
  if (showAddAccountSheet) {
    AddAccountBottomSheet(
      displayCurrency = displayCurrency,
      onDismiss = { showAddAccountSheet = false },
      onSave = viewModel::addAccount,
    )
  }

  incomeExpenseType?.let { type ->
    if (state.accounts.isNotEmpty()) {
      IncomeExpenseBottomSheet(
        type = type,
        accounts = state.accounts,
        displayCurrency = displayCurrency,
        initialFormat = calendarFormat,
        onDismiss = { incomeExpenseType = null },
        onSubmit = { cat, expr, curr, accId, note, dateMillis ->
          viewModel.executeIncomeOrExpense(type, cat, expr, curr, accId, note, dateMillis)
        },
      )
    }
  }

  if (showTransferSheet && state.accounts.size >= 2) {
    TransferBottomSheet(
      accounts = state.accounts,
      displayCurrency = displayCurrency,
      onDismiss = { showTransferSheet = false },
      onSubmit = viewModel::executeTransfer,
    )
  }

  if (showCreateGoalSheet) {
    CreateGoalBottomSheet(
      displayCurrency = displayCurrency,
      initialFormat = calendarFormat,
      onDismiss = { showCreateGoalSheet = false },
      onSubmit = viewModel::addGoal,
    )
  }

  if (showSavingDepositSheet && state.goals.isNotEmpty() && state.accounts.isNotEmpty()) {
    SavingDepositBottomSheet(
      goals = state.goals,
      accounts = state.accounts,
      preselectedGoalId = savingGoalPreselected?.id,
      displayCurrency = displayCurrency,
      onDismiss = { showSavingDepositSheet = false },
      onSubmit = viewModel::depositToGoal,
    )
  }

  khataSheetType?.let { type ->
    KhataBottomSheet(
      type = type,
      accounts = state.accounts,
      displayCurrency = displayCurrency,
      initialFormat = calendarFormat,
      onDismiss = { khataSheetType = null },
      onSubmit = { party, item, amt, accId, dateMillis ->
        viewModel.addKhata(type, party, item, amt, accId, dateMillis)
      },
    )
  }

  if (showAddRecurringSheet && state.accounts.isNotEmpty()) {
    AddRecurringBottomSheet(
      accounts = state.accounts,
      onDismiss = { showAddRecurringSheet = false },
      onSubmit = viewModel::addRecurringRule,
    )
  }

  if (showCalendarConverterDialog) {
    DualCalendarDatePickerDialog(
      initialAdMillis = nowMillis,
      initialFormat = calendarFormat,
      title = "वि.सं. ↔ ई.सं. क्यालेन्डर (BS / AD)",
      onDismiss = { showCalendarConverterDialog = false },
      onConfirm = { _, bsDate, adDate, chosenFormat ->
        calendarFormat = chosenFormat
        showCalendarConverterDialog = false
        scope.launchSnack(
          snackbarHostState,
          "छानिएको मिति: ${BsDateConverter.toNepaliDigits(bsDate.iso)} (${bsDate.monthNameNp}) • AD: ${adDate.iso}",
        )
      },
    )
  }

  latestSlipTx?.let { tx ->
    StatementSlipDialog(
      tx = tx,
      accounts = state.accounts,
      displayCurrency = displayCurrency,
      onDismiss = viewModel::dismissSlip,
      onViewFullStatement = {
        viewModel.dismissSlip()
        selectedTab = 1
      },
    )
  }
}

private fun kotlinx.coroutines.CoroutineScope.launchSnack(
  hostState: SnackbarHostState,
  message: String,
) {
  launch {
    hostState.showSnackbar(message)
  }
}
