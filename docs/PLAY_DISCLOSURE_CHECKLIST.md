# Maxxed Compass Play Disclosure Checklist

Status: draft checklist for Play Console entry. Confirm against the exact signed production artifact before submission.

## Current release-candidate posture

- No account required.
- No Maxxed cloud service required.
- No Internet permission is requested by the current Android manifest.
- Compass, trip, setting, location, camera-preview, and sky-guide workflows are designed to run locally on the device.

## Console declarations to verify

- Location access supports true-north calculation, trip tracking, distance/speed metrics, and sky-position calculations.
- Background location supports active trip tracking while the app is not foregrounded.
- Camera access supports optional Sky Scanner preview.
- Notifications support foreground trip tracking controls and status.
- Boot recovery supports restoring or reconciling tracking state after device restart or app update.

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
- Do not mark release evidence complete until signed artifacts and physical acceptance tests have recorded proof.
