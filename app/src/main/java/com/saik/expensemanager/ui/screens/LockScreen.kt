package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saik.expensemanager.data.model.AppSettings
import com.saik.expensemanager.util.PinUtils
import kotlinx.coroutines.delay

@Composable
fun AppLockLifecycleObserver(isAppLockEnabled: Boolean, onLock: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, isAppLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            // Use ON_STOP instead of ON_PAUSE to avoid locking on transient states like system dialogs or biometric prompts.
            // ON_STOP is triggered when the app is no longer visible (home screen, app switcher, or new activity).
            if (event == Lifecycle.Event.ON_STOP && isAppLockEnabled) onLock()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun LockScreen(
    settings: AppSettings,
    onPinVerified: () -> Unit,
    onBiometricClick: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && settings.biometricLockEnabled) {
                onBiometricClick()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Enter PIN", 
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
                            if (settings.pinHash != null && settings.pinSalt != null &&
                                PinUtils.verifyPin(pinInput, settings.pinSalt, settings.pinHash)) {
                                onPinVerified()
                            } else {
                                error = "Incorrect PIN"
                                pinInput = ""
                            }
                        }
                    }
                },
                onBackspace = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) }
            )
            if (settings.biometricLockEnabled) {
                Spacer(Modifier.height(32.dp))
                TextButton(
                    onClick = onBiometricClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use biometric", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun NumericKeypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(72.dp))
                    } else {
                        Surface(
                            onClick = { if (key == "⌫") onBackspace() else onDigit(key) },
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(Icons.Default.Backspace, contentDescription = "Backspace")
                                } else {
                                    Text(key, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
