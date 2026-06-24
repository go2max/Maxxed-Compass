# Maxxed Compass Release Checklist

Updated: June 23, 2026

## Source And Build

- [x] Functional compass, trip, persistence, lock-screen, and Sky Scanner baseline preserved
- [x] Kotlin warning cleanup completed
- [x] Foreground tracking process recovery and notification command handling hardened
- [x] Release debug-key fallback removed
- [x] All 88 unique constellations bundled offline with required attribution
- [x] All constellations enabled by default with persistent individual checkboxes and an All control
- [x] Constellation calculations throttled for battery use
- [x] Unit tests pass: 13 tests
- [x] Debug lint passes
- [x] Debug APK assembles successfully
- [x] Updated debug APK installed on Samsung Galaxy S22 Ultra
- [x] Constellation controls are visible on the S22 Ultra

## Indoor Device Acceptance

- [ ] Confirm picker reports 88/88 and All is checked by default
- [ ] Disable All, enable several constellations, and confirm only selected line figures render
- [ ] Restart the app and confirm constellation selections persist
- [ ] Confirm camera permission denial, grant, preview, and red-map switching
- [ ] Confirm trip Start, Pause, Resume, and Stop from both app and notification
- [ ] Confirm stopped trip appears once in history and persists after restart
- [ ] Confirm active and paused trip recovery after process termination
- [ ] Confirm lock-screen display and Keep Screen On behavior
- [ ] Confirm theme, units, night mode, history rename, deletion, and delete-all behavior

## Outdoor Acceptance

- [ ] Verify magnetic heading at north, east, south, and west
- [ ] Verify true north and displayed declination
- [ ] Verify Polaris, Moon, and one constellation against a trusted sky reference
- [ ] Complete a 10-20 minute known-distance walk
- [ ] Confirm screen-off tracking, pause exclusion, GPS filtering, and final distance
- [ ] Record starting/final battery and confirm no thermal warning

## Release Artifacts

- [ ] Confirm version name and version code
- [ ] Configure the release/upload key and expected certificate SHA-256
- [ ] Run release unit tests and lint
- [ ] Build signed release APK
- [ ] Build signed release AAB
- [ ] Verify APK and AAB signatures
- [ ] Verify signer matches the expected certificate
- [ ] Verify release manifest is non-debuggable
- [ ] Record final APK and AAB SHA-256 hashes

## Play Store

- [ ] Publish the privacy-policy URL
- [ ] Confirm support email
- [ ] Verify production 512 x 512 icon
- [ ] Create and verify 1024 x 500 feature graphic
- [ ] Capture signed-release phone screenshots
- [ ] Complete Data Safety and permission declarations
- [ ] Review background-location and boot-recovery declarations
- [ ] Finalize store listing and release notes

## Publication

- [ ] Confirm implementation commit
- [ ] Confirm release-documentation commit
- [ ] Push both commits to `origin/main`
- [ ] Confirm clean working tree and remote commit IDs

Release status: **NOT READY** until every required item above is checked and evidence is recorded.
