#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${1:-$ROOT_DIR/app/build/outputs/apk/release/app-release.apk}"
AAB="${2:-$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab}"
REPORT_DIR="$ROOT_DIR/release/verification"

fail() {
    printf 'FAIL: %s\n' "$*" >&2
    exit 1
}

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
APKSIGNER="$(command -v apksigner || true)"
APKANALYZER="$(command -v apkanalyzer || true)"
if [[ -n "$SDK_ROOT" ]]; then
    if [[ -z "$APKSIGNER" ]]; then
        APKSIGNER="$(find "$SDK_ROOT/build-tools" -type f -name apksigner 2>/dev/null | sort -V | tail -n 1)"
    fi
    if [[ -z "$APKANALYZER" ]]; then
        for tool_root in "$SDK_ROOT/cmdline-tools" "$SDK_ROOT/tools"; do
            if [[ -d "$tool_root" ]]; then
                APKANALYZER="$(find "$tool_root" -type f -name apkanalyzer 2>/dev/null | head -n 1)"
                [[ -n "$APKANALYZER" ]] && break
            fi
        done
    fi
fi
[[ -x "$APKSIGNER" ]] || fail "apksigner was not found; set ANDROID_SDK_ROOT or add it to PATH"
[[ -x "$APKANALYZER" ]] || fail "apkanalyzer was not found; set ANDROID_SDK_ROOT or add it to PATH"
command -v jarsigner >/dev/null 2>&1 || fail "jarsigner is not on PATH"
command -v keytool >/dev/null 2>&1 || fail "keytool is not on PATH"
if command -v sha256sum >/dev/null 2>&1; then
    SHA256=(sha256sum)
elif command -v shasum >/dev/null 2>&1; then
    SHA256=(shasum -a 256)
else
    fail "sha256sum or shasum is required"
fi

[[ -f "$APK" ]] || fail "APK not found: $APK"
[[ -f "$AAB" ]] || fail "AAB not found: $AAB"

mkdir -p "$REPORT_DIR"

"$APKSIGNER" verify --verbose --print-certs "$APK" | tee "$REPORT_DIR/apk-signer.txt"
jarsigner -verify -verbose -certs "$AAB" > "$REPORT_DIR/aab-signer.txt"
keytool -printcert -jarfile "$AAB" > "$REPORT_DIR/aab-certificate.txt"

DEBUGGABLE="$("$APKANALYZER" manifest debuggable "$APK" | tr -d '[:space:]')"
[[ "$DEBUGGABLE" == "false" ]] || fail "release APK is debuggable: $DEBUGGABLE"
printf 'debuggable=false\n' > "$REPORT_DIR/manifest.txt"

(
    cd "$ROOT_DIR"
    "${SHA256[@]}" "${APK#$ROOT_DIR/}" "${AAB#$ROOT_DIR/}"
) | tee "$REPORT_DIR/SHA256SUMS"

if [[ -n "${MAXXED_EXPECTED_CERT_SHA256:-}" ]]; then
    EXPECTED="$(printf '%s' "$MAXXED_EXPECTED_CERT_SHA256" | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]')"
    ACTUAL="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$REPORT_DIR/apk-signer.txt" | head -n 1 | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]')"
    AAB_ACTUAL="$(sed -n 's/^[[:space:]]*SHA256: //p' "$REPORT_DIR/aab-certificate.txt" | head -n 1 | tr -d ':[:space:]' | tr '[:lower:]' '[:upper:]')"
    [[ -n "$ACTUAL" ]] || fail "could not read the APK signer SHA-256 digest"
    [[ -n "$AAB_ACTUAL" ]] || fail "could not read the AAB signer SHA-256 digest"
    [[ "$ACTUAL" == "$EXPECTED" ]] || fail "APK signer digest does not match MAXXED_EXPECTED_CERT_SHA256"
    [[ "$AAB_ACTUAL" == "$EXPECTED" ]] || fail "AAB signer digest does not match MAXXED_EXPECTED_CERT_SHA256"
    printf 'expected_signer_sha256=%s\napk_signer_sha256=%s\naab_signer_sha256=%s\n' \
        "$EXPECTED" "$ACTUAL" "$AAB_ACTUAL" > "$REPORT_DIR/signer-match.txt"
fi

printf 'PASS: release APK and AAB are signed; APK is non-debuggable; hashes written to %s\n' "$REPORT_DIR/SHA256SUMS"
