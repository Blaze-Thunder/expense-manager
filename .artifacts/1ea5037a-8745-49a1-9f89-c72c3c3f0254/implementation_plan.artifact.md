# Fix Z-Order Overlap in Transaction Entry Screen

The goal is to prevent the floating `TransactionEntryActionBar` from obstructing the content of the `LazyColumn` in `TransactionEntryScreen.kt`. We will achieve this by dynamically measuring the action bar's height and applying it as bottom content padding to the `LazyColumn`.

## Proposed Changes

### [TransactionEntryScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt)

#### [MODIFY] [TransactionEntryScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt)
- **Imports**: Add `onGloballyPositioned`, `LocalDensity`, and `mutableIntStateOf`.
- **State Management**:
    - Add `actionBarHeightPx` to track the measured height in pixels.
    - Add `density` to convert pixels to DP.
    - Calculate `actionBarHeightDp` with a safe fallback of `88.dp`.
- **LazyColumn**: Update `contentPadding` bottom value to use `actionBarHeightDp + Dimens.SpacingLarge`.
- **Action Bar**: Apply `onGloballyPositioned` to `TransactionEntryActionBar` to update `actionBarHeightPx`.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to the Transaction Entry screen.
- Scroll to the bottom of the list and ensure the last field (Note or Occurrences) is fully visible above the Save action bar.
- Focus on the Note field and ensure it scrolls to a position that is not obstructed by the action bar when the keyboard is open.
- Test across different device sizes and font scales to ensure the dynamic measurement works correctly.
