package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.PinUtils

@Composable
fun VerifyPinScreen(
    viewModel: MainViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text(
                "Enter Current PIN", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                            .background(
                                if (index < pinInput.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(48.dp))
            NumericKeypad(
                onDigit = { digit ->
                    if (pinInput.length < 6) {
                        pinInput += digit
                        if (pinInput.length == 6) {
                            val salt = settings.pinSalt
                            val hash = settings.pinHash
                            if (hash != null && salt != null &&
                                PinUtils.verifyPin(pinInput, salt, hash)) {
                                onSuccess()
                            } else {
                                error = "Incorrect PIN"
                                pinInput = ""
                            }
                        }
                    }
                },
                onBackspace = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) }
            )
        }
    }
}
