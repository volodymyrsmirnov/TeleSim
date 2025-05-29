package com.volodymyrsmirnov.telesim.receivers

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import com.volodymyrsmirnov.telesim.data.MessageType

class SmsReceiver : AbstractReceiver() {
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val subscriptionId = intent.getIntExtra("subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID)

            var sender = ""
            var content = ""

            for (message in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                sender = message.displayOriginatingAddress ?: "Unknown"
                content += message.messageBody
            }

            val slot = getSimSlotFromSubscriptionId(context, subscriptionId)
            val displayName = getSimDisplayNameFromSimSlot(context, slot)

            Log.i(TAG, "Received SMS to $displayName from $sender: $content")

            enqueueMessage(context, slot, MessageType.Sms(sender, content, displayName))
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
}