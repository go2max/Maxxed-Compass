# Maxxed Compass Physical Test Plan

Target device: Samsung Galaxy S22 Ultra. Run these checks against the signed release APK, outdoors and away from vehicles, steel structures, magnetic cases, and chargers.

Record the app version, APK SHA-256, Android version, tester, date, weather, starting battery, and whether power saving is enabled.

## Compass And True North

1. Calibrate the phone with a figure-eight motion.
2. Place it flat and compare north, east, south, and west against a trusted surveyed or map-derived reference.
3. Repeat in portrait and landscape after rotating the device slowly through 360 degrees.
4. Pass when the heading settles without wraparound jumps and remains within 10 degrees of the reference at all four points.
5. Enable True North with a fresh GPS fix. Record magnetic heading, true heading, and displayed declination.
6. Pass when true heading equals normalized magnetic heading plus displayed declination within 1 degree, and the declination direction agrees with a current reference for the test location.

## Tracking And Notification Actions

1. Grant precise location and notification access, start a trip, and walk at least 100 meters.
2. Use Pause from the notification, walk 30 meters, and confirm distance does not materially increase.
3. Use Resume from the notification and confirm distance starts increasing again.
4. Use Stop from the notification and confirm the trip appears once in history with plausible duration and distance.
5. Start another trip, pause and resume in the app, add a segment, rename it, then stop and reopen the app. Confirm history and settings persist.

## Process Recovery

1. Start a trip and collect several GPS points.
2. Background the app, then terminate only the app process without clearing storage or force-stopping the package.
3. Confirm the foreground notification returns, tracking continues, and accumulated distance is not reset or duplicated.
4. Repeat while paused. Confirm it returns paused and does not request location updates until Resume.
5. Stop from the recovered notification and confirm the completed trip is saved exactly once.

## Lock Screen

1. Open the compass, lock the phone, then wake it without unlocking.
2. Confirm Maxxed Compass remains visible over the lock screen and the heading continues updating.
3. Enable Keep Screen On and confirm the display remains awake while the activity is foregrounded.
4. Disable it and confirm normal screen timeout resumes.

## Sky Scanner

1. With a fresh location fix, open the red map and search for Polaris, the Moon, and at least one supported planet or constellation.
2. Open the constellation picker. Confirm it lists 88 unique constellations and All is checked by default.
3. Uncheck All, enable Orion, Ursa Major, and Cassiopeia individually, and confirm only those visible patterns render as distinct star dots with connected lines when above or near the horizon.
4. Re-enable All, close and reopen the app, and confirm the selection persists and the center north marker agrees with the map projection.
5. Grant camera access and toggle the camera preview on and off three times.
6. Confirm the preview is live, correctly oriented, does not crash on permission denial, and the app clearly presents results as calculated guidance rather than plate solving.
7. Compare the Moon, Polaris, and one rendered constellation with a current trusted sky reference. Record discrepancies; do not pass if guidance points to the wrong quadrant of the sky.

## Outdoor Distance And Battery

1. Charge above 50 percent, disable charging, record battery percentage and time, and start a release-build trip.
2. Walk a known 0.5-1.5 km route for 10-20 minutes with the screen off for at least half the run.
3. Exercise one Pause/Resume cycle and leave Sky Scanner camera mode off.
4. Stop the trip and record app distance, reference distance, elapsed time, final battery, and device temperature warning state.
5. Pass when distance error is at most 10 percent on an unobstructed route, no impossible GPS jumps appear, the trip survives screen-off operation, battery drop is recorded and judged acceptable for the device conditions, and no thermal warning occurs.

## Evidence

Save screenshots of the four headings, true-north declination, active/paused notifications, recovered trip, lock screen, Sky Scanner modes, completed history, and battery pages. Enter results in `RELEASE_READINESS.json`; use `pass`, `fail`, or `not_run` only.
