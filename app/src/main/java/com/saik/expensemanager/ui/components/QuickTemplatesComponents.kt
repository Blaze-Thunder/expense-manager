package com.saik.expensemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.TransactionTemplate
import com.saik.expensemanager.ui.screens.AccountUtils
import com.saik.expensemanager.ui.screens.CategoryUtils
import com.saik.expensemanager.ui.screens.AccountSelectorSheet
import com.saik.expensemanager.ui.screens.CategorySelectorSheet
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import java.util.*

@Composable
fun StaticTransactionFabs(
    onNewTransaction: () -> Unit,
    onFromTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onFromTemplate,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Style, contentDescription = "From Template")
        }

        FloatingActionButton(
            onClick = onNewTransaction,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Transaction")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTemplatesBottomSheet(
    templates: List<TransactionTemplate>,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    logInstantly: Boolean,
    onLogInstantlyChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTemplateSelected: (TransactionTemplate) -> Unit,
    onNewTemplate: () -> Unit,
    onManageTemplates: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Templates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Log Instantly",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                    Switch(
                        checked = logInstantly,
                        onCheckedChange = onLogInstantlyChange,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            if (templates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                        Text(
                            "Save your frequent transactions as templates\nfor quick access",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(templates) { template ->
                        TemplateRow(
                            template = template,
                            currencySim = currencySim,
                            categories = categories,
                            accounts = accounts,
                            onClick = { onTemplateSelected(template) }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.SpacingSmall))

            // Action Rows
            TemplateActionRow(
                icon = Icons.Default.Add,
                label = "New Template",
                onClick = onNewTemplate
            )
            TemplateActionRow(
                icon = Icons.Default.Settings,
                label = "Manage Templates",
                onClick = onManageTemplates
            )
        }
    }
}

@Composable
fun TemplateRow(
    template: TransactionTemplate,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    val defaultColor = when (template.type) {
        "INCOME" -> Color(0xFF2E7D32)
        "EXPENSE" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }
    val iconColor = template.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: defaultColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (template.type == "TRANSFER") {
                    Icon(Icons.Default.SwapHoriz, null, tint = iconColor, modifier = Modifier.size(24.dp))
                } else if (template.emojiValue != null) {
                    Text(template.emojiValue, fontSize = 20.sp)
                } else {
                    Icon(
                        imageVector = CategoryUtils.getCategoryIcon(template.iconName),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val accName = accounts.find { it.id == template.linkedAccountId }?.name ?: "Acc"
                val toAccName = if (template.type == "TRANSFER") accounts.find { it.id == template.toAccountId }?.name ?: "Acc" else null
                Text(
                    text = if (toAccName != null) "$accName → $toAccName" else accName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = CurrencyUtils.formatAmount(template.amount, currencySim),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = defaultColor
            )
        }
    }
}

