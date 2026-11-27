# Walkthrough - Collapsible Account Sections

I have implemented collapsible sections for the Accounts screen. This allows users to toggle the visibility of different account types (Cash, Digital, Bank, etc.) with smooth animations.

## Changes

### Accounts Screen

#### [AccountsScreen.kt](file:///C:/Users/ssaik/Documents/Android Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/screens/AccountsScreen.kt)

- **Reusable Composable:** Added `CollapsibleAccountSection` to handle the header logic, expansion state, and animations uniformly.
- **State Management:** Used a `mutableStateMapOf` to track the expansion state of each section independently. All sections are collapsed by default.
- **Animations:**
    - **Chevron Rotation:** Added a keyboard arrow icon that rotates 180 degrees using `animateFloatAsState` when the section is toggled.
    - **Content Transition:** Wrapped the account lists in `AnimatedVisibility` with `expandVertically` and `fadeIn` for a smooth layout transition.
- **Interactive Headers:** The entire section header row is now clickable, improving ease of use.
- **Layout Preservation:**
    - Maintained the total balances in the section headers.
    - Preserved the special logic for "Gift Cards" (empty state message and "View all" button).
    - Preserved the "Credit Cards" total summary card within its collapsible container.

## Verification Results

### Manual Verification
- Verified that all account sections (Cash, Digital, Bank, Credit Cards, Gift Cards) appear on the Accounts screen.
- Confirmed that all sections are collapsed upon initial entry to the screen.
- Verified that tapping a header expands the section smoothly and rotates the chevron.
- Confirmed that tapping again collapses the section and hides the account items.
- Verified that "Credit Cards" shows the summary card at the top of its expanded section.
- Verified that "Gift Cards" shows the "No gift cards added" message when empty, but only when expanded.

> [!NOTE]
> The "View all gift cards" button text was shortened to "View all" in the header to ensure it fits comfortably alongside the new expansion chevron and section balance.
