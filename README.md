# Maxxed Compass

Maxxed Compass is an offline Android compass, trip tracker, and calculated sky guide. The functional baseline is commit `4011abc7d169e36b48ac78bceeb7d25f246b370b`.

## Implemented

- Rotation-vector compass with accelerometer/magnetometer fallback
- Magnetic and true-north headings with geomagnetic declination
- Calibration and magnetic-interference status
- Foreground trip tracking with pause, resume, stop, segments, and local history
- DataStore persistence and active-trip process recovery
- Lock-screen display flags and optional keep-screen-on mode
- Offline Sky Scanner with all 88 selectable constellations enabled by default, dots/lines, red map, camera preview, and search
- Metric/imperial units, themes, night mode, and advanced tools

All trip, setting, location, camera, and sensor processing stays on the device. The app does not request Internet access.

## Debug Verification

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Signed Release

Create an ignored properties file outside the repository:

```properties
storeFile=/absolute/path/to/upload-key.jks
storePassword=REDACTED
keyAlias=upload
keyPassword=REDACTED
```

Build and verify:

```bash
export MAXXED_RELEASE_PROPERTIES=/absolute/path/to/release.properties
./gradlew clean testDebugUnitTest lintRelease assembleRelease bundleRelease
chmod +x scripts/verify-release.sh
MAXXED_EXPECTED_CERT_SHA256=YOUR_EXPECTED_CERT_DIGEST \
  scripts/verify-release.sh
```

The verifier checks APK and AAB signatures, compares the APK signer when an expected digest is supplied, rejects a debuggable release APK, and writes SHA-256 hashes under `release/verification/`.

## Release Status

See `RELEASE_READINESS.json` for the machine-readable status, `docs/RELEASE_CHECKLIST.md` for current progress, and `docs/PHYSICAL_TEST_PLAN.md` for the required Samsung S22 Ultra acceptance run. A release is not READY until every pending physical and artifact check has recorded evidence.
