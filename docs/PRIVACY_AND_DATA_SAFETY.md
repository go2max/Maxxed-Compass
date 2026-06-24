# Privacy And Data Safety

## App Behavior

Maxxed Compass processes compass sensors, precise location, and an optional camera preview on the device. It stores settings, active trip points, segments, and trip history in Android DataStore on the device. It does not request Internet access and contains no advertising, analytics, account, or cloud-sync SDK.

The camera is used only for the live Sky Scanner preview. The app does not capture, retain, or transmit photos or video. Calculated sky positions use device time, location, and orientation; the feature is not image recognition or camera plate solving.

Users can delete individual saved trips or delete all local app data from Advanced tools. Android uninstall also removes app-private data, subject to the user's Android backup settings.

## Permission Disclosure

| Permission | Purpose |
| --- | --- |
| Precise/coarse location | True-north declination, coordinates, sky calculations, and trip distance |
| Background location | Continue or restore an explicitly started trip when the UI is not visible |
| Camera | Optional live Sky Scanner preview |
| Notifications | Persistent trip status with Pause, Resume, and Stop actions |
| Foreground service/location | Continue an active trip with a visible system notification |
| Receive boot completed | Restore a previously active, unpaused trip after device restart |

## Google Play Data Safety Draft

- Data shared with third parties: No
- Data transmitted off device by the app: No
- Data collected under the Play definition: No
- Data processed ephemerally/on device: precise location, camera frames, orientation sensors
- Data stored locally: settings, trip coordinates, trip statistics, and history
- Encryption in transit: Not applicable because the app does not transmit user data
- Account creation: None
- Deletion request mechanism: In-app local deletion and Android uninstall; no server-side account exists

The Play Console declarations and public privacy-policy URL must be reviewed against the final dependency report and signed manifest before production submission.
