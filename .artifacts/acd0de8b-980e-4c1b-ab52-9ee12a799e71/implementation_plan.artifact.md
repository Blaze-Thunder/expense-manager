# Implementation Plan - Collapsible Account Sections

Implement collapsible sections for the Accounts screen to improve readability and navigation, allowing users to hide/show account lists for each type (Cash, Digital, Bank, etc.).

## User Review Required

> [!IMPORTANT]
> The current `AccountsListView` uses a `Column` with `verticalScroll` instead of a `LazyColumn`. I will implement the collapsible sections within the existing `Column` structure to maintain architectural consistency while fulfilling the "reusable composable" requirement.

## Proposed Changes

### UI Components

#### [MODIFY] [AccountsScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/AccountsScreen.kt)

- **Reusable Composable:** Create `CollapsibleAccountSection` which handles the header (title, total, chevron) and the animated list of items.
- **State Management:** Add `expandedSections` state (a map of section keys to booleans) in `AccountsListView`, defaulting all to `false`.
- **Smooth Animation:**
    - Use `animateFloatAsState` for the chevron rotation (0 to 180 degrees).
    - Use `AnimatedVisibility` with `expandVertically` and `shrinkVertically` for the content transition.
- **Section Headers:** Update the section headers to be clickable across their full width to toggle expansion.
- **Integration:** Replace the manual section rendering logic in `AccountsListView` with calls to the new `CollapsibleAccountSection`.

## Verification Plan

### Automated Tests
- I will check for syntax errors and potential runtime issues in the modified file.

### Manual Verification
- Deploy the app and navigate to the Accounts screen.
- Verify that all sections are collapsed by default.
- Tap each section header and ensure it expands smoothly with a rotating chevron.
- Verify that the section totals and individual account balances are still correctly displayed.
- Verify that the "Gift Cards" empty state and "Credit Cards" total summary logic are preserved within the collapsible sections.
