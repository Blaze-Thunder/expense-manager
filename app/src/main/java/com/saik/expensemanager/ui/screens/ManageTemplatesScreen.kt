package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.TransactionTemplate
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTemplatesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val templates by viewModel.allTemplates.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val budgetBooks by viewModel.activeBudgetBooks.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    
    var editingTemplate by remember { mutableStateOf<TransactionTemplate?>(null) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showAddTemplateSheet by remember { mutableStateOf(false) }

    val currencySim = appSettings.currencyCode

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Manage Templates",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (templates.isNotEmpty()) {
                            IconButton(onClick = { isReorderMode = !isReorderMode }) {
                                Icon(
                                    imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
                                    contentDescription = "Toggle Reorder",
                                    tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        floatingActionButton = {
            if (!isReorderMode) {
                FloatingActionButton(onClick = { showAddTemplateSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Template")
                }
            }
        }
    ) { innerPadding ->
        if (templates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No templates found", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                itemsIndexed(templates, key = { _, t -> t.id }) { index, template ->
                    TemplateManagementRow(
                        template = template,
                        currencySim = appSettings.currencyCode,
                        isReorderMode = isReorderMode,
                        onEdit = { editingTemplate = template },
                        onDelete = { viewModel.deleteTemplate(template) },
                        onMoveUp = if (index > 0) {
                            {
                                val newList = templates.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index - 1, item)
                                viewModel.reorderTemplates(newList)
                            }
                        } else null,
                        onMoveDown = if (index < templates.size - 1) {
                            {
                                val newList = templates.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index + 1, item)
                                viewModel.reorderTemplates(newList)
                            }
                        } else null
                    )
                }
            }
        }

        if (editingTemplate != null) {
            SaveTemplateBottomSheet(
                viewModel = viewModel,
                onDismiss = { editingTemplate = null },
                onConfirm = { 
                    viewModel.updateTemplate(it)
                    editingTemplate = null
                },
                accounts = accounts,
                categories = categories,
                budgetBooks = budgetBooks,
                currencySim = currencySim,
                initialTemplate = editingTemplate
            )
        }

        if (showAddTemplateSheet) {
            SaveTemplateBottomSheet(
                viewModel = viewModel,
                onDismiss = { showAddTemplateSheet = false },
                onConfirm = { template ->
                    viewModel.addTemplate(
                        name = template.name,
                        amount = template.amount,
                        type = template.type,
                        category = template.category,
                        subCategory = template.subCategory,
                        accountId = template.linkedAccountId,
                        toAccountId = template.toAccountId,
                        budgetBookId = template.linkedBudgetBookId,
                        description = template.description,
                        iconName = template.iconName,
                        emojiValue = template.emojiValue,
                        colorHex = template.colorHex
                    )
                    showAddTemplateSheet = false
                },
                accounts = accounts,
                categories = categories,
                budgetBooks = budgetBooks,
                currencySim = currencySim
            )
        }
    }
}

@Composable
fun TemplateManagementRow(
    template: TransactionTemplate,
    currencySim: String,
    isReorderMode: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    SecondaryCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            val defaultColor = when (template.type) {
                "INCOME" -> Color(0xFF2E7D32)
                "EXPENSE" -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.primary
            }
            val iconColor = template.colorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: defaultColor

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (template.emojiValue != null) {
                    Text(template.emojiValue, fontSize = 18.sp)
                } else {
                    Icon(
                        CategoryUtils.getCategoryIcon(template.iconName),
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    CurrencyUtils.formatAmount(template.amount, currencySim),
                    style = MaterialTheme.typography.bodySmall,
                    color = defaultColor
                )
            }

            if (isReorderMode) {
                Row {
                    IconButton(onClick = onMoveUp ?: {}, enabled = onMoveUp != null) {
                        Icon(Icons.Default.KeyboardArrowUp, null)
                    }
                    IconButton(onClick = onMoveDown ?: {}, enabled = onMoveDown != null) {
                        Icon(Icons.Default.KeyboardArrowDown, null)
                    }
                }
            } else {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
