#!/usr/bin/env python3
"""Generate every local Preview image the app ships: wallpapers + clock previews.

Everything this script writes is a *local* file under ``app/src/main/assets/``.
There is no network access and no third-party image URL anywhere in the
pipeline: the artwork is either drawn by the vector code below, composited from
the app's own ``res/drawable`` clock assets, or cropped/graded from the base
renders staged by the designer in ``--staging``.

Outputs
    app/src/main/assets/wallpapers/<section>/<id>.jpg      720x1280 phone wallpapers
    app/src/main/assets/wallpapers/index.json              catalog manifest
    app/src/main/assets/previews/clock/<section>/<id>.png  480x480 clock previews

The manifest is the contract the Java catalogs are checked against
(``tests/test_catalog_ads_unlocks.py``); the ids here must stay in sync with
``catalog/ClockCatalog.java`` and ``catalog/WallpaperCatalog.java``.

Photo sections (Mecca / Medina / Al-Aqsa) use 3 base renders each, emitted
twice (natural grade + night grade) to reach the 6 images per section the app
expects -- the three natural ones are the free images. When a base render is
missing the section falls back to the procedural renderer, so a fresh clone can
always rebuild a complete, coherent asset set.

Arabic text (the "الله" section and the Hijri date on clock previews) is shaped
with HarfBuzz and rasterised with FreeType using the *bundled* Amiri Quran face
in ``assets/fonts/quran_font.ttf``, so the pixels match what the reader shows.

Dependencies (development machine only -- never packaged into the APK):
    pip install pillow uharfbuzz freetype-py fonttools

Usage
    python3 tools/build_local_previews.py             # build everything
    python3 tools/build_local_previews.py --check     # verify existing assets only
    python3 tools/build_local_previews.py --section aqsa
"""
from __future__ import annotations

import argparse
import json
import math
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
RES_DRAWABLE = MAIN / "res/drawable"
ASSETS = MAIN / "assets"
WALL_DIR = ASSETS / "wallpapers"
CLOCK_DIR = ASSETS / "previews/clock"
QURAN_FONT = ASSETS / "fonts/quran_font.ttf"
LATIN_FONT = ASSETS / "open_sans_regular.ttf"
LATIN_BOLD_FONT = ASSETS / "open_sans_bold.ttf"

WALL_W, WALL_H = 720, 1280
CLOCK_SIZE = 480
FREE_PER_SECTION = 3
IMAGES_PER_SECTION = 6

# ---------------------------------------------------------------- sections ---
SECTIONS = [
    {
        "id": "mecca",
        "name": "مساجد مكة",
        "source": "staging",
        "staging": ["mecca_1", "mecca_2", "mecca_3"],
        # the arcade render blows out around the shaded gallery; pull the highlights down
        "retouch": {"mecca_3": {"exposure": -0.16, "highlights": 0.62, "contrast": 1.05}},
        "titles": ["المسجد الحرام", "أروقة الحرم", "مآذن مكة",
                   "المسجد الحرام ليلًا", "أروقة الحرم ليلًا", "مآذن مكة ليلًا"],
    },
    {
        "id": "madinah",
        "name": "مساجد المدينة المنورة",
        "source": "staging",
        "staging": ["madinah_1", "madinah_2", "madinah_3"],
        "titles": ["المسجد النبوي", "مظلات الحرم", "مسجد قباء",
                   "المسجد النبوي ليلًا", "مظلات الحرم ليلًا", "مسجد قباء ليلًا"],
    },
    {
        "id": "aqsa",
        "name": "المسجد الأقصى",
        "source": "staging",
        "staging": ["aqsa_1", "aqsa_2", "aqsa_3"],
        "titles": ["قبة الصخرة", "المسجد القبلي", "ساحة الأقصى",
                   "قبة الصخرة ليلًا", "المسجد القبلي ليلًا", "ساحة الأقصى ليلًا"],
    },
    {
        "id": "mosques",
        "name": "مساجد أخرى",
        "source": "mosques",
        "titles": ["قبة عند الغروب", "مسجد تحت النجوم", "فناء الرخام", "مسجد التل",
                   "مآذن متقابلة", "زخرفة هندسية"],
    },
    {
        "id": "minarets",
        "name": "مآذن",
        "source": "minarets",
        "titles": ["مئذنة وهلال", "صف المآذن", "مآذن النجوم", "هلال الغروب",
                   "مآذن و قبة", "صفّان من المآذن"],
    },
    {
        "id": "allah",
        "name": 'صور مكتوب عليها "الله"',
        "source": "calligraphy",
        "text": "الله",
        "titles": ["الله", "الله على عاجي", "الله في الليل", "الله تحت القوس",
                   "الله بإطار", "الله — نور"],
    },
]

