package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SavingsGoal
import com.example.data.model.UserAccount
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.domain.CategoryData
import com.example.domain.CurrencyService
import com.example.domain.InstitutionCatalog
import com.example.domain.MathEvaluator
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAccountBottomSheet(
  displayCurrency: String,
  onDismiss: () -> Unit,
  onSave: (catalogId: String, customLabel: String, balanceInput: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedGroup by remember { mutableStateOf("A") }
  val filtered = remember(selectedGroup) { InstitutionCatalog.all.filter { it.group == selectedGroup } }
  var selectedCatalogId by remember(selectedGroup) { mutableStateOf(filtered.first().id) }
  var customLabel by remember { mutableStateOf("") }
  var balanceInput by remember { mutableStateOf("0") }
  var instMenuExpanded by remember { mutableStateOf(false) }

  val currentInst = InstitutionCatalog.byId(selectedCatalogId)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = "बैंक / डिजिटल वालेट / नगद थप्नुहोस् (६८ विकल्प)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        listOf(
          "A" to "'क' वाणिज्य (२०)",
          "B" to "'ख' विकास (१७)",
          "C" to "'ग' फाइनान्स (१५)",
          "WALLET" to "डिजिटल वालेट (१५)",
          "CASH" to "नगद (Cash)",
        ).forEach { (group, label) ->
          FilterChip(
            selected = selectedGroup == group,
            onClick = { selectedGroup = group },
            label = { Text(label) },
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      ExposedDropdownMenuBox(
        expanded = instMenuExpanded,
        onExpandedChange = { instMenuExpanded = !instMenuExpanded },
      ) {
        OutlinedTextField(
          value = "${currentInst.code} • ${currentInst.nameNp} (${currentInst.nameEn})",
          onValueChange = {},
          readOnly = true,
          label = { Text("संस्था छान्नुहोस्") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instMenuExpanded) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(
          expanded = instMenuExpanded,
          onDismissRequest = { instMenuExpanded = false },
        ) {
          filtered.forEach { inst ->
            DropdownMenuItem(
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(shape = RoundedCornerShape(4.dp), color = inst.color) {
                    Text(
                      text = inst.code,
                      color = Color.White,
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "${inst.nameNp} (${inst.nameEn})",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              },
              onClick = {
                selectedCatalogId = inst.id
                instMenuExpanded = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = customLabel,
        onValueChange = { customLabel = it },
        label = { Text("खाताको नाम (ऐच्छिक)") },
        placeholder = { Text(currentInst.nameNp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = balanceInput,
        onValueChange = { balanceInput = it },
        label = { Text("सुरुवाती मौज्दात ($displayCurrency)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("add_account_balance_input"),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = {
          onSave(selectedCatalogId, customLabel, balanceInput)
          onDismiss()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_account_button"),
      ) {
        Text("खाता सुरक्षित गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseBottomSheet(
  type: String,
  accounts: List<UserAccount>,
  displayCurrency: String,
  initialFormat: CalendarFormat = CalendarFormat.BS,
  onDismiss: () -> Unit,
  onSubmit: (category: String, amountExpr: String, entryCurrency: String, accountId: String, note: String, dateMillis: Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val groups = if (type == "INCOME") CategoryData.incomeGroups else CategoryData.expenseGroups
  var selectedGroup by remember { mutableStateOf(groups.keys.first()) }
  val catList = remember(selectedGroup) { groups[selectedGroup]!!.map { it.label } }
  var selectedCat by remember(selectedGroup) { mutableStateOf(catList.first()) }
  var selectedAccId by remember { mutableStateOf(accounts.first().id) }
  var entryCurrency by remember { mutableStateOf(displayCurrency) }
  var exprInput by remember { mutableStateOf("") }
  var noteInput by remember { mutableStateOf("") }
  var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var calendarFormat by remember { mutableStateOf(initialFormat) }

  var groupExp by remember { mutableStateOf(false) }
  var catExp by remember { mutableStateOf(false) }
  var currExp by remember { mutableStateOf(false) }
  var accExp by remember { mutableStateOf(false) }

  val calculatedVal = MathEvaluator.evaluate(exprInput)
  val approxNpr = CurrencyService.toNpr(calculatedVal, entryCurrency)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = if (type == "INCOME") "नयाँ आम्दानी थप्नुहोस् (Income)" else "नयाँ खर्च थप्नुहोस् (Expense)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))

      ExposedDropdownMenuBox(expanded = groupExp, onExpandedChange = { groupExp = !groupExp }) {
        OutlinedTextField(
          value = selectedGroup,
          onValueChange = {},
          readOnly = true,
          label = { Text("मुख्य समूह") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExp) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = groupExp, onDismissRequest = { groupExp = false }) {
          groups.keys.forEach { g ->
            DropdownMenuItem(
              text = { Text(g) },
              onClick = {
                selectedGroup = g
                groupExp = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      ExposedDropdownMenuBox(expanded = catExp, onExpandedChange = { catExp = !catExp }) {
        OutlinedTextField(
          value = selectedCat,
          onValueChange = {},
          readOnly = true,
          label = { Text("क्याटेगोरी") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExp) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = catExp, onDismissRequest = { catExp = false }) {
          catList.forEach { c ->
            DropdownMenuItem(
              text = { Text(c) },
              onClick = {
                selectedCat = c
                catExp = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = exprInput,
          onValueChange = { exprInput = it },
          label = { Text("रकम / रसिद क्याल्कुलेटर") },
          placeholder = { Text("उदा: 150+80+45") },
          singleLine = true,
          modifier = Modifier
            .weight(2f)
            .testTag("tx_amount_expr_input"),
        )
        ExposedDropdownMenuBox(
          expanded = currExp,
          onExpandedChange = { currExp = !currExp },
          modifier = Modifier.weight(1f),
        ) {
          OutlinedTextField(
            value = entryCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text("मुद्रा") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currExp) },
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable)
              .fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = currExp, onDismissRequest = { currExp = false }) {
            CurrencyService.currencies.keys.forEach { code ->
              DropdownMenuItem(
                text = { Text(code) },
                onClick = {
                  entryCurrency = code
                  currExp = false
                },
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "गणना गरिएको कुल: $entryCurrency ${CurrencyService.formatRaw(calculatedVal)} (≈ रु. ${CurrencyService.formatRaw(approxNpr)})",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = EmeraldPrimary,
      )

      Spacer(modifier = Modifier.height(8.dp))

      ExposedDropdownMenuBox(expanded = accExp, onExpandedChange = { accExp = !accExp }) {
        val accLabel = accounts.firstOrNull { it.id == selectedAccId }?.customLabel ?: ""
        OutlinedTextField(
          value = accLabel,
          onValueChange = {},
          readOnly = true,
          label = { Text("बैंक / वालेट / नगद") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExp) },
          modifier = Modifier
            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = accExp, onDismissRequest = { accExp = false }) {
          accounts.forEach { acc ->
            DropdownMenuItem(
              text = { Text(acc.customLabel) },
              onClick = {
                selectedAccId = acc.id
                accExp = false
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      DualCalendarDateField(
        label = "कारोबार मिति (Date BS / AD)",
        selectedAdMillis = selectedDateMillis,
        format = calendarFormat,
        onFormatChange = { calendarFormat = it },
        onDateSelected = { selectedDateMillis = it },
        dialogTitle = "कारोबार मिति छान्नुहोस्",
        testTag = "tx_date_picker_field",
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = noteInput,
        onValueChange = { noteInput = it },
        label = { Text("टिप्पणी / पसलको नाम") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tx_note_input"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          if (calculatedVal > 0.0) {
            onSubmit(selectedCat, exprInput, entryCurrency, selectedAccId, noteInput, selectedDateMillis)
            onDismiss()
          }
        },
        enabled = calculatedVal > 0.0,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("tx_submit_button"),
      ) {
        Text("सेभ गर्नुहोस् र Statement निकाल्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBottomSheet(
  accounts: List<UserAccount>,
  displayCurrency: String,
  onDismiss: () -> Unit,
  onSubmit: (fromId: String, toId: String, amount: String, note: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var fromId by remember { mutableStateOf(accounts[0].id) }
  var toId by remember { mutableStateOf(accounts[1].id) }
  var amount by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var fromExp by remember { mutableStateOf(false) }
  var toExp by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text("बैंक / वालेट ट्रान्सफर (Self Transfer)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))

      ExposedDropdownMenuBox(expanded = fromExp, onExpandedChange = { fromExp = !fromExp }) {
        OutlinedTextField(
          value = accounts.firstOrNull { it.id == fromId }?.customLabel ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("पठाउने खाता (From)") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = fromExp, onDismissRequest = { fromExp = false }) {
          accounts.forEach { acc ->
            DropdownMenuItem(text = { Text(acc.customLabel) }, onClick = { fromId = acc.id; fromExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = toExp, onExpandedChange = { toExp = !toExp }) {
        OutlinedTextField(
          value = accounts.firstOrNull { it.id == toId }?.customLabel ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("पाउने खाता (To)") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = toExp, onDismissRequest = { toExp = false }) {
          accounts.forEach { acc ->
            DropdownMenuItem(text = { Text(acc.customLabel) }, onClick = { toId = acc.id; toExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = amount,
        onValueChange = { amount = it },
        label = { Text("रकम ($displayCurrency)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("कैफियत (Note)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          onSubmit(fromId, toId, amount, note)
          onDismiss()
        },
        enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && fromId != toId,
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("ट्रान्सफर पूरा गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalBottomSheet(
  displayCurrency: String,
  initialFormat: CalendarFormat = CalendarFormat.BS,
  onDismiss: () -> Unit,
  onSubmit: (title: String, target: String, targetBsDate: String, frequency: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember { mutableStateOf("") }
  var target by remember { mutableStateOf("") }
  var targetDateMillis by remember { mutableLongStateOf(BsDateConverter.bsToAdMillis(2084, 12, 30)) }
  var calendarFormat by remember { mutableStateOf(initialFormat) }
  var freq by remember { mutableStateOf("MONTHLY") }
  var freqExp by remember { mutableStateOf(false) }

  val freqLabels = mapOf("DAILY" to "दैनिक (Daily)", "WEEKLY" to "साप्ताहिक (Weekly)", "MONTHLY" to "मासिक (Monthly)")

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text("नयाँ बचत लक्ष्य थप्नुहोस्", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("लक्ष्यको नाम (उदा: मोटरसाइकल)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = target,
        onValueChange = { target = it },
        label = { Text("लक्ष्य रकम ($displayCurrency)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      DualCalendarDateField(
        label = "लक्ष्य मिति (Target Date BS / AD)",
        selectedAdMillis = targetDateMillis,
        format = calendarFormat,
        onFormatChange = { calendarFormat = it },
        onDateSelected = { targetDateMillis = it },
        dialogTitle = "लक्ष्य मिति छान्नुहोस्",
        testTag = "goal_date_picker_field",
      )
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = freqExp, onExpandedChange = { freqExp = !freqExp }) {
        OutlinedTextField(
          value = freqLabels[freq] ?: freq,
          onValueChange = {},
          readOnly = true,
          label = { Text("बचत गर्ने तरिका") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = freqExp, onDismissRequest = { freqExp = false }) {
          freqLabels.forEach { (k, v) ->
            DropdownMenuItem(text = { Text(v) }, onClick = { freq = k; freqExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          val bsIso = BsDateConverter.adToBs(targetDateMillis).iso
          onSubmit(title, target, bsIso, freq)
          onDismiss()
        },
        enabled = title.trim().isNotEmpty() && (target.toDoubleOrNull() ?: 0.0) > 0.0,
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("लक्ष्य सुरक्षित गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingDepositBottomSheet(
  goals: List<SavingsGoal>,
  accounts: List<UserAccount>,
  preselectedGoalId: String?,
  displayCurrency: String,
  onDismiss: () -> Unit,
  onSubmit: (goalId: String, accountId: String, amount: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedGoalId by remember { mutableStateOf(preselectedGoalId ?: goals.first().id) }
  var selectedAccId by remember { mutableStateOf(accounts.first().id) }
  var amount by remember { mutableStateOf("") }
  var goalExp by remember { mutableStateOf(false) }
  var accExp by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text("लक्ष्य बचत जम्मा गर्नुहोस् (Saving)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))
      ExposedDropdownMenuBox(expanded = goalExp, onExpandedChange = { goalExp = !goalExp }) {
        OutlinedTextField(
          value = goals.firstOrNull { it.id == selectedGoalId }?.title ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("लक्ष्य छान्नुहोस्") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = goalExp, onDismissRequest = { goalExp = false }) {
          goals.forEach { g ->
            DropdownMenuItem(text = { Text(g.title) }, onClick = { selectedGoalId = g.id; goalExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = accExp, onExpandedChange = { accExp = !accExp }) {
        OutlinedTextField(
          value = accounts.firstOrNull { it.id == selectedAccId }?.customLabel ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("कुन खाताबाट बचत गर्ने?") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = accExp, onDismissRequest = { accExp = false }) {
          accounts.forEach { a ->
            DropdownMenuItem(text = { Text(a.customLabel) }, onClick = { selectedAccId = a.id; accExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = amount,
        onValueChange = { amount = it },
        label = { Text("बचत रकम ($displayCurrency)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          onSubmit(selectedGoalId, selectedAccId, amount)
          onDismiss()
        },
        enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("बचत जम्मा गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataBottomSheet(
  type: String,
  accounts: List<UserAccount>,
  displayCurrency: String,
  initialFormat: CalendarFormat = CalendarFormat.BS,
  onDismiss: () -> Unit,
  onSubmit: (partyName: String, itemDetail: String, amount: String, accountId: String, dateMillis: Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var party by remember { mutableStateOf("") }
  var item by remember { mutableStateOf("") }
  var amount by remember { mutableStateOf("") }
  var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var calendarFormat by remember { mutableStateOf(initialFormat) }
  var selectedAccId by remember {
    mutableStateOf(if (accounts.isNotEmpty()) accounts.first().id else "CREDIT_ONLY")
  }
  var accExp by remember { mutableStateOf(false) }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text(
        text = if (type == "LEND") "कसलाई सापटी दिनुभयो? (Lend)" else "कहाँबाट उधारो सामान / सापटी लिनुभयो? (Borrow)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = party,
        onValueChange = { party = it },
        label = { Text(if (type == "LEND") "व्यक्तिको नाम" else "पसल वा व्यक्तिको नाम") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = item,
        onValueChange = { item = it },
        label = { Text("सामानको विवरण / कारण (उदा: चामल, तेल, सापटी)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = amount,
        onValueChange = { amount = it },
        label = { Text("रकम ($displayCurrency)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      DualCalendarDateField(
        label = "कारोबार मिति (Date BS / AD)",
        selectedAdMillis = selectedDateMillis,
        format = calendarFormat,
        onFormatChange = { calendarFormat = it },
        onDateSelected = { selectedDateMillis = it },
        dialogTitle = "सापटी/उधारो मिति छान्नुहोस्",
        testTag = "khata_date_picker_field",
      )
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = accExp, onExpandedChange = { accExp = !accExp }) {
        val label = if (selectedAccId == "CREDIT_ONLY") {
          "उधारो सामान मात्र (बैंक ब्यालेन्स नबढाउने)"
        } else {
          accounts.firstOrNull { it.id == selectedAccId }?.customLabel ?: ""
        }
        OutlinedTextField(
          value = label,
          onValueChange = {},
          readOnly = true,
          label = { Text("खाता प्रभाव") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = accExp, onDismissRequest = { accExp = false }) {
          if (type == "BORROW") {
            DropdownMenuItem(
              text = { Text("उधारो सामान मात्र (बैंक ब्यालेन्स नबढाउने)") },
              onClick = { selectedAccId = "CREDIT_ONLY"; accExp = false },
            )
          }
          accounts.forEach { acc ->
            DropdownMenuItem(
              text = { Text(acc.customLabel) },
              onClick = { selectedAccId = acc.id; accExp = false },
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          onSubmit(party, item, amount, selectedAccId, selectedDateMillis)
          onDismiss()
        },
        enabled = party.trim().isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("रेकर्ड सुरक्षित गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringBottomSheet(
  accounts: List<UserAccount>,
  onDismiss: () -> Unit,
  onSubmit: (type: String, title: String, amountNpr: String, accountId: String, frequency: String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var type by remember { mutableStateOf("EXPENSE") }
  var freq by remember { mutableStateOf("MONTHLY") }
  var accId by remember { mutableStateOf(accounts.first().id) }
  var title by remember { mutableStateOf("") }
  var amount by remember { mutableStateOf("") }
  var typeExp by remember { mutableStateOf(false) }
  var freqExp by remember { mutableStateOf(false) }
  var accExp by remember { mutableStateOf(false) }

  val typeLabels = mapOf("INCOME" to "आम्दानी (Income)", "EXPENSE" to "खर्च (Expense)")
  val freqLabels = mapOf("DAILY" to "दैनिक (Daily)", "WEEKLY" to "साप्ताहिक (Weekly)", "MONTHLY" to "मासिक (Monthly)")

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .navigationBarsPadding(),
    ) {
      Text("स्वचालित कारोबार थप्नुहोस्", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(10.dp))
      ExposedDropdownMenuBox(expanded = typeExp, onExpandedChange = { typeExp = !typeExp }) {
        OutlinedTextField(
          value = typeLabels[type] ?: type,
          onValueChange = {},
          readOnly = true,
          label = { Text("प्रकार") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = typeExp, onDismissRequest = { typeExp = false }) {
          typeLabels.forEach { (k, v) ->
            DropdownMenuItem(text = { Text(v) }, onClick = { type = k; typeExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("शीर्षक (उदा: कोठा भाडा / तलब)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedTextField(
        value = amount,
        onValueChange = { amount = it },
        label = { Text("रकम (NPR)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = freqExp, onExpandedChange = { freqExp = !freqExp }) {
        OutlinedTextField(
          value = freqLabels[freq] ?: freq,
          onValueChange = {},
          readOnly = true,
          label = { Text("आवृत्ति (Frequency)") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = freqExp, onDismissRequest = { freqExp = false }) {
          freqLabels.forEach { (k, v) ->
            DropdownMenuItem(text = { Text(v) }, onClick = { freq = k; freqExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      ExposedDropdownMenuBox(expanded = accExp, onExpandedChange = { accExp = !accExp }) {
        OutlinedTextField(
          value = accounts.firstOrNull { it.id == accId }?.customLabel ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("खाता") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExp) },
          modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = accExp, onDismissRequest = { accExp = false }) {
          accounts.forEach { acc ->
            DropdownMenuItem(text = { Text(acc.customLabel) }, onClick = { accId = acc.id; accExp = false })
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Button(
        onClick = {
          onSubmit(type, title, amount, accId, freq)
          onDismiss()
        },
        enabled = title.trim().isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        modifier = Modifier.fillMaxWidth().height(50.dp),
      ) {
        Text("नियम सुरक्षित गर्नुहोस्")
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
