package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.GiftCard
import com.saik.expensemanager.data.model.GiftCardBrand
import com.saik.expensemanager.data.model.colorAndIcon
import com.saik.expensemanager.data.model.displayBrandName
import com.saik.expensemanager.data.model.expiryDate
import com.saik.expensemanager.data.model.isExpired
import com.saik.expensemanager.data.model.daysUntilExpiry
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftCardsScreen(
    viewModel: MainViewModel,
    filterAccountId: Long? = null,
    onBack: () -> Unit,
    onMarkUsed: (GiftCard) -> Unit = {}
) {
    val giftCards by viewModel.allGiftCards.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val currencySim = appSettings.currencyCode

    var filter by rememberSaveable { mutableStateOf("Available") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var giftCardToEdit by remember { mutableStateOf<GiftCard?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val filteredCards = remember(giftCards, filter, filterAccountId) {
        giftCards.filter { card ->
            if (filterAccountId != null && card.linkedAccountId != filterAccountId) return@filter false
            
            when (filter) {
                "Available" -> !card.isUsed && !card.isExpired()
                "Used" -> card.isUsed
                "Expired" -> card.isExpired() && !card.isUsed
                else -> true
            }
        }
    }

    val stats = remember(giftCards, accounts, filterAccountId) {
        val cardsToStat = if (filterAccountId != null) giftCards.filter { it.linkedAccountId == filterAccountId } else giftCards
        
        val availableCards = cardsToStat.filter { !it.isUsed && !it.isExpired() }
        val usedCards = cardsToStat.filter { it.isUsed }
        val expiringSoonCards = cardsToStat.filter { !it.isUsed && it.daysUntilExpiry() in 0..7 }
        
        val availableValue = if (filterAccountId != null) {
            accountBalances[filterAccountId] ?: 0.0
        } else {
            accounts.filter { it.type == "GIFT_CARD" }.sumOf { accountBalances[it.id] ?: 0.0 }
        }
        val expiringSoonValue = expiringSoonCards.sumOf { it.amount }

        object {
            val totalValue = cardsToStat.sumOf { it.amount }
            val availableValue = availableValue
            val availableCount = availableCards.size
            val usedValue = usedCards.sumOf { it.amount }
            val usedCount = usedCards.size
            val expiringSoonValue = expiringSoonValue
            val expiringSoonCount = expiringSoonCards.size
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Gift Cards",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                giftCardToEdit = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Gift Card")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Summary Card
            PrimaryCard(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                Column(modifier = Modifier.padding(Dimens.SpacingLarge)) {
                    Text("Total Value: ${CurrencyUtils.formatAmount(stats.totalValue, currencySim)}", 
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Available", style = MaterialTheme.typography.labelSmall)
                            Text("${CurrencyUtils.formatAmount(stats.availableValue, currencySim)} (${stats.availableCount})", 
                                style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Used", style = MaterialTheme.typography.labelSmall)
                            Text("${CurrencyUtils.formatAmount(stats.usedValue, currencySim)} (${stats.usedCount})", 
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (stats.expiringSoonCount > 0) {
                        Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                        Text("Expiring soon (≤7 days): ${CurrencyUtils.formatAmount(stats.expiringSoonValue, currencySim)} (${stats.expiringSoonCount} cards)", 
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLarge)
                    .padding(bottom = Dimens.SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                listOf("All", "Available", "Used", "Expired").forEach { label ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        label = { Text(label) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val grouped = filteredCards.groupBy { it.displayBrandName() }
                grouped.forEach { (brandName, cards) ->
                    item {
                        val unusedCards = cards.filter { !it.isUsed && !it.isExpired() }
                        val accountId = cards.firstOrNull()?.linkedAccountId
                        val brandAvailable = if (accountId != null) accountBalances[accountId] ?: 0.0 else 0.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(brandName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (unusedCards.isNotEmpty()) {
                                Text("Available: ${CurrencyUtils.formatAmount(brandAvailable, currencySim)} (${unusedCards.size} codes)", 
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    items(cards.sortedBy { it.daysUntilExpiry() }) { card ->
                        GiftCardItem(
                            card = card,
                            currencySim = currencySim,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(card.code))
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Code copied!",
                                        actionLabel = "Mark as Used",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.markGiftCardUsed(card.id)
                                    }
                                }
                            },
                            onToggleUsed = {
                                if (card.isUsed) viewModel.markGiftCardUnused(card.id)
                                else onMarkUsed(card)
                            },
                            onEdit = {
                                giftCardToEdit = card
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGiftCardDialog(
            viewModel = viewModel,
            giftCardToEdit = giftCardToEdit,
            onDismiss = { 
                showAddDialog = false
                giftCardToEdit = null
            }
        )
    }
}

@Composable
fun GiftCardItem(
    card: GiftCard,
    currencySim: String,
    onCopy: () -> Unit,
    onToggleUsed: () -> Unit,
    onEdit: () -> Unit
) {
    val brandEnum = remember(card.brand) { 
        try { GiftCardBrand.valueOf(card.brand) } catch (e: Exception) { GiftCardBrand.OTHER }
    }
    val (brandColor, brandIcon) = brandEnum.colorAndIcon()
    val isExpired = card.isExpired()
    val daysLeft = card.daysUntilExpiry()
    var isCodeVisible by remember { mutableStateOf(false) }

    SecondaryCard(
        modifier = Modifier
            .padding(horizontal = Dimens.SpacingLarge, vertical = 4.dp)
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingMedium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(brandColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(brandIcon, null, tint = brandColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCodeVisible) card.code else if (card.code.length > 4) "XXXX-XXXX-${card.code.takeLast(4)}" else "•••• ••••",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { isCodeVisible = !isCodeVisible },
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        ) {
                            Icon(
                                if (isCodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isCodeVisible) "Hide Code" else "Show Code",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = CurrencyUtils.formatAmount(card.amount, currencySim),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
                }
                Checkbox(
                    checked = card.isUsed,
                    onCheckedChange = { onToggleUsed() },
                    colors = CheckboxDefaults.colors(checkedColor = brandColor)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingSmall))

            val expiryDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(card.expiryDate()))
            if (isExpired) {
                Text(
                    "Expired ${kotlin.math.abs(daysLeft)} days ago ($expiryDateStr)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    "Expires: $expiryDateStr ($daysLeft days left)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysLeft <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
