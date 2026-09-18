# -*- coding: utf-8 -*-
"""Render the Readium EPUB Reader launcher icons.

Outputs (all RGBA PNG, regenerated deterministically from this script):
  app/src/main/res/mipmap/ic_launcher.png              432 x 432 legacy icon (rounded square)
  app/src/main/res/mipmap/ic_launcher_round.png        432 x 432 legacy round icon
  app/src/main/res/mipmap/ic_launcher_foreground.png   432 x 432 adaptive foreground (white glyph)
  app/src/main/res/mipmap/ic_launcher_monochrome.png   432 x 432 adaptive monochrome (black glyph)

The glyph is an open book: two facing pages with a spine and three text lines on each page, so the
icon reads as "e-book reader" at launcher and plugin-center sizes. The background color matches
values/colors.xml (ic_launcher_background).

Usage: py .python/generate_launcher_icons.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
SIZE = 432
SCALE = 4

BACKGROUND = (0x00, 0x69, 0x5C, 255)
GLYPH_WHITE = (255, 255, 255, 255)
GLYPH_BLACK = (0, 0, 0, 255)
TRANSPARENT = (0, 0, 0, 0)


def draw_glyph(draw: ImageDraw.ImageDraw, center: tuple[float, float], box: float, color: tuple[int, int, int, int]) -> None:
    cx, cy = center
    width = box
    height = box * 0.74
    left, right = cx - width / 2, cx + width / 2
    top, bottom = cy - height / 2, cy + height / 2
    stroke = int(round(box * 0.07))
    gap = box * 0.05

    # Two pages meeting at the spine, slightly tilted at the outer top corners like an open book.
    lift = height * 0.08
    left_page = [(left, top + lift), (cx - gap, top), (cx - gap, bottom), (left, bottom - lift)]
    right_page = [(cx + gap, top), (right, top + lift), (right, bottom - lift), (cx + gap, bottom)]
    for page in (left_page, right_page):
        draw.line(page + [page[0]], fill=color, width=stroke, joint="curve")

    # Spine.
    draw.line([(cx, top - stroke * 0.2), (cx, bottom + stroke * 0.6)], fill=color, width=stroke)

    # Three text lines per page.
    line_stroke = max(1, int(round(stroke * 0.55)))
    inset = box * 0.09
    for index in range(3):
        y = top + height * (0.32 + index * 0.18)
        shorten = box * 0.06 if index == 2 else 0
        draw.line([(left + inset, y + lift * 0.4), (cx - gap - inset + shorten * 0.5, y)], fill=color, width=line_stroke)
        draw.line([(cx + gap + inset, y), (right - inset - shorten, y + lift * 0.4)], fill=color, width=line_stroke)


def render(
    size: int,
    background: tuple[int, int, int, int] | None,
    mask: str | None,
    glyph_color: tuple[int, int, int, int],
    glyph_ratio: float,
) -> Image.Image:
    scaled = size * SCALE
    canvas = Image.new("RGBA", (scaled, scaled), TRANSPARENT)
    draw = ImageDraw.Draw(canvas)
    if background is not None:
        if mask == "circle":
            draw.ellipse((0, 0, scaled - 1, scaled - 1), fill=background)
        elif mask == "rounded":
            draw.rounded_rectangle((0, 0, scaled - 1, scaled - 1), radius=scaled * 0.2, fill=background)
        else:
            draw.rectangle((0, 0, scaled - 1, scaled - 1), fill=background)
    draw_glyph(draw, (scaled / 2, scaled / 2), scaled * glyph_ratio, glyph_color)
    return canvas.resize((size, size), Image.LANCZOS)


def write(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    print(f"Generated {path.relative_to(ROOT).as_posix()} {image.size[0]}x{image.size[1]}")


def main() -> None:
    target = RES / "mipmap"
    write(render(SIZE, BACKGROUND, "rounded", GLYPH_WHITE, 0.64), target / "ic_launcher.png")
    write(render(SIZE, BACKGROUND, "circle", GLYPH_WHITE, 0.58), target / "ic_launcher_round.png")
    # Adaptive layers: the glyph stays inside the 66% safe zone of the 108 dp canvas.
    write(render(SIZE, None, None, GLYPH_WHITE, 0.50), target / "ic_launcher_foreground.png")
    write(render(SIZE, None, None, GLYPH_BLACK, 0.50), target / "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()
