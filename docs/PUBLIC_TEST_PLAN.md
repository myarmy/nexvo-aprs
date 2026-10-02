# Nexvo APRS public Android test plan

## Status

This document defines the public test procedure for a future versioned Android test APK. A public GitHub Release has not yet been published. Testers should only install an APK that is explicitly linked from an official Nexvo APRS GitHub Release and has a stated version name, version code, commit reference, and checksum.

## Purpose

The test programme verifies repeatable installation and the core Android experience on phones and tablets. It collects reproducible feedback before a broader public release.

## Test scope

1. **Installation and startup** — download from the official release, install, start, and record Android version and device model.
2. **Permissions and on-device GNSS** — verify that location permission is requested clearly and that coordinates can be displayed from the device GNSS receiver when satellite visibility is available, including where cellular service is unavailable. This test does not claim satellite or RF emergency-message transmission.
3. **Map and station display** — confirm APRS-IS station reception, MapLibre map rendering, station search, station focus, and details.
4. **APRS messaging** — use only authorized amateur-radio identities and approved test recipients; distinguish a message handed to APRS-IS from a message confirmed by the matching ACK.
5. **TG 91 listening and DMR controls** — verify visible status, station selection, and passive listening behaviour on supported configurations.
6. **Beacon and local history** — review settings, local message history, and whether controls remain understandable after restarting the app.
7. **Accessibility and language quality** — review touch targets, contrast, Turkish and English text, and report missing or unclear translations.

## Tester procedure

1. Read the release notes and known limitations.
2. Install the stated APK and open the app.
3. Complete the relevant scenario(s) above without publishing credentials, API keys, exact private locations, or personal contact details in public reports.
4. Record: APK version, commit reference, device model, Android version, network type, steps taken, expected result, actual result, and screenshots with sensitive information removed.
5. Submit one issue per reproducible defect through the project’s GitHub issue tracker after the public test release is announced.

## Safety and lawful operation

Nexvo APRS does not make a phone independently transmit RF or satellite traffic. Any RF, satellite, TNC, PTT, iGate, or digipeater test requires compatible equipment, a supported service or lawful amateur-radio path, and the required operator authorization. Use only authorized callsigns and test routes.

## Exit criteria for a public test release

A test release is ready to evaluate when installation is repeatable on supported Android phones and tablets; core map, station, and messaging scenarios are documented; acknowledged and unacknowledged message states are represented accurately; known limitations are published; and incoming reports have a clear next-step status.

## Evidence already available

Physical Android tablet screenshots in the [project README](../README.md#physical-android-tablet-test-evidence) document live TG 91 listening, DMR station selection, MapLibre mapping, and separate APRS messaging states.
