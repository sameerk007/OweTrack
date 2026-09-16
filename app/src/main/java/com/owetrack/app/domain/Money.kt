package com.owetrack.app.domain

import com.owetrack.app.data.TransactionEntity
import com.owetrack.app.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Money {
    fun parseToPaise(raw: String): Long? = try {
        val clean = raw.replace(",", "").replace("₹", "").trim()
        if (clean.isBlank()) null else BigDecimal(clean).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().takeIf { it > 0 }
    } catch (_: Exception) { null }
    fun format(paise: Long, currency: String = "INR"): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        nf.currency = java.util.Currency.getInstance(currency)
        nf.maximumFractionDigits = if (paise % 100 == 0L) 0 else 2
        return nf.format(BigDecimal(paise).movePointLeft(2))
    }
    fun balance(items: Iterable<TransactionEntity>): Long = items.sumOf { if (it.type == TransactionType.GIVEN) it.amountPaise else -it.amountPaise }
    fun runningBalances(items: List<TransactionEntity>): Map<Long, Long> {
        var balance = 0L
        return items.sortedWith(compareBy<TransactionEntity> { it.transactionDate }.thenBy { it.createdAt }).associate { tx ->
            balance += if (tx.type == TransactionType.GIVEN) tx.amountPaise else -tx.amountPaise
            tx.transactionId to balance
        }
    }
}
