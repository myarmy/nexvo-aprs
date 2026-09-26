# Nexvo APRS

Nexvo APRS is a non-commercial, open-source Android and server project for amateur radio operators. It brings accessible APRS station mapping, messaging, beaconing, telemetry, and supporting digital-radio tools together in one application.

The project is led by Yusuf Uzun (`TA7TEG`) in Türkiye and is under active development. The current Android application supports phones and tablets. Its interface is multilingual: Turkish and English are available today, and the project is designed for community-driven language expansion worldwide.

## Current capabilities

- APRS-IS station reception and map display
- Callsign search, station focus and station details
- APRS messaging with local history
- Position beacon configuration
- APRS.fi integration using a user-provided API key
- Encrypted local storage for credentials and API keys
- Optional Nexvo server API for queued/offline messages
- Android phone and tablet interface
- Multilingual interface, currently Turkish and English, designed for global community translation

## Screenshots

<p align="center">
  <img src="docs/images/station-map.png" alt="Nexvo APRS station map and selected station" width="49%" />
  <img src="docs/images/tg-dmr.png" alt="Nexvo TG and DMR controls" width="49%" />
</p>
<p align="center">
  <img src="docs/images/main-menu.png" alt="Nexvo main menu" width="49%" />
  <img src="docs/images/aprs-messaging.png" alt="Nexvo APRS messaging" width="49%" />
</p>

## Planned work

- Improved map stability, marker clustering, and filters
- Repeater, weather, and emergency-information views
- Background beacon controls and adaptive beaconing
- Community testing and public documentation
- Community-driven translations and global accessibility improvements
- RF TNC, I-Gate, and digipeater research with compatible radio hardware
- Foundation work for future iOS, macOS, and Windows releases
- macOS-based build, testing, signing, and release workflow for planned Apple-platform applications
- Disaster and emergency resilience research using on-device GNSS location
- Morse/CW learning and compatible-device preparation

## Morse/CW learning and compatible-device preparation

Nexvo APRS is preparing a practical Morse/CW assistant that converts ordinary letters and numerals to International Morse Code. The first public scope is an accessible preview and practice interface with a reusable encoder, helping users learn and verify Morse representations.

The project also defines a future transport-adapter boundary for compatible radio or accessory devices. This preparation does not mean that a phone independently transmits RF or satellite traffic. Any transmission requires compatible equipment, a supported network or lawful amateur-radio path, and the appropriate operator authorization.

With the user's permission, Nexvo APRS will use the device's built-in GNSS receiver to obtain and display location coordinates when cellular service is unavailable, subject to satellite visibility and device capability. Sending an emergency message without a cellular network requires compatible satellite or radio hardware and lawful operation.

Hardware-dependent RF features are experimental and require lawful amateur-radio operation, compatible audio/PTT hardware, and real-device testing.

## Project updates and transparent funding

Nexvo APRS is community-led, free, and open source. Project progress, goals, and financial activity are shared through Open Collective.

- [Open Collective page](https://opencollective.com/nexvo-aprs)