# (sky top, sky bottom, silhouette, accent, ground)
PALETTES = {
    "mecca_dusk": ("#2b1e3f", "#c98a5a", "#150f21", "#f2d9a8", "#3a2c47"),
    "madinah_night": ("#07182e", "#1d4a6b", "#04101d", "#cfe4f2", "#0c2438"),
    "aqsa_gold": ("#12324a", "#e0a45c", "#0a1f2e", "#ffe9c2", "#1b3f58"),
    "olive_dawn": ("#2f3b26", "#b7bd7f", "#1a2214", "#f4f1d6", "#3d4a2c"),
    "indigo": ("#0d1230", "#3a2f74", "#070a1c", "#b7a9ff", "#191f4a"),
    "sand": ("#4c3a24", "#e8c48a", "#2a1f13", "#fff4dd", "#6b5233"),
    "teal": ("#04211f", "#0e6d63", "#01100f", "#9ff0e2", "#083b38"),
    "crimson": ("#2b0e18", "#8c3345", "#180609", "#f5c9b3", "#431721"),
    "emerald": ("#0b3d2e", "#12704f", "#062019", "#f3e9c8", "#0e5540"),
    "ivory": ("#f7f3e6", "#e4dcc4", "#22312a", "#0e7c5b", "#efe8d4"),
}
MOSQUE_PALETTE_ORDER = ["mecca_dusk", "madinah_night", "sand", "olive_dawn", "indigo", "teal"]
MINARET_PALETTE_ORDER = ["aqsa_gold", "indigo", "teal", "crimson", "mecca_dusk", "madinah_night"]
CALLIGRAPHY_STYLES = [  # (palette, treatment, glyph size factor)
    ("emerald", "lattice", 0.50),
    ("ivory", "plain", 0.56),
    ("indigo", "stars", 0.50),
    ("sand", "arch", 0.48),
    ("teal", "frame", 0.58),
    ("emerald", "glow", 0.66),
]

# ----------------------------------------------------------- clock catalogs ---
ANALOG_PALETTES = ["#593524", "#A87E70", "#9E6761", "#464D63", "#8E493A", "#362325", "#54574C",
                   "#A57739", "#C9D4DA", "#4F98DB", "#E8BBD1", "#C9F3A7", "#CDB8E3", "#F0DDF8"]
DIGITAL_PALETTES = ["#0B1116", "#101C2A", "#1A1330", "#05201D", "#241225", "#1D1611",
                    "#0E1B2E", "#141414", "#08242B", "#1C1016", "#0F2417", "#221A0E"]
SMART_BACKGROUNDS = ["bg3", "bg2", "bg1", "bg4", "bg5", "bg1", "pattern_gradient_00",
                     "pattern_gradient_04", "pattern_gradient_06", "pattern_gradient_17",
                     "pattern_gradient_01", "pattern_gradient_10"]
CLOCK_TIME = "10:24"
CLOCK_DATE = "MON 15 MAY"
HIJRI_DATE = "١٥ ذو القعدة"


# ---------------------------------------------------------------- colour misc --
def hex_rgb(value: str):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t: float):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(color, factor: float):
    return tuple(max(0, min(255, int(round(c * factor)))) for c in color)


def palette_of(key: str):
    return tuple(hex_rgb(c) for c in PALETTES[key])


# ----------------------------------------------------------------- drawing ----
def vertical_gradient(size, top, bottom, curve: float = 1.0):
    from PIL import Image

    strip = Image.new("RGB", (1, size[1]))
    px = strip.load()
    height = size[1]
    for y in range(height):
        px[0, y] = mix(top, bottom, (y / max(1, height - 1)) ** curve)
    return strip.resize(size)


def rounded_mask(size, radius):
    from PIL import Image, ImageDraw

    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def blur(img, radius: float):
    from PIL import ImageFilter

    return img.filter(ImageFilter.GaussianBlur(radius))


