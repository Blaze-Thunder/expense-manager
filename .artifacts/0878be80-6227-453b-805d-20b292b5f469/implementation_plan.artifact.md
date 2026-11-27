# Fix Scrolling and Swipe Bug in Transactions Screen

The user reported two issues:
1. Search results are not scrollable.
2. Swiping down sometimes triggers horizontal period navigation instead of vertical scrolling.

## Proposed Changes

### UI Components

#### [MODIFY] [TransactionsScreen.kt](file:///C:/Users/ssaik/Documents/Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionsScreen.kt)

- **Fix Search Scrolling**: Wrap `TransactionListSection` in a `LazyColumn` within `SearchResultsView` to ensure search results can be scrolled.
- **Fix Swipe Issue**:
    - Disable the horizontal drag gesture detection when the search query is not empty, as period-based navigation is irrelevant during search.
    - Improve the horizontal drag gesture detection by ensuring it only triggers for primarily horizontal movements (optional, but disabling during search should solve the user's immediate frustration).
    - Actually, I will use a more robust way to detect horizontal swipes that doesn't interfere as much with vertical scrolling.

## Verification Plan

### Manual Verification
- Deploy the app.
- Go to the Transactions screen.
- Enter a search query that returns many results.
- Verify that the results can be scrolled vertically.
- Verify that swiping down doesn't cause the period (e.g., month) to change.
- Verify that horizontal swiping still works when NOT searching (to change months/weeks).
