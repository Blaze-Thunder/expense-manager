package com.saik.expensemanager.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog as ComposeDatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.RepetitiveTransaction
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.data.model.displayBrandName
import com.saik.expensemanager.ui.components.Dimens
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.saik.expensemanager.ui.components.AddEditCategoryBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorSheet(
    initialValue: String,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf(initialValue) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2D2824)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.SpacingLarge)
                .padding(bottom = 32.dp)
                .statusBarsPadding()
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.End,
                color = Color.White
            )

            val buttons = listOf(
                listOf("C", "/", "*", "DEL"),
                listOf("7", "8", "9", "-"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "="),
                listOf(".", "0", "00")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { char ->
                            val isAction = char in listOf("C", "/", "*", "DEL", "-", "+", "=")
                            val weight = if (char == "=") 1f else 1f

                            Button(
                                onClick = {
                                    when (char) {
                                        "C" -> expression = ""
                                        "DEL" -> if (expression.isNotEmpty()) expression = expression.dropLast(1)
                                        "=" -> {
                                            expression = try { evaluateBasic(expression) } catch (_: Exception) { "Error" }
                                        }
                                        else -> expression += char
                                    }
                                },
                                modifier = Modifier.weight(weight).height(64.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (char == "=") Color(0xFFF3A66B) else if (isAction) Color(0xFF3D3834) else Color(0xFF4D4844),
                                    contentColor = if (char == "=") Color.Black else if (isAction) Color(0xFFF3A66B) else Color.White
                                )
                            ) {
                                if (char == "DEL") Icon(Icons.AutoMirrored.Filled.Backspace, null, modifier = Modifier.size(20.dp))
                                else Text(char, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val result = evaluateBasic(expression)
                        if (result != "Error") {
                            onResult(result)
                        } else {
                            onResult(expression)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3A66B))
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

fun evaluateBasic(expr: String): String {
    return try {
        val cleanExpr = expr.replace(" ", "")
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()

        var i = 0
        while (i < cleanExpr.length) {
            val c = cleanExpr[i]
            if (c.isDigit() || c == '.') {
                currentNum.append(c)
            } else {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                }
                if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    currentNum.append(c)
                } else {
                    tokens.add(c.toString())
                }
            }
            i++
        }
        if (currentNum.isNotEmpty()) tokens.add(currentNum.toString())

        if (tokens.isEmpty()) return "0"

        val pass1 = mutableListOf<String>()
        var j = 0
        while (j < tokens.size) {
            val token = tokens[j]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.size - 1).toDouble()
                val next = tokens[j + 1].toDouble()
                val res = if (token == "*") prev * next else prev / next
                pass1.add(res.toString())
                j += 2
            } else {
                pass1.add(token)
                j++
            }
        }

        var result = pass1[0].toDouble()
        var k = 1
        while (k < pass1.size) {
            val op = pass1[k]
            val nextVal = pass1[k + 1].toDouble()
            result = if (op == "+") result + nextVal else result - nextVal
            k += 2
        }

        if (result.isInfinite() || result.isNaN()) return "Error"
        if (result % 1.0 == 0.0) result.toLong().toString() else "%.2f".format(Locale.US, result)
    } catch (_: Exception) {
        "Error"
    }
}

