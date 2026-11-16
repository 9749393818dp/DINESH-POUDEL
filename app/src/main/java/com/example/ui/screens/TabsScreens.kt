package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AutoRecurringRule
import com.example.data.model.LedgerTransaction
import com.example.data.model.SavingsGoal
import com.example.domain.BsCalendar
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.domain.CurrencyService
import com.example.ui.MeruHisabUiState
import com.example.ui.components.DualCalendarDatePickerDialog
import com.example.ui.components.IncomeExpenseBarChart
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun StatementTab(
  state: MeruHisabUiState,
  nowMillis: Long,
  calendarFormat: CalendarFormat = CalendarFormat.BS,
  onCalendarFormatChange: (CalendarFormat) -> Unit = {},
  onTransactionClick: (LedgerTransaction) -> Unit,
) {
  val currency = state.preferences.displayCurrency
  val hidden = state.preferences.hideBalances
  var filterBsIso by remember { mutableStateOf<String?>(null) }
  var showFilterPicker by remember { mutableStateOf(false) }

  val displayedTransactions = remember(state.transactions, filterBsIso) {
    val target = filterBsIso
    if (target == null) state.transactions else state.transactions.filter { it.bsDate == target }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "आम्दानी बनाम खर्च विश्लेषण (Live Chart)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            IncomeExpenseBarChart(
              income = state.currentMonthIncomeNpr(nowMillis),
              expense = state.currentMonthExpenseNpr(nowMillis),
              displayCurrency = currency,
              hidden = hidden,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "आधिकारिक Statement (${displayedTransactions.size} कारोबार)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          OutlinedButton(
            onClick = { displayedTransactions.firstOrNull()?.let(onTransactionClick) },
            enabled = displayedTransactions.isNotEmpty(),
            modifier = Modifier.testTag("latest_receipt_button"),
          ) {
            Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("पछिल्लो रसिद")
          }
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val activeFilterLabel = filterBsIso?.let { iso ->
            val bs = BsDateConverter.parseBsIso(iso)
            if (bs == null) {
              iso
            } else if (calendarFormat == CalendarFormat.BS) {
              "वि.सं. ${BsDateConverter.toNepaliDigits(bs.iso)}"
            } else {
              "AD ${BsDateConverter.bsToAd(bs).iso}"
            }
          } ?: "मिति फिल्टर (BS / AD)"

          FilterChip(
            selected = filterBsIso != null,
            onClick = { showFilterPicker = true },
            leadingIcon = {
              Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text(activeFilterLabel) },
            modifier = Modifier.testTag("statement_date_filter_chip"),
          )

          if (filterBsIso != null) {
            FilterChip(
              selected = false,
              onClick = { filterBsIso = null },
              leadingIcon = {
                Icon(Icons.Filled.Close, contentDescription = "Clear date filter", modifier = Modifier.size(16.dp))
              },
              label = { Text("सबै") },
              modifier = Modifier.testTag("statement_clear_filter_chip"),
            )
          }

          Spacer(modifier = Modifier.weight(1f))

          FilterChip(
            selected = true,
            onClick = {
              onCalendarFormatChange(
                if (calendarFormat == CalendarFormat.BS) CalendarFormat.AD else CalendarFormat.BS
              )
            },
            leadingIcon = {
              Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text(if (calendarFormat == CalendarFormat.BS) "वि.सं. BS" else "AD ई.सं.") },
            modifier = Modifier.testTag("statement_format_toggle_chip"),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (displayedTransactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (filterBsIso != null) "छानिएको मितिमा कुनै कारोबार छैन।" else "कुनै Statement उपलब्ध छैन।",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        items(displayedTransactions, key = { it.id }) { tx ->
          TransactionRowCard(
            tx = tx,
            accounts = state.accounts,
            displayCurrency = currency,
            hidden = hidden,
            onClick = { onTransactionClick(tx) },
          )
        }
      }
    }
  }

  if (showFilterPicker) {
    val initialMillis = filterBsIso?.let { BsDateConverter.parseBsIso(it) }?.let {
      BsDateConverter.bsToAdMillis(it.year, it.month, it.day)
    } ?: nowMillis
    DualCalendarDatePickerDialog(
      initialAdMillis = initialMillis,
      initialFormat = calendarFormat,
      title = "Statement मिति छान्नुहोस्",
      onDismiss = { showFilterPicker = false },
      onConfirm = { _, bsDate, _, chosenFormat ->
        onCalendarFormatChange(chosenFormat)
        filterBsIso = bsDate.iso
        showFilterPicker = false
      },
    )
  }
}

