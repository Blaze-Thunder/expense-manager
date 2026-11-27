package com.saik.expensemanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.data.model.BudgetBook
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.data.model.Transaction
import com.saik.expensemanager.ui.screens.CategoryUtils
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

// Design System Tokens
object Dimens {
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingLarge = 16.dp
    val SpacingExtraLarge = 24.dp
    
    val CardRadius = 16.dp
    val ListCardRadius = 12.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopAppBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        windowInsets = WindowInsets(0.dp) // Removed statusBars insets to manually control space
    )
}

@Composable
fun ContextBar(
    activeBook: BudgetBook?,
    budgetBooks: List<BudgetBook>,
    periodLabel: String,
    onBookSelected: (Long) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBudgetBookSelector: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLarge),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Budget Book Selector
        if (showBudgetBookSelector) {
            Box(modifier = Modifier.weight(1f)) {
                BudgetBookSelector(
                    activeBook = activeBook,
                    budgetBooks = budgetBooks,
                    onBookSelected = onBookSelected
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Period Navigation - Fixed width or wrapped to prevent squashing icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(40.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onPrevClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false
            )
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun BudgetBookSelector(
    activeBook: BudgetBook?,
    budgetBooks: List<BudgetBook>,
    onBookSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBookDropdown by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(Dimens.SpacingSmall))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .clickable { showBookDropdown = true }
                .padding(horizontal = Dimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            Text(
                text = activeBook?.name ?: "No Budget",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = showBookDropdown, onDismissRequest = { showBookDropdown = false }) {
            budgetBooks.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.name, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onBookSelected(book.id); showBookDropdown = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedPeriodSelector(
    modes: List<String>,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingMedium)
    ) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                icon = {}
            ) {
                Text(
                    text = mode,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun PrimaryCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true).copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ),
        content = content
    )
}

@Composable
fun SecondaryCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    border: BorderStroke? = CardDefaults.outlinedCardBorder(enabled = true).copy(
        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = colors,
        border = border,
        content = content
    )
}

