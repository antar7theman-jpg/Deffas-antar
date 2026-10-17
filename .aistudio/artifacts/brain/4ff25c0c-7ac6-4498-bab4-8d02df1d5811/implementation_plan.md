# Implementation Plan - QR Code Scanner for Invoices Archive

## Objective
Add a QR code scanner feature to the Invoices Archive screen (`InvoicesArchiveScreen`), allowing users to scan an invoice QR code (or enter/scan invoice number/ID) and instantly display the corresponding detailed invoice preview sheet.

## Proposed Changes

### 1. `CustomersAndPurchasesScreens.kt` (`InvoicesArchiveScreen`)
- Add a QR Scanner action icon button in the Top App Bar of `InvoicesArchiveScreen`.
- Implement a QR Scanner dialog / camera scanner launcher / manual QR code entry dialog.
- When a QR code is successfully scanned or entered, find the matching `ArchivedInvoice` from `viewModel.archivedInvoices.value`.
- Automatically open the detailed invoice preview sheet for that invoice.
- If not found, show a clear notification toast or error dialog ("الفاتورة غير موجودة أو رمز الاستجابة غير صالح").

## Verification Plan
- Compile applet with `compile_applet`.
- Verify the QR code button appears in the Invoices Archive top app bar.
- Test scanning or entering an invoice QR code to verify it opens the invoice detail preview sheet.