@Composable
fun TypeButton(
    type: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        if (type == "EXPENSE") Color(0xFFE57373) else if (type == "INCOME") Color(0xFF81C784) else Color(0xFFF3A66B)
    } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)

    val contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectorSheet(
    accounts: List<Account>,
    viewModel: com.saik.expensemanager.ui.viewmodel.MainViewModel,
    currencySim: String,
    selectedAccountId: Long = -1L,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedTypes = listOf("CASH", "WALLET", "BANK", "CREDIT_CARD")
    val grouped = accounts.groupBy { it.type }
    val allTypes = predefinedTypes + grouped.keys.filter { !predefinedTypes.contains(it) }.sorted()

    val typeTitles = mapOf(
        "CASH" to "Cash",
        "WALLET" to "Digital Wallets",
        "BANK" to "Bank Accounts",
        "CREDIT_CARD" to "Credit Cards",
        "GIFT_CARD" to "Gift Cards"
    )

    // Filter chips derive labels from actual account types in data
    val filterLabels = remember(accounts) {
        val typesInData = accounts.map { it.type }.distinct()
        val ordered = listOf("CASH", "WALLET", "BANK", "CREDIT_CARD")
        val present = ordered.filter { it in typesInData } + typesInData.filter { it !in ordered }
        listOf("ALL") + present
    }

    // State: selected filter chip, search query, expanded/collapsed sections
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedSections by remember {
        mutableStateOf(allTypes.associateWith { true })
    }

    // Track recently used accounts (last 2-3, persisted for session via remember)
    var recentlyUsedIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Derive effective filter type
    val isSearchActive = searchQuery.isNotBlank()
    val effectiveFilterType = when {
        isSearchActive -> null  // search overrides filter — show all
        selectedFilterIndex == 0 -> null  // "All"
        else -> filterLabels.getOrNull(selectedFilterIndex)
    }

    val filteredAccounts = remember(accounts, effectiveFilterType, searchQuery, selectedAccountId) {
        accounts.filter { acc ->
            val closedMatch = !acc.isClosed || acc.id == selectedAccountId
            val typeMatch = effectiveFilterType == null || acc.type == effectiveFilterType
            val searchMatch = !isSearchActive ||
                acc.name.contains(searchQuery, ignoreCase = true) ||
                (typeTitles[acc.type] ?: AccountUtils.formatAccountType(acc.type))
                    .contains(searchQuery, ignoreCase = true)
            closedMatch && typeMatch && searchMatch
        }
    }

    val recentlyUsedAccounts = remember(accounts, recentlyUsedIds) {
        accounts.filter { it.id in recentlyUsedIds }
    }

    // When searching, auto-expand all sections; otherwise respect user toggles
    val autoExpanded = isSearchActive
    val displayExpandedSections = if (autoExpanded) {
        allTypes.associateWith { true }
    } else {
        expandedSections
    }

    // Group filtered accounts by type for grid rendering
    val filteredGrouped = remember(filteredAccounts) {
        filteredAccounts.groupBy { it.type }
    }
    val filteredTypesInOrder = remember(filteredGrouped, allTypes) {
        allTypes.filter { it in filteredGrouped }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF231E1A),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = 32.dp)
        ) {
            // --- Title ---
            Text(
                "Select Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            // --- Search Bar (pinned at top above filter chips) ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search accounts…", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFF3A66B),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color(0xFFF3A66B)
                ),
                shape = RoundedCornerShape(Dimens.SpacingMedium)
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // --- Filter Chips Row (horizontally scrollable) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                filterLabels.forEachIndexed { index, typeCode ->
                    val label = if (typeCode == "ALL") "All"
                    else typeTitles[typeCode] ?: AccountUtils.formatAccountType(typeCode)
                    FilterChip(
                        selected = selectedFilterIndex == index,
                        onClick = {
                            selectedFilterIndex = index
                            searchQuery = ""  // clear search when switching chips
                        },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF3A66B).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFF3A66B)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // --- Recently Used Section (always visible, horizontal scroll chips) ---
            if (recentlyUsedAccounts.isNotEmpty()) {
                Text(
                    "Recently Used",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF3A66B).copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = Dimens.SpacingSmall)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = Dimens.SpacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                ) {
                    recentlyUsedAccounts.forEach { acc ->
                        val balance by viewModel.getAccountBalanceFlow(acc.id)
                            .collectAsStateWithLifecycle(0.0)
                        CompactRecentAccountChip(
                            account = acc,
                            balance = balance,
                            currencySim = currencySim,
                            isSelected = acc.id == selectedAccountId,
                            onClick = {
                                recentlyUsedIds =
                                    (listOf(acc.id) + recentlyUsedIds.filter { it != acc.id }).take(3)
                                onSelect(acc)
                            },
                            onDismiss = {
                                recentlyUsedIds = recentlyUsedIds.filter { it != acc.id }
                            }
                        )
                    }
                }
            }

            // --- Account Categories with Horizontal LazyRow per category ---
            if (filteredAccounts.isEmpty()) {
                Text(
                    "No accounts found",
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpacingExtraLarge),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    filteredTypesInOrder.forEach { typeCode ->
                        val accList = filteredGrouped[typeCode] ?: emptyList()
                        if (accList.isEmpty()) return@forEach

                        val isExpanded = displayExpandedSections[typeCode] ?: true

                        // Category header item
                        item(key = "header_$typeCode") {
                            SectionHeader(
                                title = typeTitles[typeCode]
                                    ?: AccountUtils.formatAccountType(typeCode),
                                count = accList.size,
                                isExpanded = isExpanded,
                                onToggle = {
                                    if (!autoExpanded) {
                                        expandedSections = expandedSections.toMutableMap().apply {
                                            put(typeCode, !(this[typeCode] ?: true))
                                        }
                                    }
                                }
                            )
                        }

                        // Horizontal card row (only rendered when expanded)
                        if (isExpanded) {
                            item(key = "row_$typeCode") {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) {
                                    items(accList, key = { it.id }) { acc ->
                                        val balance by viewModel.getAccountBalanceFlow(acc.id)
                                            .collectAsStateWithLifecycle(0.0)
                                        AccountGridCard(
                                            account = acc,
                                            balance = balance,
                                            currencySim = currencySim,
                                            isSelected = acc.id == selectedAccountId,
                                            onClick = {
                                                recentlyUsedIds =
                                                    (listOf(acc.id) + recentlyUsedIds.filter { it != acc.id }).take(3)
                                                onSelect(acc)
                                            },
                                            modifier = Modifier.width(140.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Compact Account Grid Card ---
@Composable
private fun AccountGridCard(
    account: Account,
    balance: Double,
    currencySim: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceSignAdjust = if (account.isCreditCard) -balance else balance
    val balanceColor = when {
        balanceSignAdjust > 0 -> Color(0xFF4CAF50)
        balanceSignAdjust < 0 -> Color(0xFFEF5350)
        else -> Color.White.copy(alpha = 0.6f)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF3A66B).copy(alpha = 0.1f) else Color(0xFF2D2824)
        ),
        shape = RoundedCornerShape(Dimens.SpacingMedium),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFFF3A66B)) else null
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Icon circle
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
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Account name (single line, ellipsis)
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Balance (green positive, red negative)
                Text(
                    text = CurrencyUtils.formatAmount(balanceSignAdjust, currencySim)
                        .replace(".00", ""),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFFF3A66B),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Dimens.SpacingSmall)
                        .size(16.dp)
                )
            }
        }
    }
}

