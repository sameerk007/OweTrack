# OweTrack

OweTrack is an offline-first Android lending ledger for tracking money given to people and repayments received over time. Balances are always derived from the transaction history.

## Included

- Kotlin, Jetpack Compose, Material 3, Room, ViewModel, Repository, Coroutines/Flow, Navigation Compose
- People dashboard with active/settled/all filters, search, sorting, balances, and recent activity
- Fast Give/Receive flow with smart date and payment defaults, UPI provider, purpose suggestions, and exact decimal handling
- Editable/deletable transactions, overpayment warning, chronological history, and optional running balance
- Aggregate and person-level statistics plus person, monthly, payment-method, and UPI charts
- Transaction search by person, purpose, or notes with type, method, and date filters
- PDF/CSV person statements and portable JSON full backup/restore
- Optional biometric/device-credential app lock, theme, default method, and running-balance preferences
- Indian-number currency formatting (`₹1,25,000`) and exact paise storage

## Open and run

1. Open this folder in a current Android Studio installation.
2. Allow Gradle sync to finish. The project uses JDK 17.
3. Run the `app` configuration on an Android 8.0 (API 26) or later device/emulator.

The execution environment used to create this source did not include Android SDK/Gradle, so the final Android compilation must be run in Android Studio or by the included GitHub Actions workflow.

## Architecture

`PersonEntity` and `TransactionEntity` are stored in Room. Amounts use `Long` paise—not `Double`. DAO aggregate queries derive total given, received, and outstanding balances. The repository exposes reactive `Flow` values, and `OweTrackViewModel` owns UI filtering and application operations. UI routes never maintain a second balance field.

The local repository boundary and versioned JSON backup format are deliberate extension points for a future Google Drive or other opt-in sync provider.

## Privacy

The app requests no network permission. Data stays in the local Room database. Android's automatic cloud backup is disabled for the database. Export and sharing occur only after a user taps an explicit action.

## Tests

Run:

```bash
gradle testDebugUnitTest
```

The calculation tests cover the supplied lending sequence, overpayment, zero/negative rejection, decimals, same-day order, edit/delete recalculation, and large values. Room exports its schema to `app/schemas`; future database version changes should include a migration and migration test before release.
