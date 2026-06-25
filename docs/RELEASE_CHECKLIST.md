# Maxxed Compass Release Checklist

Updated: June 25, 2026

## Source And Build

- [x] Functional compass, trip, persistence, lock-screen, and Sky Scanner baseline preserved
- [x] Kotlin warning cleanup completed
- [x] Foreground tracking process recovery and notification command handling hardened
- [x] Release debug-key fallback removed
- [x] All 88 unique constellations bundled offline with required attribution
- [x] All constellations enabled by default with persistent individual checkboxes and an All control
- [x] Constellation calculations throttled for battery use
- [x] Unit tests pass: 13 tests in prior run
- [x] Debug lint passes in prior run
- [x] Debug APK assembles successfully in prior run
- [x] Updated debug APK installed on Samsung Galaxy S22 Ultra in prior run
- [x] Constellation controls are visible on the S22 Ultra in prior run
- [ ] Re-run unit tests, debug lint, release lint, debug build, signed APK, and signed AAB for the final release candidate

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
- [x] Add release evidence template: `docs/RELEASE_EVIDENCE_TEMPLATE.md`

## Play Store

- [x] Draft Play Store listing copy: `docs/PLAY_STORE_LISTING.md`
- [x] Draft Play disclosure checklist: `docs/PLAY_DISCLOSURE_CHECKLIST.md`
- [x] Publishable privacy-policy URL identified: `https://techmaxxed.com/apps/maxxed-compass/privacy/`
- [ ] Confirm support email mailbox is active: `support@techmaxxed.com`
- [ ] Verify production 512 x 512 icon
- [ ] Create and verify 1024 x 500 feature graphic
- [ ] Capture signed-release phone screenshots
- [ ] Complete Data Safety and permission declarations in Play Console
- [ ] Review background-location and boot-recovery declarations against the signed artifact
- [ ] Finalize store listing and release notes in Play Console

## Publication

- [ ] Confirm implementation commit
- [ ] Confirm release-documentation commit
- [ ] Push both commits to `origin/main`
- [ ] Confirm clean working tree and remote commit IDs

Release status: **NOT READY** until every required physical, signed-artifact, store-console, and evidence item above is checked and evidence is recorded.
