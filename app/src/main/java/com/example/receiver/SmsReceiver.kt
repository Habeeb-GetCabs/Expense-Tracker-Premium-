package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.ExpenseApplication
import com.example.data.model.PendingTransaction
import com.example.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages[0].displayOriginatingAddress ?: "Unknown"
            val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }

            Log.d("SmsReceiver", "SMS received from $sender: $fullBody")

            val parsedResult = SmsParser.parse(sender, fullBody)
            if (parsedResult != null) {
                Log.d("SmsReceiver", "Parsed transaction: $parsedResult")

                val pendingTransaction = PendingTransaction(
                    rawSms = parsedResult.rawSms,
                    sender = parsedResult.sender,
                    extractedAmount = parsedResult.amount,
                    extractedType = parsedResult.type,
                    suggestedCategory = parsedResult.category,
                    extractedMerchant = parsedResult.merchant,
                    timestamp = System.currentTimeMillis()
                )

                val pendingResult = goAsync()
                val app = context.applicationContext as? ExpenseApplication
                val repository = app?.repository ?: run {
                    val db = com.example.data.local.AppDatabase.getDatabase(context.applicationContext)
                    com.example.repository.ExpenseRepository(
                        transactionDao = db.transactionDao(),
                        pendingTransactionDao = db.pendingTransactionDao(),
                        categoryBudgetDao = db.categoryBudgetDao(),
                        paymentReminderDao = db.paymentReminderDao()
                    )
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.insertPendingTransaction(pendingTransaction)
                        Log.d("SmsReceiver", "Saved pending SMS transaction to Room database")
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Error saving pending SMS transaction", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
