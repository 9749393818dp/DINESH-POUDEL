package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LedgerTransaction
import com.example.data.model.UserAccount
import com.example.domain.BsCalendar
import com.example.domain.CurrencyService
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenSoft
import kotlin.math.max

@Composable
fun TransactionRowCard(
  tx: LedgerTransaction,
  accounts: List<UserAccount>,
  displayCurrency: String,
  hidden: Boolean,
  onClick: () -> Unit,
) {
  val isCredit = tx.type == "INCOME" || tx.type == "BORROW"
  val accountLabel = accounts.firstOrNull { it.id == tx.accountId }?.customLabel ?: "उधारो/अन्य"

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("transaction_card_${tx.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (isCredit) IncomeGreenSoft else ExpenseRedSoft),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
          contentDescription = tx.type,
          tint = if (isCredit) IncomeGreen else ExpenseRed,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${BsCalendar.toNepaliDigits(tx.bsDate)} • $accountLabel",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = tx.adTimestamp,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${if (isCredit) "+" else "-"}${CurrencyService.format(tx.amountNpr, displayCurrency, hidden)}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = if (isCredit) IncomeGreen else ExpenseRed,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Bal: ${CurrencyService.format(tx.newBalanceNpr, displayCurrency, hidden)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun StatementSlipDialog(
  tx: LedgerTransaction,
  accounts: List<UserAccount>,
  displayCurrency: String,
  onDismiss: () -> Unit,
  onViewFullStatement: () -> Unit,
) {
  val accountLabel = accounts.firstOrNull { it.id == tx.accountId }?.customLabel ?: "उधारो खाता"
  val isCredit = tx.type == "INCOME" || tx.type == "BORROW"

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(22.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.ReceiptLong,
          contentDescription = "Receipt",
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "meru hisab - Statement रसिद",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "App Develop by: Dinesh Poudel Poudel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SlipRow("Ref No:", tx.refNo)
        SlipRow("वि.सं. मिति:", BsCalendar.toNepaliDigits(tx.bsDate))
        SlipRow("लाइभ समय (AD):", tx.adTimestamp)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SlipRow("प्रकार:", tx.type)
        SlipRow("विवरण:", tx.title)
        SlipRow("क्याटेगोरी:", tx.category)
        SlipRow("खाता:", accountLabel)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SlipRow("अघिल्लो मौज्दात:", CurrencyService.format(tx.prevBalanceNpr, displayCurrency))
        SlipRow(
          label = "कारोबार रकम:",
          value = "${if (isCredit) "+" else "-"} ${CurrencyService.format(tx.amountNpr, displayCurrency)}",
          valueColor = if (isCredit) IncomeGreen else ExpenseRed,
          bold = true,
        )
        if (tx.originalCurrency != displayCurrency) {
          SlipRow(
            "मूल प्रविष्टि:",
            "${tx.originalCurrency} ${CurrencyService.formatRaw(tx.originalAmount)}",
          )
        }
        SlipRow(
          label = "नयाँ बाँकी मौज्दात:",
          value = CurrencyService.format(tx.newBalanceNpr, displayCurrency),
          bold = true,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier.testTag("slip_ok_button"),
      ) {
        Text("ठीक छ")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onViewFullStatement,
        modifier = Modifier.testTag("slip_view_statement_button"),
      ) {
        Text("पूरा Statement हेर्नुहोस्")
      }
    },
  )
}

@Composable
private fun SlipRow(
  label: String,
  value: String,
  valueColor: Color? = null,
  bold: Boolean = false,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
      color = valueColor ?: MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
    )
  }
}

@Composable
fun IncomeExpenseBarChart(
  income: Double,
  expense: Double,
  displayCurrency: String,
  hidden: Boolean,
) {
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  Column(modifier = Modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
        .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
      val maxVal = max(1.0, max(income, expense))
      val barWidth = size.width * 0.24f
      val maxBarHeight = size.height - 12.dp.toPx()
      val incX = size.width * 0.18f
      val expX = size.width * 0.58f
      val corner = CornerRadius(12.dp.toPx(), 12.dp.toPx())

      drawRoundRect(
        color = trackColor,
        topLeft = Offset(incX, 4.dp.toPx()),
        size = Size(barWidth, maxBarHeight),
        cornerRadius = corner,
      )
      drawRoundRect(
        color = trackColor,
        topLeft = Offset(expX, 4.dp.toPx()),
        size = Size(barWidth, maxBarHeight),
        cornerRadius = corner,
      )

      val incH = ((income / maxVal) * maxBarHeight).toFloat().coerceIn(0f, maxBarHeight)
      val expH = ((expense / maxVal) * maxBarHeight).toFloat().coerceIn(0f, maxBarHeight)

      if (incH > 0f) {
        drawRoundRect(
          color = IncomeGreen,
          topLeft = Offset(incX, 4.dp.toPx() + (maxBarHeight - incH)),
          size = Size(barWidth, incH),
          cornerRadius = corner,
        )
      }
      if (expH > 0f) {
        drawRoundRect(
          color = ExpenseRed,
          topLeft = Offset(expX, 4.dp.toPx() + (maxBarHeight - expH)),
          size = Size(barWidth, expH),
          cornerRadius = corner,
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalArrangement = Arrangement.SpaceAround,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("आम्दानी (Income)", style = MaterialTheme.typography.labelMedium)
        Text(
          text = CurrencyService.format(income, displayCurrency, hidden),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = IncomeGreen,
        )
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("खर्च (Expense)", style = MaterialTheme.typography.labelMedium)
        Text(
          text = CurrencyService.format(expense, displayCurrency, hidden),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = ExpenseRed,
        )
      }
    }
  }
}

@Composable
fun PinLockScreen(
  onVerifyPin: (String) -> Boolean,
) {
  var pinInput by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(EmeraldPrimary)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier
        .widthIn(max = 420.dp)
        .padding(24.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          shape = CircleShape,
          color = EmeraldPrimary.copy(alpha = 0.12f),
          modifier = Modifier.size(64.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = "PIN Lock",
              tint = EmeraldPrimary,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "meru hisab - PIN Lock",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "App Develop by: Dinesh Poudel Poudel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
          value = pinInput,
          onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
          label = { Text("४ अंकको PIN हान्नुहोस्") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pin_unlock_input"),
        )
        if (errorText != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorText!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = {
            if (!onVerifyPin(pinInput)) {
              errorText = "गलत PIN! पुनः प्रयास गर्नुहोस्।"
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("pin_unlock_button"),
        ) {
          Text("खोल्नुहोस् (Unlock)")
        }
      }
    }
  }
}
