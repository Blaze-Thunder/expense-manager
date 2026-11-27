package com.saik.expensemanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.LocalActivity
import com.saik.expensemanager.ui.components.Dimens
import com.saik.expensemanager.ui.components.StandardTopAppBar
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.BiometricAuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSetupPin: () -> Unit,
    onNavigateToVerifyPin: (onSuccess: () -> Unit) -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val biometricHelper = remember(activity) { activity?.let { BiometricAuthHelper(it) } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "App Lock Settings",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = Dimens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            SettingsRow(
                title = "App Lock",
                subtitle = "Secure your financial data with PIN",
                trailing = {
                    Switch(
                        checked = appSettings.appLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (appSettings.pinHash == null) {
                                    onNavigateToSetupPin()
                                } else {
                                    viewModel.setAppLockEnabled(true)
                                }
                            } else {
                                // Requirement: Prompt for current PIN to disable
                                onNavigateToVerifyPin {
                                    viewModel.clearAppPin()
                                }
                            }
                        }
                    )
                }
            )

            if (appSettings.appLockEnabled) {
                if (biometricHelper?.canAuthenticate() == true) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingSmall), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRow(
                        title = "Use Biometric Unlock",
                        subtitle = "Unlock using fingerprint or face",
                        trailing = {
                            Switch(
                                checked = appSettings.biometricLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        biometricHelper.authenticate(
                                            onSuccess = { viewModel.setBiometricLockEnabled(true) },
                                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                            onFailed = { Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show() }
                                        )
                                    } else {
                                        viewModel.setBiometricLockEnabled(false)
                                    }
                                }
                            )
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingSmall), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    title = "Change PIN",
                    subtitle = "Update your 6-digit security PIN",
                    onClick = {
                        // Requirement: Ask for current PIN before changing
                        onNavigateToVerifyPin {
                            onNavigateToSetupPin()
                        }
                    }
                )
            }
        }
    }
}
