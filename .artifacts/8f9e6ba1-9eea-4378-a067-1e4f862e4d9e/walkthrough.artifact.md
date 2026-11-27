# Static FAB Stack Walkthrough

I have replaced the expanding FAB menu with a static, two-button vertical stack. This unifies the FAB height across all screens and provides a faster, more direct interaction model for logging transactions.

## Changes Made

### [QuickTemplatesComponents.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/components/QuickTemplatesComponents.kt)
- **Deleted `FloatingActionButtonMenu` and `FabMenuItem`**: Removed all logic for the expanding menu, including scrims, animations, and rotation.
- **Created `StaticTransactionFabs`**: A new, simple composable that displays a primary "+" FAB and a smaller secondary "Templates" FAB above it.
- **Cleaned up Imports**: Removed now-unused animation and popup-related imports.

### [HomeScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/HomeScreen.kt) & [TransactionsScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionsScreen.kt)
- **Restored Standard `Scaffold` Slot**: Removed the root `Box` overlay and moved the FAB logic back into the `Scaffold`'s `floatingActionButton` slot.
- **Unified Positioning**: By using the native FAB slot, the buttons are now automatically aligned at the same height as all other screens (like Account Management).

## Verification Results

### Visual Verification
- **Unified Height**: The primary "+" button is now at the exact same screen coordinates across Home, Transactions, and Account Management screens.
- **Visual Hierarchy**: The secondary button is smaller (40dp) and uses a more muted color to clearly indicate it's a secondary action.

### Functional Verification
- **Primary FAB (+)**: Tapping this immediately opens the blank "Add Transaction" screen.
- **Secondary FAB (Receipt)**: Tapping this immediately opens the "Quick Templates" bottom sheet.
- **Consistency**: Both buttons remain visible whenever a budget book is active.

> [!TIP]
> This change improves performance and accessibility by removing complex window-layering (Popups) and providing a stable, predictable UI at all times.
