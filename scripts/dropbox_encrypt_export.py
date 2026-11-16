#!/usr/bin/env python3
"""Encrypt the generated ZIP before it is stored as a GitHub Actions artifact."""
from __future__ import annotations

import os
from pathlib import Path
from cryptography.fernet import Fernet


source = Path(os.environ.get("EXPORT_ZIP_PATH", "copiloto-pro-exemplos-corridas.zip"))
destination = Path(os.environ.get("EXPORT_ENCRYPTED_PATH", str(source) + ".enc"))
key = os.environ.get("EXPORT_ENCRYPTION_KEY", "").strip()
if not key:
    raise SystemExit("Missing EXPORT_ENCRYPTION_KEY secret.")
if not source.is_file():
    raise SystemExit(f"ZIP not found: {source}")
try:
    fernet = Fernet(key.encode("ascii"))
except Exception:
    raise SystemExit("EXPORT_ENCRYPTION_KEY is invalid. Generate a Fernet key and save it as a GitHub secret.") from None

# Current example set is about 100 MB; Fernet encrypts the ZIP in one pass.
encrypted = fernet.encrypt(source.read_bytes())
destination.write_bytes(encrypted)
print(f"Encrypted export created: {destination} ({destination.stat().st_size:,} bytes)")
