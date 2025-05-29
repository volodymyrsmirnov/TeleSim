package com.volodymyrsmirnov.telesim

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.volodymyrsmirnov.telesim.service.KeepAliveService
import com.volodymyrsmirnov.telesim.ui.MainViewModel
import com.volodymyrsmirnov.telesim.ui.theme.TeleSimTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        for ((permission, granted) in permissions) {
            if (permission == Manifest.permission.READ_PHONE_STATE && granted) {
                viewModel.loadSimCards()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        requestPermissions()
        requestBatteryOptimizationWhitelist()

        KeepAliveService.startService(this)

        setContent {
            TeleSimTheme {
                MainScreen(viewModel)
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationWhitelist() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            }

            try {
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SMS & Call Alerts via Telegram") })
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Bot Token Section
            OutlinedTextField(
                value = uiState.botToken,
                onValueChange = viewModel::updateBotToken,
                label = { Text("Telegram Bot Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text(
                text = "SIM Card Channels",
                style = MaterialTheme.typography.titleMedium
            )

            // Show detected SIM cards
            uiState.simCards.forEach { (slotIndex, simCard) ->
                OutlinedTextField(
                    value = uiState.simChannels[slotIndex] ?: "",
                    onValueChange = { viewModel.updateSimChannel(slotIndex, it) },
                    label = { Text("${simCard.displayName} (${simCard.carrierName}) ${simCard.phoneNumber}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("@user or numeric chat_id (-100...)") })
            }

            // Save button
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(if (uiState.isSaving) "Saving..." else "Save Settings")
            }

            // Show save message
            uiState.saveMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("Error")) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LaunchedEffect(message) {
                    delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }
}