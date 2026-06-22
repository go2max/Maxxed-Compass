# MaxxedCompass

## Current Status

This is a standalone Android Studio project that currently builds a debug APK and opens to a basic Compose screen. It is not yet feature-complete relative to the same-day task brief.

## Build

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Build APK For Individual Testing

```bash
./gradlew assembleDebug
open app/build/outputs/apk/debug/
```

Primary test artifact:

- `app/build/outputs/apk/debug/app-debug.apk`

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Current Permissions

- None requested yet

## Verified in This Session

- `testDebugUnitTest` passed
- `lintDebug` passed
- `assembleDebug` passed
- Individual debug APK was built for direct install/testing
- Debug APK copied to `../Deliverables/MaxxedCompass-debug.apk`

## Current Workflow

- Launches into a Compose-based placeholder home screen

## Major Limitations

- No compass sensor flow yet
- No trip tracking yet
- No lock-screen flow yet
- No sky preview yet
- No persistence, settings, unit conversion, or manual device validation yet

## Next Release Step

Implement the real compass heading pipeline, trip model, persistence, and offline sky preview before claiming acceptance against the task brief.