@Composable
fun GoalsTab(
  state: MeruHisabUiState,
  nowMillis: Long,
  onCreateGoalClick: () -> Unit,
  onQuickCreatePreset: (String, Double, String, String) -> Unit,
  onDepositToGoalClick: (SavingsGoal) -> Unit,
  onDeleteGoal: (String) -> Unit,
) {
  val todayBs = BsCalendar.adToBs(nowMillis).iso
  val currency = state.preferences.displayCurrency

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "लक्ष्य बचत योजना (Target Planner)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = onCreateGoalClick,
            modifier = Modifier.testTag("new_goal_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("नयाँ लक्ष्य")
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (state.goals.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.Savings,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(40.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "बचत लक्ष्य तयार गर्नुहोस् र दैनिक / साप्ताहिक / मासिक दर हेर्नुहोस्।",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                SuggestionChip(
                  onClick = { onQuickCreatePreset("मोटरसाइकल किन्ने (Motorcycle)", 500000.0, "2084-12-30", "MONTHLY") },
                  label = { Text("🏍️ मोटरसाइकल") },
                )
                SuggestionChip(
                  onClick = { onQuickCreatePreset("आपतकालीन कोष (Emergency Fund)", 100000.0, "2084-06-30", "WEEKLY") },
                  label = { Text("🛡️ आपतकालीन कोष") },
                )
              }
            }
          }
        }
      } else {
        items(state.goals, key = { it.id }) { g ->
          val remainingNpr = max(0.0, g.targetNpr - g.savedNpr)
          val daysLeft = BsCalendar.daysBetweenBs(todayBs, g.targetBsDate)
          val weeksLeft = max(1, ceil(daysLeft / 7.0).toInt())
          val monthsLeft = max(1, ceil(daysLeft / 30.0).toInt())
          val dailyNeed = remainingNpr / daysLeft
          val weeklyNeed = remainingNpr / weeksLeft
          val monthlyNeed = remainingNpr / monthsLeft
          val progress = if (g.targetNpr > 0) (g.savedNpr / g.targetNpr).toFloat().coerceIn(0f, 1f) else 0f

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = g.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f),
                )
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = EmeraldContainer,
                ) {
                  Text(
                    text = "वि.सं. ${BsCalendar.toNepaliDigits(g.targetBsDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
                IconButton(onClick = { onDeleteGoal(g.id) }) {
                  Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete goal")
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(8.dp)),
                color = EmeraldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "जम्मा: ${CurrencyService.format(g.savedNpr, currency)}",
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                  text = "लक्ष्य: ${CurrencyService.format(g.targetNpr, currency)}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                )
              }

              HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
              Text(
                text = "एकपटकमा कति बचत गर्ने? (तुलनात्मक दर):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                RateBox(Modifier.weight(1f), "दैनिक (Daily)", dailyNeed, currency, g.frequency == "DAILY")
                RateBox(Modifier.weight(1f), "साप्ताहिक (Weekly)", weeklyNeed, currency, g.frequency == "WEEKLY")
                RateBox(Modifier.weight(1f), "मासिक (Monthly)", monthlyNeed, currency, g.frequency == "MONTHLY")
              }
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = { onDepositToGoalClick(g) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("deposit_goal_${g.id}"),
              ) {
                Icon(Icons.Filled.Savings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ यो लक्ष्यमा बचत जम्मा गर्नुहोस्")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RateBox(
  modifier: Modifier,
  label: String,
  nprAmount: Double,
  displayCurrency: String,
  active: Boolean,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = if (active) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(
      1.dp,
      if (active) EmeraldPrimary else MaterialTheme.colorScheme.outline,
    ),
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = CurrencyService.format(nprAmount, displayCurrency),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
    }
  }
}


@Composable
fun SettingsTab(
  state: MeruHisabUiState,
  userEmail: String?,
  onAddRecurringClick: () -> Unit,
  onRunRuleNow: (AutoRecurringRule) -> Unit,
  onDeleteRule: (String) -> Unit,
  onSavePin: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var pinText by remember(state.preferences.appPin) { mutableStateOf(state.preferences.appPin) }
  val currency = state.preferences.displayCurrency

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("meru hisab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
              Text(
                text = "App Develop by: Dinesh Poudel Poudel\nVersion 1.0.0 • Cloud Sync + Offline Ready",
                style = MaterialTheme.typography.bodyMedium,
              )
              if (!userEmail.isNullOrBlank()) {
                Text(
                  text = "Signed in: $userEmail",
                  style = MaterialTheme.typography.labelMedium,
                  color = EmeraldPrimary,
                )
              }
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "स्वचालित कारोबार नियम (Auto-Recurring & Statement)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "मासिक तलब, कोठा भाडा वा दैनिक/मासिक बचत सेट गर्नुहोस्—दिन परिवर्तन हुनासाथ स्वतः Statement बन्छ।",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = onAddRecurringClick,
              modifier = Modifier.testTag("add_recurring_button"),
            ) {
              Icon(Icons.Filled.Autorenew, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("+ नयाँ स्वचालित कारोबार थप्नुहोस्")
            }

            state.recurringRules.forEach { r ->
              HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("${r.title} (${r.frequency})", fontWeight = FontWeight.SemiBold)
                  Text(
                    text = "${r.type} • ${CurrencyService.format(r.amountNpr, currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(onClick = { onRunRuleNow(r) }) {
                  Icon(Icons.Filled.PlayArrow, contentDescription = "Run now", tint = EmeraldPrimary)
                }
                IconButton(onClick = { onDeleteRule(r.id) }) {
                  Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete rule")
                }
              }
            }
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "एप सुरक्षा (४-अंकको PIN Lock)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = pinText,
              onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinText = it },
              label = { Text("४ अंकको PIN सेट गर्नुहोस् (हटाउन खाली राख्नुहोस्)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_pin_input"),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onSavePin(pinText) },
              modifier = Modifier.testTag("settings_save_pin_button"),
            ) {
              Text("PIN सुरक्षित गर्नुहोस्")
            }
          }
        }
      }

      item {
        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_out_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out")
        }
      }
    }
  }
}
