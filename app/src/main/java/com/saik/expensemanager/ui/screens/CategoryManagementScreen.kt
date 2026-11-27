package com.saik.expensemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.ui.components.*
import com.saik.expensemanager.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Expense, 1: Income

    val filteredCategories = remember(categories, selectedTab) {
        val type = if (selectedTab == 0) "EXPENSE" else "INCOME"
        categories.filter { it.type == type }
    }

    val groupedCategories = remember(filteredCategories) {
        val parents = filteredCategories.filter { it.parentId == null }
        val children = filteredCategories.filter { it.parentId != null }.groupBy { it.parentId }
        parents.map { it to (children[it.id] ?: emptyList()) }
    }

    val expandedParents = remember { mutableStateMapOf<Long, Boolean>() }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                StandardTopAppBar(
                    title = "Categories",
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
                
                SegmentedPeriodSelector(
                    modes = listOf("Expense", "Income"),
                    selectedMode = if (selectedTab == 0) "Expense" else "Income",
                    onModeSelected = { selectedTab = if (it == "Expense") 0 else 1 }
                )
                
                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SpacingLarge, vertical = Dimens.SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
        ) {
            groupedCategories.forEach { (parent, children) ->
                val isExpanded = expandedParents[parent.id] ?: false
                item(key = "p_${parent.id}") {
                    CategoryItemRow(
                        category = parent,
                        isParent = true,
                        isExpanded = isExpanded,
                        onExpandToggle = { expandedParents[parent.id] = !isExpanded },
                        hasChildren = children.isNotEmpty(),
                        onEdit = { categoryToEdit = it },
                        onDelete = { viewModel.deleteCategory(it) }
                    )
                }
                if (isExpanded) {
                    items(children, key = { "c_${it.id}" }) { child ->
                        CategoryItemRow(
                            category = child,
                            isParent = false,
                            onEdit = { categoryToEdit = it },
                            onDelete = { viewModel.deleteCategory(it) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showAddCategoryDialog) {
            AddEditCategoryBottomSheet(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name, type, color, parentId, iconName ->
                    viewModel.addCategory(name, type, color, parentId, iconName)
                    showAddCategoryDialog = false
                },
                categories = categories,
                initialType = if (selectedTab == 0) "EXPENSE" else "INCOME"
            )
        }

        if (categoryToEdit != null) {
            AddEditCategoryBottomSheet(
                onDismiss = { categoryToEdit = null },
                onConfirm = { name, type, color, parentId, iconName ->
                    viewModel.updateCategory(categoryToEdit!!.copy(
                        name = name,
                        type = type,
                        color = color,
                        parentId = parentId,
                        iconName = iconName
                    ))
                    categoryToEdit = null
                },
                categories = categories,
                category = categoryToEdit
            )
        }
    }
}

@Composable
fun CategoryItemRow(
    category: Category,
    isParent: Boolean,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit = {},
    hasChildren: Boolean = false,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    SecondaryCard(
        modifier = Modifier
            .padding(start = if (isParent) 0.dp else Dimens.SpacingExtraLarge)
            .clickable(enabled = isParent && hasChildren) { onExpandToggle() }
    ) {
        Row(
            modifier = Modifier.padding(Dimens.SpacingMedium).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                if (isParent && hasChildren) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (isParent) {
                    Spacer(modifier = Modifier.size(20.dp))
                }

                val isEmoji = category.iconName.isNotEmpty() && !CategoryUtils.iconList.contains(category.iconName)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(category.color).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEmoji) {
                        Text(text = category.iconName, fontSize = 18.sp)
                    } else {
                        Icon(
                            imageVector = CategoryUtils.getCategoryIcon(category.iconName),
                            contentDescription = null,
                            tint = Color(category.color),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = category.name,
                    style = if (isParent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isParent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isSystem = category.name == "Adjustment" || category.name == "Transfer"
                if (!isSystem) {
                    IconButton(onClick = { onEdit(category) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onDelete(category) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                } else {
                    // System categories show a lock icon to indicate they are protected
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "System Category",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.padding(12.dp).size(16.dp)
                    )
                }
            }
        }
    }
}
