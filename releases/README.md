# MedBill Tracker Release Notes

## v1.0.2 (2026-07-26)

### Included artifacts

- APK: `MedBillTracker-v1.0.2.apk`
- APK SHA256: `4afa8ec857530ed8ec2ded71e259eaf83f70c3db0d7a465ef3bcbbda2c69cd7c`
- Email/download package (ZIP): `MedBillTracker-v1.0.2-email.zip`
- ZIP SHA256: `1941f881146bdbb1d9b56242a999ca6e7a7283d268ca20d2de67ab18fca06982`

### Changes in this release

- Added payment method choice for bill payment flow: `CreditCard` or `Zelle`.
- Added user option to save preferred payment method in the app.
- Backend payment session creation now validates and routes supported methods explicitly.

### Distribution notes

- Use `MedBillTracker-v1.0.2-email.zip` for email attachment workflows.
- Use `MedBillTracker-v1.0.2.apk` for direct sideload installs or direct repo download.

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
