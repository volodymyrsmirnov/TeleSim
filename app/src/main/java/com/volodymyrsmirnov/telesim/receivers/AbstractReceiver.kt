package com.volodymyrsmirnov.telesim.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.volodymyrsmirnov.telesim.data.MessageType
import com.volodymyrsmirnov.telesim.sim.SimCardManager
import com.volodymyrsmirnov.telesim.telegram.TelegramWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

abstract class AbstractReceiver : BroadcastReceiver() {
    companion object {
        private val OTP_PATTERN = Pattern.compile(
            "(?:OTP|code|is|enter)\\s*:?\\s*(?:is:?\\s*)?(\\d{4,8})",
            Pattern.CASE_INSENSITIVE
        )
    }

    abstract override fun onReceive(context: Context, intent: Intent)

    protected val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Suppress("MissingPermission")
    protected fun getSimSlotFromSubscriptionId(context: Context, subscriptionId: Int): Int {
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(subscriptionId)

            return subscriptionInfo?.simSlotIndex ?: 0
        } catch (_: Exception) {
            return 0
        }
    }

    protected fun getSimDisplayNameFromSimSlot(context: Context, simSlot: Int): String {
        val simCardManager = SimCardManager(context)
        val simCard = simCardManager.getSimCardBySlot(simSlot)

        return simCard?.displayName ?: "SIM ${simSlot + 1}"
    }

    fun extractCode(smsText: String?): String? {
        if (smsText.isNullOrEmpty()) {
            return null
        }

        val matcher = OTP_PATTERN.matcher(smsText)

        while (matcher.find()) {
            matcher.group(1)?.let {
                return it
            }
        }

        return null
    }

    private fun formatMessage(messageType: MessageType): String {
        return when (messageType) {
            is MessageType.Sms -> {
                var message = "<blockquote>${messageType.content}</blockquote>"

                val code = extractCode(messageType.content)

                if (!code.isNullOrEmpty()) {
                    message += "\n\nOTP code (click to copy): <code>${code}</code>"
                }

                message += "\n\n📱 ${messageType.simDisplayName} from <code>${messageType.phoneNumber}</code>"

                return message
            }

            is MessageType.Call -> {
                "📞 ${messageType.simDisplayName} from <code>${messageType.phoneNumber}</code>"
            }
        }
    }

    fun enqueueMessage(context: Context, slot: Int, messageType: MessageType) {
        val formattedMessage = formatMessage(messageType)

        val data = workDataOf(
            TelegramWorker.KEY_SLOT to slot,
            TelegramWorker.KEY_MESSAGE to formattedMessage
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TelegramWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }
}