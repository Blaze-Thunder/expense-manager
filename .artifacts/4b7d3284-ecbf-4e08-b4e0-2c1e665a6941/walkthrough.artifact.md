# Walkthrough: Final Robust Scrolling Fix for Last Fields

I have implemented a field-level scrolling solution for the "Note" field in Transfer mode and the "Occurrences" field in recurring settings. These fields, being at the end of their respective sections, required direct anchoring to ensure they remain visible when the keyboard opens.

## Changes Made

### Recurrence Dialog Anchoring
- **TransactionDialogs.kt**:
    - Updated `RecurrenceOptionsView` to accept an `occurrencesBringIntoViewRequester`.
    - Attached the requester directly to the **Occurrences** `OutlinedTextField`. This ensures the specific field is the target of the "bring into view" request, rather than the entire card.

### Transaction Entry Refinements
- **TransactionEntryScreen.kt**:
    - Added a dedicated `occurrencesBringIntoViewRequester`.
    - Unified the **Note** section scrolling logic for all transaction types. By using pre-calculated `noteItemIndex` and `recurringItemIndex`, the scroll animation reliably targets the correct position in the `LazyColumn`.
    - Synchronized the `animateScrollToItem` (section-level) and `bringIntoView` (field-level) calls with a 180ms delay to align with Android's IME animation timing.

## Verification Results

### Manual Verification Checklist
1. **Transfer Note**: Select "Transfer", focus the "Add a note..." field. The field should scroll up smoothly and sit comfortably above the Save bar.
2. **Recurring Occurrences**: Enable "Make Recurring" and focus the "Occurrences" field. The field itself should be pushed above the keyboard.
3. **Scroll Precision**: Verify that the automated scroll places the focused field in a clear, visible area without being partially cut off by the floating bar.

> [!NOTE]
> This "hybrid" approach—scrolling to the list item first, then requesting the field itself be brought into view—provides the most resilient behavior against varying keyboard heights and screen sizes.
