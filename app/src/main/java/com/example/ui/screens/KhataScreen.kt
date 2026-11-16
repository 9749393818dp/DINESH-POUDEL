package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.KhataDueUrgency
import com.example.data.model.KhataRecord
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.domain.CurrencyService
import com.example.ui.MeruHisabUiState
import com.example.ui.components.DeleteKhataConfirmDialog
import com.example.ui.components.EditKhataDueReminderDialog
import com.example.ui.components.SettleKhataDialog
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.LendOrange
import com.example.ui.theme.MarigoldAccent
import com.example.ui.theme.MarigoldContainer
import com.example.ui.theme.SavingAmber
import kotlin.math.abs

enum class KhataStatusFilter {
  ALL,
  PENDING,
  PAID,
  DUE_REMINDERS,
}

enum class KhataTypeFilter {
  ALL,
  LEND,
  BORROW,
}

@Composable
fun KhataTab(
  state: MeruHisabUiState,
  nowMillis: Long = System.currentTimeMillis(),
  calendarFormat: CalendarFormat = CalendarFormat.BS,
  onCalendarFormatChange: (CalendarFormat) -> Unit = {},
  onLendClick: () -> Unit,
  onBorrowClick: () -> Unit,
  onSettleKhata: (KhataRecord, String) -> Unit,
  onMarkPending: (KhataRecord) -> Unit,
  onToggleReminder: (KhataRecord) -> Unit,
  onUpdateDueReminder: (khataId: String, dueDateMillis: Long?, reminderEnabled: Boolean, notes: String) -> Unit,
  onDeleteKhata: (String) -> Unit,
) {
  val currency = state.preferences.displayCurrency
  val hidden = state.preferences.hideBalances
  val todayAdIso = remember(nowMillis / 60_000L) {
    BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(nowMillis)).iso
  }

  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf(KhataStatusFilter.ALL) }
  var typeFilter by remember { mutableStateOf(KhataTypeFilter.ALL) }

  var settlingRecord by remember { mutableStateOf<KhataRecord?>(null) }
  var editingReminderRecord by remember { mutableStateOf<KhataRecord?>(null) }
  var deletingRecord by remember { mutableStateOf<KhataRecord?>(null) }

  val pendingCount = remember(state.khatas) { state.khatas.count { !it.settled } }
  val paidCount = remember(state.khatas) { state.khatas.count { it.settled } }
  val dueAlerts = remember(state.khatas, todayAdIso) { state.dueReminderKhatas(todayAdIso) }
  val overdueCount = remember(dueAlerts, todayAdIso) {
    dueAlerts.count { it.dueUrgency(todayAdIso) == KhataDueUrgency.OVERDUE }
  }

  val filteredKhatas = remember(state.khatas, searchQuery, statusFilter, typeFilter, todayAdIso) {
    val q = searchQuery.trim().lowercase()
    state.khatas
      .filter { rec ->
        val matchesType = when (typeFilter) {
          KhataTypeFilter.ALL -> true
          KhataTypeFilter.LEND -> rec.type == "LEND"
          KhataTypeFilter.BORROW -> rec.type == "BORROW"
        }
        val matchesStatus = when (statusFilter) {
          KhataStatusFilter.ALL -> true
          KhataStatusFilter.PENDING -> !rec.settled
          KhataStatusFilter.PAID -> rec.settled
          KhataStatusFilter.DUE_REMINDERS -> !rec.settled && (rec.hasDueDate() || rec.reminderEnabled)
        }
        val matchesQuery = q.isEmpty() ||
          rec.partyName.lowercase().contains(q) ||
          rec.itemDetail.lowercase().contains(q) ||
          rec.notes.lowercase().contains(q)
        matchesType && matchesStatus && matchesQuery
      }
      .sortedWith(
        compareBy<KhataRecord> { it.settled }
          .thenBy { it.daysUntilDue(todayAdIso) ?: Int.MAX_VALUE }
      )
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize()
        .testTag("khata_lazy_column"),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // 1. Net Lending & Borrowing Summary Hero Card
      item {
        KhataSummaryCard(
          pendingLendNpr = state.pendingLendTotalNpr,
          pendingBorrowNpr = state.pendingBorrowTotalNpr,
          pendingCount = pendingCount,
          paidCount = paidCount,
          displayCurrency = currency,
          hidden = hidden,
        )
      }

      // 2. Due Date Reminders Alert Banner
      if (dueAlerts.isNotEmpty()) {
        item {
          DueReminderBanner(
            alerts = dueAlerts,
            overdueCount = overdueCount,
            todayAdIso = todayAdIso,
            displayCurrency = currency,
            hidden = hidden,
            onFilterDueClick = { statusFilter = KhataStatusFilter.DUE_REMINDERS },
          )
        }
      }

      // 3. Lend & Borrow Action Buttons
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onLendClick,
            colors = ButtonDefaults.buttonColors(containerColor = LendOrange),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("khata_lend_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ सापटी (Lend)", maxLines = 1)
          }
          Button(
            onClick = onBorrowClick,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("khata_borrow_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ उधारो (Borrow)", maxLines = 1)
          }
        }
      }

      // 4. Search & Filters
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("नाम वा विवरण खोज्नुहोस्...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search khata") },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                  }
                }
              },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("khata_search_input"),
            )
            FilterChip(
              selected = true,
              onClick = {
                onCalendarFormatChange(
                  if (calendarFormat == CalendarFormat.BS) CalendarFormat.AD else CalendarFormat.BS
                )
              },
              leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
              label = { Text(if (calendarFormat == CalendarFormat.BS) "BS" else "AD") },
              modifier = Modifier.testTag("khata_format_toggle_chip"),
            )
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              FilterChip(
                selected = statusFilter == KhataStatusFilter.ALL,
                onClick = { statusFilter = KhataStatusFilter.ALL },
                label = { Text("सबै (${state.khatas.size})") },
                modifier = Modifier.testTag("khata_status_filter_all"),
              )
            }
            item {
              FilterChip(
                selected = statusFilter == KhataStatusFilter.PENDING,
                onClick = { statusFilter = KhataStatusFilter.PENDING },
                label = { Text("बाँकी / Pending ($pendingCount)") },
                modifier = Modifier.testTag("khata_status_filter_pending"),
              )
            }
            item {
              FilterChip(
                selected = statusFilter == KhataStatusFilter.PAID,
                onClick = { statusFilter = KhataStatusFilter.PAID },
                label = { Text("भुक्तानी / Paid ($paidCount)") },
                modifier = Modifier.testTag("khata_status_filter_paid"),
              )
            }
            item {
              FilterChip(
                selected = statusFilter == KhataStatusFilter.DUE_REMINDERS,
                onClick = { statusFilter = KhataStatusFilter.DUE_REMINDERS },
                leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("देय अलर्ट (${dueAlerts.size})") },
                modifier = Modifier.testTag("khata_status_filter_due"),
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = typeFilter == KhataTypeFilter.ALL,
              onClick = { typeFilter = KhataTypeFilter.ALL },
              label = { Text("दुवै प्रकार") },
              modifier = Modifier.testTag("khata_type_filter_all"),
            )
            FilterChip(
              selected = typeFilter == KhataTypeFilter.LEND,
              onClick = { typeFilter = KhataTypeFilter.LEND },
              label = { Text("सापटी (Lend)") },
              modifier = Modifier.testTag("khata_type_filter_lend"),
            )
            FilterChip(
              selected = typeFilter == KhataTypeFilter.BORROW,
              onClick = { typeFilter = KhataTypeFilter.BORROW },
              label = { Text("उधारो (Borrow)") },
              modifier = Modifier.testTag("khata_type_filter_borrow"),
            )
          }
        }
      }

      // 5. Khata List or Empty State
      if (filteredKhatas.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.Handshake,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(40.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (state.khatas.isEmpty()) {
                  "अहिलेसम्म कुनै सापटी वा उधारो रेकर्ड छैन।"
                } else {
                  "छानिएको फिल्टरमा कुनै रेकर्ड भेटिएन।"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(filteredKhatas, key = { it.id }) { record ->
          KhataRecordCard(
            record = record,
            todayAdIso = todayAdIso,
            calendarFormat = calendarFormat,
            displayCurrency = currency,
            hidden = hidden,
            onMarkPaidClick = { settlingRecord = record },
            onMarkPendingClick = { onMarkPending(record) },
            onBellClick = {
              if (record.hasDueDate()) {
                onToggleReminder(record)
              } else {
                editingReminderRecord = record
              }
            },
            onEditDueDateClick = { editingReminderRecord = record },
            onDeleteClick = { deletingRecord = record },
          )
        }
      }
    }
  }

  settlingRecord?.let { record ->
    SettleKhataDialog(
      record = record,
      accounts = state.accounts,
      displayCurrency = currency,
      onDismiss = { settlingRecord = null },
      onConfirm = { accId ->
        settlingRecord = null
        onSettleKhata(record, accId)
      },
    )
  }

  editingReminderRecord?.let { record ->
    EditKhataDueReminderDialog(
      record = record,
      initialFormat = calendarFormat,
      onFormatChange = onCalendarFormatChange,
      onDismiss = { editingReminderRecord = null },
      onSave = { dueMillis, enabled, updatedNotes ->
        editingReminderRecord = null
        onUpdateDueReminder(record.id, dueMillis, enabled, updatedNotes)
      },
    )
  }

  deletingRecord?.let { record ->
    DeleteKhataConfirmDialog(
      record = record,
      displayCurrency = currency,
      onDismiss = { deletingRecord = null },
      onConfirmDelete = {
        val targetId = record.id
        deletingRecord = null
        onDeleteKhata(targetId)
      },
    )
  }
}