// --- Section Header with Collapse/Expand ---
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = Dimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3A66B)
            )
            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess
            else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- Compact Recent Account Chip (AssistChip with name + balance) ---
@Composable
private fun CompactRecentAccountChip(
    account: Account,
    balance: Double,
    currencySim: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val balanceSignAdjust = if (account.isCreditCard) -balance else balance
    val balanceColor = when {
        balanceSignAdjust > 0 -> Color(0xFF4CAF50)
        balanceSignAdjust < 0 -> Color(0xFFEF5350)
        else -> Color.White.copy(alpha = 0.6f)
    }

    AssistChip(
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(balanceSignAdjust, currencySim)
                            .replace(".00", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                }
                if (isSelected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFF3A66B)
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = AccountUtils.getAccountIcon(account.iconName),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color(0xFFF3A66B) else MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(12.dp),
                    tint = Color.White.copy(alpha = 0.4f)
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) Color(0xFFF3A66B).copy(alpha = 0.1f) else Color(0xFF2D2824)
        ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFF3A66B) else Color.White.copy(alpha = 0.15f))
    )
}

sealed class CategoryPickerPage {
    object PrimaryList : CategoryPickerPage()
    data class SubcategoryList(val parent: Category) : CategoryPickerPage()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorSheet(
    viewModel: com.saik.expensemanager.ui.viewmodel.MainViewModel,
    type: String, // "EXPENSE" or "INCOME"
    initialParentName: String? = null,
    initialSubCategoryName: String? = null,
    onSelect: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    
    var currentPage by remember { mutableStateOf<CategoryPickerPage>(CategoryPickerPage.PrimaryList) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogParentId by remember { mutableStateOf<Long?>(null) }

    // Lifted LazyListStates to preserve scroll position across internal navigation
    val primaryListState = rememberLazyListState()
    val subcategoryListState = rememberLazyListState()

    val parents = remember(categories, type) { 
        categories.filter { it.parentId == null && it.type == type } 
    }
    val childrenByParent = remember(categories, type) { 
        categories.filter { it.parentId != null && it.type == type }.groupBy { it.parentId } 
    }

    // Auto-navigate if editing
    LaunchedEffect(Unit) {
        if (!initialParentName.isNullOrEmpty()) {
            val parent = parents.find { it.name == initialParentName }
            if (parent != null) {
                if (!initialSubCategoryName.isNullOrEmpty()) {
                    currentPage = CategoryPickerPage.SubcategoryList(parent)
                } else if (childrenByParent.containsKey(parent.id)) {
                     currentPage = CategoryPickerPage.SubcategoryList(parent)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF231E1A),
        contentColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        // Handle back press internally within the sheet
        BackHandler(enabled = currentPage is CategoryPickerPage.SubcategoryList) {
            currentPage = CategoryPickerPage.PrimaryList
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState is CategoryPickerPage.SubcategoryList) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "category_picker_nav"
            ) { page ->
                when (page) {
                    is CategoryPickerPage.PrimaryList -> {
                        PrimaryCategoryPage(
                            parents = parents,
                            lazyListState = primaryListState,
                            onParentClick = { parent ->
                                if (childrenByParent.containsKey(parent.id)) {
                                    currentPage = CategoryPickerPage.SubcategoryList(parent)
                                } else {
                                    onSelect(parent.name, null)
                                }
                            },
                            onAddNew = {
                                addDialogParentId = null
                                showAddDialog = true
                            }
                        )
                    }
                    is CategoryPickerPage.SubcategoryList -> {
                        SubcategoryPage(
                            parent = page.parent,
                            children = childrenByParent[page.parent.id] ?: emptyList(),
                            selectedSubName = initialSubCategoryName,
                            lazyListState = subcategoryListState,
                            onBack = { currentPage = CategoryPickerPage.PrimaryList },
                            onSelect = { subName -> onSelect(page.parent.name, subName) },
                            onAddNew = {
                                addDialogParentId = page.parent.id
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCategoryBottomSheet(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, categoryType, color, parentId, iconName ->
                viewModel.addCategory(name, categoryType, color, parentId, iconName) { newId ->
                    if (parentId == null) {
                        onSelect(name, null)
                    } else {
                        val parent = parents.find { it.id == parentId }
                        if (parent != null) {
                            onSelect(parent.name, name)
                        }
                    }
                    showAddDialog = false
                }
            },
            categories = categories,
            initialType = type,
            initialParentId = addDialogParentId,
            lockType = true,
            lockParent = addDialogParentId != null
        )
    }
}

@Composable
fun PrimaryCategoryPage(
    parents: List<Category>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onParentClick: (Category) -> Unit,
    onAddNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Select Category", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(Dimens.SpacingLarge)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge)
        ) {
            items(parents) { parent ->
                CategoryRow(
                    category = parent,
                    showChevron = true,
                    onClick = { onParentClick(parent) }
                )
            }
            
            item {
                AddNewActionRow(
                    label = "Add New Category",
                    onClick = onAddNew
                )
            }
        }
    }
}

@Composable
fun SubcategoryPage(
    parent: Category,
    children: List<Category>,
    selectedSubName: String?,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit,
    onAddNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingLarge)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                parent.name, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge)
        ) {
            item {
                CategoryRow(
                    category = parent,
                    labelOverride = "${parent.name} (General)",
                    isSelected = selectedSubName == null,
                    onClick = { onSelect(null) }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Dimens.SpacingSmall),
                    color = Color.White.copy(alpha = 0.1f)
                )
                
                if (children.isNotEmpty()) {
                    Text(
                        "Subcategories",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = Dimens.SpacingSmall)
                    )
                }
            }

            items(children) { child ->
                CategoryRow(
                    category = child,
                    isSelected = child.name == selectedSubName,
                    onClick = { onSelect(child.name) }
                )
            }

            item {
                AddNewActionRow(
                    label = "Add New Subcategory",
                    onClick = onAddNew
                )
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    labelOverride: String? = null,
    showChevron: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(category.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val iconName = category.iconName
                val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                if (isEmoji) {
                    Text(text = iconName, fontSize = 18.sp)
                } else {
                    Icon(
                        CategoryUtils.getCategoryIcon(iconName), 
                        null, 
                        tint = Color(category.color), 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = labelOverride ?: category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (showChevron) {
                Icon(
                    Icons.Default.KeyboardArrowRight, 
                    null, 
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddNewActionRow(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSmall)
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add, 
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftCardSelectorSheet(
    giftCards: List<GiftCard>,
    onSelect: (GiftCard?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF231E1A),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Gift Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSelect(null); onDismiss() }) {
                    Text("Clear Selection", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            if (giftCards.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No gift cards available", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                    items(giftCards) { gc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(gc); onDismiss() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CardGiftcard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(gc.code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("${gc.displayBrandName()} • Amount: ${CurrencyUtils.formatAmount(gc.amount, "")}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            if (gc.isUsed) {
                                Text("USED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetBookSelectorSheet(
    budgetBooks: List<BudgetBook>,
    currencySim: String,
    onSelect: (BudgetBook) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF231E1A),
        contentColor = Color.White
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
            Text("Select Budget Book", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                items(budgetBooks) { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(book) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        book.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (book.isDefault) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) {
                                            Text("Default", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                val limit = book.monthlyLimit
                                Text(
                                    if (limit != null) "Limit: ${CurrencyUtils.formatAmount(limit, currencySim)}" else "Spend Tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecurrenceOptionsView(
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    interval: Int,
    onIntervalChange: (Int) -> Unit,
    endDate: Long?,
    onEndDateChange: (Long?) -> Unit,
    occurrences: Int?,
    onOccurrencesChange: (Int?) -> Unit,
    dayOfWeek: Int?,
    onDayOfWeekChange: (Int?) -> Unit,
    dayOfMonth: Int?,
    onDayOfMonthChange: (Int?) -> Unit,
    reflectionTiming: String = "ON_DATE",
    onReflectionTimingChange: (String) -> Unit = {},
    onAnyFieldFocused: () -> Unit = {},
    occurrencesBringIntoViewRequester: BringIntoViewRequester? = null
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val scope = rememberCoroutineScope()
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Frequency
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onAnyFieldFocused() },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    frequencies.forEach { f ->
                        DropdownMenuItem(
                            text = { Text(f, style = MaterialTheme.typography.bodyLarge) },
                            onClick = { 
                                onFrequencyChange(f)
                                expanded = false 
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = interval.toString(),
                onValueChange = { onIntervalChange(it.toIntOrNull() ?: 1) },
                label = { Text("Interval") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(0.5f)
                    .onFocusChanged { if (it.isFocused) onAnyFieldFocused() },
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (frequency == "WEEKLY") {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val calendarDays = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
            Text("Day of Week", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                days.forEachIndexed { index, day ->
                    SegmentedButton(
                        selected = dayOfWeek == calendarDays[index],
                        onClick = { onDayOfWeekChange(calendarDays[index]) },
                        shape = SegmentedButtonDefaults.itemShape(index, days.size)
                    ) { Text(day, fontSize = 10.sp) }
                }
            }
        }

        if (frequency == "MONTHLY") {
            OutlinedTextField(
                value = dayOfMonth?.toString() ?: "",
                onValueChange = { onDayOfMonthChange(it.toIntOrNull()) },
                label = { Text("Day of Month (1-31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onAnyFieldFocused() }
            )
        }

        // Reflection Timing
        Column {
            Text("Reflection Timing", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = reflectionTiming == "ON_DATE",
                    onClick = { onReflectionTimingChange("ON_DATE") },
                    label = { Text("On Date") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = reflectionTiming == "FIRST_OF_MONTH",
                    onClick = { onReflectionTimingChange("FIRST_OF_MONTH") },
                    label = { Text("1st of Month") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = if (endDate != null) sdf.format(Date(endDate)) else "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Until (Optional)") },
                modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                trailingIcon = { if (endDate != null) IconButton(onClick = { onEndDateChange(null) }) { Icon(Icons.Default.Clear, null) } },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            OutlinedTextField(
                value = occurrences?.toString() ?: "",
                onValueChange = { onOccurrencesChange(it.toIntOrNull()) },
                label = { Text("Occurrences", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(0.7f)
                    .then(if (occurrencesBringIntoViewRequester != null) Modifier.bringIntoViewRequester(occurrencesBringIntoViewRequester) else Modifier)
                    .onFocusChanged { 
                        if (it.isFocused) {
                            onAnyFieldFocused()
                            scope.launch {
                                delay(150)
                                occurrencesBringIntoViewRequester?.bringIntoView()
                            }
                        }
                    }
            )
        }
    }

    if (showDatePicker) {
        ComposeDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onEndDateChange(it) }
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
