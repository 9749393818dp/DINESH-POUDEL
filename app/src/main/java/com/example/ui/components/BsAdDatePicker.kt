package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.AdDate
import com.example.domain.BsDate
import com.example.domain.BsDateConverter
import com.example.domain.CalendarFormat
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.MarigoldAccent

@Composable
fun DualCalendarDateField(
  label: String,
  selectedAdMillis: Long,
  format: CalendarFormat,
  onFormatChange: (CalendarFormat) -> Unit,
  onDateSelected: (Long) -> Unit,
  modifier: Modifier = Modifier,
  dialogTitle: String = label,
  testTag: String = "dual_date_field",
) {
  var showDialog by remember { mutableStateOf(false) }

  val adDate = remember(selectedAdMillis) {
    BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(selectedAdMillis))
  }
  val bsDate = remember(adDate) { BsDateConverter.adToBsDate(adDate) }

  val primaryText = when (format) {
    CalendarFormat.BS -> BsDateConverter.formatBsFull(bsDate, useNepaliScript = true)
    CalendarFormat.AD -> BsDateConverter.formatAdFull(adDate)
  }
  val secondaryText = when (format) {
    CalendarFormat.BS -> "AD: ${adDate.iso} (${BsDateConverter.formatAdShort(adDate)})"
    CalendarFormat.AD -> "वि.सं.: ${BsDateConverter.toNepaliDigits(bsDate.iso)} (${BsDateConverter.formatBsShort(bsDate)})"
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { showDialog = true }
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Filled.CalendarMonth,
        contentDescription = label,
        tint = EmeraldPrimary,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = primaryText,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = secondaryText,
          style = MaterialTheme.typography.labelSmall,
          color = EmeraldPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            onFormatChange(if (format == CalendarFormat.BS) CalendarFormat.AD else CalendarFormat.BS)
          }
          .minimumInteractiveComponentSize()
          .testTag("${testTag}_toggle"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.SwapHoriz,
            contentDescription = "Switch BS/AD format",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (format == CalendarFormat.BS) "BS" else "AD",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }
  }

  if (showDialog) {
    DualCalendarDatePickerDialog(
      initialAdMillis = selectedAdMillis,
      initialFormat = format,
      title = dialogTitle,
      onDismiss = { showDialog = false },
      onConfirm = { millis, _, _, chosenFormat ->
        onFormatChange(chosenFormat)
        onDateSelected(millis)
        showDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DualCalendarDatePickerDialog(
  initialAdMillis: Long = System.currentTimeMillis(),
  initialFormat: CalendarFormat = CalendarFormat.BS,
  title: String = "मिति छान्नुहोस् (BS / AD)",
  onDismiss: () -> Unit,
  onConfirm: (selectedAdMillis: Long, bsDate: BsDate, adDate: AdDate, activeFormat: CalendarFormat) -> Unit,
) {
  val todayAd = remember { BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(System.currentTimeMillis())) }
  val todayBs = remember(todayAd) { BsDateConverter.adToBsDate(todayAd) }

  val initialAd = remember(initialAdMillis) {
    BsDateConverter.clampAdDate(BsDateConverter.millisToAdDate(initialAdMillis))
  }
  val initialBs = remember(initialAd) { BsDateConverter.adToBsDate(initialAd) }

  var activeFormat by remember { mutableStateOf(initialFormat) }
  var selectedAd by remember { mutableStateOf(initialAd) }
  val selectedBs = remember(selectedAd) { BsDateConverter.adToBsDate(selectedAd) }

  var viewBsYear by remember { mutableIntStateOf(initialBs.year) }
  var viewBsMonth by remember { mutableIntStateOf(initialBs.month) }
  var viewAdYear by remember { mutableIntStateOf(initialAd.year) }
  var viewAdMonth by remember { mutableIntStateOf(initialAd.month) }
  var showYearMonthPicker by remember { mutableStateOf(false) }

  fun switchFormat(target: CalendarFormat) {
    if (activeFormat == target) return
    activeFormat = target
    showYearMonthPicker = false
    if (target == CalendarFormat.BS) {
      viewBsYear = selectedBs.year
      viewBsMonth = selectedBs.month
    } else {
      viewAdYear = selectedAd.year
      viewAdMonth = selectedAd.month
    }
  }

  fun jumpToToday() {
    selectedAd = todayAd
    viewBsYear = todayBs.year
    viewBsMonth = todayBs.month
    viewAdYear = todayAd.year
    viewAdMonth = todayAd.month
    showYearMonthPicker = false
  }

  val canGoPrev = if (activeFormat == CalendarFormat.BS) {
    !(viewBsYear == BsDateConverter.MIN_BS_YEAR && viewBsMonth == 1)
  } else {
    viewAdYear > BsDateConverter.MIN_AD_DATE.year ||
      (viewAdYear == BsDateConverter.MIN_AD_DATE.year && viewAdMonth > BsDateConverter.MIN_AD_DATE.month)
  }

  val canGoNext = if (activeFormat == CalendarFormat.BS) {
    !(viewBsYear == BsDateConverter.MAX_BS_YEAR && viewBsMonth == 12)
  } else {
    viewAdYear < BsDateConverter.MAX_AD_DATE.year ||
      (viewAdYear == BsDateConverter.MAX_AD_DATE.year && viewAdMonth < BsDateConverter.MAX_AD_DATE.month)
  }

  fun goPrevMonth() {
    if (!canGoPrev) return
    if (activeFormat == CalendarFormat.BS) {
      if (viewBsMonth == 1) {
        viewBsMonth = 12
        viewBsYear--
      } else {
        viewBsMonth--
      }
    } else {
      if (viewAdMonth == 1) {
        viewAdMonth = 12
        viewAdYear--
      } else {
        viewAdMonth--
      }
    }
  }

  fun goNextMonth() {
    if (!canGoNext) return
    if (activeFormat == CalendarFormat.BS) {
      if (viewBsMonth == 12) {
        viewBsMonth = 1
        viewBsYear++
      } else {
        viewBsMonth++
      }
    } else {
      if (viewAdMonth == 12) {
        viewAdMonth = 1
        viewAdYear++
      } else {
        viewAdMonth++
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 420.dp),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      shadowElevation = 12.dp,
    ) {
      Column {
        // 1. Emerald Header with BS/AD Toggle + Dual Preview
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = listOf(EmeraldPrimary, EmeraldSecondary, EmeraldDeep)
              )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.width(8.dp))
              CalendarFormatTogglePill(
                activeFormat = activeFormat,
                onSelect = ::switchFormat,
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
              targetState = activeFormat to selectedAd,
              transitionSpec = {
                (fadeIn(animationSpec = spring())).togetherWith(fadeOut(animationSpec = spring()))
              },
              label = "header_date_preview",
            ) { (fmt, ad) ->
              val bs = BsDateConverter.adToBsDate(ad)
              Column {
                Text(
                  text = if (fmt == CalendarFormat.BS) {
                    BsDateConverter.formatBsFull(bs, useNepaliScript = true)
                  } else {
                    BsDateConverter.formatAdFull(ad)
                  },
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag("date_picker_primary_preview"),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color.White.copy(alpha = 0.16f),
                ) {
                  Text(
                    text = if (fmt == CalendarFormat.BS) {
                      "Gregorian (AD): ${ad.iso} • ${BsDateConverter.formatAdShort(ad)}"
                    } else {
                      "Bikram Sambat (वि.सं.): ${BsDateConverter.toNepaliDigits(bs.iso)} • ${BsDateConverter.formatBsShort(bs)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MarigoldAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                      .testTag("date_picker_secondary_preview"),
                  )
                }
              }
            }
          }
        }

        // 2. Scrollable Month/Year Bar + Calendar Grid
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
        ) {
        val monthTitle = if (activeFormat == CalendarFormat.BS) {
          val mName = BsDateConverter.nepaliMonths[viewBsMonth - 1]
          val mEn = BsDateConverter.bsMonthsEn[viewBsMonth - 1]
          "${BsDateConverter.toNepaliDigits(viewBsYear.toString())} $mName ($mEn)"
        } else {
          "${BsDateConverter.adMonthsEn[viewAdMonth - 1]} $viewAdYear"
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = ::goPrevMonth,
            enabled = canGoPrev && !showYearMonthPicker,
            modifier = Modifier.testTag("date_picker_prev_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
              contentDescription = "Previous month",
            )
          }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (showYearMonthPicker) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable { showYearMonthPicker = !showYearMonthPicker }
              .testTag("date_picker_month_year_toggle"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = if (showYearMonthPicker) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = "Select year or month",
                modifier = Modifier.size(18.dp),
              )
            }
          }

          IconButton(
            onClick = ::goNextMonth,
            enabled = canGoNext && !showYearMonthPicker,
            modifier = Modifier.testTag("date_picker_next_month"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Next month",
            )
          }
        }

        // 3. Calendar Day Grid or Quick Year/Month Picker
        if (showYearMonthPicker) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
          ) {
            Text(
              text = if (activeFormat == CalendarFormat.BS) "वर्ष छान्नुहोस् (Year BS)" else "Select Year (AD)",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              val years = if (activeFormat == CalendarFormat.BS) {
                (BsDateConverter.MIN_BS_YEAR..BsDateConverter.MAX_BS_YEAR).toList()
              } else {
                (BsDateConverter.MIN_AD_DATE.year..BsDateConverter.MAX_AD_DATE.year).toList()
              }
              years.forEach { yr ->
                val isSelectedYr = if (activeFormat == CalendarFormat.BS) yr == viewBsYear else yr == viewAdYear
                FilterChip(
                  selected = isSelectedYr,
                  onClick = {
                    if (activeFormat == CalendarFormat.BS) {
                      viewBsYear = yr
                    } else {
                      viewAdYear = yr
                      if (yr == BsDateConverter.MIN_AD_DATE.year && viewAdMonth < BsDateConverter.MIN_AD_DATE.month) {
                        viewAdMonth = BsDateConverter.MIN_AD_DATE.month
                      } else if (yr == BsDateConverter.MAX_AD_DATE.year && viewAdMonth > BsDateConverter.MAX_AD_DATE.month) {
                        viewAdMonth = BsDateConverter.MAX_AD_DATE.month
                      }
                    }
                  },
                  label = {
                    Text(
                      text = if (activeFormat == CalendarFormat.BS) BsDateConverter.toNepaliDigits(yr.toString()) else yr.toString(),
                    )
                  },
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = if (activeFormat == CalendarFormat.BS) "महिना छान्नुहोस् (Month BS)" else "Select Month (AD)",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              (1..12).forEach { m ->
                val enabled = if (activeFormat == CalendarFormat.BS) {
                  true
                } else {
                  val afterMin = viewAdYear > BsDateConverter.MIN_AD_DATE.year || m >= BsDateConverter.MIN_AD_DATE.month
                  val beforeMax = viewAdYear < BsDateConverter.MAX_AD_DATE.year || m <= BsDateConverter.MAX_AD_DATE.month
                  afterMin && beforeMax
                }
                val isSelectedMonth = if (activeFormat == CalendarFormat.BS) m == viewBsMonth else m == viewAdMonth
                FilterChip(
                  selected = isSelectedMonth,
                  enabled = enabled,
                  onClick = {
                    if (activeFormat == CalendarFormat.BS) {
                      viewBsMonth = m
                    } else {
                      viewAdMonth = m
                    }
                    showYearMonthPicker = false
                  },
                  label = {
                    Text(
                      text = if (activeFormat == CalendarFormat.BS) {
                        BsDateConverter.nepaliMonths[m - 1]
                      } else {
                        BsDateConverter.adMonthsShortEn[m - 1]
                      },
                    )
                  },
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }
        } else {
          // Weekday Header Row
          val weekdayLabels = if (activeFormat == CalendarFormat.BS) {
            BsDateConverter.nepaliDaysShort
          } else {
            BsDateConverter.englishDaysShort
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
          ) {
            weekdayLabels.forEachIndexed { idx, label ->
              Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (idx == 6) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // 7-column calendar cells
          val totalDays = if (activeFormat == CalendarFormat.BS) {
            BsDateConverter.daysInBsMonth(viewBsYear, viewBsMonth)
          } else {
            BsDateConverter.daysInAdMonth(viewAdYear, viewAdMonth)
          }
          val leadingOffset = if (activeFormat == CalendarFormat.BS) {
            BsDateConverter.startWeekdayOfBsMonth(viewBsYear, viewBsMonth)
          } else {
            BsDateConverter.startWeekdayOfAdMonth(viewAdYear, viewAdMonth)
          }
          val totalSlots = leadingOffset + totalDays
          val rows = (totalSlots + 6) / 7

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            for (rowIndex in 0 until rows) {
              Row(modifier = Modifier.fillMaxWidth()) {
                for (colIndex in 0 until 7) {
                  val cellIndex = rowIndex * 7 + colIndex
                  val dayNumber = cellIndex - leadingOffset + 1
                  if (dayNumber < 1 || dayNumber > totalDays) {
                    Spacer(
                      modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.18f),
                    )
                  } else {
                    val isEnabled = if (activeFormat == CalendarFormat.BS) {
                      true
                    } else {
                      BsDateConverter.isValidAdDate(viewAdYear, viewAdMonth, dayNumber)
                    }
                    val isSelected = if (activeFormat == CalendarFormat.BS) {
                      selectedBs.year == viewBsYear && selectedBs.month == viewBsMonth && selectedBs.day == dayNumber
                    } else {
                      selectedAd.year == viewAdYear && selectedAd.month == viewAdMonth && selectedAd.day == dayNumber
                    }
                    val isToday = if (activeFormat == CalendarFormat.BS) {
                      todayBs.year == viewBsYear && todayBs.month == viewBsMonth && todayBs.day == dayNumber
                    } else {
                      todayAd.year == viewAdYear && todayAd.month == viewAdMonth && todayAd.day == dayNumber
                    }
                    val isSaturday = colIndex == 6
                    val dayText = if (activeFormat == CalendarFormat.BS) {
                      BsDateConverter.toNepaliDigits(dayNumber.toString())
                    } else {
                      dayNumber.toString()
                    }

                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.18f)
                        .padding(1.5.dp)
                        .clip(CircleShape)
                        .then(
                          if (isSelected) {
                            Modifier.background(EmeraldPrimary)
                          } else if (isToday) {
                            Modifier.border(1.5.dp, EmeraldPrimary, CircleShape)
                          } else {
                            Modifier
                          }
                        )
                        .clickable(enabled = isEnabled) {
                          selectedAd = if (activeFormat == CalendarFormat.BS) {
                            BsDateConverter.bsToAd(viewBsYear, viewBsMonth, dayNumber)
                          } else {
                            AdDate(viewAdYear, viewAdMonth, dayNumber)
                          }
                        }
                        .testTag("date_picker_day_$dayNumber"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = dayText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                          !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                          isSelected -> Color.White
                          isToday -> EmeraldPrimary
                          isSaturday -> ExpenseRed
                          else -> MaterialTheme.colorScheme.onSurface
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        }
        }

        // 4. Footer Actions
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = ::jumpToToday,
            modifier = Modifier.testTag("date_picker_today_button"),
          ) {
            Text(if (activeFormat == CalendarFormat.BS) "आज (Today)" else "Today")
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("date_picker_cancel_button"),
            ) {
              Text("रद्द")
            }
            Button(
              onClick = {
                val chosenMillis = BsDateConverter.adToMillis(selectedAd)
                onConfirm(chosenMillis, selectedBs, selectedAd, activeFormat)
              },
              modifier = Modifier.testTag("date_picker_confirm_button"),
            ) {
              Text("छान्नुहोस्")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarFormatTogglePill(
  activeFormat: CalendarFormat,
  onSelect: (CalendarFormat) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.White.copy(alpha = 0.18f),
  ) {
    Row(
      modifier = Modifier.padding(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FormatChipOption(
        label = "वि.सं. BS",
        selected = activeFormat == CalendarFormat.BS,
        onClick = { onSelect(CalendarFormat.BS) },
        testTag = "calendar_toggle_bs",
      )
      FormatChipOption(
        label = "AD ई.सं.",
        selected = activeFormat == CalendarFormat.AD,
        onClick = { onSelect(CalendarFormat.AD) },
        testTag = "calendar_toggle_ad",
      )
    }
  }
}

@Composable
private fun FormatChipOption(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(17.dp),
    color = if (selected) Color.White else Color.Transparent,
    modifier = Modifier
      .clip(RoundedCornerShape(17.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = if (selected) EmeraldPrimary else Color.White,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    )
  }
}