@Composable
fun TransactionCard(
    tx: Transaction,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    val amountColor = when (tx.type) {
        "INCOME" -> Color(0xFF2E7D32)
        "EXPENSE" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.ListCardRadius))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            // Icon resolution - Optimized with remember
            val displayCat by remember(tx.category, tx.subCategory, tx.type, categories) {
                derivedStateOf {
                    val parent = categories.find { it.name == tx.category && it.type == tx.type && it.parentId == null }
                    if (!tx.subCategory.isNullOrEmpty()) {
                        categories.find { it.name == tx.subCategory && it.type == tx.type && it.parentId == parent?.id } 
                            ?: categories.find { it.name == tx.subCategory && it.type == tx.type && it.parentId != null }
                    } else parent ?: categories.find { it.name == tx.category && it.type == tx.type }
                }
            }

            val iconName = displayCat?.iconName ?: (if (tx.type == "TRANSFER") "SwapHoriz" else "Label")
            val iconColor = displayCat?.color?.let { Color(it) } ?: amountColor
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (tx.type == "TRANSFER") {
                    Icon(Icons.Default.SwapHoriz, null, tint = iconColor, modifier = Modifier.size(20.dp))
                } else {
                    val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                    if (isEmoji) {
                        Text(iconName, fontSize = 18.sp)
                    } else {
                        Icon(
                            imageVector = CategoryUtils.getCategoryIcon(iconName),
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifEmpty { if (tx.type == "TRANSFER") "Transfer" else tx.category },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (tx.description == "Balance Correction") FontWeight.Normal else FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (tx.description == "Balance Correction") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tx.type != "TRANSFER") {
                            val categoryText = if (!tx.subCategory.isNullOrEmpty()) "${tx.category}/${tx.subCategory}" else tx.category
                            Text(categoryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val acc = remember(tx.linkedAccountId, accounts) { accounts.find { it.id == tx.linkedAccountId } }
                        val accName = acc?.name ?: "Acc"
                        val toAccName = remember(tx.type, tx.toAccountId, accounts) { if (tx.type == "TRANSFER") accounts.find { it.id == tx.toAccountId }?.name ?: "Acc" else null }
                        Text(
                            text = if (toAccName != null) "$accName → $toAccName" else accName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Fee-waiver eligibility indicator
                        val isFeeWaiverEligible = remember(tx, acc) {
                            acc?.isCreditCard == true && acc.trackFeeWaiver && (
                                (tx.type == "EXPENSE" && tx.description != "Balance Correction" && tx.category != "Adjustment") ||
                                (tx.type == "INCOME" && (tx.category == "Refund" || tx.category == "Refunds"))
                            )
                        }
                        if (isFeeWaiverEligible) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Fee-waiver eligible",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (tx.isGeneratedFromRecurrence) {
                        Text(
                            text = "Recurring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Amount and Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatAmountWithSign(tx.amount, currencySim, tx.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                val timeStr = remember(tx.dateTime) {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(tx.dateTime))
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TransactionListSection(
    transactions: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    currencySim: String,
    onTransactionClick: (Transaction) -> Unit,
    showDateHeaders: Boolean = true,
    modifier: Modifier = Modifier,
    /// If provided, the daily closing balance for each date key (midnight epoch ms) is shown
    /// in a two-line right-aligned Column below the day's net total.  Only meaningful when
    /// showDateHeaders is true and the caller is rendering a single-account ledger.
    showDailyBalances: Map<Long, Double>? = null
) {
    val grouped = remember(transactions) {
        transactions.groupBy { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateTime }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { (_, txs) ->
            txs.sortedWith(
                compareBy<Transaction> { it.isGeneratedFromRecurrence }
                    .thenByDescending { it.dateTime }
            )
        }.toList().sortedByDescending { it.first }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        grouped.forEach { (dateMs, txs) ->
            if (showDateHeaders) {
                val dayTotal = txs.sumOf { 
                    when (it.type) {
                        "INCOME" -> it.amount
                        "EXPENSE" -> -it.amount
                        else -> 0.0
                    }
                }
                val dailyBalance: Double? = showDailyBalances?.get(dateMs)
                val hasDailyBalance = dailyBalance != null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- LEFT: date label (allows wrapping) ---
                    Text(
                        text = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(Date(dateMs)),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        softWrap = true
                    )

                    if (hasDailyBalance) {
                        // --- RIGHT: Two-line stacked Column — net total above, Bal: closing below ---
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = CurrencyUtils.formatAmountWithSign(dayTotal, ""),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (dayTotal > 0) Color(0xFF2E7D32) else if (dayTotal < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                            )
                            /*
                             * Balance color convention:
                             *   For Cash / Wallet / Bank:   positive = muted green (funds), negative = muted red (overdraft)
                             *   For Credit Cards:          debt (negative) = muted red, credit overpayment (positive) = muted green
                             *   Use muted tones so the net total remains the visually dominant figure.
                             */
                            val balanceColor = when {
                                dailyBalance > 0 -> Color(0xFF4CAF50).copy(alpha = 0.6f)   // muted green
                                dailyBalance < 0 -> Color(0xFFEF5350).copy(alpha = 0.6f)   // muted red
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                            Text(
                                text = "Bal: " + CurrencyUtils.formatAmount(dailyBalance, currencySim),
                                style = MaterialTheme.typography.labelSmall,
                                color = balanceColor,
                                maxLines = 1
                            )
                        }
                    } else {
                        // --- RIGHT (no balance): existing single-line net total ---
                        Text(
                            text = CurrencyUtils.formatAmountWithSign(dayTotal, ""),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dayTotal > 0) Color(0xFF2E7D32) else if (dayTotal < 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            txs.forEach { tx ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically { it / 2 }
                ) {
                    TransactionCard(tx, currencySim, categories, accounts) { onTransactionClick(tx) }
                }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    tx: Transaction,
    currencySim: String,
    categories: List<Category>,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    TransactionCard(tx, currencySim, categories, accounts, onClick)
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall)
    )
}

@Composable
fun SummaryFlowItem(label: String, amount: Double, color: Color, currencySim: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(CurrencyUtils.formatAmount(amount, currencySim), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
