package com.volodymyrsmirnov.telesim.sim

import android.content.Context
import android.telephony.SubscriptionManager
import com.volodymyrsmirnov.telesim.data.SimCardInfo

class SimCardManager(context: Context) {
    private val subscriptionManager =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    @Suppress("MissingPermission")
    fun getAvailableSimCards(): Map<Int, SimCardInfo> {
        val fallback = mapOf(
            0 to SimCardInfo("SIM 1", "Unknown", null),
            1 to SimCardInfo("SIM 2", "Unknown", null)
        )

        return try {
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

            activeSubscriptions.associate { subscription ->
                @Suppress("DEPRECATION")
                subscription.simSlotIndex to SimCardInfo(
                    subscription.displayName?.toString() ?: "SIM ${subscription.simSlotIndex + 1}",
                    subscription.carrierName?.toString() ?: "Unknown",
                    subscription.number
                )
            }
        } catch (_: SecurityException) {
            fallback
        } catch (_: Exception) {
            fallback
        }
    }

    fun getSimCardBySlot(slotIndex: Int): SimCardInfo? {
        val availableSimCards = getAvailableSimCards()

        if (!availableSimCards.containsKey(slotIndex)) return null

        return availableSimCards[slotIndex]
    }
}