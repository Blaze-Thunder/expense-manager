package com.saik.expensemanager.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    viewModel: MainViewModel,
    isStats: Boolean = false,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    // Current State from ViewModel
    val filterDateRange by (if (isStats) viewModel.statsFilterDateRange else viewModel.filterDateRange).collectAsState()
    val filterAccountIds by (if (isStats) viewModel.statsFilterAccountIds else viewModel.filterAccountIds).collectAsState()
    val filterCategoryNames by (if (isStats) viewModel.statsFilterCategoryNames else viewModel.filterCategoryNames).collectAsState()
    val filterTransactionTypes by (if (isStats) viewModel.statsFilterTransactionTypes else viewModel.filterTransactionTypes).collectAsState()

    // Temp State for the sheet
    var tempDateRange by remember { mutableStateOf(filterDateRange) }
    var tempAccountIds by remember { mutableStateOf(filterAccountIds) }
    var tempCategoryNames by remember { mutableStateOf(filterCategoryNames) }
    var tempTransactionTypes by remember { mutableStateOf(filterTransactionTypes) }

    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = tempDateRange?.first,
        initialSelectedEndDateMillis = tempDateRange?.second
    )

    // Collapsible states
    var dateExpanded by remember { mutableStateOf(true) }
    var typeExpanded by remember { mutableStateOf(true) }
    var accountsExpanded by remember { mutableStateOf(tempAccountIds.isNotEmpty()) }
    var categoriesExpanded by remember { mutableStateOf(tempCategoryNames.isNotEmpty()) }

    // Search states for sections
    var accountSearch by remember { mutableStateOf("") }
    var categorySearch by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.9f)) {
            // Sticky Header
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Scrollable Middle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.SpacingLarge)
            ) {
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // 1. Date Range
                CollapsibleSection(
                    title = "Date Range",
                    isExpanded = dateExpanded,
                    onToggle = { dateExpanded = !dateExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.SpacingSmall),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        FilterChip(
                            selected = tempDateRange == null,
                            onClick = { tempDateRange = null },
                            label = { Text("All Time") },
                            leadingIcon = if (tempDateRange == null) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        val sdf = SimpleDateFormat("MMM d", Locale.US)
                        val dateLabel = if (tempDateRange != null) {
                            "${sdf.format(Date(tempDateRange!!.first))} - ${sdf.format(Date(tempDateRange!!.second))}"
                        } else "Custom Range"
                        FilterChip(
                            selected = tempDateRange != null,
                            onClick = { showDateRangePicker = true },
                            label = { Text(dateLabel) },
                            leadingIcon = if (tempDateRange != null) {
                                { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // 2. Transaction Type
                CollapsibleSection(
                    title = "Transaction Type",
                    isExpanded = typeExpanded,
                    onToggle = { typeExpanded = !typeExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.SpacingSmall),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        val types = listOf("EXPENSE", "INCOME", "TRANSFER")
                        types.forEach { type ->
                            FilterChip(
                                selected = tempTransactionTypes.contains(type),
                                onClick = {
                                    tempTransactionTypes = if (tempTransactionTypes.contains(type)) {
                                        tempTransactionTypes - type
                                    } else {
                                        tempTransactionTypes + type
                                    }
                                },
                                label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) },
                                leadingIcon = if (tempTransactionTypes.contains(type)) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // 3. Accounts
                CollapsibleSection(
                    title = "Accounts",
                    isExpanded = accountsExpanded,
                    onToggle = { accountsExpanded = !accountsExpanded },
                    badgeCount = tempAccountIds.size
                ) {
                    Column(modifier = Modifier.padding(vertical = Dimens.SpacingSmall)) {
                        if (accounts.size > 8) {
                            FilterSearchField(
                                value = accountSearch,
                                onValueChange = { accountSearch = it },
                                placeholder = "Search accounts..."
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                        ) {
                            accounts.filter { it.name.contains(accountSearch, ignoreCase = true) }.forEach { acc ->
                                FilterChip(
                                    selected = tempAccountIds.contains(acc.id),
                                    onClick = {
                                        tempAccountIds = if (tempAccountIds.contains(acc.id)) {
                                            tempAccountIds - acc.id
                                        } else {
                                            tempAccountIds + acc.id
                                        }
                                    },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }
                }

                // 4. Categories
                CollapsibleSection(
                    title = "Categories",
                    isExpanded = categoriesExpanded,
                    onToggle = { categoriesExpanded = !categoriesExpanded },
                    badgeCount = tempCategoryNames.size
                ) {
                    Column(modifier = Modifier.padding(vertical = Dimens.SpacingSmall)) {
                        FilterSearchField(
                            value = categorySearch,
                            onValueChange = { categorySearch = it },
                            placeholder = "Search categories..."
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                        val filteredCats = categories.filter { it.name.contains(categorySearch, ignoreCase = true) }
                        val expenseCats = filteredCats.filter { it.type == "EXPENSE" }.distinctBy { it.name }
                        val incomeCats = filteredCats.filter { it.type == "INCOME" }.distinctBy { it.name }

                        if (expenseCats.isNotEmpty()) {
                            Text(
                                "Expense",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                            ) {
                                expenseCats.forEach { cat ->
                                    FilterChip(
                                        selected = tempCategoryNames.contains(cat.name),
                                        onClick = {
                                            tempCategoryNames = if (tempCategoryNames.contains(cat.name)) {
                                                tempCategoryNames - cat.name
                                            } else {
                                                tempCategoryNames + cat.name
                                            }
                                        },
                                        label = { Text(cat.name) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                        }

                        if (incomeCats.isNotEmpty()) {
                            Text(
                                "Income",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                            ) {
                                incomeCats.forEach { cat ->
                                    FilterChip(
                                        selected = tempCategoryNames.contains(cat.name),
                                        onClick = {
                                            tempCategoryNames = if (tempCategoryNames.contains(cat.name)) {
                                                tempCategoryNames - cat.name
                                            } else {
                                                tempCategoryNames + cat.name
                                            }
                                        },
                                        label = { Text(cat.name) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))
            }

            // Sticky Footer
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpacingLarge)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            tempDateRange = null
                            tempAccountIds = emptySet()
                            tempCategoryNames = emptySet()
                            tempTransactionTypes = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                    Button(
                        onClick = {
                            if (isStats) {
                                viewModel.setStatsFilterDateRange(tempDateRange)
                                viewModel.setStatsFilterAccounts(tempAccountIds)
                                viewModel.setStatsFilterCategories(tempCategoryNames)
                                viewModel.setStatsFilterTransactionTypes(tempTransactionTypes)
                            } else {
                                viewModel.setFilterDateRange(tempDateRange)
                                viewModel.setFilterAccounts(tempAccountIds)
                                viewModel.setFilterCategories(tempCategoryNames)
                                viewModel.setFilterTransactionTypes(tempTransactionTypes)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        tempDateRange = Pair(start, end)
                    }
                    showDateRangePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    badgeCount: Int = 0,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = Dimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (badgeCount > 0) {
                    Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            content()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun FilterSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
