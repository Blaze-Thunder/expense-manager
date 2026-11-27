# Final Robust Scrolling Fix for Last Fields

Implement targeted field-level scrolling for the Note field (Transfer mode) and the Occurrences field, which are the last inputs in their respective containers.

## Proposed Changes

### [TransactionDialogs.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionDialogs.kt)

#### [MODIFY] [RecurrenceOptionsView](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionDialogs.kt)
- Update signature to accept `occurrencesBringIntoViewRequester: BringIntoViewRequester`.
- Attach the requester to the Occurrences `OutlinedTextField`.

### [TransactionEntryScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt)

#### [MODIFY] [Requester Declarations](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt#L150-L160)
- Add `val occurrencesBringIntoViewRequester = remember { BringIntoViewRequester() }`.

#### [MODIFY] [Index Calculation](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt#L225-L230)
- Make indices explicit for Note and Recurring sections based on layout visibility.

#### [MODIFY] [Note Field Focus](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt#L525-L545)
- Use the calculated `noteItemIndex`.
- Coordinate `animateScrollToItem` and `bringIntoView` with a 180ms delay.

#### [MODIFY] [Recurring Section Callback](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionEntryScreen.kt#L605-L625)
- Pass `occurrencesBringIntoViewRequester` to `RecurrenceOptionsView`.
- Update `onAnyFieldFocused` to scroll to `recurringItemIndex` and call `occurrencesBringIntoViewRequester.bringIntoView()`.

## Verification Plan

### Manual Verification
- **Transfer Note**: Select "Transfer", focus Note. Verify it scrolls above the Save bar.
- **Occurrences**: Select "Expense", enable Recurring, focus Occurrences. Verify it scrolls above the Save bar.
- **No Regression**: Check standard Expense/Income note scrolling.
