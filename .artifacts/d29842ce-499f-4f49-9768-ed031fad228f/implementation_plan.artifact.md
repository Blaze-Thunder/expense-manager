# Implementation Plan - FAB Menu Scrim and Touch Bug Fixes

This plan addresses the "bottom white bar" gap and the touch-leak issue when the FAB menu is expanded.

## User Review Required

> [!IMPORTANT]
> I will be changing the FAB menu implementation to use a `Popup` for the expanded state. This ensures the scrim covers the entire screen (including the bottom navigation bar) and correctly blocks touches to the background content.

## Proposed Changes

### UI & Layout Fixes

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/ssaik/OneDrive/Documents/Sai Kiran Documents/Projects/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/HomeScreen.kt) & [TransactionsScreen.kt](file:///C:/Users/ssaik/OneDrive/Documents/Sai Kiran Documents/Projects/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/TransactionsScreen.kt)
- Add `contentWindowInsets = WindowInsets(0.dp)` to the inner `Scaffold`. This removes the double-padding gap ("white bar") at the bottom of the screens, as the outer `Scaffold` in `MainActivity` already handles the navigation bar insets.

#### [MODIFY] [QuickTemplatesComponents.kt](file:///C:/Users/ssaik/OneDrive/Documents/Sai Kiran Documents/Projects/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/components/QuickTemplatesComponents.kt)
- **Refactor `FloatingActionButtonMenu`**:
    - Wrap the expanded menu items and the scrim in a `Popup`.
    - Use `PopupProperties(focusable = true)` to ensure the back button and touches are handled correctly.
    - This allows the scrim to truly fill the screen and prevents any touch leakage to the underlying screen or navigation pane.
    - Ensure the FAB itself remains visible and accessible to toggle the menu.

## Verification Plan

### Automated Tests
- Build the project to ensure no layout or parameter errors.

### Manual Verification
1.  **Gap Fix**: Verify that the "white bar" above the navigation pane is gone on the Home and Transactions screens.
2.  **Scrim Coverage**: Open the FAB menu. Verify that the dark scrim now covers the entire screen, including the bottom navigation bar.
3.  **Touch Blocking**: While the FAB menu is open, try to tap on a transaction or a navigation tab. Verify that these touches are blocked by the scrim and only dismiss the menu.
4.  **Back Button**: Verify that pressing the system back button while the menu is open collapses the menu instead of exiting the app/screen.
