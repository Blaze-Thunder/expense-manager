package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.GiftCardBrand
import com.saik.expensemanager.ui.components.Dimens
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGiftCardDialog(
    viewModel: MainViewModel,
    giftCardToEdit: GiftCard? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var brand by remember { mutableStateOf(giftCardToEdit?.brand?.let { 
        try { GiftCardBrand.valueOf(it) } catch (e: Exception) { GiftCardBrand.OTHER }
    } ?: GiftCardBrand.AMAZON_PAY) }
    var customBrandName by remember { mutableStateOf(giftCardToEdit?.customBrandName ?: "") }
    var code by remember { mutableStateOf(giftCardToEdit?.code ?: "") }
    var amount by remember { mutableStateOf(giftCardToEdit?.amount?.toString() ?: "") }
    var purchaseDate by remember { mutableStateOf(giftCardToEdit?.purchaseDate ?: System.currentTimeMillis()) }
    var validityDays by remember { mutableStateOf(giftCardToEdit?.validityDays?.toString() ?: "365") }
    var note by remember { mutableStateOf(giftCardToEdit?.note ?: "") }
    
    var brandExpanded by remember { mutableStateOf(false) }
    var validityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingLarge)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (giftCardToEdit == null) "Add Gift Card" else "Edit Gift Card", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            // Brand
            ExposedDropdownMenuBox(
                expanded = brandExpanded,
                onExpandedChange = { brandExpanded = !brandExpanded }
            ) {
                OutlinedTextField(
                    value = brand.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Brand") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = brandExpanded,
                    onDismissRequest = { brandExpanded = false }
                ) {
                    GiftCardBrand.entries.forEach { b ->
                        DropdownMenuItem(
                            text = { Text(b.displayName) },
                            onClick = {
                                brand = b
                                brandExpanded = false
                            }
                        )
                    }
                }
            }

            if (brand == GiftCardBrand.OTHER) {
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                OutlinedTextField(
                    value = customBrandName,
                    onValueChange = { customBrandName = it },
                    label = { Text("Custom Brand Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errors.containsKey("brand")
                )
                if (errors.containsKey("brand")) {
                    Text(errors["brand"]!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Code
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code") },
                modifier = Modifier.fillMaxWidth(),
                isError = errors.containsKey("code")
            )
            if (errors.containsKey("code")) {
                Text(errors["code"]!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errors.containsKey("amount")
            )
            if (errors.containsKey("amount")) {
                Text(errors["amount"]!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Purchase Date
            OutlinedTextField(
                value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(purchaseDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Purchase Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Validity
            val validityPresets = listOf("90", "180", "365", "730", "Custom")
            ExposedDropdownMenuBox(
                expanded = validityExpanded,
                onExpandedChange = { validityExpanded = !validityExpanded }
            ) {
                OutlinedTextField(
                    value = if (validityDays in validityPresets.dropLast(1)) "$validityDays Days" else "Custom",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Validity") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = validityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = validityExpanded,
                    onDismissRequest = { validityExpanded = false }
                ) {
                    validityPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(if (preset == "Custom") "Custom" else "$preset Days") },
                            onClick = {
                                if (preset != "Custom") validityDays = preset
                                validityExpanded = false
                            }
                        )
                    }
                }
            }

            if (validityDays !in validityPresets.dropLast(1)) {
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                OutlinedTextField(
                    value = validityDays,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) validityDays = it },
                    label = { Text("Custom Validity (Days)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errors.containsKey("validity")
                )
                if (errors.containsKey("validity")) {
                    Text(errors["validity"]!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.SpacingExtraLarge))

            // Save Button
            Button(
                onClick = {
                    val newErrors = mutableMapOf<String, String>()
                    if (brand == GiftCardBrand.OTHER && customBrandName.isBlank()) newErrors["brand"] = "Required"
                    if (code.isBlank()) newErrors["code"] = "Required"
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (amountVal <= 0.0) newErrors["amount"] = "Must be > 0"
                    val valDays = validityDays.toIntOrNull() ?: 0
                    if (valDays <= 0) newErrors["validity"] = "Required"
                    
                    if (newErrors.isEmpty()) {
                        if (giftCardToEdit == null) {
                            viewModel.addGiftCard(
                                brand = brand.name,
                                customBrandName = if (brand == GiftCardBrand.OTHER) customBrandName else null,
                                code = code,
                                amount = amountVal,
                                purchaseDate = purchaseDate,
                                validityDays = valDays,
                                note = note.ifBlank { null }
                            )
                        } else {
                            viewModel.updateGiftCard(
                                giftCardToEdit.copy(
                                    brand = brand.name,
                                    customBrandName = if (brand == GiftCardBrand.OTHER) customBrandName else null,
                                    code = code,
                                    amount = amountVal,
                                    purchaseDate = purchaseDate,
                                    validityDays = valDays,
                                    note = note.ifBlank { null }
                                )
                            )
                        }
                        onDismiss()
                    } else {
                        errors = newErrors
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (giftCardToEdit == null) "Save Gift Card" else "Update Gift Card")
            }
            
            if (giftCardToEdit != null) {
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                TextButton(
                    onClick = {
                        viewModel.deleteGiftCard(giftCardToEdit)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Gift Card")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = purchaseDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { purchaseDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