@Composable
private fun TemplateActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTemplateBottomSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirm: (TransactionTemplate) -> Unit,
    accounts: List<Account>,
    categories: List<Category>,
    budgetBooks: List<BudgetBook>,
    currencySim: String,
    initialTemplate: TransactionTemplate? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var amountStr by remember { mutableStateOf(initialTemplate?.amount?.let { if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var type by remember { mutableStateOf(initialTemplate?.type ?: "EXPENSE") }
    var selectedCategoryName by remember { mutableStateOf(initialTemplate?.category ?: "") }
    var selectedSubCategoryName by remember { mutableStateOf(initialTemplate?.subCategory) }
    var selectedAccountId by remember { mutableLongStateOf(initialTemplate?.linkedAccountId ?: -1L) }
    var toAccountId by remember { mutableLongStateOf(initialTemplate?.toAccountId ?: -1L) }
    var selectedBudgetBookId by remember { mutableLongStateOf(initialTemplate?.linkedBudgetBookId ?: budgetBooks.find { it.isDefault }?.id ?: -1L) }
    var description by remember { mutableStateOf(initialTemplate?.description ?: "") }
    var iconName by remember { mutableStateOf(initialTemplate?.iconName ?: "Star") }
    var emojiValue by remember { mutableStateOf(initialTemplate?.emojiValue) }
    var colorHex by remember { mutableStateOf(initialTemplate?.colorHex) }

    var showAccountSelector by remember { mutableStateOf(false) }
    var showToAccountSelector by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showBudgetBookSelector by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFF9800),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B), Color(0xFF37474F)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            Text(
                text = if (initialTemplate == null) "New Template" else "Edit Template",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Template Name") },
                placeholder = { Text("e.g. Morning Coffee") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                
                SegmentedPeriodSelector(
                    modes = listOf("Expense", "Income", "Transfer"),
                    selectedMode = type.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    onModeSelected = { type = it.uppercase() },
                    modifier = Modifier.weight(1.5f).padding(horizontal = 0.dp)
                )
            }

            SecondaryCard {
                Column {
                    val acc = accounts.find { it.id == selectedAccountId }
                    FieldRow(
                        icon = acc?.let { AccountUtils.getAccountIcon(it.iconName) } ?: Icons.Default.AccountBalanceWallet,
                        label = "Account",
                        value = acc?.name ?: "Select",
                        onClick = { showAccountSelector = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Dimens.SpacingLarge), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    if (type == "TRANSFER") {
                        val toAcc = accounts.find { it.id == toAccountId }
                        FieldRow(
                            icon = toAcc?.let { AccountUtils.getAccountIcon(it.iconName) } ?: Icons.Default.AccountBalanceWallet,
                            label = "To Account",
                            value = toAcc?.name ?: "Select",
                            onClick = { showToAccountSelector = true }
                        )
                    } else {
                        val parentCat = categories.find { it.name == selectedCategoryName && it.type == type && it.parentId == null }
                        val subCat = if (selectedSubCategoryName != null) {
                            categories.find { it.name == selectedSubCategoryName && it.type == type && it.parentId == parentCat?.id }
                        } else null
                        val displayCat = subCat ?: parentCat
                        
                        FieldRow(
                            customIcon = {
                                val icName = displayCat?.iconName ?: "Label"
                                val isEm = icName.isNotEmpty() && !CategoryUtils.iconList.contains(icName)
                                if (isEm) {
                                    Text(icName)
                                } else {
                                    Icon(CategoryUtils.getCategoryIcon(icName), null, modifier = Modifier.size(20.dp))
                                }
                            },
                            label = "Category",
                            value = if (selectedSubCategoryName != null) "$selectedCategoryName/$selectedSubCategoryName" else selectedCategoryName.ifEmpty { "Select" },
                            onClick = { showCategorySelector = true }
                        )
                    }
                }
            }

            // Appearance Section
            SectionHeader("Template Style", modifier = Modifier.padding(horizontal = 0.dp))
            SecondaryCard {
                Column(
                    modifier = Modifier.padding(Dimens.SpacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    // Preview
                    val previewColor = colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(previewColor.copy(alpha = 0.2f))
                            .border(2.dp, previewColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (emojiValue != null) {
                            Text(emojiValue!!, fontSize = 24.sp)
                        } else {
                            Icon(
                                CategoryUtils.getCategoryIcon(iconName),
                                null,
                                tint = previewColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                        Button(
                            onClick = { showIconPicker = true; emojiValue = null },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Icon")
                        }
                        Button(
                            onClick = { showEmojiPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Emoji")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        presetColors.forEach { color ->
                            val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                            val isSelected = colorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { colorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && amountStr.toDoubleOrNull() != null && selectedAccountId != -1L) {
                        onConfirm(
                            TransactionTemplate(
                                id = initialTemplate?.id ?: 0,
                                name = name,
                                amount = amountStr.toDoubleOrNull() ?: 0.0,
                                type = type,
                                category = selectedCategoryName,
                                subCategory = selectedSubCategoryName,
                                linkedAccountId = selectedAccountId,
                                toAccountId = if (type == "TRANSFER") toAccountId else null,
                                linkedBudgetBookId = selectedBudgetBookId,
                                description = description,
                                iconName = iconName,
                                emojiValue = emojiValue,
                                colorHex = colorHex,
                                displayOrder = initialTemplate?.displayOrder ?: 0
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Template", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showAccountSelector) {
        AccountSelectorSheet(
            accounts = accounts,
            viewModel = viewModel,
            currencySim = currencySim,
            selectedAccountId = selectedAccountId,
            onSelect = { 
                selectedAccountId = it.id
                showAccountSelector = false
            },
            onDismiss = { showAccountSelector = false }
        )
    }

    if (showToAccountSelector) {
        AccountSelectorSheet(
            accounts = accounts,
            viewModel = viewModel,
            currencySim = currencySim,
            selectedAccountId = toAccountId,
            onSelect = { 
                toAccountId = it.id
                showToAccountSelector = false
            },
            onDismiss = { showToAccountSelector = false }
        )
    }

    if (showCategorySelector) {
        CategorySelectorSheet(
            viewModel = viewModel,
            type = type,
            initialParentName = selectedCategoryName,
            initialSubCategoryName = selectedSubCategoryName,
            onSelect = { parent, sub -> 
                selectedCategoryName = parent
                selectedSubCategoryName = sub
                showCategorySelector = false
            },
            onDismiss = { showCategorySelector = false }
        )
    }

    if (showIconPicker) {
        IconPickerBottomSheet(
            onDismiss = { showIconPicker = false },
            onIconSelected = { 
                iconName = it
                showIconPicker = false
            },
            selectedIcon = iconName,
            color = colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
        )
    }

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { 
                emojiValue = it
                showEmojiPicker = false
            },
            selectedEmoji = emojiValue ?: ""
        )
    }
}

@Composable
fun FieldRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    customIcon: @Composable (() -> Unit)? = null,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    errorText: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(Dimens.SpacingLarge)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        icon,
                        null,
                        tint = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(0.1f)) // Small buffer

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (errorText != null) MaterialTheme.colorScheme.error else valueColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 36.dp, top = 4.dp)
            )
        }
    }
}
