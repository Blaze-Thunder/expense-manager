package com.saik.expensemanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.util.CurrencyUtils

@Composable
fun AdjustBalanceDialog(
    account: Account,
    currentCalculatedBalance: Double,
    currencySim: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var targetBalanceStr by remember { mutableStateOf("") }
    val target = targetBalanceStr.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Balance:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = CurrencyUtils.formatAmount(currentCalculatedBalance, currencySim), 
                        style = MaterialTheme.typography.bodySmall, 
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = targetBalanceStr,
                    onValueChange = { targetBalanceStr = it },
                    label = { Text("New Actual Balance") },
                    prefix = { Text(currencySim) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (target != null) {
                    val diff = target - currentCalculatedBalance
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Adjustment:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = CurrencyUtils.formatAmountWithSign(diff, currencySim),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diff >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    target?.let { onConfirm(it) }
                    onDismiss()
                },
                enabled = target != null
            ) {
                Text("Adjust")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
