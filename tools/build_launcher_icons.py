#!/usr/bin/env python3
"""Rasterises the legacy-density launcher icons (API < 26) from the same original brand geometry
used by the adaptive icon and the splash mark: a deep-emerald tile, a warm-gold circle of
remembrance, an eight-point geometric star and a still centre point.

API 26+ devices use res/mipmap-anydpi-v26/ic_launcher.xml (adaptive, vector foreground); these PNGs
exist only so API 23-25 launchers have a bitmap.

Run:  python3 tools/build_launcher_icons.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"

EMERALD = (10, 61, 47, 255)
EMERALD_DEEP = (7, 43, 33, 255)
GOLD = (198, 164, 100, 255)
IVORY = (244, 238, 226, 255)

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SS = 8  # supersampling factor


def draw_mark(draw, size, cx, cy, radius, stroke, colour, accent):
    r = radius
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=colour, width=stroke)
    half = r * 0.70
    draw.rectangle([cx - half, cy - half, cx + half, cy + half], outline=accent, width=stroke)
    d = r * 0.99
    draw.polygon([(cx, cy - d), (cx + d, cy), (cx, cy + d), (cx - d, cy)], outline=accent,
                 width=stroke)
    dot = r * 0.135
    draw.ellipse([cx - dot, cy - dot, cx + dot, cy + dot], fill=accent)


def radial_tile(size, round_icon):
    """Deep emerald tile with a soft centre lift, matching the app's calm palette."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    cx = cy = (size - 1) / 2.0
    maxd = math.hypot(cx, cy)
    for y in range(size):
        for x in range(size):
            t = min(1.0, math.hypot(x - cx, y - cy) / maxd)
            r = int(EMERALD[0] + (EMERALD_DEEP[0] - EMERALD[0]) * t)
            g = int(EMERALD[1] + (EMERALD_DEEP[1] - EMERALD[1]) * t)
            b = int(EMERALD[2] + (EMERALD_DEEP[2] - EMERALD[2]) * t)
            px[x, y] = (r, g, b, 255)
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    if round_icon:
        md.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=255)
    img.putalpha(mask)
    return img


def build(size, round_icon):
    big = size * SS
    img = radial_tile(big, round_icon)
    draw = ImageDraw.Draw(img)
    stroke = max(2, int(big * 0.030))
    draw_mark(draw, big, big / 2.0, big / 2.0, big * 0.285, stroke, IVORY, GOLD)
    return img.resize((size, size), Image.LANCZOS)


def main():
    for density, size in DENSITIES.items():
        out = RES / f"mipmap-{density}"
        out.mkdir(parents=True, exist_ok=True)
        build(size, False).save(out / "ic_launcher.png")
        build(size, True).save(out / "ic_launcher_round.png")
        print(f"mipmap-{density}: {size}px")


if __name__ == "__main__":
    main()