def vignette(img, amount: float = 0.32, power: float = 2.4):
    """Darken the corners with a radial falloff mask (single light source)."""
    from PIL import Image, ImageChops, ImageDraw

    width, height = img.size
    small = (max(2, width // 6), max(2, height // 6))
    mask = Image.new("L", small, 0)
    draw = ImageDraw.Draw(mask)
    cx, cy = small[0] / 2, small[1] / 2
    max_r = math.hypot(cx, cy)
    for i in range(48, 0, -1):
        radius = max_r * (i / 48)
        draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                     fill=int(255 * amount * (i / 48) ** power))
    mask = mask.resize((width, height))
    veil = Image.merge("RGB", [mask.point(lambda v: 255 - v) for _ in range(3)])
    return ImageChops.multiply(img, veil)


def horizon_glow(img, horizon: int, sigma: int, color, strength: float = 0.6):
    """Gaussian band of light centred on the horizon -- no rectangular edges."""
    from PIL import Image, ImageDraw

    width, height = img.size
    glow = Image.new("RGB", (width, height), (0, 0, 0))
    draw = ImageDraw.Draw(glow)
    sigma = max(1, sigma)
    for y in range(max(0, horizon - sigma * 4), min(height, horizon + sigma * 4)):
        t = math.exp(-((y - horizon) ** 2) / (2 * sigma * sigma))
        if t > 0.01:
            draw.line([(0, y), (width, y)], fill=tuple(int(c * t * strength) for c in color))
    light = glow.convert("L")
    bright = Image.new("RGB", img.size, color)
    from PIL import ImageChops

    lit = Image.composite(bright, img, light)
    return ImageChops.screen(img, lit)


def star_field(img, count: int, seed: int = 3, max_r: float = 2.6, horizon: float = 0.62):
    from PIL import ImageDraw

    draw = ImageDraw.Draw(img)
    rnd = random.Random(seed)
    for _ in range(count):
        x = rnd.randrange(img.size[0])
        y = rnd.randrange(int(img.size[1] * horizon))
        r = rnd.uniform(0.4, max_r)
        tone = 255 if r > 1.6 else rnd.randint(150, 235)
        draw.ellipse([x - r, y - r, x + r, y + r], fill=(tone, tone, min(255, tone + 8)))
    return img


def eight_point_star(draw, cx, cy, radius, fill, outline=None, rotation: float = 0.0):
    def square(phase):
        return [(cx + radius * math.cos(math.radians(rotation + phase + k * 90)),
                 cy + radius * math.sin(math.radians(rotation + phase + k * 90))) for k in range(4)]

    for phase in (0, 45):
        draw.polygon(square(phase), fill=fill, outline=outline)


def lattice_pattern(img, cell: int, color, alpha: int = 24):
    from PIL import Image, ImageDraw

    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    tone = color + (alpha,)
    for row, y in enumerate(range(0, img.size[1] + cell, cell)):
        offset = cell // 2 if row % 2 else 0
        for x in range(0, img.size[0] + cell, cell):
            eight_point_star(draw, x + offset, y, int(cell * 0.33), tone)
    return Image.alpha_composite(img.convert("RGBA"), layer).convert("RGB")


def crescent(img, cx, cy, radius, color, tilt: float = -18.0, depth: float = 0.44):
    """Waxing crescent with a soft halo (the classic Islamic night motif)."""
    from PIL import Image, ImageDraw

    layer = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(layer)
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=255)
    dx = radius * depth * math.cos(math.radians(tilt))
    dy = radius * depth * math.sin(math.radians(tilt))
    inner = radius * 0.86
    draw.ellipse([cx + dx - inner, cy + dy - inner, cx + dx + inner, cy + dy + inner], fill=0)
    halo = blur(layer, radius * 0.55)
    tint = Image.new("RGB", img.size, color)
    glow = Image.new("RGB", img.size, mix(color, (255, 255, 255), 0.35))
    img = Image.composite(glow, img, halo.point(lambda v: v // 5))
    layer = blur(layer, max(1.0, radius / 45))
    return Image.composite(tint, img, layer)


def dome(draw, cx, base_y, width, height, color, outline=None, finial: bool = True):
    radius_x, radius_y = width / 2, height / 2
    draw.ellipse([cx - radius_x, base_y - height, cx + radius_x, base_y - height + 2 * radius_y], fill=color)
    draw.ellipse([cx - radius_x * 0.9, base_y - height * 0.95, cx + radius_x * 0.9, base_y - height * 0.3],
                 fill=color)
    draw.rectangle([cx - radius_x * 0.66, base_y - height * 0.5, cx + radius_x * 0.66, base_y], fill=color)
    if outline:
        draw.arc([cx - radius_x, base_y - height, cx + radius_x, base_y - height + 2 * radius_y],
                 180, 360, fill=outline, width=2)
    if finial:
        draw.line([cx, base_y - height - 4, cx, base_y - height - height * 0.16], fill=outline or color, width=3)


def minaret(draw, cx, base_y, height, width, color, accent=None, balconies: int = 2):
    half = width / 2
    draw.rectangle([cx - half, base_y - height, cx + half, base_y], fill=color)
    cap = height * 0.14
    draw.polygon([(cx - half * 1.3, base_y - height), (cx + half * 1.3, base_y - height),
                  (cx, base_y - height - cap)], fill=color)
    tip = base_y - height - cap
    draw.line([cx, tip, cx, tip - height * 0.055], fill=accent or color, width=2)
    draw.ellipse([cx - width * 0.16, tip - height * 0.085, cx + width * 0.16, tip - height * 0.055],
                 fill=accent or color)
    for i in range(1, balconies + 1):
        y = base_y - height * (0.3 + 0.21 * i)
        draw.rectangle([cx - half * 1.8, y, cx + half * 1.8, y + height * 0.022], fill=color)
        if accent:
            draw.rectangle([cx - half * 0.6, y - height * 0.07, cx + half * 0.6, y - height * 0.02],
                           fill=accent)


def arch_outline(draw, x0, x1, base_y, top_y, color, width: int = 2):
    """Mihrab arch drawn as a stroke (used as a frame, not a filled shape)."""
    mid = (x0 + x1) / 2
    half = (x1 - x0) / 2
    left, right = [], []
    for i in range(41):
        t = i / 40
        y = base_y - (base_y - top_y) * t
        spread = half * (1 - t ** 1.35)
        left.append((mid - spread, y))
        right.append((mid + spread, y))
    draw.line(left, fill=color, width=width, joint="curve")
    draw.line(right[::-1], fill=color, width=width, joint="curve")
    draw.line([left[-1], right[0]], fill=color, width=width)


def pointed_arch(draw, x0, x1, base_y, top_y, color):
    mid = (x0 + x1) / 2
    half = (x1 - x0) / 2
    steps = 24
    left, right = [], []
    for i in range(steps + 1):
        t = i / steps
        y = base_y - (base_y - top_y) * t
        spread = half * (1 - t ** 1.4)
        left.append((mid - spread, y))
        right.append((mid + spread, y))
    draw.polygon(left + right[::-1], fill=color)


# ------------------------------------------------------------- arabic text ----
_FACE_CACHE: dict = {}


def _hb_pair(font_path: Path):
    import uharfbuzz as hb

    key = str(font_path)
    if key not in _FACE_CACHE:
        face = hb.Face(Path(font_path).read_bytes())
        _FACE_CACHE[key] = (hb, face)
    return _FACE_CACHE[key]


def shape_arabic(text: str, font_path: Path, size: int):
    """Shaped runs [(glyphId, xAdvance, xOffset, yOffset)] using HarfBuzz.

    "الله" collapses to Amiri's real ligature instead of four disconnected
    letters. Returns [] when uharfbuzz is missing so the build degrades to a
    text-free panel rather than emitting broken Arabic.
    """
    try:
        hb, face = _hb_pair(font_path)
    except Exception as error:  # pragma: no cover - dev env dependent
        print(f"  ! shaping unavailable ({error}); emitting text-free panel")
        return []
    font = hb.Font(face)
    font.scale = (size, size)
    buffer = hb.Buffer()
    buffer.add_str(text)
    buffer.guess_segment_properties()
    hb.shape(font, buffer, None)  # None -> HarfBuzz's default feature set for the script
    return [(info.codepoint, pos.x_advance, pos.x_offset, pos.y_offset)
            for info, pos in zip(buffer.glyph_infos, buffer.glyph_positions)]


def render_glyph_mask(canvas_size, origin, pixel_size, runs, font_path: Path):
    """Rasterise shaped glyph ids through FreeType into an alpha mask.

    ``origin`` is the pen position *in canvas coordinates* (x, baseline y), so the
    caller can centre the ink without a second crop step.
    """
    from PIL import Image

    try:
        import freetype
    except Exception:
        return None
    face = freetype.Face(str(font_path))
    face.set_pixel_sizes(0, int(pixel_size))
    mask = Image.new("L", canvas_size, 0)
    mask_px = mask.load()
    width, height = canvas_size
    pen_x, baseline = float(origin[0]), float(origin[1])
    for glyph_id, advance, x_offset, y_offset in runs:
        face.load_glyph(glyph_id, freetype.FT_LOAD_RENDER | freetype.FT_LOAD_TARGET_NORMAL)
        slot = face.glyph
        bitmap = slot.bitmap
        glyph_w, glyph_rows = bitmap.width, bitmap.rows
        if not (glyph_w and glyph_rows):
            pen_x += advance
            continue
        origin_x = int(pen_x + x_offset + slot.bitmap_left)
        origin_y = int(baseline - y_offset - slot.bitmap_top)
        buffer = bytes(bitmap.buffer)
        for y in range(glyph_rows):
            target_y = origin_y + y
            if not 0 <= target_y < height:
                continue
            row_start = y * bitmap.pitch
            for x in range(glyph_w):
                value = buffer[row_start + x]
                if not value:
                    continue
                target_x = origin_x + x
                if not 0 <= target_x < width:
                    continue
                if value > mask_px[target_x, target_y]:
                    mask_px[target_x, target_y] = value
        pen_x += advance
    return mask


def draw_shaped(img, text, size_px, color, center, font_path: Path = QURAN_FONT):
    """Centred, correctly shaped Arabic string drawn onto ``img``.

    HarfBuzz does the joining and ligatures, so "الله" becomes Amiri's real
    ligature instead of four disconnected letters. The pen position is nudged
    once using the rendered ink bounding box, which keeps tall ascenders and
    deep descenders optically centred instead of clipped.
    """
    from PIL import Image

    runs = shape_arabic(text, font_path, size_px)
    if not runs:
        return draw_pillow_text(img, text, int(size_px * 0.8), color, center, font_path)
    x, y = float(center[0]), float(center[1])
    mask = None
    for _ in range(2):
        mask = render_glyph_mask(img.size, (int(x - size_px * 1.6), int(y)), size_px, runs, font_path)
        if mask is None:  # freetype unavailable -> last resort
            return draw_pillow_text(img, text, int(size_px * 0.8), color, center, font_path)
        bbox = mask.getbbox()
        if not bbox:
            return img
        left, top, right, bottom = bbox
        x += center[0] - (left + right) / 2
        y += center[1] - (top + bottom) / 2
    return Image.composite(Image.new("RGB", img.size, color), img, mask)


def draw_pillow_text(img, text, size_px, color, center, font_path: Path = LATIN_FONT, tracking: int = 0):
    from PIL import Image, ImageDraw, ImageFont

    font = ImageFont.truetype(str(font_path), size_px)
    draw = ImageDraw.Draw(img)
    if tracking:
        total = sum(draw.textlength(ch, font=font) + tracking for ch in text) - tracking
        x = center[0] - total / 2
        for ch in text:
            draw.text((x, center[1]), ch, font=font, fill=color, anchor="lm")
            x += draw.textlength(ch, font=font) + tracking
    else:
        draw.text((center[0], center[1]), text, font=font, fill=color, anchor="mm")
    return img


# ----------------------------------------------------------- wallpaper craft --
def skyline(palette_key: str, variant: int, seed: int = 11, scale: int = 2):
    from PIL import Image, ImageDraw

    width, height = WALL_W * scale, WALL_H * scale
    top, bottom, dark, accent, ground = palette_of(palette_key)
    img = vertical_gradient((width, height), top, bottom, curve=1.15)

    horizon = int(height * 0.72)
    img = horizon_glow(img, horizon, height // 7, mix(bottom, accent, 0.85), strength=0.55)

    if variant in (2, 5, 6):
        img = star_field(img, 190, seed=seed + variant, horizon=0.55)
    if variant in (2, 4, 5):
        img = crescent(img, int(width * (0.72 if variant != 4 else 0.28)), int(height * 0.2),
                       int(width * (0.1 if variant == 4 else 0.12)), mix(accent, (255, 255, 255), 0.25))

    # far haze ridge, drawn into its own alpha layer so the sky stays clean
    rnd = random.Random(seed + variant * 7)
    ridge = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    fd = ImageDraw.Draw(ridge)
    for _ in range(7):
        cx = rnd.randrange(width)
        dw = rnd.randint(int(width * 0.2), int(width * 0.45))
        fd.ellipse([cx - dw / 2, horizon - dw * 0.42, cx + dw / 2, horizon + dw * 0.25],
                   fill=mix(top, dark, 0.34) + (150,))
    ridge = ridge.rotate(0)
    ridge = Image.alpha_composite(Image.new("RGBA", (width, height), (0, 0, 0, 0)), blur(ridge, height * 0.006))
    img = Image.alpha_composite(img.convert("RGBA"), ridge).convert("RGB")
    draw = ImageDraw.Draw(img)

    base_y = horizon + int(height * 0.035)
    draw.rectangle([0, base_y, width, height], fill=mix(dark, ground, 0.42))
    draw.rectangle([0, base_y, width, base_y + 3], fill=shade(accent, 0.55))

    body_w = int(width * (0.66 if variant != 3 else 0.8))
    body_h = int(height * 0.155)
    cx = width // 2 if variant != 4 else int(width * 0.42)
    draw.rectangle([cx - body_w / 2, base_y - body_h, cx + body_w / 2, base_y], fill=dark)
    dome(draw, cx, base_y - body_h, int(width * 0.3), int(height * 0.16), dark,
         outline=shade(accent, 0.45))
    for sign in (-1, 1):
        dome(draw, cx + sign * body_w * 0.36, base_y - body_h, int(width * 0.115), int(height * 0.07),
             dark, outline=shade(accent, 0.32), finial=False)
    arches = 7 if variant == 3 else 5
    for i in range(arches):
        t = i / (arches - 1)
        ax = cx - body_w * 0.42 + t * body_w * 0.84
        aw = body_w / (arches * 2.5)
        pointed_arch(draw, ax - aw, ax + aw, base_y - 4, base_y - body_h * 0.74, mix(dark, accent, 0.16))
    for sign, factor in ((-1, 1.0), (1, 0.86)):
        mx = cx + sign * (body_w / 2 + width * (0.05 if variant != 6 else 0.12))
        minaret(draw, mx, base_y, int(height * 0.3 * factor), int(width * 0.045), dark,
                accent=shade(accent, 0.72), balconies=3 if variant == 1 else 2)
    if variant in (4, 6):
        img = lattice_pattern(img, int(width * 0.19), accent, alpha=20)
    if variant in (1, 3):
        floor = vertical_gradient((width, int(height * 0.12)), mix(ground, accent, 0.3), mix(dark, ground, 0.5))
        img.paste(blur(floor, height * 0.004), (0, base_y + 4))
    return img.resize((WALL_W, WALL_H), Image.LANCZOS)


def minaret_study(palette_key: str, variant: int, scale: int = 2):
    from PIL import Image, ImageDraw

    width, height = WALL_W * scale, WALL_H * scale
    top, bottom, dark, accent, ground = palette_of(palette_key)
    img = vertical_gradient((width, height), top, bottom, curve=1.3)
    if variant % 2:
        img = star_field(img, 200, seed=variant + 5, max_r=3.0, horizon=0.55)
    img = crescent(img, int(width * (0.26 if variant % 3 == 0 else 0.74)), int(height * 0.14),
                   int(width * 0.105), mix(accent, (255, 255, 255), 0.18))
    draw = ImageDraw.Draw(img)
    base_y = int(height * 0.9)
    draw.rectangle([0, base_y, width, height], fill=mix(dark, ground, 0.3))
    count = 2 + (variant % 3)
    for i in range(count):
        x = int(width * (0.16 + 0.68 * ((i + 0.5) / count)))
        tall = int(height * (0.66 - 0.08 * (i % 3)))
        minaret(draw, x, base_y, tall, int(width * 0.05), dark, accent=shade(accent, 0.82),
                balconies=2 + (i % 2))
    draw.rectangle([width * 0.1, base_y - height * 0.15, width * 0.9, base_y], fill=dark)
    dome(draw, width * 0.5, base_y - height * 0.15, int(width * 0.22), int(height * 0.1), dark,
         outline=shade(accent, 0.42))
    for i in range(5):
        ax = width * 0.18 + i * width * 0.16
        pointed_arch(draw, ax - width * 0.033, ax + width * 0.033, base_y - int(height * 0.012),
                     base_y - height * 0.095, mix(dark, accent, 0.15))
    if variant in (2, 5):
        img = lattice_pattern(img, int(width * 0.13), accent, alpha=15)
    return img.resize((WALL_W, WALL_H), Image.LANCZOS)


def calligraphy_panel(index: int, text: str, scale: float = 1):
    from PIL import Image, ImageDraw

    palette_key, treatment, glyph_factor = CALLIGRAPHY_STYLES[index % len(CALLIGRAPHY_STYLES)]
    top, bottom, dark, accent, ground = palette_of(palette_key)
    light_panel = palette_key == "ivory"
    ink = accent if light_panel else mix(accent, (255, 255, 255), 0.12)
    size = WALL_W
    img = vertical_gradient((size, WALL_H), top, bottom, curve=1.1)

    if treatment == "stars":
        img = star_field(img, 260, seed=31 + index, horizon=1.0, max_r=2.2)
        img = lattice_pattern(img, int(size * 0.2), mix(accent, bottom, 0.55), alpha=10)
    if treatment == "lattice":
        img = lattice_pattern(img, int(size * 0.16), mix(accent, bottom, 0.45), alpha=22)
    if treatment == "glow":
        radius = int(size * 0.62)
        radial = Image.new("L", (size, WALL_H), 0)
        rd = ImageDraw.Draw(radial)
        cx, cy = size / 2, WALL_H * 0.42
        for step in range(40, 0, -1):
            t = step / 40
            rd.ellipse([cx - radius * t, cy - radius * t, cx + radius * t, cy + radius * t],
                       fill=int(120 * (1 - t) ** 1.6))
        img = Image.composite(blur(Image.new("RGB", (size, WALL_H), mix(accent, (255, 255, 255), 0.3)),
                                   size * 0.05), img, blur(radial, size * 0.03))
    if treatment == "arch":
        draw = ImageDraw.Draw(img)
        stroke = mix(accent, bottom, 0.25)
        for inset, weight in ((0.0, 3), (0.055, 1)):
            arch_outline(draw, size * (0.11 + inset), size * (0.89 - inset),
                         WALL_H * (0.88 - inset * 0.4), WALL_H * (0.07 + inset * 0.5), stroke, weight)
        img = lattice_pattern(img, int(size * 0.13), mix(accent, bottom, 0.5), alpha=12)

    img = draw_shaped(img, text or "الله", int(size * glyph_factor), ink, (size / 2, WALL_H * 0.42))

    draw = ImageDraw.Draw(img)
    pad = int(size * 0.055)
    frame = mix(accent, bottom, 0.3)
    draw.rectangle([pad, pad, size - pad, WALL_H - pad], outline=frame, width=2)
    draw.rectangle([pad + 11, pad + 11, size - pad - 11, WALL_H - pad - 11], outline=frame, width=1)
    if treatment in ("plain", "frame"):
        for cx, cy in ((pad, pad), (size - pad, pad), (pad, WALL_H - pad), (size - pad, WALL_H - pad)):
            eight_point_star(draw, cx, cy, int(size * 0.05), mix(accent, bottom, 0.1))
    if treatment in ("glow", "lattice"):
        # a quiet verse-band under the name, drawn as a thin double rule
        draw.line([pad + 34, int(WALL_H * 0.72), size - pad - 34, int(WALL_H * 0.72)], fill=frame, width=1)
    return img


def retouch_photo(img, spec: dict):
    """Small tone fixes (exposure / highlight rolloff / contrast) for a base render."""
    from PIL import ImageEnhance

    out = img
    if "exposure" in spec:
        out = ImageEnhance.Brightness(out).enhance(max(0.4, 1.0 + spec["exposure"]))
    if "highlights" in spec:
        rolloff = float(spec["highlights"])
        knee = 168

        def curve(value: int) -> int:
            over = max(0, value - knee)
            return int(max(0, min(255, value - over * rolloff)))

        out = out.point(curve)
    if "contrast" in spec:
        out = ImageEnhance.Contrast(out).enhance(spec["contrast"])
    return out


def grade_photo(img, night: bool):
    from PIL import ImageEnhance, ImageOps

    out = img.convert("RGB")
    if night:
        gray = out.convert("L").point(lambda v: int(v * 0.88))
        out = ImageOps.colorize(gray, black="#040a18", white="#9dc9ea", mid="#245079")
        out = ImageEnhance.Color(out).enhance(0.7)
        out = ImageEnhance.Contrast(out).enhance(1.07)
    else:
        out = ImageEnhance.Color(out).enhance(1.07)
        out = ImageEnhance.Brightness(out).enhance(1.02)
    return out


def crop_to(img, box_w: int, box_h: int, focus: float = 0.5):
    from PIL import Image

    src = img.convert("RGB")
    sw, sh = src.size
    ratio = box_w / box_h
    if sw / sh > ratio:
        new_w, new_h = int(sh * ratio), sh
    else:
        new_w, new_h = sw, int(sw / ratio)
    left = (sw - new_w) // 2
    top = int((sh - new_h) * focus)
    return src.crop((left, top, left + new_w, top + new_h)).resize((box_w, box_h), Image.LANCZOS)


def render_wallpaper(section: dict, index: int, staging: Path, warnings: list):
    from PIL import Image

    source = section["source"]
    night = index % 2 == 1
    slot = index // 2
    if source == "staging":
        base = staging / f"{section['staging'][slot]}.png"
        if not base.exists():
            base = staging / f"{section['staging'][slot]}.jpg"
        if base.exists():
            img = crop_to(Image.open(base), WALL_W, WALL_H, focus=0.42 if night else 0.5)
            spec = section.get("retouch", {}).get(base.stem)
            if spec:
                img = retouch_photo(img, spec)
            img = grade_photo(img, night)
            if night:
                img = img.rotate(-1.0, resample=Image.BICUBIC, expand=False,
                                 center=(WALL_W / 2, WALL_H / 2))
                img = crop_to(img, WALL_W, WALL_H, focus=0.5)
            return vignette(img, 0.3)
        warnings.append(f"staging render missing: {base.name} -> procedural fallback for {section['id']}")
        source = "mosques"
    if source == "calligraphy":
        return calligraphy_panel(index, section.get("text", "الله"))
    if source == "minarets":
        return minaret_study(MINARET_PALETTE_ORDER[index % 6], index)
    return skyline(MOSQUE_PALETTE_ORDER[index % 6], index + 1)


def save_jpeg(img, target: Path, quality: int = 80):
    target.parent.mkdir(parents=True, exist_ok=True)
    img.save(target, "JPEG", quality=quality, optimize=True)


# ------------------------------------------------------------------- build ----
def build_wallpapers(staging: Path, only: str | None, check_only: bool) -> list[str]:
    warnings: list[str] = []
    manifest = {"version": 1, "freePerSection": FREE_PER_SECTION, "sections": []}
    for section in SECTIONS:
        if only and section["id"] != only:
            continue
        sid = section["id"]
        items = []
        for index in range(IMAGES_PER_SECTION):
            item_id = f"{sid}_{index + 1}"
            target = WALL_DIR / sid / f"{item_id}.jpg"
            items.append({"id": item_id, "file": f"wallpapers/{sid}/{item_id}.jpg",
                          "title": section["titles"][index], "free": index < FREE_PER_SECTION})
            if check_only:
                if not target.exists() or target.stat().st_size < 1000:
                    warnings.append(f"missing wallpaper: {target.relative_to(ROOT)}")
                continue
            save_jpeg(render_wallpaper(section, index, staging, warnings), target)
            print(f"  wallpaper {target.relative_to(ROOT)}  {target.stat().st_size // 1024} KB")
        manifest["sections"].append({"id": sid, "name": section["name"], "items": items})
        if only:
            break
    if not check_only and not only:
        WALL_DIR.mkdir(parents=True, exist_ok=True)
        (WALL_DIR / "index.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"  manifest  {WALL_DIR / 'index.json'}")
    return warnings


def preview_analog(index: int, size: int = CLOCK_SIZE):
    """Poster of the real clock: the app's own dial and hand art, frozen at 10:09.

    Geometry mirrors HandsOverlay.java -- each hand image is as tall as the dial and
    is rotated clockwise around the dial centre -- so the preview and the live
    wallpaper agree.
    """
    from PIL import Image, ImageDraw

    color = hex_rgb(ANALOG_PALETTES[index % len(ANALOG_PALETTES)])
    card = vertical_gradient((size, size), shade(color, 1.25), shade(color, 0.68), curve=1.15)
    img = Image.composite(card, Image.new("RGB", (size, size), (255, 255, 255)),
                          rounded_mask((size, size), int(size * 0.12)))
    draw = ImageDraw.Draw(img)
    dial = int(size * 0.86)
    cx = cy = size / 2

    face = load_drawable(f"clock_bg_{index + 1}.png")
    if face is None:
        draw.ellipse([cx - dial / 2, cy - dial / 2, cx + dial / 2, cy + dial / 2],
                     fill=mix(color, (255, 255, 255), 0.72), outline=shade(color, 0.8), width=3)
    else:
        # contact shadow so the dial lifts off the card
        shadow = Image.new("L", (size, size), 0)
        ImageDraw.Draw(shadow).ellipse([cx - dial * 0.48, cy - dial * 0.44, cx + dial * 0.48,
                                        cy + dial * 0.52], fill=110)
        img = Image.composite(Image.new("RGB", (size, size), shade(color, 0.42)), img, blur(shadow, size * 0.03))
        draw = ImageDraw.Draw(img)
        face = face.convert("RGBA").resize((dial, dial), Image.LANCZOS)
        img.paste(face, (int(cx - dial / 2), int(cy - dial / 2)), face)

    angles = {"hour": 304.5, "minute": 54.0, "second": 216.0}
    for key, prefix, weight in (("hour", "clock_hour", 3), ("minute", "clock_minte", 2),
                                ("second", "clock_second", 1)):
        hand = load_drawable(f"{prefix}_{index + 1}.png")
        angle = angles[key]
        if hand is not None:
            height = dial
            width = max(4, int(hand.size[0] * (dial / max(1, hand.size[1]))))
            hand = hand.convert("RGBA").resize((width, height), Image.LANCZOS)
            hand = hand.rotate(-angle, expand=True, resample=Image.BICUBIC)
            img.paste(hand, (int(cx - hand.size[0] / 2), int(cy - hand.size[1] / 2)), hand)
        else:
            radians = math.radians(angle - 90)
            length = dial * (0.24 if key == "hour" else 0.33 if key == "minute" else 0.38)
            draw.line([(cx - length * 0.18 * math.cos(radians), cy - length * 0.18 * math.sin(radians)),
                       (cx + length * math.cos(radians), cy + length * math.sin(radians))],
                      fill=(206, 74, 62) if key == "second" else shade(color, 0.4), width=weight * 2)
    return img


def preview_digital(index: int, size: int = CLOCK_SIZE):
    from PIL import Image, ImageDraw

    bg = hex_rgb(DIGITAL_PALETTES[index % len(DIGITAL_PALETTES)])
    img = vertical_gradient((size, size), shade(bg, 2.6), bg, curve=1.2)
    img = Image.composite(img, Image.new("RGB", (size, size), (255, 255, 255)),
                          rounded_mask((size, size), int(size * 0.12)))
    draw = ImageDraw.Draw(img)
    accent = mix(bg, (226, 190, 120), 0.6) if index % 3 else mix(bg, (120, 214, 255), 0.62)
    style = index % 3
    pad = int(size * 0.085)
    if style == 0:
        draw.rounded_rectangle([pad, pad, size - pad, size - pad], radius=int(size * 0.075),
                               outline=mix(bg, (255, 255, 255), 0.2), width=2)
        time_y, date_y = size * 0.44, size * 0.63
    elif style == 1:
        draw.rounded_rectangle([pad, pad, size - pad, size * 0.64], radius=int(size * 0.06),
                               fill=mix(bg, (255, 255, 255), 0.07))
        time_y, date_y = size * 0.34, size * 0.52
        draw.line([pad, size * 0.72, size - pad, size * 0.72], fill=accent, width=2)
    else:
        draw.ellipse([size * 0.15, size * 0.15, size * 0.85, size * 0.85],
                     outline=mix(bg, (255, 255, 255), 0.16), width=2)
        time_y, date_y = size * 0.45, size * 0.585
    img = draw_pillow_text(img, CLOCK_TIME, int(size * 0.185), mix((255, 255, 255), accent, 0.3),
                           (size / 2, time_y), LATIN_BOLD_FONT)
    img = draw_pillow_text(img, CLOCK_DATE, int(size * 0.05), mix(bg, (255, 255, 255), 0.55),
                           (size / 2, date_y), LATIN_FONT, tracking=3)
    if style == 2:
        img = draw_shaped(img, HIJRI_DATE, int(size * 0.06), accent, (size / 2, size * 0.71))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([size * 0.32, size * 0.85, size * 0.68, size * 0.895],
                           radius=int(size * 0.02), fill=accent)
    return img


def preview_smart(index: int, size: int = CLOCK_SIZE):
    from PIL import Image, ImageDraw

    background = load_drawable(SMART_BACKGROUNDS[index % len(SMART_BACKGROUNDS)] + ".png")
    if background is not None:
        img = background.convert("RGB").resize((size, size), Image.LANCZOS)
    else:
        img = vertical_gradient((size, size), (18, 58, 74), (6, 19, 26), curve=1.1)
    img = Image.composite(img, Image.new("RGB", (size, size), (255, 255, 255)),
                          rounded_mask((size, size), int(size * 0.12)))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([size * 0.09, size * 0.28, size * 0.91, size * 0.72],
                           radius=int(size * 0.07), fill=(8, 12, 16, 200))
    accent = mix((255, 255, 255), (214, 186, 120), 0.35 + 0.12 * (index % 3))
    img = draw_pillow_text(img, CLOCK_TIME, int(size * 0.15), (250, 250, 250),
                           (size / 2, size * 0.42), LATIN_BOLD_FONT)
    img = draw_pillow_text(img, CLOCK_DATE, int(size * 0.048), mix((255, 255, 255), accent, 0.45),
                           (size / 2, size * 0.54), LATIN_FONT, tracking=3)
    img = draw_shaped(img, HIJRI_DATE, int(size * 0.058), accent, (size / 2, size * 0.64))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([size * 0.35, size * 0.8, size * 0.65, size * 0.845],
                           radius=int(size * 0.02), fill=accent)
    return img


def load_drawable(filename: str):
    from PIL import Image

    path = RES_DRAWABLE / filename
    if not path.exists():
        return None
    return Image.open(path)


def build_clock_previews(check_only: bool) -> list[str]:
    warnings: list[str] = []
    plans = ([("analog", index, preview_analog) for index in range(len(ANALOG_PALETTES))]
             + [("digital", index, preview_digital) for index in range(len(DIGITAL_PALETTES))]
             + [("smart", index, preview_smart) for index in range(len(SMART_BACKGROUNDS))])
    for section, index, renderer in plans:
        item_id = f"{section}_{index + 1:02d}"
        target = CLOCK_DIR / section / f"{item_id}.png"
        if check_only:
            if not target.exists() or target.stat().st_size < 400:
                warnings.append(f"missing clock preview: {target.relative_to(ROOT)}")
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        renderer(index).save(target, "PNG", optimize=True)
        print(f"  preview   {target.relative_to(ROOT)}  {target.stat().st_size // 1024} KB")
    return warnings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staging", default=str(Path.home() / "staging/wallpapers"),
                        help="directory holding the base renders for the photo sections")
    parser.add_argument("--section", default=None, help="rebuild one wallpaper section only")
    parser.add_argument("--check", action="store_true", help="verify existing assets, write nothing")
    parser.add_argument("--skip-clocks", action="store_true")
    parser.add_argument("--skip-wallpapers", action="store_true")
    args = parser.parse_args()

    staging = Path(args.staging).expanduser()
    warnings: list[str] = []
    if not args.skip_wallpapers:
        print(f"[wallpapers] staging={staging} section={args.section or 'all'}")
        warnings += build_wallpapers(staging, args.section, args.check)
    if not args.skip_clocks:
        print("[clock previews]")
        warnings += build_clock_previews(args.check)

    for warning in warnings:
        print("  ! " + warning)
    if not args.check:
        wallpapers = list(WALL_DIR.rglob("*.jpg"))
        previews = list(CLOCK_DIR.rglob("*.png"))
        total = sum(path.stat().st_size for path in wallpapers + previews)
        print(f"\n{len(wallpapers)} wallpapers + {len(previews)} clock previews, "
              f"{total / 1024 / 1024:.1f} MB total, all local (no URLs)")
    return 1 if warnings else 0


if __name__ == "__main__":
    sys.exit(main())