@Composable
private fun KhataSummaryCard(
  pendingLendNpr: Double,
  pendingBorrowNpr: Double,
  pendingCount: Int,
  paidCount: Int,
  displayCurrency: String,
  hidden: Boolean,
) {
  val netNpr = pendingLendNpr - pendingBorrowNpr
  Surface(
    shape = RoundedCornerShape(20.dp),
    shadowElevation = 4.dp,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("khata_summary_card"),
  ) {
    Box(
      modifier = Modifier
        .background(Brush.linearGradient(colors = listOf(EmeraldPrimary, EmeraldSecondary, EmeraldDeep)))
        .padding(16.dp),
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "सापटी र उधारो खाता (Khata)",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.16f),
          ) {
            Text(
              text = "बाँकी $pendingCount • Paid $paidCount",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          SummaryMiniPill(
            modifier = Modifier.weight(1f),
            label = "पाउनुपर्ने (Lend)",
            value = CurrencyService.format(pendingLendNpr, displayCurrency, hidden),
            valueColor = Color(0xFFB9F6CA),
          )
          SummaryMiniPill(
            modifier = Modifier.weight(1f),
            label = "तिर्नुपर्ने (Borrow)",
            value = CurrencyService.format(pendingBorrowNpr, displayCurrency, hidden),
            valueColor = Color(0xFFFFCCBC),
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = if (netNpr >= 0) {
            "खुद पाउनुपर्ने: ${CurrencyService.format(netNpr, displayCurrency, hidden)}"
          } else {
            "खुद तिर्नुपर्ने: ${CurrencyService.format(abs(netNpr), displayCurrency, hidden)}"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MarigoldAccent,
        )
      }
    }
  }
}

