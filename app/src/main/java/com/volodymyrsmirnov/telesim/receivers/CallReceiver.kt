package com.volodymyrsmirnov.telesim.receivers

import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log
import com.volodymyrsmirnov.telesim.data.MessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallReceiver : AbstractReceiver() {
    companion object {
        private var previousState: String = ""

        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        // avoid duplicate processing of the same state
        if (previousState == state) return

        previousState = state ?: ""

        if (state != TelephonyManager.EXTRA_STATE_IDLE) return

        val resolver = context.contentResolver

        coroutineScope.launch {
            delay(500) // Wait for call log to update

            val cursor = resolver.query(
                CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                if (!it.moveToFirst()) return@launch

                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val subscriptionId = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID))

                val slot = getSimSlotFromSubscriptionId(context, subscriptionId)
                val displayName = getSimDisplayNameFromSimSlot(context, slot)

                Log.i(TAG, "Received call to $displayName from $number")

                enqueueMessage(context, slot, MessageType.Call(number, displayName))
            }
        }
    }
}