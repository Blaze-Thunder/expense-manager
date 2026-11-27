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
import androidx.compose.ui.unit.dp
import com.saik.expensemanager.ui.viewmodel.MainViewModel

@Composable
fun SetupPinScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val pinInput = if (step == 1) firstPin else secondPin

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
                if (step == 1) "Set a 6-digit PIN" else "Confirm your PIN", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
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
                    if (step == 1) {
                        if (firstPin.length < 6) {
                            firstPin += digit
                            if (firstPin.length == 6) {
                                step = 2
                                error = null
                            }
                        }
                    } else {
                        if (secondPin.length < 6) {
                            secondPin += digit
                            if (secondPin.length == 6) {
                                if (firstPin == secondPin) {
                                    viewModel.setAppPin(firstPin)
                                    onBack()
                                } else {
                                    error = "PINs do not match"
                                    firstPin = ""
                                    secondPin = ""
                                    step = 1
                                }
                            }
                        }
                    }
                },
                onBackspace = {
                    if (step == 1) {
                        if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                    } else {
                        if (secondPin.isNotEmpty()) secondPin = secondPin.dropLast(1)
                    }
                }
            )
        }
    }
}
