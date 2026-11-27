package com.saik.expensemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    
    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var isReorderMode by rememberSaveable { mutableStateOf(false) }
    
    val predefinedTypes = listOf("CASH", "WALLET", "BANK", "CREDIT_CARD", "GIFT_CARD")
    
    val typeOrder = remember(appSettings.accountTypeOrder, accounts) {
        val saved = appSettings.accountTypeOrder.split(",").filter { it.isNotBlank() }
        val current = accounts.map { it.type }.distinct()
        (saved + current).distinct()
    }

    val typeTitles = mapOf(
        "CASH" to "Cash", 
        "WALLET" to "Digital Wallets", 
        "BANK" to "Bank Accounts", 
        "CREDIT_CARD" to "Credit Cards",
        "GIFT_CARD" to "Gift Cards"
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Accounts Management",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isReorderMode = !isReorderMode }) {
                            Icon(
                                imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
                                contentDescription = "Toggle Reorder",
                                tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        floatingActionButton = {
            if (!isReorderMode) {
                FloatingActionButton(onClick = { showAddAccountDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        }
    ) { innerPadding ->
        if (isReorderMode) {
            ReorderView(
                accounts = accounts,
                typeOrder = typeOrder,
                typeTitles = typeTitles,
                onAccountOrderChange = { viewModel.updateAccountOrder(it) },
                onTypeOrderChange = { viewModel.updateAccountTypeOrder(it) },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
            ) {
                typeOrder.forEach { typeCode ->
                    val list = accounts.filter { it.type == typeCode && !it.isClosed }.sortedBy { it.displayOrder }
                    if (list.isNotEmpty() || predefinedTypes.contains(typeCode)) {
                        item(key = typeCode) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                                SectionHeader(typeTitles[typeCode] ?: typeCode, modifier = Modifier.padding(horizontal = 0.dp))
                                if (list.isEmpty()) {
                                    Text("No accounts configured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 4.dp))
                                } else {
                                    list.forEach { acc ->
                                        ManagementAccountCard(
                                            account = acc,
                                            viewModel = viewModel,
                                            currencySymbol = appSettings.currencyCode,
                                            onClick = { editingAccount = acc }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val closedAccounts = accounts.filter { it.isClosed }
                if (closedAccounts.isNotEmpty()) {
                    item(key = "closed_accounts") {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                            SectionHeader("Closed Accounts", modifier = Modifier.padding(horizontal = 0.dp))
                            closedAccounts.forEach { acc ->
                                ManagementAccountCard(
                                    account = acc,
                                    viewModel = viewModel,
                                    currencySymbol = appSettings.currencyCode,
                                    onClick = { editingAccount = acc }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        
        if (showAddAccountDialog) {
            AddCustomAccountDialog(
                existingTypes = typeOrder,
                onAddAccount = { name, t, isCc, cStart, cEnd, icon, initialBal ->
                    viewModel.addAccount(name, t, -1L, isCc, cStart, cEnd, icon, initialBal)
                    showAddAccountDialog = false
                },
                onDismiss = { showAddAccountDialog = false }
            )
        }

        if (editingAccount != null) {
            EditAccountDialog(
                account = editingAccount!!,
                viewModel = viewModel,
                onUpdateAccount = { viewModel.updateAccount(it) },
                onDeleteAccount = { viewModel.deleteAccount(it) },
                onCorrectBalance = { target -> viewModel.correctAccountBalance(editingAccount!!, target) },
                onDismiss = { editingAccount = null }
            )
        }
    }
}

@Composable
fun ReorderView(
    accounts: List<Account>,
    typeOrder: List<String>,
    typeTitles: Map<String, String>,
    onAccountOrderChange: (List<Account>) -> Unit,
    onTypeOrderChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var types by remember { mutableStateOf(typeOrder) }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
    ) {
        item {
            Text("Reorder Account Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(types.size) { index ->
            val type = types[index]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DragHandle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(typeTitles[type] ?: type, modifier = Modifier.weight(1f))
                    
                    Row {
                        IconButton(onClick = { 
                            if (index > 0) {
                                val newList = types.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index - 1, item)
                                types = newList
                                onTypeOrderChange(newList)
                            }
                        }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        IconButton(onClick = { 
                            if (index < types.size - 1) {
                                val newList = types.toMutableList()
                                val item = newList.removeAt(index)
                                newList.add(index + 1, item)
                                types = newList
                                onTypeOrderChange(newList)
                            }
                        }, enabled = index < types.size - 1) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text("Reorder Accounts within Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        types.forEach { type ->
            val typeAccounts = accounts.filter { it.type == type }.sortedBy { it.displayOrder }
            if (typeAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = typeTitles[type] ?: type,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(typeAccounts.size) { index ->
                    val acc = typeAccounts[index]
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AccountUtils.getAccountIcon(acc.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(acc.name, modifier = Modifier.weight(1f))
                            
                            Row {
                                IconButton(onClick = { 
                                    if (index > 0) {
                                        val newList = typeAccounts.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        onAccountOrderChange(newList)
                                    }
                                }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, null)
                                }
                                IconButton(onClick = { 
                                    if (index < typeAccounts.size - 1) {
                                        val newList = typeAccounts.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        onAccountOrderChange(newList)
                                    }
                                }, enabled = index < typeAccounts.size - 1) {
                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    viewModel: MainViewModel,
    onUpdateAccount: (Account) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onCorrectBalance: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var cycleStart by remember { mutableStateOf(account.billingCycleStartDay.toString()) }
    var cycleEnd by remember { mutableStateOf(account.billingCycleEndDay.toString()) }
    var selectedIconName by remember { mutableStateOf(account.iconName) }

    // Fee Tracking Fields
    var trackFeeWaiver by remember { mutableStateOf(account.trackFeeWaiver) }
    var annualFeeAmount by remember { mutableStateOf(account.annualFeeAmount?.toString() ?: "") }
    var feeWaiverThreshold by remember { mutableStateOf(account.feeWaiverThreshold?.toString() ?: "") }
    var feeCycleStartDate by remember { mutableLongStateOf(account.feeCycleStartDate ?: System.currentTimeMillis()) }
    var feeCycleType by remember { mutableStateOf(account.feeCycleType ?: "YEARLY") }
    
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    
    val currentBalance by viewModel.getAccountBalanceFlow(account.id).collectAsStateWithLifecycle(0.0)
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val currencySim = appSettings.currencyCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountUtils.getAccountIcon(selectedIconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Text("Edit Account", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("ACCOUNT NAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Text("ICON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp), // Minor padding for icons
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    AccountUtils.iconList.forEach { iconName ->
                        val isSelected = selectedIconName == iconName
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedIconName = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AccountUtils.getAccountIcon(iconName),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp)) // Fix: preventing clipping of last icon
                }

                if (account.isCreditCard) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                        OutlinedTextField(value = cycleStart, onValueChange = { cycleStart = it }, label = { Text("Cycle Start Day") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = cycleEnd, onValueChange = { cycleEnd = it }, label = { Text("Cycle End (Optional)") }, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                    // Fee Tracking Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.SpacingMedium)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Track Annual Fee Waiver", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Monitor eligible spend vs threshold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = trackFeeWaiver, onCheckedChange = { trackFeeWaiver = it })
                            }

                            AnimatedVisibility(visible = trackFeeWaiver) {
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall), modifier = Modifier.padding(top = Dimens.SpacingMedium)) {
                                    OutlinedTextField(
                                        value = annualFeeAmount,
                                        onValueChange = { annualFeeAmount = it },
                                        label = { Text("Annual Fee Amount") },
                                        prefix = { Text(currencySim) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = feeWaiverThreshold,
                                        onValueChange = { feeWaiverThreshold = it },
                                        label = { Text("Spend Threshold") },
                                        prefix = { Text(currencySim) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
                                    var showFeeDatePicker by remember { mutableStateOf(false) }
                                    
                                    OutlinedTextField(
                                        value = dateFmt.format(Date(feeCycleStartDate)),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Fee Cycle Start Date") },
                                        trailingIcon = { IconButton(onClick = { showFeeDatePicker = true }) { Icon(Icons.Default.CalendarToday, null) } },
                                        modifier = Modifier.fillMaxWidth().clickable { showFeeDatePicker = true },
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    if (showFeeDatePicker) {
                                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = feeCycleStartDate)
                                        DatePickerDialog(
                                            onDismissRequest = { showFeeDatePicker = false },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    feeCycleStartDate = datePickerState.selectedDateMillis ?: feeCycleStartDate
                                                    showFeeDatePicker = false
                                                }) { Text("OK") }
                                            }
                                        ) {
                                            DatePicker(state = datePickerState)
                                        }
                                    }

                                    Text(
                                        "If eligible spend reaches the threshold during the fee cycle, the annual fee is waived.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Button(
                    onClick = { showAdjustBalanceDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Balance, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Adjust Current Balance")
                }

                Button(
                    onClick = {
                        viewModel.toggleAccountClosed(account)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (account.isClosed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = if (account.isClosed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (account.isClosed) Icons.Default.LockOpen else Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (account.isClosed) "Re-open Account" else "Close Account")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val cStart = cycleStart.toIntOrNull() ?: 1
                    val cEndInput = cycleEnd.toIntOrNull()
                    val cEnd = cEndInput ?: if (cStart == 1) 31 else (cStart - 1)
                    
                    onUpdateAccount(account.copy(
                        name = name,
                        billingCycleStartDay = cStart,
                        billingCycleEndDay = cEnd,
                        iconName = selectedIconName,
                        trackFeeWaiver = trackFeeWaiver,
                        annualFeeAmount = annualFeeAmount.toDoubleOrNull(),
                        feeWaiverThreshold = feeWaiverThreshold.toDoubleOrNull(),
                        feeCycleStartDate = if (trackFeeWaiver) feeCycleStartDate else null,
                        feeCycleType = if (trackFeeWaiver) feeCycleType else null,
                        feeThresholdEnabled = trackFeeWaiver && annualFeeAmount.isNotBlank() && feeWaiverThreshold.isNotBlank()
                    ))
                    
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onDeleteAccount(account); onDismiss() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    )

    if (showAdjustBalanceDialog) {
        AdjustBalanceDialog(
            account = account,
            currentCalculatedBalance = currentBalance,
            currencySim = currencySim,
            onDismiss = { showAdjustBalanceDialog = false },
            onConfirm = { target -> onCorrectBalance(target) }
        )
    }
}


@Composable
fun ManagementAccountCard(
    account: Account,
    viewModel: MainViewModel,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val bal by viewModel.getAccountBalanceFlow(account.id).collectAsStateWithLifecycle(0.0)
    val colorFlow = if (bal >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

    SecondaryCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f), 
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountUtils.getAccountIcon(account.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = account.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (account.isClosed) {
                            Surface(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CLOSED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        if (account.isCreditCard) "Credit Card" else AccountUtils.formatAccountType(account.type),
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingMedium))

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 100.dp) // Ensure balance has enough width
            ) {
                val balanceSignAdjust = if (account.isCreditCard) -bal else bal
                Text(
                    text = CurrencyUtils.formatAmount(balanceSignAdjust, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorFlow,
                    maxLines = 1
                )
                Text(
                    text = if (account.isCreditCard) "Outstanding" else "Balance", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomAccountDialog(
    existingTypes: List<String>,
    onAddAccount: (name: String, type: String, isCreditCard: Boolean, cycleStart: Int, cycleEnd: Int, iconName: String, initialBalance: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var initialBalanceStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CASH") }
    var isCustomType by remember { mutableStateOf(false) }
    var customTypeName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Payments") }
    
    var isCreditCard by remember { mutableStateOf(false) }
    var cycleStart by remember { mutableStateOf("1") }
    var cycleEnd by remember { mutableStateOf("") }
    
    var showTypeDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(selectedType) {
        if (!isCustomType) {
            selectedIconName = when (selectedType) {
                "CASH" -> "Payments"
                "WALLET" -> "Wallet"
                "BANK" -> "AccountBalance"
                "CREDIT_CARD" -> "CreditCard"
                "GIFT_CARD" -> "CardGiftcard"
                else -> selectedIconName
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AccountUtils.getAccountIcon(selectedIconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
                Text("Add Account", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("ACCOUNT NAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Text("INITIAL BALANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = initialBalanceStr, 
                    onValueChange = { initialBalanceStr = it }, 
                    label = { Text("Current Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp)
                )

                Text("ICON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    AccountUtils.iconList.forEach { iconName ->
                        val isSelected = selectedIconName == iconName
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedIconName = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AccountUtils.getAccountIcon(iconName),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp)) // Fix: preventing clipping of last icon
                }

                ExposedDropdownMenuBox(expanded = showTypeDropdown, onExpandedChange = { showTypeDropdown = it }) {
                    val currentIcon = when (if (isCustomType) "" else selectedType) {
                        "CASH" -> Icons.Default.Payments
                        "WALLET" -> Icons.Default.AccountBalanceWallet
                        "BANK" -> Icons.Default.AccountBalance
                        "CREDIT_CARD" -> Icons.Default.CreditCard
                        else -> Icons.Default.AccountBalance
                    }
                    OutlinedTextField(
                        value = if (isCustomType) "Custom: $customTypeName" else AccountUtils.formatAccountType(selectedType), 
                        onValueChange = {}, 
                        readOnly = true, 
                        label = { Text("Account Type") }, 
                        leadingIcon = { Icon(currentIcon, null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) }, 
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = showTypeDropdown, 
                        onDismissRequest = { showTypeDropdown = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        existingTypes.forEach { t -> 
                            val icon = when (t) {
                                "CASH" -> Icons.Default.Payments
                                "WALLET" -> Icons.Default.AccountBalanceWallet
                                "BANK" -> Icons.Default.AccountBalance
                                "GIFT_CARD" -> Icons.Default.CardGiftcard
                                else -> Icons.Default.CreditCard
                            }
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(AccountUtils.formatAccountType(t), style = MaterialTheme.typography.bodyLarge)
                                    }
                                }, 
                                onClick = { selectedType = t; isCustomType = false; isCreditCard = (t == "CREDIT_CARD"); showTypeDropdown = false },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) 
                        }
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                    Text("Create Custom Type...", style = MaterialTheme.typography.bodyLarge) 
                                }
                            }, 
                            onClick = { isCustomType = true; isCreditCard = false; showTypeDropdown = false },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                
                if (isCustomType) {
                    OutlinedTextField(value = customTypeName, onValueChange = { customTypeName = it.uppercase(Locale.US) }, label = { Text("Custom Type Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
                
                if (selectedType == "CREDIT_CARD" || isCreditCard) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                        OutlinedTextField(value = cycleStart, onValueChange = { cycleStart = it }, label = { Text("Cycle Start") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = cycleEnd, onValueChange = { cycleEnd = it }, label = { Text("Cycle End") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    val t = if (isCustomType && customTypeName.isNotBlank()) customTypeName else selectedType
                    val cStart = cycleStart.toIntOrNull() ?: 1
                    val cEndInput = cycleEnd.toIntOrNull()
                    val cEnd = cEndInput ?: if (cStart == 1) 31 else (cStart - 1)
                    val initialBal = initialBalanceStr.toDoubleOrNull() ?: 0.0
                    onAddAccount(name, t, t == "CREDIT_CARD" || isCreditCard, cStart, cEnd, selectedIconName, initialBal)
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
