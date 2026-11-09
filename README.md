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

## Using the app

1. **Settings:** add your details, logo and bank account, and choose how
   invoices are numbered (to continue your old `26031` series, pick
   "Year + number" and set the start number).
2. **Clients:** add each client once, with their usual currency.
3. **Invoices:** tap *New invoice*, pick the client, add items (a fixed
   amount, or rate × quantity), and save. It starts as a draft.
4. On the invoice, tap *View / share PDF* to send it, *Mark as sent*, and
   later *Record payment* when money arrives. Part payments add up, and an
   invoice turns *Overdue* once its due date passes.

## Invoice PDF

`core/.../pdf/InvoicePdfLayout.kt` draws the invoice in the original design
(A4, Inter font, orange `#FF5400` accent), measured from the hand-made invoice
template. It draws onto a small `PageCanvas` interface, so the same layout is
used by Android (`AndroidPdfWriter`, built on `PdfDocument`) and by the
desktop/tests (`PdfBoxWriter`, built on Apache PDFBox). Long text wraps and
many line items flow onto extra pages, with the bank details at the bottom of
the last page. The due date and the GST note can each be switched off in the
business profile.

The Inter fonts (SIL Open Font License) are bundled in `core/src/jvmMain/resources/fonts`.

## Building

```bash
./gradlew :core:jvmTest                  # shared logic tests
./gradlew :androidApp:assembleDebug      # debug APK (needs the Android SDK)
./gradlew :core:jvmTest -PexcludeAndroid=true   # core only, no Android SDK needed
```

Every push and pull request is built on GitHub Actions. Builds are signed with
one permanent key from the repository secrets `KEYSTORE_BASE64` (the keystore,
base64-encoded, alias `myinvoice`) and `KEYSTORE_PASSWORD`, so each new APK
installs as an update over the previous one. Without those secrets the build
falls back to a throwaway debug key. The debug APK is
attached to each run under **Artifacts** and can be installed on a phone
directly.
