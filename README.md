# HJOOZATI Experimental

An isolated Android prototype for testing the next HJOOZATI product direction. It is intentionally separate from the stable/main app.

## Safety / separation
- Android application ID: `com.hjoozati.experimental` (installs beside the main app).
- Room database: `hjoozati_experimental.db` (does not open or modify the main app database).
- Experimental UI and features must not be merged into the main product without explicit owner approval.
- Official supplied purple/gold/black logo is used as the launcher artwork; do not replace it with generated alternatives.

## Included in this prototype
- Futuristic black, violet and gold glass-inspired Compose interface.
- Dashboard with local booking summaries.
- Local Room-backed booking creation and overlap prevention.
- Booking list and cancellation.
- Experimental navigation areas for tournaments, analytics, AI assistant and advanced tools.
- GitHub Actions workflow to build a debug APK and upload it as an artifact.

## Prototype status
Some advanced feature destinations are currently UI previews/placeholders. Tournament engine, customer CRM/loyalty, AI integration, reminders, exports, encrypted backup, staff permissions, widgets and other approved ideas are not yet implemented as production functionality. They should be implemented incrementally with tests.

## Build
Requires JDK 17 and Gradle 8.9+ compatible with Android Gradle Plugin 8.7.3.

```bash
gradle :app:assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions can build it on each push to `main`/`master` or manually through **Actions → HJOOZATI Experimental - Android APK → Run workflow**.
