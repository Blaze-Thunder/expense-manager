# Bug Fix: Editing a Generated Transaction Updates the Recurring Template

## Problem

When a recurring transaction (e.g., "Groceries" with bank "HDFC") was generated and appeared in the
transaction list, and the user edited just that single transaction instance (e.g., changed bank to "SBI"),
the **recurring template itself** was also being updated. This caused all future generated instances to
also use "SBI" instead of preserving the original template.

## Root Cause

In [TransactionEntryScreen.kt](file:///C:/Users/ssaik/Documents/Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt),
line 118, the `isRepeat` toggle defaulted to:

```kotlin
var isRepeat by remember { mutableStateOf(initialTransaction?.repetitiveId != null) }
```

This meant that when editing any generated transaction, the "Repeat" toggle was automatically ON.
When saving, the code (lines 700-719) would then call **both**:

1. `viewModel.updateRepetitiveTransaction(...)` — updating the recurring template
2. `viewModel.updateTransaction(...)` — updating the single transaction instance

## Fix

Changed line 118 so `isRepeat` defaults to `false` when editing:

```kotlin
// Default to false when editing so only the single transaction instance is edited.
// The user can toggle Repeat ON if they wish to update the recurring template as well.
var isRepeat by remember { mutableStateOf(false) }
```

Now editing a generated transaction will:
- **By default**: only update that single transaction instance, unlinking it from the recurrence
- **If user toggles Repeat ON**: update both the template and the instance (explicit intent)

## Verification

- ✅ Build succeeds (`app:assembleDebug`)
- ✅ No new lint errors introduced