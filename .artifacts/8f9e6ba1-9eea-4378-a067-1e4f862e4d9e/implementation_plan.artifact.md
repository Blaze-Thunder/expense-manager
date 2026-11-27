# Static FAB Stack Implementation

The goal is to replace the expanding FAB menu with two static, always-visible buttons on the Home and Transactions screens. This simplifies the UI and ensures unified FAB positioning across all screens by using the standard `Scaffold` slots.

## User Review Required

> [!NOTE]
> The expanding FAB menu (animations, scrim, rotation) will be entirely removed in favor of a vertical stack of two buttons: a primary "+" FAB and a smaller secondary "receipt" FAB above it.

## Proposed Changes

### [Component] UI Components

#### [NEW] [StaticTransactionFabs](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/components/QuickTemplatesComponents.kt)
- A new composable containing a `Column` with a `SmallFloatingActionButton` (Templates) on top and a standard `FloatingActionButton` (Add) on bottom.
- Spacing between buttons: 12dp.

#### [DELETE] [FloatingActionButtonMenu](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/components/QuickTemplatesComponents.kt)
- Remove the old expanding FAB menu logic and its helper `FabMenuItem`.

### [Component] Screens

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/HomeScreen.kt)
- Remove the root `Box` wrapper used for the overlay.
- Re-enable the `Scaffold`'s `floatingActionButton` slot.
- Use `StaticTransactionFabs` inside the slot.
- Remove `showFabMenu` state.

#### [MODIFY] [TransactionsScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionsScreen.kt)
- Remove the root `Box` wrapper used for the overlay.
- Re-enable the `Scaffold`'s `floatingActionButton` slot.
- Use `StaticTransactionFabs` inside the slot.
- Remove `showFabMenu` state.

## Verification Plan

### Manual Verification
1. **Visual Alignment**: Verify that the primary "+" FAB on the Home screen is at the exact same height and position as the one on the "Accounts Management" screen.
2. **Button Stack**: Verify the smaller secondary button appears correctly above the primary one.
3. **Actions**:
   - Tap primary FAB: Opens Add Transaction screen.
   - Tap secondary FAB: Opens Quick Templates bottom sheet.
4. **Consistency**: Verify the same behavior and positioning on the Transactions screen.
