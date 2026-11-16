#!/usr/bin/env python3
"""One-time interactive Dropbox OAuth code exchange.

Run locally. Inputs are hidden where appropriate. Only the refresh token is
printed; add it directly to GitHub Actions secrets and do not share it.
"""
from __future__ import annotations

import base64
import getpass
import json
import urllib.error
import urllib.parse
import urllib.request


def main() -> None:
    app_key = input("Dropbox App key: ").strip()
    app_secret = getpass.getpass("Dropbox App secret (hidden): ").strip()
    auth_code = getpass.getpass("Dropbox authorization code (hidden): ").strip()
    if not app_key or not app_secret or not auth_code:
        raise SystemExit("App key, App secret and authorization code are required.")

    form = urllib.parse.urlencode({
        "code": auth_code,
        "grant_type": "authorization_code",
    }).encode("ascii")
    basic = base64.b64encode(f"{app_key}:{app_secret}".encode()).decode("ascii")
    request = urllib.request.Request(
        "https://api.dropbox.com/oauth2/token",
        data=form,
        headers={
            "Authorization": f"Basic {basic}",
            "Content-Type": "application/x-www-form-urlencoded",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            result = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read(2048).decode("utf-8", errors="replace")
        raise SystemExit(f"Dropbox OAuth exchange failed ({error.code}): {detail}") from None

    refresh_token = result.get("refresh_token")
    if not refresh_token:
        raise SystemExit(
            "Dropbox did not return a refresh token. Re-authorize with "
            "token_access_type=offline and try again."
        )
    print("\nREFRESH TOKEN (copy to GitHub secret DROPBOX_REFRESH_TOKEN):")
    print(refresh_token)
    print("\nDo not share this value or commit it to a repository.")


if __name__ == "__main__":
    main()
