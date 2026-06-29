# Maxxed Compass Launch-Today Runbook

Updated: June 29, 2026

This runbook is for the foreground/local-first MVP branch:

```bash
git checkout launch-today-foreground-mvp
git pull origin launch-today-foreground-mvp
```

## 1. Confirm Java and Android SDK

Use the Android Studio JBR on macOS:

```bash
cd /Users/maxmac/Downloads/Maxxed-Compass
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
java -version
```

If the repo lives somewhere else, change only the `cd` path.

## 2. Configure release signing

Create a local file outside the repo. Do not commit it.

```bash
cat > "$HOME/.maxxed-compass-release.properties" <<'EOF'
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=REPLACE_ME
keyAlias=REPLACE_ME
keyPassword=REPLACE_ME
EOF

export MAXXED_RELEASE_PROPERTIES="$HOME/.maxxed-compass-release.properties"
```

## 3. Run final source checks

```bash
./gradlew clean testDebugUnitTest lintDebug lintRelease assembleDebug
```

Stop if anything fails.

## 4. Build signed release artifacts

```bash
./gradlew assembleRelease bundleRelease
```

Expected outputs:

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

## 5. Verify the signed artifacts

Check the APK is signed and non-debuggable:

```bash
APKSIGNER="$ANDROID_HOME/build-tools/35.0.0/apksigner"
AAPT="$ANDROID_HOME/build-tools/35.0.0/aapt"
APK="app/build/outputs/apk/release/app-release.apk"
AAB="app/build/outputs/bundle/release/app-release.aab"

"$APKSIGNER" verify --verbose --print-certs "$APK"
"$AAPT" dump badging "$APK" | grep -i debuggable && echo "FAIL: debuggable" || echo "PASS: not debuggable"
shasum -a 256 "$APK" "$AAB"
```

Confirm the release manifest does not request background location or boot completed:

```bash
./gradlew :app:processReleaseMainManifest
MANIFEST="app/build/intermediates/merged_manifests/release/processReleaseMainManifest/AndroidManifest.xml"
grep -E "ACCESS_BACKGROUND_LOCATION|RECEIVE_BOOT_COMPLETED" "$MANIFEST" && echo "FAIL: remove background declarations" || echo "PASS: foreground MVP manifest"
```

## 6. Install and smoke test on Samsung S22 Ultra

```bash
adb install -r "$APK"
```

Minimum same-day smoke pass:

- App opens from launcher.
- Compass heading updates.
- True-north mode shows declination after location grant.
- Trip Start, Pause, Resume, Stop works inside app.
- Foreground tracking notification appears during active trip.
- Stopped trip appears once in history and persists after restart.
- Sky Scanner opens; red map works; camera permission grant/deny does not crash.
- Constellation picker shows 88/88 and selected figures render.
- Theme, units, and night mode persist after restart.

## 7. Play Console upload posture

Use the generated AAB:

```text
app/build/outputs/bundle/release/app-release.aab
```

For this MVP, do not claim:

- background location
- boot recovery
- emergency navigation
- certified marine/aviation/survey use
- cloud star recognition
- uploaded trip/location/camera data

## 8. Merge gate

Merge the branch only after:

- final Gradle checks pass
- signed APK/AAB build succeeds
- signer certificate is verified
- manifest check passes
- smoke test passes
- screenshots and feature graphic are captured or explicitly deferred for internal testing
