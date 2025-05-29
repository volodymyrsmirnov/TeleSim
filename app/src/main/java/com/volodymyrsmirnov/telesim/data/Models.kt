package com.volodymyrsmirnov.telesim.data

import kotlinx.serialization.Serializable

@Serializable
data class SimCardInfo(
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?
)

@Serializable
data class AppSettings(
    val botToken: String = "",
    val simChannels: Map<Int, String> = emptyMap(),
)

sealed class MessageType {
    data class Sms(
        val phoneNumber: String,
        val content: String,
        val simDisplayName: String
    ) : MessageType()

    data class Call(
        val phoneNumber: String,
        val simDisplayName: String
    ) : MessageType()
}