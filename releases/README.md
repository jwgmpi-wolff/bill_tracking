# MedBill Tracker Release Notes

## v1.0.1 (2026-07-26)

### Included artifact

- APK: `MedBillTracker-v1.0.1.apk`
- SHA256: `434173f48243987bc1234c5a7a8ce3f500ba24ac7cc47c5f3188cbf76e78422f`

### Changes in this release

- Android app default API endpoint now targets the Azure hosted backend:
  - `https://app-medbill-api-3622.azurewebsites.net/`
- Add Bill screen supports scan-from-picture OCR and manual entry fallback.
- Deploy script now continues when resource group already exists and reuses existing RG location.
- Backend JWT audience validation now accepts both `api://<appId>` and `<appId>` token audience formats to reduce false 401 responses.
- Backend startup health remains in degraded-safe mode with `databaseReady` reporting in health endpoints.

### Install notes

- Enable install from unknown sources on the target Android device.
- If a previously installed build uses a different signing key, uninstall the old app first.
