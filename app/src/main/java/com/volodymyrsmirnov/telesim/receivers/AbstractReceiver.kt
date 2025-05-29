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

abstract class AbstractReceiver : BroadcastReceiver() {
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

    private fun formatMessage(messageType: MessageType): String {
        return when (messageType) {
            is MessageType.Sms -> {
                "<blockquote>${messageType.content}</blockquote>\n\n📱 ${messageType.simDisplayName} from <code>${messageType.phoneNumber}</code>"
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