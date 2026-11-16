package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.KhataRecord
import com.example.data.model.UserAccount
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.domain.CurrencyService
import com.example.ui.MeruHisabViewModel
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleKhataDialog(
  record: KhataRecord,
  accounts: List<UserAccount>,
  displayCurrency: String,
  onDismiss: () -> Unit,
  onConfirm: (accountId: String) -> Unit,
) {
  var settleWithAccount by remember { mutableStateOf(accounts.isNotEmpty()) }
  var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "${record.partyName} — भुक्तानी (Mark Paid)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = "रकम: ${CurrencyService.format(record.amountNpr, displayCurrency)} • ${record.itemDetail}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (accounts.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (settleWithAccount) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { settleWithAccount = true }
              .testTag("settle_mode_account"),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = settleWithAccount,
                onClick = { settleWithAccount = true },
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text("बैंक/वालेटमा मिलान र रसिद", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = if (record.type == "LEND") "खातामा रकम जम्मा (+Income) हुन्छ" else "खाताबाट रकम घट्छ (-Expense)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (settleWithAccount) {
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
              val selectedName = accounts.firstOrNull { it.id == selectedAccId }?.customLabel ?: ""
              OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text("खाता छान्नुहोस्") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                  .fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { acc ->
                  DropdownMenuItem(
                    text = { Text(acc.customLabel) },
                    onClick = {
                      selectedAccId = acc.id
                      expanded = false
                    },
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (!settleWithAccount) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { settleWithAccount = false }
            .testTag("settle_mode_status_only"),
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = !settleWithAccount,
              onClick = { settleWithAccount = false },
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Paid स्थिति मात्र चिन्ह लगाउनुहोस्", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = "कुनै बैंक वा वालेट ब्यालेन्स परिवर्तन हुँदैन",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val target = if (settleWithAccount && selectedAccId.isNotEmpty()) {
            selectedAccId
          } else {
            MeruHisabViewModel.STATUS_ONLY_ACCOUNT_ID
          }
          onConfirm(target)
        },
        modifier = Modifier.testTag("confirm_settle_khata_button"),
      ) {
        Text("भुक्तानी (Paid)")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_settle_khata_button"),
      ) {
        Text("रद्द")
      }
    },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditKhataDueReminderDialog(
  record: KhataRecord,
  initialFormat: CalendarFormat,
  onFormatChange: (CalendarFormat) -> Unit,
  onDismiss: () -> Unit,
  onSave: (dueDateMillis: Long?, reminderEnabled: Boolean, notes: String) -> Unit,
) {
  val initialDueMillis = remember(record.id) {
    val resolvedIso = record.resolvedDueAdIso()
    val parsedAd = if (resolvedIso.isNotBlank()) BsDateConverter.parseAdIso(resolvedIso) else null
    if (parsedAd != null) {
      BsDateConverter.adToMillis(parsedAd)
    } else {
      System.currentTimeMillis() + 7L * 86_400_000L
    }
  }
  var dueMillis by remember(record.id) { mutableLongStateOf(initialDueMillis) }
  var reminderEnabled by remember(record.id) {
    mutableStateOf(if (record.hasDueDate()) record.reminderEnabled else true)
  }
  var notes by remember(record.id) { mutableStateOf(record.notes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "${record.partyName} — देय मिति रिमाइन्डर",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          listOf(0 to "आज", 3 to "+३ दिन", 7 to "+७ दिन", 15 to "+१५ दिन", 30 to "+३० दिन").forEach { (days, label) ->
            SuggestionChip(
              onClick = { dueMillis = System.currentTimeMillis() + days.toLong() * 86_400_000L },
              label = { Text(label) },
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        DualCalendarDateField(
          label = "देय मिति (Due Date BS / AD)",
          selectedAdMillis = dueMillis,
          format = initialFormat,
          onFormatChange = onFormatChange,
          onDateSelected = { dueMillis = it },
          dialogTitle = "देय मिति छान्नुहोस्",
          testTag = "edit_due_date_field",
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("रिमाइन्डर अलर्ट चालु", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(
              text = "ड्यासबोर्ड र खाता स्क्रिनमा अलर्ट देखाउनुहोस्",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = reminderEnabled,
            onCheckedChange = { reminderEnabled = it },
            modifier = Modifier.testTag("edit_due_reminder_switch"),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("नोट / सम्झना (ऐच्छिक)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_due_notes_input"),
        )
        if (record.hasDueDate()) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = { onSave(null, false, notes) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("clear_due_date_button"),
          ) {
            Text("देय मिति हटाउनुहोस्")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(dueMillis, reminderEnabled, notes) },
        modifier = Modifier.testTag("save_due_reminder_button"),
      ) {
        Text("सुरक्षित गर्नुहोस्")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_due_reminder_button"),
      ) {
        Text("रद्द")
      }
    },
  )
}

@Composable
fun DeleteKhataConfirmDialog(
  record: KhataRecord,
  displayCurrency: String,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("खाता रेकर्ड मेटाउने?") },
    text = {
      Text(
        "${record.partyName} (${CurrencyService.format(record.amountNpr, displayCurrency)}) को रेकर्ड स्थायी रूपमा मेटाउन चाहनुहुन्छ?",
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmDelete,
        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
        modifier = Modifier.testTag("confirm_delete_khata_button"),
      ) {
        Text("मेटाउनुहोस्")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("रद्द") }
    },
  )
}
