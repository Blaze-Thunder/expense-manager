package com.saik.expensemanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saik.expensemanager.data.model.Category
import com.saik.expensemanager.ui.screens.CategoryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Long?, String) -> Unit,
    categories: List<Category>,
    category: Category? = null,
    initialType: String = "EXPENSE",
    initialParentId: Long? = null,
    lockType: Boolean = false,
    lockParent: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf(category?.name ?: "") }
    var type by remember { mutableStateOf(category?.type ?: initialType) }
    var selectedColor by remember { mutableStateOf(category?.color ?: Color(0xFF2196F3).toArgb()) }
    var parentId by remember { mutableStateOf(category?.parentId ?: initialParentId) }
    var iconName by remember { mutableStateOf(category?.iconName ?: "Label") }
    
    var showIconPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFFF9800),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF607D8B), Color(0xFF37474F)
    )
    
    val suggestedIcons = listOf("Restaurant", "ShoppingCart", "DirectionsCar", "Home", "Bolt", "Paid", "Favorite", "Work")

    val possibleParents = categories.filter { it.type == type && it.parentId == null && it.id != category?.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = Dimens.SpacingExtraLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLarge)
        ) {
            Text(
                text = if (category == null) "Add Category" else "Edit Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Category Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                placeholder = { Text("e.g. Groceries") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Type Selector
            if (!lockType) {
                SegmentedPeriodSelector(
                    modes = listOf("Expense", "Income"),
                    selectedMode = if (type == "EXPENSE") "Expense" else "Income",
                    onModeSelected = { 
                        type = it.uppercase()
                        if (!lockParent) parentId = null 
                    },
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }

            // Parent Category Selector (Modern Style)
            if (!lockParent && possibleParents.isNotEmpty()) {
                val selectedParent = possibleParents.find { it.id == parentId }
                ParentSelectorField(
                    label = "Parent Category",
                    selectedParentName = selectedParent?.name ?: "No Parent",
                    selectedParentColor = selectedParent?.let { Color(it.color) },
                    onClick = { showParentPicker = true }
                )
            } else if (lockParent && parentId != null) {
                 val parent = categories.find { it.id == parentId }
                 if (parent != null) {
                     ParentSelectorField(
                         label = "Parent Category",
                         selectedParentName = parent.name,
                         selectedParentColor = Color(parent.color),
                         onClick = { /* Locked */ },
                         enabled = false
                     )
                 }
            }

            // Appearance Section
            SectionHeader("Appearance", modifier = Modifier.padding(horizontal = 0.dp))
            
            SecondaryCard {
                Column(
                    modifier = Modifier.padding(Dimens.SpacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                ) {
                    // Preview Chip
                    val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(selectedColor).copy(alpha = 0.2f))
                            .border(2.dp, Color(selectedColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isEmoji) {
                            Text(text = iconName, fontSize = 32.sp)
                        } else {
                            Icon(
                                imageVector = CategoryUtils.getCategoryIcon(iconName),
                                contentDescription = null,
                                tint = Color(selectedColor),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
                    ) {
                        Button(
                            onClick = { showIconPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Icon")
                        }
                        Button(
                            onClick = { showEmojiPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Emoji")
                        }
                    }
                    
                    // Quick Suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                    ) {
                        suggestedIcons.forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (iconName == icon) Color(selectedColor).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                    .clickable { iconName = icon },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryUtils.getCategoryIcon(icon),
                                    contentDescription = null,
                                    tint = if (iconName == icon) Color(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Color Palette
            Text("Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                presetColors.forEach { color ->
                    val isSelected = selectedColor == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color.toArgb() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { 
                        if (name.isNotBlank() && name != "Adjustment" && name != "Transfer") {
                            onConfirm(name, type, selectedColor, parentId, iconName)
                        }
                    },
                    enabled = name.isNotBlank() && name != "Adjustment" && name != "Transfer",
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerBottomSheet(
            onDismiss = { showIconPicker = false },
            onIconSelected = { 
                iconName = it
                showIconPicker = false
            },
            selectedIcon = iconName,
            color = Color(selectedColor)
        )
    }

    if (showEmojiPicker) {
        EmojiPickerBottomSheet(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { 
                iconName = it
                showEmojiPicker = false
            },
            selectedEmoji = iconName
        )
    }

    if (showParentPicker) {
        ParentCategoryPickerBottomSheet(
            onDismiss = { showParentPicker = false },
            categories = possibleParents,
            onParentSelected = { 
                parentId = it?.id
                showParentPicker = false
            },
            selectedParentId = parentId
        )
    }
}

@Composable
fun ParentSelectorField(
    label: String,
    selectedParentName: String,
    selectedParentColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedParentColor != null) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(selectedParentColor)
                    )
                }
                Text(
                    text = selectedParentName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentCategoryPickerBottomSheet(
    onDismiss: () -> Unit,
    categories: List<Category>,
    onParentSelected: (Category?) -> Unit,
    selectedParentId: Long?
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = Dimens.SpacingExtraLarge)
        ) {
            Text(
                "Select Parent Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Dimens.SpacingLarge))
            
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                // "No Parent" Option
                Surface(
                    onClick = { onParentSelected(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedParentId == null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Text("No Parent", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        if (selectedParentId == null) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                categories.forEach { category ->
                    val isSelected = category.id == selectedParentId
                    Surface(
                        onClick = { onParentSelected(category) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.color).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconName = category.iconName
                                val isEmoji = iconName.isNotEmpty() && !CategoryUtils.iconList.contains(iconName)
                                if (isEmoji) {
                                    Text(text = iconName, fontSize = 16.sp)
                                } else {
                                    Icon(
                                        imageVector = CategoryUtils.getCategoryIcon(iconName),
                                        contentDescription = null,
                                        tint = Color(category.color),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerBottomSheet(
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit,
    selectedIcon: String,
    color: Color
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredIcons = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CategoryUtils.iconGroups
        } else {
            val filtered = CategoryUtils.iconList.filter { it.contains(searchQuery, ignoreCase = true) }
            mapOf("Search Results" to filtered)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = Dimens.SpacingExtraLarge)
        ) {
            Text("Select Icon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Dimens.SpacingMedium))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search icons...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
            )
            
            Spacer(Modifier.height(Dimens.SpacingLarge))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                filteredIcons.forEach { (group, icons) ->
                    if (searchQuery.isBlank() && icons.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = Dimens.SpacingSmall)
                            )
                        }
                    }
                    items(icons) { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = color,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onIconSelected(icon) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryUtils.getCategoryIcon(icon),
                                contentDescription = null,
                                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerBottomSheet(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    selectedEmoji: String
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SpacingLarge)
                .padding(bottom = Dimens.SpacingExtraLarge)
        ) {
            Text("Select Emoji", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Dimens.SpacingLarge))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                modifier = Modifier.heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                items(CategoryUtils.emojiList) { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}
