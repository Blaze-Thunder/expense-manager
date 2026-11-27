package com.saik.expensemanager.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog as ComposeDatePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.itemsIndexed
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.GiftCardBrand
import com.saik.expensemanager.data.model.GiftCardData
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.data.model.TransactionTemplate
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.ExperimentalFoundationApi

class GiftCardItemState(
    val id: String = UUID.randomUUID().toString(),
    initialBrand: GiftCardBrand = GiftCardBrand.AMAZON_PAY,
    initialCustomBrand: String = "",
    initialCode: String = "",
    initialAmount: String = "",
    initialValidityDays: String = "365",
    initialNote: String = "",
    initialIsExpanded: Boolean = true
) {
    var brand by mutableStateOf(initialBrand)
    var customBrandName by mutableStateOf(initialCustomBrand)
    var code by mutableStateOf(initialCode)
    var amount by mutableStateOf(initialAmount)
    var validityDays by mutableStateOf(initialValidityDays)
    var note by mutableStateOf(initialNote)
    var isExpanded by mutableStateOf(initialIsExpanded)
    var brandExpanded by mutableStateOf(false)
    var validityExpanded by mutableStateOf(false)
    var codeError by mutableStateOf<String?>(null)
    var amountError by mutableStateOf<String?>(null)
    var brandError by mutableStateOf<String?>(null)
    var validityError by mutableStateOf<String?>(null)
    val codeFocusRequester = FocusRequester()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionEntryScreen(
    viewModel: MainViewModel,
    initialTransaction: Transaction? = null,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val budgetBooks by viewModel.budgetBooks.collectAsStateWithLifecycle()
    val activeBudgetBooks by viewModel.activeBudgetBooks.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBudgetBook.collectAsStateWithLifecycle()
    val allRepetitiveTransactions by viewModel.allRepetitiveTransactions.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    val currencySim = activeBook?.currencyCode ?: appSettings.currencyCode

    var amountStr by remember { 
        mutableStateOf(
            initialTransaction?.amount?.let { 
                if (it % 1.0 == 0.0) it.toInt().toString() 
                else String.format(Locale.US, "%.2f", it)
            } ?: ""
        ) 
    }
    var type by remember { mutableStateOf(initialTransaction?.type ?: "EXPENSE") }
    var selectedCategoryName by remember { mutableStateOf(initialTransaction?.category ?: "") }
    var selectedSubCategoryName by remember { mutableStateOf(initialTransaction?.subCategory) }
    var selectedAccountId by remember { mutableLongStateOf(initialTransaction?.linkedAccountId ?: -1L) }
    var selectedBudgetBookId by remember { 
        mutableLongStateOf(
            initialTransaction?.linkedBudgetBookId ?: 
            budgetBooks.find { it.isDefault }?.id ?: 
            activeBook?.id ?: -1L
        ) 
    }
    var toAccountId by remember { mutableLongStateOf(initialTransaction?.toAccountId ?: -1L) }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var timestamp by remember { mutableLongStateOf(initialTransaction?.dateTime ?: System.currentTimeMillis()) }
    var linkedGiftCardId by remember { mutableLongStateOf(initialTransaction?.linkedGiftCardId ?: -1L) }
    var isCreditCardPayment by remember { 
        mutableStateOf(
            initialTransaction?.isCreditCardPayment ?: true 
        ) 
    }

    val toAccount = accounts.find { it.id == toAccountId }
    val showCcPaymentToggle = type == "TRANSFER" && toAccount?.isCreditCard == true
    val isTransferToGiftCard = type == "TRANSFER" && toAccount?.type == "GIFT_CARD"
    val giftCardItems = remember { mutableStateListOf<GiftCardItemState>() }

    LaunchedEffect(toAccountId, type) {
        if (isTransferToGiftCard) {
            giftCardItems.clear()
            var loaded = false
            if (initialTransaction != null && initialTransaction.id != 0L) {
                val existingCards = viewModel.getGiftCardsForTransaction(initialTransaction.id)
                if (existingCards.isNotEmpty()) {
                    existingCards.forEach { card ->
                        val b = try { GiftCardBrand.valueOf(card.brand) } catch (_: Exception) { GiftCardBrand.OTHER }
                        giftCardItems.add(
                            GiftCardItemState(
                                initialBrand = b,
                                initialCustomBrand = card.customBrandName ?: "",
                                initialCode = card.code,
                                initialAmount = if (card.amount % 1.0 == 0.0) card.amount.toInt().toString() else String.format(Locale.US, "%.2f", card.amount),
                                initialValidityDays = card.validityDays.toString(),
                                initialNote = card.note ?: ""
                            )
                        )
                    }
                    loaded = true
                }
            }
            if (!loaded) {
                val targetAcc = accounts.find { it.id == toAccountId }
                val guessedBrand = when {
                    targetAcc?.name?.contains("Amazon", ignoreCase = true) == true -> GiftCardBrand.AMAZON_PAY
                    targetAcc?.name?.contains("Flipkart", ignoreCase = true) == true -> GiftCardBrand.FLIPKART
                    targetAcc?.name?.contains("Myntra", ignoreCase = true) == true -> GiftCardBrand.MYNTRA
                    targetAcc?.name?.contains("Google", ignoreCase = true) == true || targetAcc?.name?.contains("Play", ignoreCase = true) == true -> GiftCardBrand.GOOGLE_PLAY
                    targetAcc?.name?.contains("Paytm", ignoreCase = true) == true -> GiftCardBrand.PAYTM
                    else -> GiftCardBrand.OTHER
                }
                val customBrand = if (guessedBrand == GiftCardBrand.OTHER) targetAcc?.name?.replace(" Gift Card", "", ignoreCase = true)?.trim() ?: "" else ""
                giftCardItems.add(GiftCardItemState(initialBrand = guessedBrand, initialCustomBrand = customBrand, initialAmount = amountStr))
            }
        } else {
            giftCardItems.clear()
        }
    }

    val rep = remember(initialTransaction, allRepetitiveTransactions) {
        initialTransaction?.repetitiveId?.let { id -> allRepetitiveTransactions.find { it.id == id } }
    }

    // Default to false when editing so only the single transaction instance is edited.
    // The user can toggle Repeat ON if they wish to update the recurring template as well.
    var isRepeat by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(rep?.frequency ?: "MONTHLY") }
    var interval by remember { mutableIntStateOf(rep?.interval ?: 1) }
    var endDate by remember { mutableStateOf(rep?.endDate) }
    var occurrences by remember { mutableStateOf(rep?.occurrences) }
    var dayOfWeek by remember { mutableStateOf(rep?.dayOfWeek) }
    var dayOfMonth by remember { mutableStateOf(rep?.dayOfMonth) }
    var reflectionTiming by remember { mutableStateOf(rep?.reflectionTiming ?: "ON_DATE") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var toAccountError by remember { mutableStateOf<String?>(null) }

    var showCalculator by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showBudgetBookSelector by remember { mutableStateOf(false) }
    var showToAccountSelector by remember { mutableStateOf(false) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showSaveTemplateSheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dateTimeFormat = remember { SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.US) }
    val amountFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    var isDescriptionFocused by remember { mutableStateOf(false) }
    
    val noteBringIntoViewRequester = remember { BringIntoViewRequester() }
    val recurringBringIntoViewRequester = remember { BringIntoViewRequester() }
    val occurrencesBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isEditing = initialTransaction != null && initialTransaction.id != 0L

    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250.milliseconds)
        isReady = true
        kotlinx.coroutines.delay(100.milliseconds)
        if (initialTransaction == null) {
            amountFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            // Optional: minimal placeholder top bar
            Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = MaterialTheme.colorScheme.surface) {}
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        TopAppBar(
                            title = { 
                                Column {
                                    Text(if (!isEditing) "New Entry" else "Edit Entry", fontWeight = FontWeight.Bold)
                                    if (isEditing) {
                                        val currentBook = budgetBooks.find { it.id == selectedBudgetBookId }
                                        if (currentBook != null) {
                                            Text(
                                                text = "Book: ${currentBook.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSaveTemplateSheet = true }) {
                                    Icon(
                                        Icons.Default.Style,
                                        contentDescription = "Save as Template",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (isEditing) {
                                    IconButton(onClick = { showDeleteConfirmation = true }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete", 
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    }
                },
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = {}
            ) { innerPadding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Dimens.SpacingLarge,
                        end = Dimens.SpacingLarge,
                        top = Dimens.SpacingLarge,
                        bottom = Dimens.SpacingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
                ) {
                    // Item 0: Type Selector
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            color = Color.Transparent
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val types = listOf(
                                    Triple("INCOME", "Income", Color(0xFF81C784)),
                                    Triple("EXPENSE", "Expense", Color(0xFFE57373)),
                                    Triple("TRANSFER", "Transfer", MaterialTheme.colorScheme.outline)
                                )
                                
                                types.forEachIndexed { index, (typeCode, label, color) ->
                                    val isSelected = type == typeCode
                                    val isLast = index == types.size - 1
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (isSelected) color else Color.Transparent)
                                            .clickable { 
                                                type = typeCode
                                                if (typeCode == "TRANSFER") isRepeat = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (!isLast) {
                                        VerticalDivider(
                                            modifier = Modifier.fillMaxHeight(),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Item 1: Amount Section
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 2.dp,
                                    color = if (amountError != null) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currencySim,
                                        style = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        BasicTextField(
                                            value = amountStr,
                                            onValueChange = { 
                                                amountStr = it
                                                if (amountError != null) amountError = null
                                            },
                                            textStyle = TextStyle(
                                                fontSize = 32.sp, 
                                                fontWeight = FontWeight.Bold, 
                                                color = if (amountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, 
                                                textAlign = TextAlign.Start
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = { 
                                                    if (selectedAccountId == -1L) {
                                                        showAccountSelector = true
                                                    } else if (type != "TRANSFER" && selectedCategoryName.isEmpty()) {
                                                        showCategorySelector = true
                                                    } else if (type == "TRANSFER" && toAccountId == -1L) {
                                                        showToAccountSelector = true
                                                    } else if (description.isEmpty()) {
                                                        descriptionFocusRequester.requestFocus()
                                                    } else {
                                                        focusManager.clearFocus()
                                                    }
                                                }
                                            ),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(amountFocusRequester),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (amountStr.isEmpty()) {
                                                        Text(
                                                            text = "0",
                                                            style = TextStyle(
                                                                fontSize = 32.sp, 
                                                                fontWeight = FontWeight.Bold, 
                                                                color = MaterialTheme.colorScheme.outline
                                                            )
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    )

                                    IconButton(
                                        onClick = { showCalculator = true },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Calculate,
                                            contentDescription = "Calculator",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            
                            amountError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Item 2: Field Cards
                    item {
                        SecondaryCard(modifier = Modifier.animateContentSize()) {
                            Column {
                                val selectedBook = budgetBooks.find { it.id == selectedBudgetBookId }
                                FieldRow(
                                    icon = Icons.Default.Book,
                                    label = "Budget Book",
                                    value = selectedBook?.name ?: "Select",
                                    onClick = { showBudgetBookSelector = true }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                val selectedAccount = accounts.find { it.id == selectedAccountId }
                                FieldRow(
                                    icon = selectedAccount?.let { AccountUtils.getAccountIcon(it.iconName) } ?: Icons.Default.AccountBalanceWallet,
                                    label = "Account",
                                    value = selectedAccount?.name ?: "Select",
                                    errorText = accountError,
                                    onClick = { showAccountSelector = true }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 52.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                if (type != "TRANSFER") {
                                    val parentCat = categories.find { it.name == selectedCategoryName && it.type == type && it.parentId == null }
                                    val subCat = if (selectedSubCategoryName != null) {
                                        categories.find { it.name == selectedSubCategoryName && it.type == type && it.parentId == parentCat?.id }
                                    } else null
                                    
                                    val displayCat = subCat ?: parentCat
                                    val iconName = displayCat?.iconName ?: "Label"
                                    val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                                    val isSelected = selectedCategoryName.isNotEmpty()
                                    val catColor = if (isSelected) displayCat?.color?.let { Color(it) } ?: Color(0xFFF3A66B) else MaterialTheme.colorScheme.onSurface
                                    
                                    FieldRow(
                                        customIcon = {
                                            if (isEmoji) {
                                                Text(text = iconName, fontSize = 16.sp)
                                            } else {
                                                Icon(
                                                    CategoryUtils.getCategoryIcon(iconName), 
                                                    null, 
                                                    tint = if (categoryError != null) MaterialTheme.colorScheme.error else (if (isSelected) catColor else MaterialTheme.colorScheme.outline),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        label = "Category",
                                        value = if (selectedSubCategoryName != null) "$selectedCategoryName/$selectedSubCategoryName" else selectedCategoryName.ifEmpty { "Select" },
                                        valueColor = catColor,
                                        errorText = categoryError,
                                        onClick = { showCategorySelector = true }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                } else {
                                    val toAccount = accounts.find { it.id == toAccountId }
                                    FieldRow(
                                        icon = toAccount?.let { AccountUtils.getAccountIcon(it.iconName) } ?: Icons.AutoMirrored.Filled.Login,
                                        label = "To Account",
                                        value = toAccount?.name ?: "Select To Account",
                                        errorText = toAccountError,
                                        onClick = { showToAccountSelector = true }
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                                FieldRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Date",
                                    value = dateTimeFormat.format(Date(timestamp)),
                                    onClick = { showDatePicker = true }
                                )
                            }
                        }
                    }

                    // Item 3: Credit Card Toggle
                    if (showCcPaymentToggle) {
                        item {
                            SecondaryCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.SpacingLarge),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Credit Card Payment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Track this transfer as a bill payment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isCreditCardPayment,
                                        onCheckedChange = { isCreditCardPayment = it }
                                    )
                                }
                            }
                        }
                    }

                    // Gift Cards Section for Transfer to Gift Card
                    if (isTransferToGiftCard) {
                        itemsIndexed(
                            items = giftCardItems,
                            key = { _, item -> item.id }
                        ) { index, item ->
                            val isExpanded = item.isExpanded
                            val cardTitle = when {
                                item.brand == GiftCardBrand.OTHER && item.customBrandName.isNotBlank() -> item.customBrandName
                                else -> item.brand.displayName
                            }
                            val amountText = item.amount.ifEmpty { "0" }
                            val codeText = item.code.ifEmpty { "No code" }

                            SecondaryCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimens.SpacingLarge)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { item.isExpanded = !item.isExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CardGiftcard,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = if (giftCardItems.size == 1) "Gift Card Details" else "Gift Card #${index + 1}: $cardTitle",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Amount: $currencySim$amountText • Code: $codeText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (giftCardItems.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        giftCardItems.removeAt(index)
                                                        val sum = giftCardItems.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                                                        if (sum > 0.0) {
                                                            amountStr = if (sum % 1.0 == 0.0) sum.toInt().toString() else String.format(Locale.US, "%.2f", sum)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove Gift Card",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { item.isExpanded = !item.isExpanded }) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                                                )
                                            }
                                        }
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                                        // Brand ExposedDropdownMenuBox
                                        ExposedDropdownMenuBox(
                                            expanded = item.brandExpanded,
                                            onExpandedChange = { item.brandExpanded = !item.brandExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = item.brand.displayName,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Brand") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = item.brandExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = item.brandExpanded,
                                                onDismissRequest = { item.brandExpanded = false }
                                            ) {
                                                GiftCardBrand.entries.forEach { b ->
                                                    DropdownMenuItem(
                                                        text = { Text(b.displayName) },
                                                        onClick = {
                                                            item.brand = b
                                                            item.brandExpanded = false
                                                            item.brandError = null
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        if (item.brand == GiftCardBrand.OTHER) {
                                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                            OutlinedTextField(
                                                value = item.customBrandName,
                                                onValueChange = {
                                                    item.customBrandName = it
                                                    if (item.brandError != null) item.brandError = null
                                                },
                                                label = { Text("Custom Brand Name") },
                                                modifier = Modifier.fillMaxWidth(),
                                                isError = item.brandError != null,
                                                supportingText = item.brandError?.let { err -> { Text(err) } },
                                                singleLine = true
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                                        // Code
                                        OutlinedTextField(
                                            value = item.code,
                                            onValueChange = {
                                                item.code = it
                                                if (item.codeError != null) item.codeError = null
                                            },
                                            label = { Text("Gift Card Code") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(item.codeFocusRequester),
                                            isError = item.codeError != null,
                                            supportingText = item.codeError?.let { err -> { Text(err) } },
                                            singleLine = true
                                        )

                                        LaunchedEffect(item.isExpanded) {
                                            if (item.isExpanded && item.code.isEmpty()) {
                                                delay(100)
                                                try {
                                                    item.codeFocusRequester.requestFocus()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                                        // Amount
                                        OutlinedTextField(
                                            value = item.amount,
                                            onValueChange = { newValue ->
                                                if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
                                                    item.amount = newValue
                                                    if (item.amountError != null) item.amountError = null
                                                    val sum = giftCardItems.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                                                    if (sum > 0.0) {
                                                        amountStr = if (sum % 1.0 == 0.0) sum.toInt().toString() else String.format(Locale.US, "%.2f", sum)
                                                    }
                                                }
                                            },
                                            label = { Text("Card Amount") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            isError = item.amountError != null,
                                            supportingText = item.amountError?.let { err -> { Text(err) } },
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                                        // Validity (Days)
                                        val validityPresets = listOf("90", "180", "365", "730", "Custom")
                                        ExposedDropdownMenuBox(
                                            expanded = item.validityExpanded,
                                            onExpandedChange = { item.validityExpanded = !item.validityExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = if (item.validityDays in validityPresets.dropLast(1)) "${item.validityDays} Days" else "Custom",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Validity") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = item.validityExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = item.validityExpanded,
                                                onDismissRequest = { item.validityExpanded = false }
                                            ) {
                                                validityPresets.forEach { preset ->
                                                    DropdownMenuItem(
                                                        text = { Text(if (preset == "Custom") "Custom" else "$preset Days") },
                                                        onClick = {
                                                            if (preset != "Custom") {
                                                                item.validityDays = preset
                                                                item.validityError = null
                                                            }
                                                            item.validityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        if (item.validityDays !in validityPresets.dropLast(1)) {
                                            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                            OutlinedTextField(
                                                value = item.validityDays,
                                                onValueChange = {
                                                    if (it.isEmpty() || it.toIntOrNull() != null) {
                                                        item.validityDays = it
                                                        if (item.validityError != null) item.validityError = null
                                                    }
                                                },
                                                label = { Text("Custom Validity (Days)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = item.validityError != null,
                                                supportingText = item.validityError?.let { err -> { Text(err) } },
                                                singleLine = true
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

                                        // Note
                                        OutlinedTextField(
                                            value = item.note,
                                            onValueChange = { item.note = it },
                                            label = { Text("Note (Optional)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = {
                                    val lastBrand = giftCardItems.lastOrNull()?.brand ?: GiftCardBrand.AMAZON_PAY
                                    val lastCustomBrand = giftCardItems.lastOrNull()?.customBrandName ?: ""
                                    giftCardItems.add(
                                        GiftCardItemState(
                                            initialBrand = lastBrand,
                                            initialCustomBrand = lastCustomBrand,
                                            initialIsExpanded = true
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Another Gift Card")
                            }
                        }
                    }

                    // Item 4: Note Section
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { 
                                description = it
                                if (descriptionError != null) descriptionError = null
                            },
                            placeholder = { Text("Add a note...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(descriptionFocusRequester)
                                .bringIntoViewRequester(noteBringIntoViewRequester)
                                .onFocusChanged { 
                                    isDescriptionFocused = it.isFocused
                                    if (it.isFocused) {
                                        scope.launch {
                                            delay(150)
                                            noteBringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                            leadingIcon = { Icon(Icons.Outlined.Notes, null) },
                            shape = RoundedCornerShape(16.dp),
                            isError = descriptionError != null,
                            supportingText = {
                                descriptionError?.let { Text(it) }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedBorderColor = Color(0xFFF3A66B)
                            )
                        )
                    }

                    // Item 5: Recurring Section
                    if (type != "TRANSFER") {
                        item {
                            SecondaryCard(
                                modifier = Modifier.bringIntoViewRequester(recurringBringIntoViewRequester)
                            ) {
                                Column(modifier = Modifier.padding(Dimens.SpacingLarge), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3A66B).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Sync, null, tint = Color(0xFFF3A66B), modifier = Modifier.size(20.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Make Recurring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Auto-repeat this entry on a schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = isRepeat, 
                                            onCheckedChange = { isRepeat = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF3A66B), checkedTrackColor = Color(0xFF8B5D33))
                                        )
                                    }

                                    if (isRepeat) {
                                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                                        RecurrenceOptionsView(
                                            frequency = frequency,
                                            onFrequencyChange = { frequency = it },
                                            interval = interval,
                                            onIntervalChange = { interval = it },
                                            endDate = endDate,
                                            onEndDateChange = { endDate = it },
                                            occurrences = occurrences,
                                            onOccurrencesChange = { occurrences = it },
                                            dayOfWeek = dayOfWeek,
                                            onDayOfWeekChange = { dayOfWeek = it },
                                            dayOfMonth = dayOfMonth,
                                            onDayOfMonthChange = { dayOfMonth = it },
                                            reflectionTiming = reflectionTiming,
                                            onReflectionTimingChange = { reflectionTiming = it },
                                            onAnyFieldFocused = {
                                                scope.launch {
                                                    delay(150)
                                                    recurringBringIntoViewRequester.bringIntoView()
                                                }
                                            },
                                            occurrencesBringIntoViewRequester = occurrencesBringIntoViewRequester
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TransactionEntryActionBar(
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding(),
                onTemplateClick = { showTemplatesSheet = true },
                onSaveClick = {
                    // Reset errors
                    amountError = null
                    accountError = null
                    categoryError = null
                    descriptionError = null
                    toAccountError = null

                    val amount = amountStr.trim().toDoubleOrNull() ?: 0.0
                    var hasError = false

                    if (amount <= 0) {
                        amountError = "Enter a valid amount greater than 0"
                        hasError = true
                    }
                    if (selectedAccountId == -1L) {
                        accountError = "Select an account"
                        hasError = true
                    }
                    if (type != "TRANSFER" && selectedCategoryName.isEmpty()) {
                        categoryError = "Select a category"
                        hasError = true
                    }
                    if (type == "TRANSFER") {
                        if (toAccountId == -1L) {
                            toAccountError = "Select destination account"
                            hasError = true
                        } else if (toAccountId == selectedAccountId) {
                            toAccountError = "Cannot transfer to the same account"
                            hasError = true
                        }
                    }

                    if (isTransferToGiftCard) {
                        if (giftCardItems.isEmpty()) {
                            val guessedBrand = when {
                                toAccount?.name?.contains("Amazon", ignoreCase = true) == true -> GiftCardBrand.AMAZON_PAY
                                toAccount?.name?.contains("Flipkart", ignoreCase = true) == true -> GiftCardBrand.FLIPKART
                                toAccount?.name?.contains("Myntra", ignoreCase = true) == true -> GiftCardBrand.MYNTRA
                                toAccount?.name?.contains("Google", ignoreCase = true) == true || toAccount?.name?.contains("Play", ignoreCase = true) == true -> GiftCardBrand.GOOGLE_PLAY
                                toAccount?.name?.contains("Paytm", ignoreCase = true) == true -> GiftCardBrand.PAYTM
                                else -> GiftCardBrand.OTHER
                            }
                            val customBrand = if (guessedBrand == GiftCardBrand.OTHER) toAccount?.name?.replace(" Gift Card", "", ignoreCase = true)?.trim() ?: "" else ""
                            giftCardItems.add(GiftCardItemState(initialBrand = guessedBrand, initialCustomBrand = customBrand, initialAmount = amountStr))
                        }
                        var gcHasError = false
                        giftCardItems.forEach { item ->
                            var itemHasError = false
                            if (item.brand == GiftCardBrand.OTHER && item.customBrandName.isBlank()) {
                                item.brandError = "Custom brand required"
                                itemHasError = true
                            }
                            if (item.code.isBlank()) {
                                item.codeError = "Card code required"
                                itemHasError = true
                            }
                            val amt = item.amount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                item.amountError = "Must be > 0"
                                itemHasError = true
                            }
                            val valDays = item.validityDays.toIntOrNull() ?: 0
                            if (valDays <= 0) {
                                item.validityError = "Required"
                                itemHasError = true
                            }
                            if (itemHasError) {
                                item.isExpanded = true
                                gcHasError = true
                            }
                        }
                        if (gcHasError) {
                            hasError = true
                        }
                        if (description.trim().isEmpty() && toAccount != null) {
                            description = "Transfer to ${toAccount.name}"
                        }
                    }

                    if (description.trim().isEmpty()) {
                        descriptionError = "Description is required"
                        hasError = true
                    }

                    if (!hasError) {
                        if (isTransferToGiftCard) {
                            val giftCardDataList = giftCardItems.map { item ->
                                GiftCardData(
                                    brand = item.brand.name,
                                    customBrandName = if (item.brand == GiftCardBrand.OTHER) item.customBrandName else null,
                                    code = item.code,
                                    amount = item.amount.toDoubleOrNull() ?: 0.0,
                                    validityDays = item.validityDays.toIntOrNull() ?: 365,
                                    note = item.note.ifBlank { null }
                                )
                            }
                            if (!isEditing) {
                                viewModel.addTransferWithGiftCards(
                                    amount = amount,
                                    dateTimeMs = timestamp,
                                    fromAccountId = selectedAccountId,
                                    toAccountId = toAccountId,
                                    budgetBookId = selectedBudgetBookId,
                                    description = description,
                                    giftCardItems = giftCardDataList
                                )
                            } else {
                                viewModel.updateTransferWithGiftCards(
                                    transaction = initialTransaction.copy(
                                        amount = amount,
                                        type = type,
                                        category = "",
                                        subCategory = null,
                                        dateTime = timestamp,
                                        linkedAccountId = selectedAccountId,
                                        toAccountId = toAccountId,
                                        description = description,
                                        linkedBudgetBookId = selectedBudgetBookId
                                    ),
                                    giftCardItems = giftCardDataList
                                )
                            }
                            onDismiss()
                        } else if (!isEditing) {
                            if (isRepeat) {
                                viewModel.addRepetitiveTransaction(
                                    timestamp, amount, type, selectedCategoryName, frequency, interval, 
                                    selectedAccountId, selectedBudgetBookId, description, endDate, occurrences, 
                                    dayOfWeek, dayOfMonth, if (type == "TRANSFER") toAccountId else null, 
                                    reflectionTiming, subCategory = selectedSubCategoryName,
                                    isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                )
                            } else {
                                viewModel.addTransaction(
                                    amount = amount, 
                                    type = type, 
                                    category = selectedCategoryName, 
                                    dateTimeMs = timestamp, 
                                    accountId = selectedAccountId, 
                                    budgetBookId = selectedBudgetBookId, 
                                    description = description, 
                                    toAccountId = if (type == "TRANSFER") toAccountId else null, 
                                    subCategory = selectedSubCategoryName,
                                    linkedGiftCardId = if (linkedGiftCardId != -1L) linkedGiftCardId else null,
                                    isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                )
                            }
                        } else {
                            if (isRepeat) {
                                if (initialTransaction.repetitiveId != null) {
                                    // Update existing recurrence
                                    rep?.let {
                                        viewModel.updateRepetitiveTransaction(it.copy(
                                            amount = amount,
                                            type = type,
                                            category = if (type == "TRANSFER") "" else selectedCategoryName,
                                            subCategory = if (type == "TRANSFER") null else selectedSubCategoryName,
                                            frequency = frequency,
                                            interval = interval,
                                            linkedAccountId = selectedAccountId,
                                            toAccountId = if (type == "TRANSFER") toAccountId else null,
                                            description = description,
                                            endDate = endDate,
                                            occurrences = occurrences,
                                            dayOfWeek = dayOfWeek,
                                            dayOfMonth = dayOfMonth,
                                            reflectionTiming = reflectionTiming,
                                            linkedBudgetBookId = selectedBudgetBookId,
                                            isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                        ))
                                    }
                                    // Also update the transaction itself
                                    viewModel.updateTransaction(initialTransaction.copy(
                                        amount = amount,
                                        type = type,
                                        category = if (type == "TRANSFER") "" else selectedCategoryName,
                                        subCategory = if (type == "TRANSFER") null else selectedSubCategoryName,
                                        dateTime = timestamp,
                                        linkedAccountId = selectedAccountId,
                                        toAccountId = if (type == "TRANSFER") toAccountId else null,
                                        description = description,
                                        linkedBudgetBookId = selectedBudgetBookId,
                                        isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                    ))
                                } else {
                                    // Move from non-recurring to recurring
                                    viewModel.addRepetitiveTransaction(
                                        timestamp, amount, type, if (type == "TRANSFER") "" else selectedCategoryName,
                                        frequency, interval, selectedAccountId, selectedBudgetBookId,
                                        description, endDate, occurrences, dayOfWeek, dayOfMonth,
                                        if (type == "TRANSFER") toAccountId else null, reflectionTiming,
                                        linkTransactionId = initialTransaction.id,
                                        subCategory = selectedSubCategoryName,
                                        isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                    )
                                }
                            } else {
                                viewModel.updateTransaction(initialTransaction.copy(
                                    amount = amount,
                                    type = type,
                                    category = if (type == "TRANSFER") "" else selectedCategoryName,
                                    subCategory = if (type == "TRANSFER") null else selectedSubCategoryName,
                                    dateTime = timestamp,
                                    linkedAccountId = selectedAccountId,
                                    toAccountId = if (type == "TRANSFER") toAccountId else null,
                                    description = description,
                                    repetitiveId = null, // Unlink if repeat turned off
                                    repetitiveOccurrenceDate = null,
                                    isGeneratedFromRecurrence = false,
                                    linkedBudgetBookId = selectedBudgetBookId,
                                    isCreditCardPayment = if (type == "TRANSFER" && toAccount?.isCreditCard == true) isCreditCardPayment else false
                                ))
                            }
                        }
                        onDismiss()
                    }
                }
            )
        }
    }

    // scope moved to top

    if (showCalculator) {
        CalculatorSheet(
            initialValue = amountStr,
            onResult = {
                amountStr = it
                showCalculator = false
            },
            onDismiss = { showCalculator = false }
        )
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
                if (type != "TRANSFER" && selectedCategoryName.isEmpty()) {
                    showCategorySelector = true
                } else if (type == "TRANSFER" && toAccountId == -1L) {
                    showToAccountSelector = true
                } else if (description.isEmpty()) {
                    scope.launch {
                        kotlinx.coroutines.delay(100.milliseconds)
                        descriptionFocusRequester.requestFocus()
                    }
                }
            },
            onDismiss = { showAccountSelector = false }
        )
    }

    if (showBudgetBookSelector) {
        BudgetBookSelectorSheet(
            budgetBooks = activeBudgetBooks,
            currencySim = currencySim,
            onSelect = { 
                selectedBudgetBookId = it.id
                showBudgetBookSelector = false
            },
            onDismiss = { showBudgetBookSelector = false }
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
                if (description.isEmpty() && it.type != "GIFT_CARD") {
                    scope.launch {
                        kotlinx.coroutines.delay(100.milliseconds)
                        descriptionFocusRequester.requestFocus()
                    }
                }
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
                if (description.isEmpty()) {
                    scope.launch {
                        kotlinx.coroutines.delay(100.milliseconds)
                        descriptionFocusRequester.requestFocus()
                    }
                }
            },
            onDismiss = { showCategorySelector = false }
        )
    }

    if (showDatePicker) {
        ComposeDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        val timeCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        
                        TimePickerDialog(context, { _, hh, mm ->
                            cal.set(Calendar.HOUR_OF_DAY, hh)
                            cal.set(Calendar.MINUTE, mm)
                            timestamp = cal.timeInMillis
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    }
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

    if (showDeleteConfirmation && initialTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Entry", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this transaction?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(initialTransaction)
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTemplatesSheet) {
        QuickTemplatesBottomSheet(
            templates = allTemplates,
            currencySim = currencySim,
            categories = categories,
            accounts = accounts,
            logInstantly = appSettings.logTemplatesInstantly,
            onLogInstantlyChange = { viewModel.updateLogTemplatesInstantly(it) },
            onDismiss = { showTemplatesSheet = false },
            onTemplateSelected = { template ->
                showTemplatesSheet = false
                if (appSettings.logTemplatesInstantly) {
                    viewModel.logTransactionFromTemplate(template)
                    onDismiss()
                } else {
                    val prefill = viewModel.getPrefillFromTemplate(template)
                    amountStr = if (prefill.amount % 1.0 == 0.0) prefill.amount.toInt().toString() else prefill.amount.toString()
                    type = prefill.type
                    selectedCategoryName = prefill.category
                    selectedSubCategoryName = prefill.subCategory
                    selectedAccountId = prefill.linkedAccountId
                    toAccountId = prefill.toAccountId ?: -1L
                    selectedBudgetBookId = prefill.linkedBudgetBookId
                    description = prefill.description
                }
            },
            onNewTemplate = {
                showTemplatesSheet = false
                showSaveTemplateSheet = true
            },
            onManageTemplates = {
                // Not easily reachable from here since we are in a fullscreen dialog
                // but we can close this entry and navigate if we had a navigation callback
                showTemplatesSheet = false
            }
        )
    }

    if (showSaveTemplateSheet) {
        SaveTemplateBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSaveTemplateSheet = false },
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
                showSaveTemplateSheet = false
            },
            accounts = accounts,
            categories = categories,
            budgetBooks = budgetBooks,
            currencySim = currencySim,
            initialTemplate = TransactionTemplate(
                name = description,
                amount = amountStr.toDoubleOrNull() ?: 0.0,
                type = type,
                category = selectedCategoryName,
                subCategory = selectedSubCategoryName,
                linkedAccountId = selectedAccountId,
                toAccountId = if (type == "TRANSFER") toAccountId else null,
                linkedBudgetBookId = selectedBudgetBookId,
                description = description
            )
        )
    }
}

@Composable
fun TransactionEntryActionBar(
    modifier: Modifier = Modifier,
    onTemplateClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(vertical = Dimens.SpacingSmall),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onTemplateClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = "Quick Templates",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3A66B))
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }
        }
    }
}
