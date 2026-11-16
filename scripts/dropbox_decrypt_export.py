#!/usr/bin/env python3
"""Decrypt the downloaded Copiloto Pro export artifact locally."""
from __future__ import annotations

import getpass
import sys
from pathlib import Path
from cryptography.fernet import Fernet, InvalidToken


source = Path(sys.argv[1] if len(sys.argv) > 1 else "copiloto-pro-exemplos-corridas.zip.enc")
destination = Path(sys.argv[2] if len(sys.argv) > 2 else "copiloto-pro-exemplos-corridas.zip")
if not source.is_file():
    raise SystemExit(f"Encrypted artifact not found: {source}")
key = getpass.getpass("Encryption key (hidden): ").strip()
try:
    decrypted = Fernet(key.encode("ascii")).decrypt(source.read_bytes())
except (ValueError, InvalidToken):
    raise SystemExit("Could not decrypt: invalid key or damaged encrypted artifact.") from None
destination.write_bytes(decrypted)
print(f"ZIP ready: {destination} ({destination.stat().st_size:,} bytes)")
