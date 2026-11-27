# UI Overlap and Transparency Fix Walkthrough

I have fixed the issue where the `HomeScreen` content was visible behind the `TransactionEntryScreen` and through the floating action bar.

## Changes Made

### 1. Solid Root Background
Replaced the root `Box` with a `Surface` that uses `MaterialTheme.colorScheme.surface` as its solid color. This ensures that the `TransactionEntryScreen` is fully opaque and covers any content from previous screens, even during keyboard interactions.

### 2. Opaque Floating Action Bar
Updated `TransactionEntryActionBar` to use a `Surface` with `tonalElevation` as its root. This makes the area around the "Save" and "Templates" buttons opaque, preventing visual overlap with the scrollable content behind them.

### 3. Unified Layout Lifecycle
Moved all dialogs and bottom sheets (Calculator, Selectors, etc.) inside the `ready` state's `Surface` container. This ensures they all benefit from the same solid background and consistent theme styling.

## Verification Results
- **Build Status**: Successful.
- **Visuals**: The `TransactionEntryScreen` now provides a solid, non-transparent background that correctly hides the `HomeScreen` content as requested.