@Composable
private fun SummaryMiniPill(
  modifier: Modifier,
  label: String,
  value: String,
  valueColor: Color,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = Color.White.copy(alpha = 0.14f),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.85f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun DueReminderBanner(
  alerts: List<KhataRecord>,
  overdueCount: Int,
  todayAdIso: String,
  displayCurrency: String,
  hidden: Boolean,
  onFilterDueClick: () -> Unit,
) {
  val isUrgent = overdueCount > 0
  val containerColor = if (isUrgent) ExpenseRedSoft else MarigoldContainer
  val accentColor = if (isUrgent) ExpenseRed else SavingAmber

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("khata_due_reminder_banner"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = accentColor)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isUrgent) {
              "भाखा नाघेको / देय मिति अलर्ट (${alerts.size})"
            } else {
              "आगामी देय मिति रिमाइन्डर (${alerts.size})"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
          )
        }
        TextButton(
          onClick = onFilterDueClick,
          modifier = Modifier.testTag("khata_filter_due_banner_button"),
        ) {
          Text("सबै हेर्नुहोस्", color = accentColor, fontWeight = FontWeight.Bold)
        }
      }
      alerts.take(2).forEach { alert ->
        val days = alert.daysUntilDue(todayAdIso) ?: 0
        val dueLabel = when {
          days < 0 -> "${abs(days)} दिन भाखा नाघ्यो"
          days == 0 -> "आज देय"
          else -> "$days दिन बाँकी"
        }
        Text(
          text = "• ${alert.partyName}: ${CurrencyService.format(alert.amountNpr, displayCurrency, hidden)} ($dueLabel)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KhataRecordCard(
  record: KhataRecord,
  todayAdIso: String,
  calendarFormat: CalendarFormat,
  displayCurrency: String,
  hidden: Boolean,
  onMarkPaidClick: () -> Unit,
  onMarkPendingClick: () -> Unit,
  onBellClick: () -> Unit,
  onEditDueDateClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val isLend = record.type == "LEND"
  val urgency = record.dueUrgency(todayAdIso)
  val daysLeft = record.daysUntilDue(todayAdIso)
  val borderColor = when {
    record.settled -> MaterialTheme.colorScheme.outline
    urgency == KhataDueUrgency.OVERDUE -> ExpenseRed.copy(alpha = 0.65f)
    urgency == KhataDueUrgency.DUE_TODAY -> SavingAmber.copy(alpha = 0.70f)
    else -> MaterialTheme.colorScheme.outline
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("khata_card_${record.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (record.settled) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    ),
    border = BorderStroke(1.dp, borderColor),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FlowRow(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          BadgePill(
            text = if (isLend) "सापटी (LEND)" else "उधारो (BORROW)",
            bgColor = if (isLend) LendOrange.copy(alpha = 0.14f) else EmeraldContainer,
            textColor = if (isLend) LendOrange else EmeraldPrimary,
          )
          BadgePill(
            text = if (record.settled) "Paid • भुक्तानी भयो" else "Pending • बाँकी",
            bgColor = if (record.settled) IncomeGreenSoft else MarigoldContainer,
            textColor = if (record.settled) IncomeGreen else SavingAmber,
          )
          if (!record.settled && daysLeft != null) {
            val (dueText, bg, fg) = when (urgency) {
              KhataDueUrgency.OVERDUE -> Triple("भाखा नाघ्यो (${abs(daysLeft)} दिन)", ExpenseRedSoft, ExpenseRed)
              KhataDueUrgency.DUE_TODAY -> Triple("आज देय मिति (Due Today)", ExpenseRedSoft, ExpenseRed)
              KhataDueUrgency.UPCOMING -> Triple("$daysLeft दिन बाँकी", EmeraldContainer, EmeraldPrimary)
              KhataDueUrgency.NONE -> Triple("", Color.Transparent, Color.Transparent)
            }
            if (dueText.isNotEmpty()) {
              BadgePill(text = dueText, bgColor = bg, textColor = fg)
            }
          }
        }
        if (!record.settled) {
          IconButton(
            onClick = onBellClick,
            modifier = Modifier.testTag("khata_reminder_bell_${record.id}"),
          ) {
            Icon(
              imageVector = if (record.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
              contentDescription = if (record.reminderEnabled) "Disable due reminder" else "Set or enable due reminder",
              tint = if (record.reminderEnabled) SavingAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = record.partyName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textDecoration = if (record.settled) TextDecoration.LineThrough else null,
          )
          Text(
            text = record.itemDetail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = CurrencyService.format(record.amountNpr, displayCurrency, hidden),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (record.settled) MaterialTheme.colorScheme.onSurfaceVariant else if (isLend) IncomeGreen else ExpenseRed,
        )
      }

      if (record.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "नोट: ${record.notes}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "दर्ता मिति: ${formatKhataDate(record.bsDate, record.adTimestamp.take(10), calendarFormat)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (record.hasDueDate()) {
        Text(
          text = "देय मिति: ${formatKhataDate(record.dueBsDate, record.resolvedDueAdIso(), calendarFormat)}",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (!record.settled && urgency == KhataDueUrgency.OVERDUE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (record.settled && (record.paidBsDate.isNotBlank() || record.paidAdDate.isNotBlank())) {
        Text(
          text = "भुक्तानी मिति: ${formatKhataDate(record.paidBsDate, record.paidAdDate, calendarFormat)}",
          style = MaterialTheme.typography.labelSmall,
          color = IncomeGreen,
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (!record.settled) {
          Button(
            onClick = onMarkPaidClick,
            modifier = Modifier
              .weight(1f)
              .testTag("settle_khata_${record.id}"),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark Paid", maxLines = 1)
          }
          OutlinedButton(
            onClick = onEditDueDateClick,
            modifier = Modifier.testTag("edit_due_khata_${record.id}"),
          ) {
            Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (record.hasDueDate()) "देय मिति" else "+ रिमाइन्डर", maxLines = 1)
          }
        } else {
          OutlinedButton(
            onClick = onMarkPendingClick,
            modifier = Modifier
              .weight(1f)
              .testTag("reopen_khata_${record.id}"),
          ) {
            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mark Pending", maxLines = 1)
          }
        }
        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier.testTag("delete_khata_${record.id}"),
        ) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Delete khata record",
            tint = ExpenseRed,
          )
        }
      }
    }
  }
}

@Composable
private fun BadgePill(
  text: String,
  bgColor: Color,
  textColor: Color,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

private fun formatKhataDate(bsIso: String, adIso: String, format: CalendarFormat): String {
  return when (format) {
    CalendarFormat.BS -> {
      val resolvedBs = bsIso.ifBlank {
        BsDateConverter.parseAdIso(adIso)?.let { BsDateConverter.adToBsDate(it).iso } ?: adIso
      }
      val resolvedAd = adIso.ifBlank { BsDateConverter.bsIsoToAd(resolvedBs)?.iso ?: "" }
      val bsLabel = if (resolvedBs.isNotBlank()) "वि.सं. ${BsDateConverter.toNepaliDigits(resolvedBs)}" else ""
      if (resolvedAd.isNotBlank()) "$bsLabel ($resolvedAd)" else bsLabel
    }

    CalendarFormat.AD -> {
      val resolvedAd = adIso.ifBlank { BsDateConverter.bsIsoToAd(bsIso)?.iso ?: bsIso }
      val resolvedBs = bsIso.ifBlank {
        BsDateConverter.parseAdIso(resolvedAd)?.let { BsDateConverter.adToBsDate(it).iso } ?: ""
      }
      if (resolvedBs.isNotBlank()) "AD $resolvedAd (वि.सं. ${BsDateConverter.toNepaliDigits(resolvedBs)})" else "AD $resolvedAd"
    }
  }
}
