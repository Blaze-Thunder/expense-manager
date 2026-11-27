package com.saik.expensemanager.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Account
import com.saik.expensemanager.ui.components.Dimens
import com.saik.expensemanager.ui.components.SecondaryCard
import com.saik.expensemanager.ui.components.StandardTopAppBar
import com.saik.expensemanager.ui.viewmodel.MainViewModel
import com.saik.expensemanager.ui.viewmodel.PaymentHistoryItem
import com.saik.expensemanager.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    account: Account,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    var currentYearCal by remember { mutableStateOf(Calendar.getInstance()) }
    
    val filterRange = remember(currentYearCal, account.billingCycleStartDay) {
        val start = (currentYearCal.clone() as Calendar).apply {
            set(Calendar.MONTH, 0)
            set(Calendar.DAY_OF_MONTH, account.billingCycleStartDay)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }
        start.timeInMillis to end.timeInMillis
    }

    val history by viewModel.getPaymentHistory(account.id, filterRange).collectAsStateWithLifecycle(emptyList())
    val insights by viewModel.computeCreditCardInsights(account, filterRange).collectAsStateWithLifecycle(null)
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val currencySim = appSettings.currencyCode

    var lastTime by remember { mutableLongStateOf(currentYearCal.timeInMillis) }
    var slideDirection by remember { mutableIntStateOf(1) }

    LaunchedEffect(currentYearCal.timeInMillis) {
        if (currentYearCal.timeInMillis != lastTime) {
            slideDirection = if (currentYearCal.timeInMillis > lastTime) 1 else -1
            lastTime = currentYearCal.timeInMillis
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
                    title = "Payment History",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.padding(end = Dimens.SpacingMedium)
                        ) {
                            IconButton(
                                onClick = { 
                                    val next = (currentYearCal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
                                    currentYearCal = next
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = SimpleDateFormat("yyyy", Locale.US).format(currentYearCal.time),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            IconButton(
                                onClick = { 
                                    val next = (currentYearCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                                    currentYearCal = next
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                
                // Period detail subtitle matching Design System
                val dfRange = SimpleDateFormat("dd.MM.yyyy", Locale.US)
                Text(
                    text = "${dfRange.format(Date(filterRange.first))} ~ ${dfRange.format(Date(filterRange.second))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = Dimens.SpacingLarge).padding(bottom = Dimens.SpacingSmall)
                )
                
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    ) { innerPadding ->
        var horizontalDragAmount by remember { mutableFloatStateOf(0f) }
        
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pointerInput(currentYearCal) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (horizontalDragAmount > 100) {
                                val next = (currentYearCal.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
                                currentYearCal = next
                            } else if (horizontalDragAmount < -100) {
                                val next = (currentYearCal.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                                currentYearCal = next
                            }
                            horizontalDragAmount = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        horizontalDragAmount += dragAmount
                    }
                }
        ) {
            AnimatedContent(
                targetState = currentYearCal.timeInMillis,
                transitionSpec = {
                    if (slideDirection > 0) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "payment_history_period_transition"
            ) { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Header
                    insights?.let { data ->
                        PaymentSummaryHeader(data, currencySim)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Dimens.SpacingLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                    ) {
                        if (history.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("No payment history found for this period", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            val grouped = history.groupBy { 
                                SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(it.date))
                            }
                            
                            grouped.forEach { (month, items) ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Dimens.SpacingSmall),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = month,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        val monthlyTotal = items.sumOf { it.amount }
                                        Text(
                                            text = CurrencyUtils.formatAmount(monthlyTotal, currencySim),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                items(items) { item ->
                                    PaymentHistoryRow(item, currencySim)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentSummaryHeader(data: com.saik.expensemanager.ui.viewmodel.CreditCardDetailedInsights, currencySim: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.SpacingLarge), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PaymentSummaryItem("Paid by You", data.paidThisCycle, Color(0xFF2E7D32), currencySim)
                PaymentSummaryItem("Credits Applied", data.creditsAppliedThisCycle, Color(0xFF0288D1), currencySim)
                PaymentSummaryItem("Total Payments", data.totalReductionThisCycle, MaterialTheme.colorScheme.primary, currencySim)
            }
        }
    }
}

@Composable
fun PaymentSummaryItem(label: String, amount: Double, color: Color, currencySim: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(CurrencyUtils.formatAmount(amount, currencySim), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PaymentHistoryRow(item: PaymentHistoryItem, currencySim: String) {
    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    
    SecondaryCard {
        Row(
            modifier = Modifier.padding(Dimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            val icon = if (item.type == "PAYMENT") Icons.Default.Payments else Icons.Default.Redeem
            val tint = if (item.type == "PAYMENT") Color(0xFF2E7D32) else Color(0xFF0288D1)
            
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.type == "PAYMENT") "Payment from ${item.category}" else item.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(dateFmt.format(Date(item.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            
            Text(
                text = CurrencyUtils.formatAmount(item.amount, currencySim),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
        }
    }
}
