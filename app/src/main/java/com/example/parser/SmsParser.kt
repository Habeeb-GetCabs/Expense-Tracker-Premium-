package com.example.parser

import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val merchant: String,
    val category: String,
    val rawSms: String,
    val sender: String
)

object SmsParser {

    private val expenseKeywords = listOf(
        "debited", "spent", "paid", "sent", "withdrawn", "purchase", "charged", "dr", "transferred to"
    )

    private val incomeKeywords = listOf(
        "credited", "received", "added", "deposited", "cr", "refund", "salary", "cashback", "transferred from"
    )

    // Regex for amount matching: Rs. 500, INR 1,200.50, $45.00, 500.00 INR, etc.
    private val amountPattern = Pattern.compile(
        "(?:rs\\.?|inr|usd|\\$|eur|€|gbp|£)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )
    private val altAmountPattern = Pattern.compile(
        "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr)",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for merchant extraction
    private val merchantAtPattern = Pattern.compile(
        "(?:at|to|vpa|info[:\\s]|for)\\s+([a-zA-Z0-9&\\s._-]{2,25}?)(?=\\s+(?:on|ref|avail|bal|using|via|a/c|card|net|end|is|has|was|date)|[.,\\n]|$)",
        Pattern.CASE_INSENSITIVE
    )

    private val merchantFromPattern = Pattern.compile(
        "(?:from|by)\\s+([a-zA-Z0-9&\\s._-]{2,25}?)(?=\\s+(?:on|ref|avail|bal|using|via|a/c|card|net|end|is|has|was|date)|[.,\\n]|$)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Attempts to parse an SMS message. Returns null if the SMS does not look like a financial transaction.
     */
    fun parse(sender: String, messageBody: String): ParsedSmsResult? {
        val lowerBody = messageBody.lowercase(Locale.ROOT)

        // Determine transaction type
        val isExpense = expenseKeywords.any { lowerBody.contains(it) }
        val isIncome = incomeKeywords.any { lowerBody.contains(it) }

        if (!isExpense && !isIncome) {
            return null // Not a transaction SMS
        }

        val type = if (isExpense) "EXPENSE" else "INCOME"

        // Extract amount
        var amount: Double? = null
        val matcher = amountPattern.matcher(messageBody)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
        }
        if (amount == null) {
            val altMatcher = altAmountPattern.matcher(messageBody)
            if (altMatcher.find()) {
                val amountStr = altMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
            }
        }

        if (amount == null || amount <= 0.0) {
            return null // Could not parse a valid amount
        }

        // Extract merchant name
        var merchant = "Unknown Merchant"
        val merchantMatcher = if (isIncome) {
            merchantFromPattern.matcher(messageBody)
        } else {
            merchantAtPattern.matcher(messageBody)
        }

        if (merchantMatcher.find()) {
            val rawMerchant = merchantMatcher.group(1)?.trim()
            if (!rawMerchant.isNullOrBlank()) {
                merchant = cleanMerchantName(rawMerchant)
            }
        } else if (sender.isNotBlank()) {
            merchant = sender.replace(Regex("[^a-zA-Z0-9]"), "").takeLast(8)
        }

        // Auto-categorize
        val category = autoCategorize(merchant, lowerBody)

        return ParsedSmsResult(
            amount = amount,
            type = type,
            merchant = merchant,
            category = category,
            rawSms = messageBody,
            sender = sender
        )
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.replace(Regex("(?i)\\b(ref|txn|ac|account|bank|ltd|pvt|upi)\\b"), "")
            .trim()
            .split(" ")
            .take(3)
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            .ifEmpty { "Bank Transfer" }
    }

    fun autoCategorize(merchant: String, bodyText: String): String {
        val combined = "$merchant $bodyText".lowercase(Locale.ROOT)

        return when {
            combined.containsAny("hospital", "medical", "pharmacy", "apollo", "medplus", "doctor", "clinic", "pharma", "1mg", "netmeds", "health") -> "Medical"
            combined.containsAny("grocery", "blinkit", "zepto", "instamart", "supermarket", "bigbasket", "dmart", "spensers", "provision") -> "Grocery"
            combined.containsAny("swiggy", "zomato", "restaurant", "cafe", "starbucks", "mcdonald", "kfc", "pizza", "food", "dining", "diner") -> "Dining"
            combined.containsAny("amazon", "flipkart", "myntra", "ajio", "zara", "store", "fashion", "mall", "shopping", "decathlon") -> "Shopping"
            combined.containsAny("uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "hpcl", "bpcl", "iocl", "shell", "parking", "toll", "transit") -> "Transport"
            combined.containsAny("recharge", "jio", "airtel", "vi", "electricity", "bescom", "water bill", "broadband", "wifi", "gas bill", "utility") -> "Utilities"
            combined.containsAny("salary", "payroll", "stipend", "bonus", "dividend", "employer") -> "Salary"
            combined.containsAny("rent", "landlord", "housing", "society", "maintenance") -> "Rent"
            combined.containsAny("movie", "netflix", "bookmyshow", "hotstar", "spotify", "prime", "cinema", "entertainment") -> "Entertainment"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
