# Readiness

Last updated: 2026-06-25

## Status

**NOT READY / PHYSICAL AND SIGNED RELEASE EVIDENCE PENDING**

Maxxed Compass has a functional offline baseline, but the repository-owned release record still blocks production readiness until physical tests and signed artifacts are verified.

## Current evidence

- Functional baseline commit is documented as `4011abc7d169e36b48ac78bceeb7d25f246b370b`.
- Implemented features include rotation-vector compass with fallback sensors, magnetic/true north, calibration/interference status, foreground trip tracking, process recovery, lock-screen display behavior, and offline Sky Scanner.
- Store/disclosure materials are drafted under `docs/`.
- Machine-readable readiness state exists in `RELEASE_READINESS.json` and currently reports `overall_status: NOT_READY`.

## Blocking launch gates

- Complete required physical tests: heading accuracy, true north, trip tracking, notification actions, process recovery, lock-screen display, sky scanner, 10-20 minute outdoor distance, and 10-20 minute outdoor battery run.
- Build signed APK/AAB with production signing and record hashes.
- Verify APK signer, AAB signature, and non-debuggable release manifest.
- Confirm support mailbox activation and final Play Console privacy/store verification.
- Capture signed-release screenshots and required 1024 x 500 feature graphic.
- Update `RELEASE_READINESS.json` with final pass evidence before changing this file to READY.

## Ready when

Mark **READY** only after `RELEASE_READINESS.json` records passing physical tests, signed artifact verification, artifact hashes, active support/privacy links, complete store graphics/screenshots, and no remaining exact release failures.