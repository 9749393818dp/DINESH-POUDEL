package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
  val code: String,
  val symbol: String,
  val name: String,
  val rateFromNpr: Double,
)

object CurrencyService {
  val currencies: Map<String, CurrencyInfo> = linkedMapOf(
    "NPR" to CurrencyInfo("NPR", "रु.", "नेपाली रुपैयाँ (NPR)", 1.0),
    "USD" to CurrencyInfo("USD", "$", "US Dollar (USD)", 0.00735),
    "QAR" to CurrencyInfo("QAR", "QAR", "कतारी रियाल (QAR)", 0.0267),
    "AED" to CurrencyInfo("AED", "AED", "UAE Dirham (AED)", 0.0270),
    "SAR" to CurrencyInfo("SAR", "SAR", "साउदी रियाल (SAR)", 0.0276),
    "INR" to CurrencyInfo("INR", "₹", "भारतीय रुपैयाँ (INR)", 0.625),
    "EUR" to CurrencyInfo("EUR", "€", "Euro (EUR)", 0.0068),
    "GBP" to CurrencyInfo("GBP", "£", "British Pound (GBP)", 0.0058),
    "AUD" to CurrencyInfo("AUD", "A$", "Australian Dollar (AUD)", 0.0112),
    "MYR" to CurrencyInfo("MYR", "RM", "Malaysian Ringgit (MYR)", 0.0325),
    "KRW" to CurrencyInfo("KRW", "₩", "South Korean Won (KRW)", 10.15),
    "JPY" to CurrencyInfo("JPY", "¥", "Japanese Yen (JPY)", 1.12),
  )

  private val numberFormatter: DecimalFormat
    get() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

  fun toNpr(amount: Double, fromCurrency: String): Double {
    val rate = currencies[fromCurrency]?.rateFromNpr ?: 1.0
    return if (rate == 0.0) amount else amount / rate
  }

  fun fromNpr(nprAmount: Double, toCurrency: String): Double {
    val rate = currencies[toCurrency]?.rateFromNpr ?: 1.0
    return nprAmount * rate
  }

  fun format(nprAmount: Double, displayCurrency: String, hidden: Boolean = false): String {
    if (hidden) return "••••••"
    val info = currencies[displayCurrency] ?: currencies["NPR"]!!
    val converted = fromNpr(nprAmount, displayCurrency)
    return "${info.symbol} ${numberFormatter.format(converted)}"
  }

  fun formatRaw(amount: Double): String = numberFormatter.format(amount)
}

object MathEvaluator {
  fun evaluate(input: String): Double {
    return try {
      val clean = input.replace(" ", "").replace("×", "*").replace("÷", "/")
      if (clean.isEmpty()) return 0.0
      val addParts = clean.split("+")
      var sum = 0.0
      for (addPart in addParts) {
        if (addPart.isEmpty()) continue
        val subParts = addPart.split("-")
        var subVal = evalMulDiv(subParts[0])
        for (i in 1 until subParts.size) {
          subVal -= evalMulDiv(subParts[i])
        }
        sum += subVal
      }
      if (sum.isNaN() || sum.isInfinite()) 0.0 else sum
    } catch (_: Exception) {
      input.toDoubleOrNull() ?: 0.0
    }
  }

  private fun evalMulDiv(expr: String): Double {
    if (expr.isEmpty()) return 0.0
    val mulParts = expr.split("*")
    var res = 1.0
    for (m in mulParts) {
      if (m.contains("/")) {
        val divParts = m.split("/")
        var dVal = divParts[0].toDoubleOrNull() ?: 0.0
        for (i in 1 until divParts.size) {
          val divisor = divParts[i].toDoubleOrNull() ?: 1.0
          if (divisor != 0.0) dVal /= divisor
        }
        res *= dVal
      } else {
        res *= m.toDoubleOrNull() ?: 0.0
      }
    }
    return res
  }
}
