# Maxxed Compass Release Evidence Template

Use this file as the evidence checklist for the final release candidate. A release is not READY until every required item is filled with a date, device/build identifier, result, and artifact reference.

## Build identity

- Version name:
- Version code:
- Commit SHA:
- Build date:
- Build machine:
- Release properties file location used locally:
- Expected upload certificate SHA-256:

## Automated checks

| Check | Command | Result | Evidence |
| --- | --- | --- | --- |
| Unit tests | `./gradlew testDebugUnitTest` | Pending | |
| Debug lint | `./gradlew lintDebug` | Pending | |
| Release lint | `./gradlew lintRelease` | Pending | |
| Debug build | `./gradlew assembleDebug` | Pending | |
| Signed release APK | `./gradlew assembleRelease` | Pending | |
| Signed release AAB | `./gradlew bundleRelease` | Pending | |
| Release verifier | `scripts/verify-release.sh` | Pending | |

## Device acceptance

Primary test device: Samsung Galaxy S22 Ultra
Android version:
Build installed:
Install method:

| Scenario | Result | Notes / evidence |
| --- | --- | --- |
| Launch and first-run permissions | Pending | |
| Magnetic heading N/E/S/W | Pending | |
| True north and declination | Pending | |
| Calibration guidance and interference warning | Pending | |
| Trip start/pause/resume/stop in app | Pending | |
| Trip notification controls | Pending | |
| Active-trip recovery after process death | Pending | |
| History save/rename/delete/delete-all | Pending | |
| Keep-screen-on and lock-screen behavior | Pending | |
| Sky Scanner red map | Pending | |
| Sky Scanner camera preview permission flow | Pending | |
| Constellation selector 88/88 default | Pending | |
| Constellation selection persistence | Pending | |
| 10-20 minute known-distance walk | Pending | |
| Screen-off tracking and pause exclusion | Pending | |
| Battery and thermal observation | Pending | |

## Store assets

| Asset | Result | Artifact |
| --- | --- | --- |
| 512 x 512 icon | Pending | |
| 1024 x 500 feature graphic | Pending | |
| Phone screenshots from signed build | Pending | |
| Store listing entered | Pending | |
| Privacy policy URL verified live | Pending | |
| Support email verified active | Pending | |
| Play declarations reviewed | Pending | |
| Release notes entered | Pending | |

## Final release decision

- Release manager:
- Decision: Pending
- Blocking issues:
- Follow-up issues:
