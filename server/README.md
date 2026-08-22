# Nexvo APRS temporary API

Temporary PHP/MySQL backend for `api.selabiz.com`. It is intentionally portable so the same routes and database can later move to a VDS or Raspberry Pi 5.

Public document root contents come from `public/`. On cPanel, keep the real configuration at `/home/<cpanel-user>/nexvo-private/config.php`, outside `public_html`, and never commit it.

Endpoints:

- `GET /health`
- `POST /v1/messages`
- `GET /v1/messages?callsign=TA7TEG`
- `POST /v1/devices/register`

All `/v1/*` routes require `Authorization: Bearer <token>`.
