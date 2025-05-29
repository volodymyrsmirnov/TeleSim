package com.volodymyrsmirnov.telesim.telegram

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.volodymyrsmirnov.telesim.data.SettingsRepository
import kotlinx.coroutines.flow.first

class TelegramWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TAG = "TelegramWorker"
        const val KEY_SLOT = "KEY_SLOT"
        const val KEY_MESSAGE = "KEY_MESSAGE"
    }

    override suspend fun doWork(): Result {
        val slot = inputData.getInt(KEY_SLOT, -1)
        val text = inputData.getString(KEY_MESSAGE)

        if (text.isNullOrBlank() || slot == -1) {
            return Result.failure()
        }

        val settingsRepository = SettingsRepository(applicationContext)

        val settings = settingsRepository.settingsFlow.first()

        if (settings.botToken.isBlank()) {
            Log.w(TAG, "Bot token is not set")
            return Result.failure()
        }

        val chatId = settings.simChannels[slot]

        if (chatId.isNullOrBlank()) {
            Log.w(TAG, "No chat ID configured for SIM slot $slot")
            return Result.failure()
        }

        val telegram = TelegramClient()

        val result = telegram.sendMessage(settings.botToken, chatId, text)
        var resultException = result.exceptionOrNull()

        return if (result.isSuccess) {
            Log.i(TAG, "Message sent successfully to chat $chatId")
            Result.success()
        } else if (resultException is TelegramClient.FatalException) {
            Log.e(TAG, "Fatal error while sending message", resultException)
            Result.failure()
        } else {
            Log.w(TAG, "Error while sending message, will retry shortly", resultException)
            Result.retry()
        }
    }
}
