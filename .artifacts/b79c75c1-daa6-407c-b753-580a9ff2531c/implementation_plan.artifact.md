# Fix Missed Backup Dates for Cloud and Local Backups

## Background

Users report that automatic backups miss days even when they are actively using the app and phone. Four interconnected root causes were identified.

## Root Causes

### 1. `ExistingPeriodicWorkPolicy.UPDATE` Resets Timing on Every Config Change
Every time `scheduleBackup()` runs (app start, toggling any backup setting, changing frequency), the `UPDATE` policy resets WorkManager's internal timer. A change at 3pm resets a daily backup scheduled for midnight, causing a missed day.

### 2. WorkManager Fails Silent on Constraint-Driven Skips
Both local and cloud workers return `Result.success()` when constraints aren't met or backup is not attempted. WorkManager considers the period "done" and doesn't retry until the next cycle.

### 3. Cloud Backup Requires `UNMETERED` Network by Default
If the user is on mobile data, the cloud backup constraint (`NetworkType.UNMETERED`) is never satisfied, so the backup never runs. There's no fallback to mobile data.

### 4. No Constraint Broadening for Retries
The constraints (`RequiresBatteryNotLow`, `UNMETERED`) are fixed. Even on retry (from backoff), if constraints aren't met, the retry is queued but never executes. WorkManager doesn't try on a broader set of constraints.

## Proposed Changes

---

### [BackupWorker.kt] — Core Worker Logic

#### [MODIFY] [BackupWorker.kt](file:///C:/Users/ssaik/Documents/Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/data/backup/BackupWorker.kt)

**Changes:**
1. **Retry on constraint misses**: When backup is not attempted (e.g., battery low, no network), return `Result.retry()` instead of `Result.success()`. This ensures WorkManager tries again later in the same period.
2. **Better error handling**: Return `Result.retry()` for transient failures (local backup i/o errors, cloud IO exceptions), and only return `Result.failure()` for permanent errors (missing folder URI, auth issues).
3. **Add a guard against infinite retries**: Use `runAttemptCount` to cap retries at a reasonable number and only then fall back to `Result.success()`.

---

### [MainViewModel.kt] — Scheduling Improvement

#### MODIFY [MainViewModel.kt](file:///C:/Users/ssaik/Documents/Projects/Expense Manager/app/src/main/java/com/saik/expensemanager/ui/viewmodel/MainViewModel.kt)

**Changes in `scheduleBackup()`:**

1. **Use `ExistingPeriodicWorkPolicy.KEEP` when work already exists**: This prevents the timer from resetting on every config change. Frequency changes will use `UPDATE`, but config toggles in settings will `KEEP` the existing schedule to avoid resetting the timer.
2. **Expand flex interval for daily backups**: Change from 30 minutes to 4 hours for daily backups so there's a much wider window for constraints to be satisfied.
3. **Cloud backup: fall back to `CONNECTED` on retry**: Use `NetworkType.CONNECTED` (any network) instead so the cloud backup can work on mobile data. (Since we already have the `cloudBackupOnMetered` setting, use it for the initial attempt, then broaden to `CONNECTED` on retry).

---

## Verification Plan

### Automated Tests
- Build the project using `app:assembleDebug` to verify compilation.