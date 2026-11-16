package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KhataDueUrgency
import com.example.data.model.LedgerTransaction
import com.example.data.model.UserAccount
import com.example.domain.AppLocalizations
import com.example.domain.BsDateConverter
import com.example.domain.CurrencyService
import com.example.domain.InstitutionCatalog
import com.example.ui.MeruHisabUiState
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedSoft
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LendOrange
import com.example.ui.theme.MarigoldContainer
import com.example.ui.theme.SavingAmber

@Composable
fun DashboardTab(
  state: MeruHisabUiState,
  nowMillis: Long,
  onToggleHide: () -> Unit,
  onAddAccount: () -> Unit,
  onIncomeClick: () -> Unit,
  onExpenseClick: () -> Unit,
  onTransferClick: () -> Unit,
  onSavingClick: () -> Unit,
  onLendClick: () -> Unit,
  onBorrowClick: () -> Unit,
  onViewAllStatements: () -> Unit,
  onOpenKhataTab: () -> Unit,
  onTransactionClick: (LedgerTransaction) -> Unit,
) {
  val lang = state.preferences.lang
  val currency = state.preferences.displayCurrency
  val hidden = state.preferences.hideBalances
  val totalNpr = state.totalBalanceNpr
  val monthInc = state.currentMonthIncomeNpr(nowMillis)
  val monthExp = state.currentMonthExpenseNpr(nowMillis)
  val todayAdIso = remember(nowMillis / 60_000L) {
    BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(nowMillis)).iso
  }
  val khataDueAlerts = remember(state.khatas, todayAdIso) {
    state.dueReminderKhatas(todayAdIso)
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
    ) {
      // 1. Hero Net Balance Card
      item {
        Surface(
          shape = RoundedCornerShape(22.dp),
          shadowElevation = 6.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier = Modifier
              .background(
                Brush.linearGradient(
                  colors = listOf(EmeraldPrimary, EmeraldSecondary, EmeraldDeep)
                )
              )
              .padding(18.dp),
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = AppLocalizations.t(lang, "total_balance"),
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.80f),
                )
                IconButton(
                  onClick = onToggleHide,
                  modifier = Modifier.testTag("toggle_balance_visibility"),
                ) {
                  Icon(
                    imageVector = if (hidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle balance visibility",
                    tint = Color.White,
                  )
                }
              }

              Text(
                text = CurrencyService.format(totalNpr, currency, hidden),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )

              if (currency != "NPR" && !hidden) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "≈ रु. ${CurrencyService.formatRaw(totalNpr)} NPR",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.78f),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                MetricMiniCard(
                  modifier = Modifier.weight(1f),
                  icon = Icons.Filled.ArrowDownward,
                  iconTint = Color(0xFFB9F6CA),
                  label = AppLocalizations.t(lang, "month_income"),
                  value = CurrencyService.format(monthInc, currency, hidden),
                )
                MetricMiniCard(
                  modifier = Modifier.weight(1f),
                  icon = Icons.Filled.ArrowUpward,
                  iconTint = Color(0xFFFFD180),
                  label = AppLocalizations.t(lang, "month_expense"),
                  value = CurrencyService.format(monthExp, currency, hidden),
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }

      // 2. Quick Actions Horizontal 2-Row Grid
      item {
        Text(
          text = AppLocalizations.t(lang, "quick_actions"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
          val colWidth = ((maxWidth - 12.dp) / 2).coerceIn(145.dp, 220.dp)
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(196.dp),
          ) {
            item {
              Column(
                modifier = Modifier
                  .width(colWidth)
                  .height(196.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_income"),
                  icon = Icons.Filled.ArrowDownward,
                  iconTint = IncomeGreen,
                  testTag = "quick_action_income",
                  onClick = onIncomeClick,
                )
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_transfer"),
                  icon = Icons.Filled.SwapHoriz,
                  iconTint = Color.White,
                  filledGreen = true,
                  testTag = "quick_action_transfer",
                  onClick = onTransferClick,
                )
              }
            }
            item {
              Column(
                modifier = Modifier
                  .width(colWidth)
                  .height(196.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_expense"),
                  icon = Icons.Filled.ArrowUpward,
                  iconTint = ExpenseRed,
                  testTag = "quick_action_expense",
                  onClick = onExpenseClick,
                )
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_lend"),
                  icon = Icons.AutoMirrored.Filled.CallMade,
                  iconTint = LendOrange,
                  testTag = "quick_action_lend",
                  onClick = onLendClick,
                )
              }
            }
            item {
              Column(
                modifier = Modifier
                  .width(colWidth)
                  .height(196.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_saving"),
                  icon = Icons.Filled.Savings,
                  iconTint = SavingAmber,
                  testTag = "quick_action_saving",
                  onClick = onSavingClick,
                )
                QuickActionCard(
                  modifier = Modifier.weight(1f),
                  label = AppLocalizations.t(lang, "action_borrow"),
                  icon = Icons.AutoMirrored.Filled.CallReceived,
                  iconTint = IncomeGreen,
                  testTag = "quick_action_borrow",
                  onClick = onBorrowClick,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      if (khataDueAlerts.isNotEmpty()) {
        item {
          val overdue = khataDueAlerts.count { it.dueUrgency(todayAdIso) == KhataDueUrgency.OVERDUE }
          val bannerBg = if (overdue > 0) ExpenseRedSoft else MarigoldContainer
          val accent = if (overdue > 0) ExpenseRed else SavingAmber
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onOpenKhataTab)
              .testTag("dashboard_khata_reminder_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bannerBg),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = accent)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (overdue > 0) {
                    "सापटी/उधारो देय मिति अलर्ट (${khataDueAlerts.size})"
                  } else {
                    "आगामी भुक्तानी रिमाइन्डर (${khataDueAlerts.size})"
                  },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = accent,
                )
                val top = khataDueAlerts.first()
                Text(
                  text = "${top.partyName} • ${CurrencyService.format(top.amountNpr, currency, hidden)}",
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              TextButton(onClick = onOpenKhataTab) {
                Text("खोल्नुहोस्", color = accent, fontWeight = FontWeight.Bold)
              }
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }
      }

      // 3. My Banks & Wallets
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = AppLocalizations.t(lang, "my_accounts"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
          )
          TextButton(
            onClick = onAddAccount,
            modifier = Modifier.testTag("add_account_header_button"),
          ) {
            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(AppLocalizations.t(lang, "add_account"), maxLines = 1)
          }
        }

        if (state.accounts.isEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = AppLocalizations.t(lang, "no_accounts"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = onAddAccount,
                modifier = Modifier.testTag("add_account_empty_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppLocalizations.t(lang, "add_account"))
              }
            }
          }
        } else {
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            items(state.accounts, key = { it.id }) { acc ->
              AccountCarouselCard(
                account = acc,
                displayCurrency = currency,
                hidden = hidden,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
      }

      // 4. Recent Transactions & Statement
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = AppLocalizations.t(lang, "recent_tx"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          TextButton(
            onClick = onViewAllStatements,
            modifier = Modifier.testTag("view_all_statements_button"),
          ) {
            Text(AppLocalizations.t(lang, "view_all"))
          }
        }
      }

      if (state.transactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = AppLocalizations.t(lang, "no_tx"),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        items(state.transactions.take(6), key = { it.id }) { tx ->
          TransactionRowCard(
            tx = tx,
            accounts = state.accounts,
            displayCurrency = currency,
            hidden = hidden,
            onClick = { onTransactionClick(tx) },
          )
        }
      }

      // 5. Footer Credit
      item {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "meru hisab • ${AppLocalizations.t(lang, "dev_credit")}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        )
      }
    }
  }
}

@Composable
private fun MetricMiniCard(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  iconTint: Color,
  label: String,
  value: String,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = Color.White.copy(alpha = 0.14f),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = Color.White.copy(alpha = 0.80f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun QuickActionCard(
  modifier: Modifier = Modifier,
  label: String,
  icon: ImageVector,
  iconTint: Color,
  filledGreen: Boolean = false,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(20.dp),
    color = if (filledGreen) EmeraldPrimary else MaterialTheme.colorScheme.surface,
    border = BorderStroke(
      1.dp,
      if (filledGreen) EmeraldPrimary else MaterialTheme.colorScheme.outline,
    ),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconTint,
        modifier = Modifier.size(28.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (filledGreen) Color.White else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun AccountCarouselCard(
  account: UserAccount,
  displayCurrency: String,
  hidden: Boolean,
) {
  val info = InstitutionCatalog.byId(account.catalogId)
  Surface(
    modifier = Modifier
      .width(172.dp)
      .height(94.dp),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, info.color.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = info.color,
        ) {
          Text(
            text = info.code,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = account.customLabel,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = CurrencyService.format(account.balanceNpr, displayCurrency, hidden),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = info.color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
