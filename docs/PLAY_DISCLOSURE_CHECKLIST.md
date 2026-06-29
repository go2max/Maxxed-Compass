# Maxxed Compass Play Disclosure Checklist

Status: launch-today foreground MVP checklist for Play Console entry. Confirm against the exact signed production artifact before submission.

## Current release-candidate posture

- No account required.
- No Maxxed cloud service required.
- No Internet permission is requested by the Android manifest.
- No background-location permission is requested by the launch-today MVP manifest.
- No boot-completed receiver is declared by the launch-today MVP manifest.
- Compass, foreground trip tracking, settings, location, camera-preview, and sky-guide workflows are designed to run locally on the device.

## Console declarations to verify

- Location access supports true-north calculation, active foreground trip tracking, distance/speed metrics, and sky-position calculations.
- Foreground service location supports an active user-started trip while the tracking service is running.
- Camera access supports optional Sky Scanner preview.
- Notifications support foreground trip tracking controls and status.
- Do not declare background location for this MVP unless the manifest is changed again and tested.
- Do not declare boot recovery for this MVP unless the manifest is changed again and tested.

## Privacy policy URL

Use the live public policy after confirming it matches the signed build:

https://techmaxxed.com/apps/maxxed-compass/privacy/

## Support email

Use the company support inbox after activation:

support@techmaxxed.com

## Submission guardrails

- Do not claim certified marine, aviation, emergency, or land-survey navigation.
- Do not claim survey-grade heading or distance accuracy.
- Do not claim cloud star recognition.
- Do not imply that trip history, exact location, or camera preview is uploaded by Maxxed Compass.
- Do not claim background tracking or boot recovery in the Play listing for this foreground MVP.
- Do not mark release evidence complete until signed artifacts and physical acceptance tests have recorded proof.
