#!/usr/bin/env python3
"""Export Dropbox examples, OCR images, and upload a private text report."""
from __future__ import annotations

import base64
import json
import os
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath

API = "https://api.dropboxapi.com/2"
CONTENT = "https://content.dropboxapi.com/2"
IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tif", ".tiff"}


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def request_json(url: str, token: str, payload: dict) -> dict:
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=body,
        headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=90) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read(2048).decode("utf-8", errors="replace")
        raise RuntimeError(f"Dropbox API error ({exc.code}): {detail}") from None


def refresh_access_token(app_key: str, app_secret: str, refresh_token: str) -> str:
    credentials = urllib.parse.urlencode({
        "grant_type": "refresh_token",
        "refresh_token": refresh_token,
    }).encode("ascii")
    basic = base64.b64encode(f"{app_key}:{app_secret}".encode()).decode("ascii")
    req = urllib.request.Request(
        "https://api.dropbox.com/oauth2/token",
        data=credentials,
        headers={
            "Authorization": f"Basic {basic}",
            "Content-Type": "application/x-www-form-urlencoded",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            result = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read(2048).decode("utf-8", errors="replace")
        raise RuntimeError(f"Dropbox OAuth error ({exc.code}): {detail}") from None
    return result["access_token"]


def list_files(token: str, folder: str) -> list[dict]:
    result = request_json(
        f"{API}/files/list_folder", token,
        {"path": folder, "recursive": True, "include_deleted": False,
         "include_non_downloadable_files": False},
    )
    entries = list(result.get("entries", []))
    while result.get("has_more"):
        result = request_json(
            f"{API}/files/list_folder/continue", token, {"cursor": result["cursor"]},
        )
        entries.extend(result.get("entries", []))
    return [item for item in entries if item.get(".tag") == "file"]


def download_file(token: str, dropbox_path: str, destination: Path) -> None:
    arg = json.dumps({"path": dropbox_path}, ensure_ascii=True, separators=(",", ":"))
    req = urllib.request.Request(
        f"{CONTENT}/files/download",
        headers={"Authorization": f"Bearer {token}", "Dropbox-API-Arg": arg},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as response, destination.open("wb") as out:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                out.write(chunk)
    except urllib.error.HTTPError as exc:
        detail = exc.read(2048).decode("utf-8", errors="replace")
        raise RuntimeError(f"Download failed for {dropbox_path} ({exc.code}): {detail}") from None


def upload_report(token: str, report_path: Path, dropbox_path: str) -> dict:
    arg = json.dumps(
        {"path": dropbox_path, "mode": "add", "autorename": True, "mute": True},
        ensure_ascii=True, separators=(",", ":"),
    )
    req = urllib.request.Request(
        f"{CONTENT}/files/upload",
        data=report_path.read_bytes(),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/octet-stream",
            "Dropbox-API-Arg": arg,
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read(2048).decode("utf-8", errors="replace")
        raise RuntimeError(f"Dropbox report upload failed ({exc.code}): {detail}") from None


def create_ocr_report(files: list[dict], temp_root: Path, report_path: Path) -> int:
    from PIL import Image, ImageEnhance, ImageOps
    import pytesseract

    image_files = [
        item for item in files
        if PurePosixPath(item.get("path_display") or item.get("name", "")).suffix.lower()
        in IMAGE_EXTENSIONS
    ]
    lines = [
        "RELATÓRIO OCR — EXEMPLOS DE CORRIDAS",
        f"Gerado em: {datetime.now(timezone.utc).astimezone().isoformat(timespec='seconds')}",
        f"Imagens encontradas: {len(image_files)}",
        "Idioma OCR: português + inglês",
        "",
    ]
    processed = 0
    for index, item in enumerate(image_files, start=1):
        display_path = item.get("path_display") or item.get("path_lower") or item.get("name", "")
        relative = PurePosixPath(display_path).relative_to(PurePosixPath(os.environ["DROPBOX_SOURCE_FOLDER"]))
        local_path = temp_root.joinpath(*relative.parts)
        lines.extend(["=" * 78, f"IMAGEM {index}/{len(image_files)}: {relative.as_posix()}", "-" * 78])
        try:
            with Image.open(local_path) as source:
                image = ImageOps.exif_transpose(source).convert("L")
                if image.width < 1800:
                    scale = min(3, max(1, 1800 // max(1, image.width)))
                    if scale > 1:
                        image = image.resize((image.width * scale, image.height * scale))
                image = ImageEnhance.Contrast(image).enhance(1.5)
                text = pytesseract.image_to_string(image, lang="por+eng", config="--psm 6")
            text = text.strip()
            lines.append(text if text else "[Nenhum texto reconhecido]")
            processed += 1
        except Exception as exc:
            lines.append(f"[FALHA NO OCR: {type(exc).__name__}]")
        lines.append("")
        print(f"OCR {index}/{len(image_files)} concluído")
    lines.extend(["=" * 78, f"IMAGENS PROCESSADAS: {processed}/{len(image_files)}", ""])
    report_path.write_text("\n".join(lines), encoding="utf-8")
    return len(image_files)


def main() -> int:
    app_key = require_env("DROPBOX_APP_KEY")
    app_secret = require_env("DROPBOX_APP_SECRET")
    refresh_token = require_env("DROPBOX_REFRESH_TOKEN")
    folder = require_env("DROPBOX_SOURCE_FOLDER")
    zip_path = Path(os.environ.get("EXPORT_ZIP_PATH", "dropbox-corridas.zip"))
    if not folder.startswith("/"):
        raise RuntimeError("DROPBOX_SOURCE_FOLDER must be an absolute Dropbox folder path beginning with /")

    token = refresh_access_token(app_key, app_secret, refresh_token)
    files = list_files(token, folder)
    if not files:
        raise RuntimeError(f"No downloadable files found in Dropbox folder: {folder}")
    total_bytes = sum(int(item.get("size", 0)) for item in files)
    print(f"Found {len(files)} files ({total_bytes:,} bytes). Starting batch download.")

    root = PurePosixPath(folder)
    downloaded = 0
    with tempfile.TemporaryDirectory(prefix="dropbox-export-") as temp:
        temp_root = Path(temp)
        with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED,
                             compresslevel=1, allowZip64=True) as archive:
            for index, item in enumerate(files, start=1):
                display_path = item.get("path_display") or item.get("path_lower")
                if not display_path:
                    continue
                relative = PurePosixPath(display_path).relative_to(root)
                if ".." in relative.parts or relative.is_absolute():
                    raise RuntimeError(f"Unsafe Dropbox path encountered: {display_path}")
                local_path = temp_root.joinpath(*relative.parts)
                local_path.parent.mkdir(parents=True, exist_ok=True)
                download_file(token, item["path_lower"], local_path)
                archive.write(local_path, arcname=relative.as_posix())
                downloaded += 1
                print(f"Downloaded {index}/{len(files)}")

        if downloaded == 0:
            raise RuntimeError("No files were downloaded.")
        report_path = Path(temp) / "ocr-report.txt"
        image_count = create_ocr_report(files, temp_root, report_path)
        if image_count == 0:
            raise RuntimeError("No supported image files were found for OCR.")
        stamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
        report_name = f"OCR_Corridas_{stamp}.txt"
        uploaded = upload_report(token, report_path, f"{folder.rstrip('/')}/{report_name}")
        print(f"OCR_REPORT_UPLOADED={uploaded.get('path_display', report_name)}")
        print(f"OCR_IMAGES_FOUND={image_count}")

    if not zip_path.is_file() or zip_path.stat().st_size == 0:
        raise RuntimeError("ZIP export was not created correctly.")
    print(f"ZIP_READY={zip_path}")
    print(f"FILES_EXPORTED={downloaded}")
    print(f"ZIP_SIZE_BYTES={zip_path.stat().st_size}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
