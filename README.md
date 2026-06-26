# Maxxed Compass

Maxxed Compass is an offline Android compass, trip tracker, and calculated sky guide for hikers, field work, quick orientation, and night-sky reference. It is designed as a local-first utility: heading, trip, location, camera, settings, and sky-guide behavior stay on device, and the app does not request Internet access.

The functional baseline is commit `4011abc7d169e36b48ac78bceeb7d25f246b370b`.

## Website Summary

Use this copy when referencing the project externally:

> Maxxed Compass is an offline Android compass and trip utility with magnetic/true-north headings, calibration status, foreground trip tracking, local history, lock-screen display support, and a calculated Sky Scanner for constellation reference. It is built for practical outdoor orientation, not navigation guarantees or emergency positioning.

## Readiness

Current release status is tracked in [`READINESS.md`](READINESS.md) and machine-readable release evidence is tracked in [`RELEASE_READINESS.json`](RELEASE_READINESS.json). The app is **not production-ready** until every required physical test, signed artifact check, store asset, and support/privacy verification is recorded.

## Implemented

- Rotation-vector compass with accelerometer/magnetometer fallback
- Magnetic and true-north headings with geomagnetic declination
- Calibration and magnetic-interference status
- Foreground trip tracking with pause, resume, stop, segments, and local history
- DataStore persistence and active-trip process recovery
- Lock-screen display flags and optional keep-screen-on mode
- Offline Sky Scanner with all 88 selectable constellations enabled by default, dots/lines, red map, camera preview, and search
- Metric/imperial units, themes, night mode, and advanced tools

## Privacy And Data Handling

- No Internet permission is required for the documented local-first feature set.
- Trip records, settings, sensor readings, location-derived values, and sky-guide state are processed on device.
- The project should not claim cloud sync, account recovery, live maps, emergency location, or social sharing unless those features are added and disclosed later.

## Debug Verification

```bash
./gradlew clean testDebugUnitTest lintDebug assembleDebug
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Signed Release

Create an ignored local release properties file outside the repository, then run the signed-release build and verifier described in `docs/RELEASE_CHECKLIST.md`.

The verifier checks APK and AAB signatures, compares the APK signer when an expected digest is supplied, rejects a debuggable release APK, and writes SHA-256 hashes under `release/verification/`.

## Required Physical Acceptance

Before Play submission, record evidence for:

- heading accuracy and true-north behavior
- calibration and magnetic-interference prompts
- foreground trip tracking and segment persistence
- notification actions and process recovery
- lock-screen display behavior
- Sky Scanner search, persistence, and basic directional usability
- 10-20 minute outdoor distance and battery acceptance run

## Store And Disclosure Materials

Current release-polish materials live under `docs/`:

- `docs/PLAY_STORE_LISTING.md`: Play Store title, descriptions, release notes, screenshot checklist, and copy guardrails.
- `docs/PLAY_DISCLOSURE_CHECKLIST.md`: Play Console declaration checklist for local-first Compass behavior.
- `docs/RELEASE_EVIDENCE_TEMPLATE.md`: final acceptance evidence template for signed artifacts, device testing, store assets, and release decision.

## Release Status

See `RELEASE_READINESS.json` for the machine-readable status, `docs/RELEASE_CHECKLIST.md` for current progress, and `docs/PHYSICAL_TEST_PLAN.md` for the required Samsung S22 Ultra acceptance run. A release is not READY until every pending physical and artifact check has recorded evidence.
