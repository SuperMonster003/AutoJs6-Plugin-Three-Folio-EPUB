#!/usr/bin/env python3
"""Scale the README screenshots captured by `EpubReaderScreenshotCaptureTest` (roadmap P8).

Usage:
  py .python/generate_screenshots.py <directory-with-device-pngs>

The capture runs on a real device with
  adb shell am instrument -w -r -e screenshots 1 -e class <package>.EpubReaderScreenshotCaptureTest <package>.test/androidx.test.runner.AndroidJUnitRunner
and leaves the PNG files under the plugin's `files/p2-evidence/screenshots/`; pull them with `run-as`
into a directory and point this script at it. Every file is scaled to WIDTH pixels (the README shows
four per row) and written to docs/images/screenshots/ as an optimized PNG. Only the repository's own
fixtures (`docs/fixtures`) may appear in these images (roadmap D22).
"""
import sys
from pathlib import Path

from PIL import Image

WIDTH = 360
NAMES = [
    "reader",
    "table-of-contents",
    "preferences",
    "search",
    "bookmarks",
    "read-aloud",
    "dark-theme",
    "sepia-theme",
    "vertical-ja",
    "fixed-layout",
    "launcher",
    "settings",
]
OUTPUT = Path(__file__).resolve().parent.parent / "docs" / "images" / "screenshots"


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__)
        return 2
    source = Path(argv[1])
    OUTPUT.mkdir(parents=True, exist_ok=True)
    written = 0
    for name in NAMES:
        path = source / f"{name}.png"
        if not path.is_file():
            print(f"missing {path}")
            continue
        with Image.open(path) as image:
            image = image.convert("RGB")
            height = round(image.height * WIDTH / image.width)
            scaled = image.resize((WIDTH, height), Image.LANCZOS)
            target = OUTPUT / f"{name}.png"
            scaled.save(target, format="PNG", optimize=True)
            print(f"{target.name}: {scaled.width}x{scaled.height}, {target.stat().st_size} bytes")
            written += 1
    return 0 if written == len(NAMES) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
