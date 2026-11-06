# My Invoice

An app for creating and tracking freelance invoices. Android first, with a
macOS app planned later.

## Project layout

- `core/` — shared Kotlin Multiplatform code: the data model, the on-device
  SQLite database (SQLDelight), invoice numbering, amount in words, money
  formatting and payment status. It has no Android dependencies, so the same
  code can power a macOS (Compose Desktop) app later.
- `androidApp/` — the Android app (Jetpack Compose).

## Key rules

- **Amounts** are stored in minor units (paise or cents) to avoid rounding errors.
- **Currencies:** INR, USD, AUD and EUR. INR uses Indian grouping (₹1,23,456.00)
  and lakh/crore in words; others use thousands/millions.
- **Invoice numbers** follow a configurable pattern. The default is
  `{PREFIX}/{FY}/{SEQ:3}` (e.g. `RS/26-27/001`), restarting every 1 April.
  `{YY}{SEQ:3}` reproduces the older `26031` style, and a starting number lets
  you continue an existing series.
- **GST:** not registered, so invoices carry no tax lines and can show
  "Supplier not registered under GST".
- **Payment status** is Draft, Sent, Partly paid, Paid, Overdue or Void. Paid and
  Overdue are worked out from recorded payments and the due date. Sent invoices
  can be voided but not deleted, so numbers are never reused.

## Building

```bash
./gradlew :core:jvmTest                  # shared logic tests
./gradlew :androidApp:assembleDebug      # debug APK (needs the Android SDK)
./gradlew :core:jvmTest -PexcludeAndroid=true   # core only, no Android SDK needed
```

Every push and pull request is built on GitHub Actions. The debug APK is
attached to each run under **Artifacts** and can be installed on a phone
directly.
